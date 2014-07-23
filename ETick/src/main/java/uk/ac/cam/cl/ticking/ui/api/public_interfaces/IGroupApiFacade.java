package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.actors.Group;

@Path("/group")
@Produces("application/json")
public interface IGroupApiFacade {
	
	@GET
	@Path("/{gid}/users")
	@Produces("application/json")
	public abstract Response getUsers(@PathParam("gid")String gid);
	
	@GET
	@Path("/")
	@Produces("application/json")
	public abstract Response getGroups();
	
	@POST
	@Path("/")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response addGroup(Group g);

}