package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import uk.ac.cam.cl.git.api.DuplicateRepoNameException;
import uk.ac.cam.cl.git.api.ForkRequestBean;
import uk.ac.cam.cl.git.api.RepoUserRequestBean;
import uk.ac.cam.cl.git.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationFile;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

import com.google.inject.Inject;

public class TickApiFacade implements ITickApiFacade {

	private IDataManager db;
	private ConfigurationFile config;

	@Inject
	public TickApiFacade(IDataManager db, ConfigurationFile config) {
		this.db = db;
		this.config = config;
	}

	@Override
	public Response getTick(String tid) {
		Tick t = db.getTick(tid);
		return Response.ok().entity(t).build();
	}

	@Override
	public Response getTicks(String gid) {
		List<Tick> tks = db.getGroupTicks(gid);
		return Response.ok().entity(tks).build();
	}

	@Override
	public Response newTick(HttpServletRequest request, Tick tick)
			throws IOException, DuplicateRepoNameException {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(config.getGitApiLocation());

		WebInterface proxy = target.proxy(WebInterface.class);
		String repo = proxy.addRepository(new RepoUserRequestBean(tick
				.getName(), crsid));

		// Execution will only reach this point if there are no git errors else
		// IOException is thrown
		tick.setAuthor(crsid);
		tick.setRepo(repo);
		db.saveTick(tick);
		return Response.status(201).entity(tick).build();
	}

	@Override
	public Response forkTick(HttpServletRequest request, String tid)
			throws IOException {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(config.getGitApiLocation());
		WebInterface proxy = target.proxy(WebInterface.class);
		String output;
		try {
			output = proxy.forkRepository(new ForkRequestBean(null, crsid, tid,
					null));
		} catch (DuplicateRepoNameException e) {
			output = e.getMessage()
					+ "(This is not your first fork, however previous data has not been cleared)";
		}

		// Execution will only reach this point if there are no git errors else
		// IOException is thrown
		return Response.status(201).entity(output).build();
	}
}