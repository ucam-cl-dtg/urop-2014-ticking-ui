package uk.ac.cam.cl.ticking.ui.api.facades;

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
import uk.ac.cam.cl.git.api.IllegalCharacterException;
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
import uk.ac.cam.cl.ticking.ui.util.PermissionsManager;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class ForkApiFacade implements IForkApiFacade {

	private static final Logger log = LoggerFactory
			.getLogger(ForkApiFacade.class.getName());

	private IDataManager db;
	private ConfigurationLoader<Configuration> config;

	private WebInterface gitServiceProxy;
	private ITestService testServiceProxy;
	private TickSignups tickSignupService;

	private PermissionsManager permissions;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public ForkApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config,
			ITestService testServiceProxy, WebInterface gitServiceProxy,
			TickSignups tickSignupService, PermissionsManager permissions) {
		this.db = db;
		this.config = config;
		this.testServiceProxy = testServiceProxy;
		this.gitServiceProxy = gitServiceProxy;
		this.tickSignupService = tickSignupService;
		this.permissions = permissions;
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
			// Currently happens every time a student clicks on an un-forked
			// tick
			// log.error("Requested fork " + Fork.generateForkId(crsid, tickId)
			// + " but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		boolean signedUp = fork.isSignedUp();
		boolean serviceSignedUp = tickSignupService.studentHasBookingForTick(
				crsid, tickId);
		if (signedUp == serviceSignedUp) {
			return Response.ok(fork).build();
		} else {
			log.warn("User " + crsid + " requested fork for tick " + tickId
					+ " and had to update signup consistency");
			fork.setSignedUp(serviceSignedUp);
			db.saveFork(fork);
			return Response.status(Status.CREATED).entity(fork).build();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response forkTick(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		Tick tick = db.getTick(tickId);

		/* Return if the tick doesn't exist */
		if (tick == null) {
			log.error("Requested tick " + tickId
					+ " for forking, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Does it already exist? */
		if (fork != null) {
			boolean signedUp = fork.isSignedUp();
			boolean serviceSignedUp = tickSignupService
					.studentHasBookingForTick(crsid, tickId);
			if (signedUp == serviceSignedUp) {
				return Response.ok(fork).build();
			} else {
				log.warn("User " + crsid + " requested fork for tick " + tickId
						+ " and had to update signup consistency");
				fork.setSignedUp(serviceSignedUp);
				db.saveFork(fork);
				return Response.status(Status.CREATED).entity(fork).build();
			}

		}

		/* Check permissions */

		if (!permissions.tickRole(crsid, tickId, Role.SUBMITTER)) {
			log.warn("User " + crsid + " tried to fork "
					+ Fork.generateForkId(crsid, tickId)
					+ " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Has the deadline passed? */
		DateTime extension = tick.getExtensions().get(crsid);

		if (extension != null) {
			tick.setDeadline(extension);
		}

		if (tick.getDeadline() != null && tick.getDeadline().isBeforeNow()) {
			return Response.status(Status.NOT_FOUND).entity(Strings.DEADLINE)
					.build();
		}

		/* Create and save fork object */
		try {
			fork = new Fork(crsid, tickId, "");
			fork.setForking(true);
			db.insertFork(fork);

		} catch (DuplicateDataEntryException e) {
			log.error(
					"User " + crsid
							+ " failed to insert fork into database with id "
							+ fork.getForkId(), e);
			throw new RuntimeException("Schrodinger's fork");
			// The fork simultaneously does and doesn't exist
		}

		/* Call the git service */
		String repo = null;
		String repoName = Tick.replaceDelimeter(tickId);

		try {
			repo = gitServiceProxy.forkRepository(config.getConfig()
					.getSecurityToken(), new ForkRequestBean(null, crsid,
					repoName));

		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName().equals(IOException.class.getName())) {
				log.error("User " + crsid + " failed to fork repository for "
						+ tickId + "\nCause: " + s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

			if (s.getClassName().equals(
					DuplicateRepoNameException.class.getName())) {
				/*
				 * The repo has been forked previously and the exception carries
				 * the URI as its message so just carry on with this
				 */
				log.warn("User " + crsid + " failed to fork repository for "
						+ tickId + "\nCause: " + s.toString());
				repo = s.getMessage();

			}

			if (s.getClassName().equals(
					IllegalCharacterException.class.getName())) {

				log.warn("User " + crsid + " failed to fork repository for "
						+ tickId + "\nCause: " + s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(s.getMessage()).build();

			} else {
				log.error("User " + crsid + " failed to fork repository for "
						+ tickId + "\nCause: " + s.toString());
				return Response.status(Status.BAD_REQUEST)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

		} catch (IOException | DuplicateRepoNameException
				| IllegalCharacterException e) {
			log.error("User " + crsid + " failed to fork repository for "
					+ tickId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
			// Due to exception chaining this shouldn't happen
		}

		fork.setRepo(repo);
		fork.setForking(false);
		db.saveFork(fork);

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
		if (!permissions.tickRole(myCrsid, tickId, Role.MARKER)) {
			log.warn("User " + myCrsid + " tried to mark "
					+ Fork.generateForkId(crsid, tickId)
					+ " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Does the required fork object exist? */
		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		if (fork != null) {
			/* Are we trying to mark the most recent report? */
			if (fork.getLastReport() != null || fork.getLastReport()!=forkBean.getReportDate()) {
				log.error("User " + myCrsid + " tried to mark a report for "
						+ Fork.generateForkId(crsid, tickId)
						+ " with date "+forkBean.getReportDate()+" but the most recent is "+fork.getLastReport());
				return Response.status(Status.FORBIDDEN)
						.entity(Strings.LASTREPORT).build();
			}
			
			if (forkBean.getHumanPass() != null) {

				/* Call the test service */
				ReportResult result = forkBean.getHumanPass() ? ReportResult.PASS
						: ReportResult.FAIL;
				try {
					testServiceProxy.setTickerResult(config.getConfig()
							.getSecurityToken(), crsid, tickId, result,
							forkBean.getTickerComments(), forkBean
									.getCommitId(), forkBean.getReportDate()
									.getMillis());

				} catch (InternalServerErrorException e) {
					RemoteFailureHandler h = new RemoteFailureHandler();
					SerializableException s = h.readException(e);

					log.error("User " + myCrsid
							+ " failed to set ticker result for "
							+ Fork.generateForkId(crsid, tickId) + "\nCause: "
							+ s.toString());

					return Response.status(Status.NOT_FOUND)
							.entity(Strings.MISSING).build();

				} catch (UserNotInDBException | TickNotInDBException
						| ReportNotFoundException e) {
					log.error(
							"User " + myCrsid
									+ " failed to set ticker result for "
									+ Fork.generateForkId(crsid, tickId), e);
					return Response.status(Status.NOT_FOUND).entity(e).build();
				}

				/* Merge the forkBean and fetched fork */
				fork.setHumanPass(forkBean.getHumanPass());
				fork.setLastTickedBy(myCrsid);
				fork.setLastTickedOn(DateTime.now());
				log.info(DateTime.now().toString());

				/*
				 * If the ticker failed us
				 */
				if (!forkBean.getHumanPass()) {
					fork.setSignedUp(false);
					fork.incrementHumanFails();
					/* Call the tick signup service to set preferred ticker */
					List<String> groupIds = db.getTick(tickId).getGroups();
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
		log.error("User " + myCrsid + " requested fork "
				+ Fork.generateForkId(crsid, tickId)
				+ " for ticking, but it couldn't be found");
		return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
				.build();

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

		if (!(permissions.tickRole(myCrsid, tickId, Role.MARKER) || permissions
				.forkCreator(myCrsid, myCrsid, tickId))) {
			log.warn("User " + crsid + " tried to get files for "
					+ Fork.generateForkId(crsid, tickId)
					+ " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Call the git service */
		List<FileBean> files;

		try {
			files = gitServiceProxy.getAllFiles(config.getConfig()
					.getSecurityToken(),
					crsid + "/" + Tick.replaceDelimeter(tickId), commitId);

		} catch (InternalServerErrorException e) {

			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName().equals(IOException.class.getName())) {
				log.error("User " + myCrsid
						+ " failed to get repository files for "
						+ Fork.generateForkId(crsid, tickId) + "\nCause: "
						+ s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

			if (s.getClassName().equals(
					RepositoryNotFoundException.class.getName())) {
				log.error("User " + myCrsid
						+ " failed to get repository files for "
						+ Fork.generateForkId(crsid, tickId) + "\nCause: "
						+ s.toString());
				return Response.status(Status.NOT_FOUND)
						.entity(Strings.MISSING).build();

			} else {
				log.error("User " + myCrsid
						+ " failed to get repository files for "
						+ Fork.generateForkId(crsid, tickId) + "\nCause: "
						+ s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

		} catch (IOException | RepositoryNotFoundException e) {
			log.error(
					"User " + myCrsid + " failed to get repository files for "
							+ Fork.generateForkId(crsid, tickId), e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		}

		/* Return the files */
		return Response.ok(files).build();
	}

}