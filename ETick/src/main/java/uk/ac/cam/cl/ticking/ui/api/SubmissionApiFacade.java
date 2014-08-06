package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
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
	private ConfigurationLoader<Configuration> config;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public SubmissionApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config) {
		this.db = db;
		this.config = config;
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

		ResteasyClient testClient = new ResteasyClientBuilder().build();
		ResteasyWebTarget testTarget = testClient.target(config.getConfig()
				.getTestApiLocation());

		ITestService testProxy = testTarget.proxy(ITestService.class);
		try {
			testProxy.runNewTest(crsid, tickId, forkRepoName);
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

		ResteasyClient testClient = new ResteasyClientBuilder().build();
		ResteasyWebTarget testTarget = testClient.target(config.getConfig()
				.getTestApiLocation());

		ITestService testProxy = testTarget.proxy(ITestService.class);

		Status status;
		try {
			status = testProxy.pollStatus(crsid, tickId);
		} catch (NoSuchTestException e) {
			return Response.status(404).entity(e).build();
		}
		
		if (status.getProgress()==status.getMaxProgress()) {
			Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
			fork.setTesting(false);
			db.saveFork(fork);
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

		ResteasyClient testClient = new ResteasyClientBuilder().build();
		ResteasyWebTarget testTarget = testClient.target(config.getConfig()
				.getTestApiLocation());

		ITestService testProxy = testTarget.proxy(ITestService.class);

		Report status;
		try {
			status = testProxy.getLastReport(crsid, tickId);
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

		ResteasyClient testClient = new ResteasyClientBuilder().build();
		ResteasyWebTarget testTarget = testClient.target(config.getConfig()
				.getTestApiLocation());

		ITestService testProxy = testTarget.proxy(ITestService.class);

		List<Report> status;
		try {
			status = testProxy.getAllReports(crsid, tickId);
		} catch (UserNotInDBException | TickNotInDBException e) {
			return Response.status(404).entity(e).build();
		}

		return Response.ok(status).build();
	}

}
