package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;

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
import uk.ac.cam.cl.git.api.RepositoryNotFoundException;
import uk.ac.cam.cl.ticking.signups.TickSignups;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class SubmissionApiFacade implements ISubmissionApiFacade {

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade#submit
	 * (javax.servlet.http.HttpServletRequest, java.lang.String)
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
		} catch (IOException e) {
			return Response.status(500).entity(e).build();
		} catch (TestStillRunningException e) {
			return Response.status(503).entity(e).build();
		} catch (TestIDNotFoundException e) {
			return Response.status(404).entity(e).build();
		} catch (NoCommitsToRepoException e) {
			return Response.status(400).entity(e).build();
		}

		fork.setTesting(true);
		db.saveFork(fork);
		return Response.status(201).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade#getStatus
	 * (javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public Response getStatus(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		Status status;
		try {
			status = testServiceProxy.pollStatus(crsid, tickId);
		} catch (NoSuchTestException e) {
			return Response.status(404).entity(e).build();
		}

		if (status.getProgress() == status.getMaxProgress()) {
			Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
			fork.setTesting(false);
			fork.setReportAvailable(true);
			
			List<String> groupIds = db.getTick(tickId).getGroups();
			
			boolean unitPass = status.getInfo().equals(ReportResult.PASS);
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
			//TODO whitelist signup
		}

		return Response.ok(status).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade#getLast
	 * (javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public Response getLast(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		Report status;
		try {
			status = testServiceProxy.getLastReport(crsid, tickId);
		} catch (UserNotInDBException | TickNotInDBException e) {
			return Response.status(404).entity(e).build();
		}

		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		fork.setUnitPass(status.getTestResult().equals(ReportResult.PASS));
		db.saveFork(fork);

		return Response.ok(status).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade#getAll
	 * (javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public Response getAll(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		List<Report> status;
		try {
			status = testServiceProxy.getAllReports(crsid, tickId);
		} catch (UserNotInDBException | TickNotInDBException e) {
			return Response.status(404).entity(e).build();
		}

		return Response.ok(status).build();
	}

}
