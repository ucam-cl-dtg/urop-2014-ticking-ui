package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import uk.ac.cam.cl.git.AddRequestBean;
import uk.ac.cam.cl.git.ForkRequestBean;
import uk.ac.cam.cl.git.public_interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IApiFacade;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

import com.google.inject.Inject;

@Path("/")
public class ApiFacade implements IApiFacade {

	private IDataManager db;

	@Inject
	public ApiFacade(IDataManager db) {
		this.db = db;
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
	public Response newTick(@Context HttpServletRequest request, Tick tick) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client
				.target("http://localhost:8080/UROP_GIT/rest");

		WebInterface proxy = target.proxy(WebInterface.class);
		try {
			proxy.addRepository(new AddRequestBean(tick.getName(), crsid));
		} catch (IOException e) {
			//TODO change this to something less bad
			return Response.status(500).build();
		}

		tick.setAuthor(crsid);
		db.saveTick(tick);
		return Response.ok().build();
	}

	@Override
	public Response forkTick(@Context HttpServletRequest request,
			@PathParam("name") String name) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client
				.target("http://localhost:8080/UROP_GIT/rest");

		WebInterface proxy = target.proxy(WebInterface.class);
		Response output = null;
		try {
			output = proxy.getForkURL(new ForkRequestBean(null, crsid, name,
					null));
		} catch (Exception e) {
			//TODO change this to something less bad
			return Response.status(500).build();
		}
		return output;
	}

}
