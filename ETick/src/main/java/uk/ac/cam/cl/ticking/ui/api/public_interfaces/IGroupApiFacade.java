package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/group")
@Produces("application/json")
public interface IGroupApiFacade {
	
	@GET
	@Path("/{gid}/users")
	@Produces("application/json")
	public abstract Response getUsers(@PathParam("gid")String gid);

}