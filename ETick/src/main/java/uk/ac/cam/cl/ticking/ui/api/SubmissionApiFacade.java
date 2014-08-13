package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import publicinterfaces.ITestService;
import publicinterfaces.NoCommitsToRepoException;
import publicinterfaces.NoSuchTestException;
import publicinterfaces.Report;
import publicinterfaces.ReportResult;
import publicinterfaces.Status;
import publicinterfaces.TestIDNotFoundException;
import publicinterfaces.TestStillRunningException;
import publicinterfaces.TickNotInDBException;
import publicinterfaces.UserNotInDBException;
import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.dtg.teaching.exceptions.SerializableException;
import uk.ac.cam.cl.git.api.RepositoryNotFoundException;
import uk.ac.cam.cl.ticking.signups.TickSignups;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class SubmissionApiFacade implements ISubmissionApiFacade {

	private static final Logger log = LoggerFactory
			.getLogger(SubmissionApiFacade.class.getName());

	private IDataManager db;
	// not currently used but could quite possibly be needed in the future, will
	// remove if not
	@SuppressWarnings("unused")
	private ConfigurationLoader<Configuration> config;

	private ITestService testServiceProxy;
	private TickSignups tickSignupService;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public SubmissionApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config,
			ITestService testServiceProxy, TickSignups tickSignupService) {
		this.db = db;
		this.config = config;
		this.testServiceProxy = testServiceProxy;
		this.tickSignupService = tickSignupService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response submit(HttpServletRequest request, String tickId)
			throws RepositoryNotFoundException {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the fork object, returning if not found */
		Tick tick = db.getTick(tickId);
		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		if (fork == null) {
			log.error("Requested fork " + Fork.generateForkId(crsid, tickId)
					+ " to submission, but it couldn't be found");
			return Response.status(404).build();
		}

		/*
		 * Check if the deadline has passed, replace the deadline if an
		 * extension exists for this user
		 */
		DateTime extension = tick.getExtensions().get(crsid);

		if (extension != null) {
			tick.setDeadline(extension);
		}

		if (tick.getDeadline() != null && tick.getDeadline().isBeforeNow()) {
			return Response.status(400).entity(Strings.DEADLINE).build();
		}

		/* Parse id for the git service via the test service */
		String repoName = Tick.replaceDelimeter(tickId);

		String forkRepoName = crsid + "/" + repoName;

		/* Call the git service */
		try {
			testServiceProxy.runNewTest(crsid, tickId, forkRepoName);

		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName().equals(IOException.class.getName())) {
				log.error("Tried to start new test on " + repoName, e);
				return Response.status(500).entity(e).build();
			}

			if (s.getClassName().equals(
					TestStillRunningException.class.getName())) {
				log.error("Tried to start new test on " + repoName, e);
				return Response.status(503).entity(e).build();

			}

			if (s.getClassName()
					.equals(TestIDNotFoundException.class.getName())) {
				log.error("Tried to start new test on " + repoName, e);
				return Response.status(404).entity(e).build();

			}

			if (s.getClassName().equals(
					NoCommitsToRepoException.class.getName())) {
				log.error("Tried to start new test on " + repoName, e);
				return Response.status(400).entity(e).build();

			} else {
				log.error("Tried to start new test on " + repoName, e);
				return Response.status(500).entity(e).build();
			}

		} catch (IOException | TestStillRunningException
				| TestIDNotFoundException | NoCommitsToRepoException e) {
			log.error("Tried to start new test on " + repoName, e);
			return Response.status(500).entity(e).build();
		}

		/* The fork is not submitted for testing */
		fork.setTesting(true);

		/* Save and return the fork object */
		db.saveFork(fork);
		return Response.status(201).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getStatus(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the fork object, returning if not found */
		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		if (fork == null) {
			log.error("Requested fork " + Fork.generateForkId(crsid, tickId)
					+ " to get testing status, but it couldn't be found");
			return Response.status(404).build();
		}

		/* Call the test service */
		Status status;
		try {
			status = testServiceProxy.pollStatus(crsid, tickId);
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);
			log.error("Tried getting the running status of " + crsid + " "
					+ tickId, s.getCause());
			return Response.status(404).entity(e).build();
		} catch (NoSuchTestException e) {
			log.error("Tried getting the running status of " + crsid + " "
					+ tickId, e);
			return Response.status(500).entity(e).build();
		}

		/* Check if the tests are complete */
		if (status.getProgress() == status.getMaxProgress()) {

			/* The fork has finished testing and the report is available */
			fork.setTesting(false);
			fork.setReportAvailable(true);

			/*
			 * Get all of the groups this tick is in and set whether the user
			 * can sign up for ticking in them or not
			 */
			List<String> groupIds = db.getTick(tickId).getGroups();

			boolean unitPass = status.getInfo().equals("PASS");
			if (unitPass) {
				for (String groupId : groupIds) {
					tickSignupService.allowSignup(crsid, groupId, tickId);
				}
			} else {
				for (String groupId : groupIds) {
					tickSignupService.disallowSignup(crsid, groupId, tickId);
				}
			}

			/* Set whether the fork passed and save it */
			fork.setUnitPass(unitPass);
			db.saveFork(fork);
		}

		/* Return the status object */
		return Response.ok(status).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getLast(HttpServletRequest request, String tickId,
			String crsid) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* The user operated on is the caller */
		if (crsid.equals("")) {
			crsid = myCrsid;
		}

		/* Get the fork object, returning if not found */
		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		if (fork == null) {
			log.error("Requested fork " + Fork.generateForkId(crsid, tickId)
					+ " to get testing status, but it couldn't be found");
			return Response.status(404).build();
		}

		/* Check permissions */
		boolean marker = false;
		List<String> groupIds = db.getTick(tickId).getGroups();
		for (String groupId : groupIds) {
			List<Role> roles = db.getRoles(groupId, myCrsid);
			if (roles.contains(Role.MARKER)) {
				marker = true;
			}
		}
		if (!(marker || myCrsid.equals(db.getFork(
				Fork.generateForkId(crsid, tickId)).getAuthor()))) {
			log.warn("User " + crsid + " tried to access fork "
					+ Fork.generateForkId(crsid, tickId)
					+ " but was denied permission");
			return Response.status(401).entity(Strings.INVALIDROLE).build();
		}

		/* Call the test service */
		Report status;
		try {
			status = testServiceProxy.getLastReport(crsid, tickId);
			
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);
			log.error("Tried getting last report for " + crsid + " " + tickId,
					s.getCause());
			return Response.status(404).entity(e).build();
			
		} catch (UserNotInDBException | TickNotInDBException e) {
			log.error("Tried getting last report for " + crsid + " " + tickId,
					e);
			return Response.status(404).entity(e).build();
		}

		/*
		 * Set the fork's unit pass depending on the status of the report and
		 * save it
		 */
		fork.setUnitPass(status.getTestResult().equals(ReportResult.PASS));
		db.saveFork(fork);

		/* Return the report */
		return Response.ok(status).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getAll(HttpServletRequest request, String tickId,
			String crsid) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		
		/* The user operated on is the caller */
		if (crsid.equals("")) {
			crsid = myCrsid;
		}
		
		/* Get the fork object, returning if not found */
		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		if (fork == null) {
			log.error("Requested fork " + Fork.generateForkId(crsid, tickId)
					+ " to get testing status, but it couldn't be found");
			return Response.status(404).build();
		}

		/* Check permissions */
		boolean marker = false;
		
		List<String> groupIds = db.getTick(tickId).getGroups();
		for (String groupId : groupIds) {
			List<Role> roles = db.getRoles(groupId, myCrsid);
			if (roles.contains(Role.MARKER)) {
				marker = true;
			}
		}
		
		if (!(marker || myCrsid.equals(db.getFork(Fork.generateForkId(crsid,
				tickId))))) {
			log.warn("User " + crsid + " tried to access fork "
					+ Fork.generateForkId(crsid, tickId)
					+ " but was denied permission");
			return Response.status(401).entity(Strings.INVALIDROLE).build();
		}

		/* Call the test service */
		List<Report> status;
		try {
			status = testServiceProxy.getAllReports(crsid, tickId);
			
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);
			log.error("Tried getting all reports for " + crsid + " " + tickId,
					s.getCause());
			return Response.status(404).entity(e).build();
			
		} catch (UserNotInDBException | TickNotInDBException e) {
			log.error("Tried getting all reports for " + crsid + " " + tickId,
					e);
			return Response.status(404).entity(e).build();
		}

		/*Return all of the reports*/
		return Response.ok(status).build();
	}

}
