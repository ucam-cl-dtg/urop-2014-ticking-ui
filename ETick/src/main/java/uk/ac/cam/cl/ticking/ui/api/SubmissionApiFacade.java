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

		Tick tick = db.getTick(tickId);
		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		if (fork == null) {
			return Response.status(404).build();
		}

		DateTime extension = tick.getExtensions().get(crsid);
		if (extension != null) {
			tick.setDeadline(extension);
		}

		if (tick.getDeadline() != null && tick.getDeadline().isBeforeNow()) {
			return Response.status(400).entity(Strings.DEADLINE).build();
		}

		String repoName = Tick.replaceDelimeter(tickId);

		String forkRepoName = crsid + "/" + repoName;

		try {
			testServiceProxy.runNewTest(crsid, tickId, forkRepoName);
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);
			log.error("Tried to start new test on " + repoName, s.getCause());
			return Response.status(500).entity(e).build();
		} catch (IOException e) {
			log.error("Tried to start new test on " + repoName, e);
			return Response.status(500).entity(e).build();
		} catch (TestStillRunningException e) {
			log.error("Tried to start new test on " + repoName, e);
			return Response.status(503).entity(e).build();
		} catch (TestIDNotFoundException e) {
			log.error("Tried to start new test on " + repoName, e);
			return Response.status(404).entity(e).build();
		} catch (NoCommitsToRepoException e) {
			log.error("Tried to start new test on " + repoName, e);
			return Response.status(400).entity(e).build();
		}

		fork.setTesting(true);
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
			return Response.status(404).entity(e).build();
		}

		if (status.getProgress() == status.getMaxProgress()) {
			Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
			fork.setTesting(false);
			fork.setReportAvailable(true);

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
			fork.setUnitPass(unitPass);
			db.saveFork(fork);
		}

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
		if (crsid.equals("")) {
			crsid = myCrsid;
		}
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
			return Response.status(401).entity(Strings.INVALIDROLE).build();
		}
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

		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		fork.setUnitPass(status.getTestResult().equals(ReportResult.PASS));
		db.saveFork(fork);

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

		if (crsid.equals("")) {
			crsid = myCrsid;
		}
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
			return Response.status(401).entity(Strings.INVALIDROLE).build();
		}

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

		return Response.ok(status).build();
	}

}
