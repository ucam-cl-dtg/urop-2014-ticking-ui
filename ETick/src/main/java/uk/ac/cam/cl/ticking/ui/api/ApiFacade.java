package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import uk.ac.cam.cl.git.AddRequestBean;
import uk.ac.cam.cl.git.public_interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.auth.RavenManager;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

import com.google.inject.Inject;

@Path("/")
public class ApiFacade {

	private IDataManager db;
	private RavenManager raven;

	@Inject
	public ApiFacade(IDataManager db, RavenManager raven) {
		this.db = db;
		this.raven = raven;
	}

	@GET
	@Path("tick/{tick}")
	@Produces("application/json")
	public Response getTick(@PathParam("tick") String tick) {
		Tick t = db.getTick(tick);
		return Response.ok().entity(t).build();
	}

	@GET
	@Path("tick/{group}")
	@Produces("application/json")
	public Response getTicks(@PathParam("group") String group) {
		List<Tick> grps = db.getGroupTicks(group);
		return Response.ok().entity(grps).build();
	}

	@POST
	@Path("tick/new")
	@Produces("application/json")
	@Consumes("application/json")
	public Response newTick(@Context HttpServletRequest request, Tick tick) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		
		ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target("http://localhost:8080/UROP_GIT");

        WebInterface proxy = target.proxy(WebInterface.class);
        try {
			proxy.addRepository(new AddRequestBean(tick.getName(), crsid));
		} catch (IOException e) {
			return Response.status(400).build();
		}
        
		tick.setAuthor(crsid);
		db.saveTick(tick);
		return Response.ok().build();
	}

}
