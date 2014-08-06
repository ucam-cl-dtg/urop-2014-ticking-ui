package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import publicinterfaces.ITestService;
import publicinterfaces.ReportNotFoundException;
import publicinterfaces.ReportResult;
import publicinterfaces.TickNotInDBException;
import publicinterfaces.UserNotInDBException;
import uk.ac.cam.cl.git.api.DuplicateRepoNameException;
import uk.ac.cam.cl.git.api.ForkRequestBean;
import uk.ac.cam.cl.git.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IForkApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.ForkBean;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

import com.google.inject.Inject;

public class ForkApiFacade implements IForkApiFacade {

	Logger log = Logger.getLogger(ConfigurationLoader.class.getName());
	private IDataManager db;
	private ConfigurationLoader<Configuration> config;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public ForkApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config) {
		this.db = db;
		this.config = config;
	}

	@Override
	public Response getFork(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		return Response.ok(fork).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade#forkTick
	 * (javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public Response forkTick(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		if (fork != null) {
			return Response.ok(fork).build();
		}
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(config.getConfig()
				.getGitApiLocation());
		WebInterface proxy = target.proxy(WebInterface.class);
		String repo = null;
		String repoName = Tick.replaceDelimeter(tickId);
		try {
			repo = proxy.forkRepository(new ForkRequestBean(null, crsid,
					repoName, null));
		} catch (DuplicateRepoNameException e) {
			repo = e.getMessage();
		} catch (IOException e) {
			// The repo that was being forked was empty, however, it has still
			// been forked thus continue

		}

		try {
			fork = new Fork(crsid, tickId, repo);
			db.insertFork(fork);
		} catch (DuplicateDataEntryException e) {
			throw new RuntimeException("Schrodinger's repository");
			// The fork simultaneously does and doesn't exist
		}

		// Execution will only reach this point if there are no git errors else
		// IOException is thrown
		return Response.status(Status.CREATED).entity(fork).build();
	}

	@Override
	public Response updateFork(HttpServletRequest request, String tickId,
			ForkBean forkBean) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		Fork fork = db.getFork(Fork.generateForkId(crsid, tickId));
		if (fork != null) {
			if (forkBean.getHumanPass()!=null) {
				fork.setHumanPass(forkBean.getHumanPass());
				if (forkBean.getHumanPass()) {
					ResteasyClient testClient = new ResteasyClientBuilder().build();
					ResteasyWebTarget testTarget = testClient.target(config
							.getConfig().getTestApiLocation());
	
					ITestService testProxy = testTarget.proxy(ITestService.class);
					try {
						testProxy.setTickerResult(crsid, tickId, ReportResult.PASS,
								forkBean.getTickerComments(), forkBean.getCommitId());
					} catch (UserNotInDBException | TickNotInDBException
							| ReportNotFoundException e) {
						return Response.status(Status.NOT_FOUND).entity(e).build();
					}
				}
			}
			if (forkBean.getUnitPass()!=null) {
				fork.setUnitPass(forkBean.getUnitPass());
			}
			if (forkBean.isSignedUp()!=null) {
				fork.setSignedUp(forkBean.isSignedUp());
			}
			db.saveFork(fork);
			return Response.status(Status.CREATED).entity(fork).build();
		}
		return Response.status(Status.NOT_FOUND).build();

	}

}