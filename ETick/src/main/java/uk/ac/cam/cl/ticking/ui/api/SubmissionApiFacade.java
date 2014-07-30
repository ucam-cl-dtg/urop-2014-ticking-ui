package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import publicinterfaces.AbstractReport;
import publicinterfaces.ITestService;
import publicinterfaces.NoSuchTestException;
import publicinterfaces.Status;
import publicinterfaces.TestIDNotFoundException;
import publicinterfaces.TestStillRunningException;
import publicinterfaces.TickNotInDBException;
import publicinterfaces.UserNotInDBException;
import uk.ac.cam.cl.git.api.RepositoryNotFoundException;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationFile;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

import com.google.inject.Inject;

public class SubmissionApiFacade implements ISubmissionApiFacade {
	
	@SuppressWarnings("unused")
	// not currently used but could quite possibly be needed in the future, will
	// remove if not
	private IDataManager db;
	private ConfigurationLoader<Configuration> config;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public SubmissionApiFacade(IDataManager db, ConfigurationLoader<Configuration> config) {
		this.db = db;
		this.config = config;
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade#submit(javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public Response submit(HttpServletRequest request, String tickId) throws RepositoryNotFoundException {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		
		String repoName = Tick.replaceDelimeter(tickId);
		
		String forkRepoName = crsid+"/"+repoName;
		
		//Execution will not reach this pointunless the repo can be found by the GitAPI
		ResteasyClient testClient = new ResteasyClientBuilder().build();
		ResteasyWebTarget testTarget = testClient.target(config.getConfig().getTestApiLocation());

		ITestService testProxy = testTarget.proxy(ITestService.class);
		try {
			testProxy.runNewTest(crsid, repoName, forkRepoName);
		} catch (IOException e) {
			return Response.status(500).entity(e).build();
		} catch (TestStillRunningException e) {
			return Response.status(503).entity(e).build();
		} catch (TestIDNotFoundException e) {
			return Response.status(404).entity(e).build();
		}
		
		return Response.status(201).build();
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade#getStatus(javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public Response getStatus(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		
		String repoName = Tick.replaceDelimeter(tickId);
		
		ResteasyClient testClient = new ResteasyClientBuilder().build();
		ResteasyWebTarget testTarget = testClient.target(config.getConfig().getTestApiLocation());

		ITestService testProxy = testTarget.proxy(ITestService.class);
		
		Status status;
		try {
			status = testProxy.pollStatus(crsid,repoName);
		} catch (NoSuchTestException e) {
			return Response.status(404).entity(e).build();
		}
		
		return Response.ok(status).build();
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade#getLast(javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public Response getLast(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		
		String repoName = Tick.replaceDelimeter(tickId);
		
		ResteasyClient testClient = new ResteasyClientBuilder().build();
		ResteasyWebTarget testTarget = testClient.target(config.getConfig().getTestApiLocation());

		ITestService testProxy = testTarget.proxy(ITestService.class);
		
		AbstractReport status;
		try {
			status = testProxy.getLastReport(crsid,repoName);
		} catch (UserNotInDBException | TickNotInDBException e) {
			return Response.status(404).entity(e).build();
		}
		
		return Response.ok(status).build();
	}

	/* (non-Javadoc)
	 * @see uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade#getAll(javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public Response getAll(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		
		String repoName = Tick.replaceDelimeter(tickId);
		
		ResteasyClient testClient = new ResteasyClientBuilder().build();
		ResteasyWebTarget testTarget = testClient.target(config.getConfig().getTestApiLocation());

		ITestService testProxy = testTarget.proxy(ITestService.class);
		
		List<AbstractReport> status;
		try {
			status = testProxy.getAllReports(crsid,repoName);
		} catch (UserNotInDBException | TickNotInDBException e) {
			return Response.status(404).entity(e).build();
		}
		
		return Response.ok(status).build();
	}

}
