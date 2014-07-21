package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import uk.ac.cam.cl.git.AddRequestBean;
import uk.ac.cam.cl.git.ForkRequestBean;
import uk.ac.cam.cl.git.public_interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationFile;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class ApiFacade implements IApiFacade {

	private IDataManager db;
	private ConfigurationFile config;

	@Inject
	public ApiFacade(IDataManager db, ConfigurationFile config) {
		this.db = db;
		this.config = config;
	}

	@Override
	public Response getTick(@PathParam("tick") String tick) {
		Tick t = db.getTick(tick);
		return Response.ok().entity(t).build();
	}

	@Override
	public Response getTicks(@PathParam("group") String group) {
		List<Tick> grps = db.getGroupTicks(group);
		return Response.ok().entity(grps).build();
	}

	@Override
	public Response newTick(@Context HttpServletRequest request, Tick tick)
			throws IOException {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(Strings.GIT);

		WebInterface proxy = target.proxy(WebInterface.class);
		proxy.addRepository(new AddRequestBean(tick.getName(), crsid));

		// Execution will only reach this point if there are no git errors else
		// IOException is thrown
		tick.setAuthor(crsid);
		db.saveTick(tick);
		return Response.ok().build();
	}

	@Override
	public Response forkTick(@Context HttpServletRequest request,
			@PathParam("name") String name) throws IOException {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(Strings.GIT);
		WebInterface proxy = target.proxy(WebInterface.class);
		Response output = null;
		output = proxy.getForkURL(new ForkRequestBean(null, crsid, name, null));

		// Execution will only reach this point if there are no git errors else
		// IOException is thrown
		return output;
	}
	
	@Override
	public Response getUserGroups(@Context HttpServletRequest request) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Group> groups = db.getGroups(db.getUser(crsid));
		Collections.sort(groups);
		return Response.ok(groups).build();
	}
		

}
