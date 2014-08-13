package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import publicinterfaces.ITestService;
import publicinterfaces.ReportNotFoundException;
import publicinterfaces.ReportResult;
import publicinterfaces.TickNotInDBException;
import publicinterfaces.UserNotInDBException;
import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.dtg.teaching.exceptions.SerializableException;
import uk.ac.cam.cl.git.api.DuplicateRepoNameException;
import uk.ac.cam.cl.git.api.FileBean;
import uk.ac.cam.cl.git.api.ForkRequestBean;
import uk.ac.cam.cl.git.api.RepositoryNotFoundException;
import uk.ac.cam.cl.git.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.signups.TickSignups;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IForkApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.ForkBean;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class ForkApiFacade implements IForkApiFacade {

	private static final Logger log = LoggerFactory
			.getLogger(ForkApiFacade.class.getName());

	private IDataManager db;
	// not currently used but could quite possibly be needed in the future, will
	// remove if not
	@SuppressWarnings("unused")
	private ConfigurationLoader<Configuration> config;

	private WebInterface gitServiceProxy;
	private ITestService testServiceProxy;
	private TickSignups tickSignupService;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public ForkApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config,
			ITestService testServiceProxy, WebInterface gitServiceProxy,
			TickSignups tickSignupService) {
		this.db = db;
		this.config = config;
		this.testServiceProxy = testServiceProxy;
		this.gitServiceProxy = gitServiceProxy;
		this.tickSignupService = tickSignupService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getFork(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));

		if (fork == null) {
			log.error("Requested fork " + Fork.generateForkId(crsid, tickId)
					+ " but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		return Response.ok(fork).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response forkTick(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));

		/* Does it already exist? */
		if (fork != null) {
			return Response.ok(fork).build();
		}

		/* Check permissions */
		boolean submitter = false;
		List<String> groupIds = db.getTick(tickId).getGroups();

		for (String groupId : groupIds) {
			List<Role> roles = db.getRoles(groupId, crsid);
			if (roles.contains(Role.SUBMITTER)) {
				submitter = true;
			}
		}

		if (!submitter) {
			log.warn("User " + crsid + " tried to fork "
					+ Fork.generateForkId(crsid, tickId)
					+ " but was denied permission");
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Call the git service */
		String repo = null;
		String repoName = Tick.replaceDelimeter(tickId);

		try {
			repo = gitServiceProxy.forkRepository(new ForkRequestBean(null,
					crsid, repoName, null));

		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName().equals(IOException.class.getName())) {
				log.error("Tried to fork repository for " + tickId,
						s.getCause(), s.getStackTrace());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

			if (s.getClassName().equals(
					DuplicateRepoNameException.class.getName())) {
				/*
				 * The repo has been forked previously and the exception carries
				 * the URI as it's message so just carry on with this
				 */
				log.warn("Tried to fork repository for " + tickId,
						s.getCause(), s.getStackTrace());
				repo = s.getMessage();

			} else {
				log.error("Tried to fork repository for " + tickId,
						s.getCause(), s.getStackTrace());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

		} catch (IOException | DuplicateRepoNameException e) {
			log.error("Tried to fork repository for " + tickId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
			// Due to exception chaining this shouldn't happen
		}

		/* Create and save fork object */
		try {
			fork = new Fork(crsid, tickId, repo);
			db.insertFork(fork);

		} catch (DuplicateDataEntryException e) {
			log.error(
					"Tried to insert fork into database with id "
							+ fork.getForkId(), e);
			throw new RuntimeException("Schrodinger's fork");
			// The fork simultaneously does and doesn't exist
		}

		return Response.status(Status.CREATED).entity(fork).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response markFork(HttpServletRequest request, String crsid,
			String tickId, ForkBean forkBean) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Check permissions */
		boolean marker = false;
		List<String> groupIds = db.getTick(tickId).getGroups();

		for (String groupId : groupIds) {
			List<Role> roles = db.getRoles(groupId, myCrsid);
			if (roles.contains(Role.MARKER)) {
				marker = true;
			}
		}

		if (!marker) {
			log.warn("User " + crsid + " tried to mark "
					+ Fork.generateForkId(crsid, tickId)
					+ " but was denied permission");
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Does the required fork object exist? */
		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		if (fork != null) {
			if (forkBean.getHumanPass() != null) {

				/* Call the test service */
				ReportResult result = forkBean.getHumanPass() ? ReportResult.PASS
						: ReportResult.FAIL;
				try {
					testServiceProxy.setTickerResult(crsid, tickId, result,
							forkBean.getTickerComments(), forkBean
									.getCommitId(), forkBean.getReportDate()
									.getMillis());

				} catch (InternalServerErrorException e) {
					RemoteFailureHandler h = new RemoteFailureHandler();
					SerializableException s = h.readException(e);

					log.error(
							"Tried to set ticker result for "
									+ Fork.generateForkId(crsid, tickId),
							s.getCause());
					return Response.status(Status.NOT_FOUND)
							.entity(Strings.IDEMPOTENTRETRY).build();

				} catch (UserNotInDBException | TickNotInDBException
						| ReportNotFoundException e) {
					log.error(
							"Tried to set ticker result for "
									+ Fork.generateForkId(crsid, tickId), e);
					return Response.status(Status.NOT_FOUND).entity(e).build();
				}

				/* Merge the forkBean and fetched fork */
				fork.setHumanPass(forkBean.getHumanPass());
				fork.setLastTickedBy(crsid);
				fork.setLastTickedOn(DateTime.now());

				/*
				 * If the ticker failed us, require us to resubmit to the unit
				 * tester
				 */
				if (!forkBean.getHumanPass()) {
					fork.setUnitPass(false);
					fork.setSignedUp(false);

					/* Call the tick signup service to set preferred ticker */
					for (String groupId : groupIds) {
						tickSignupService.assignTickerForTickForUser(crsid,
								groupId, tickId, forkBean.getTicker());
					}
				}
			}

			/* Save and return the fork object */
			db.saveFork(fork);

			return Response.status(Status.CREATED).entity(fork).build();
		}
		log.error("Requested fork " + Fork.generateForkId(crsid, tickId)
				+ " for ticking, but it couldn't be found");
		return Response.status(Status.NOT_FOUND).build();

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getAllFiles(HttpServletRequest request, String crsid,
			String tickId, String commitId) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* The user operated on is the caller */
		if (crsid.equals("")) {
			crsid = myCrsid;
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
			log.warn("User " + crsid + " tried to get files for "
					+ Fork.generateForkId(crsid, tickId)
					+ " but was denied permission");
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Call the git service */
		List<FileBean> files;

		try {
			files = gitServiceProxy.getAllFiles(
					crsid + "/" + Tick.replaceDelimeter(tickId), commitId);

		} catch (InternalServerErrorException e) {

			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName().equals(IOException.class.getName())) {
				log.error(
						"Tried to get repository files for "
								+ Fork.generateForkId(crsid, tickId),
						s.getCause(), s.getStackTrace());
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
						.build();
			}

			if (s.getClassName().equals(
					RepositoryNotFoundException.class.getName())) {
				log.error(
						"Tried to get repository files for "
								+ Fork.generateForkId(crsid, tickId),
						s.getCause(), s.getStackTrace());
				return Response.status(Status.NOT_FOUND).entity(e).build();

			} else {
				log.error(
						"Tried to get repository files for "
								+ Fork.generateForkId(crsid, tickId),
						s.getCause(), s.getStackTrace());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(s.getCause()).build();
			}

		} catch (IOException | RepositoryNotFoundException e) {
			log.error(
					"Tried to get repository files for "
							+ Fork.generateForkId(crsid, tickId), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		}

		/*Return the files*/
		return Response.ok(files).build();
	}

}