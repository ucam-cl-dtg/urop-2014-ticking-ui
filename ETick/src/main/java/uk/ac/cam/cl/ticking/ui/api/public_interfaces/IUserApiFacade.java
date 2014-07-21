package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

@Path("/user")
@Produces("application/json")
public interface IUserApiFacade {

	/**
	 * Uses the user's session to determine the crsid and then returns all
	 * groups which that user is a member of.
	 * 
	 * @param request
	 * @return the list of groups that the current user is a member of
	 */
	@GET
	@Path("/groups")
	@Produces("application/json")
	public abstract Response getGroups(@Context HttpServletRequest request);
	
	@GET
	@Path("/{group}/roles")
	@Produces("application/json")
	public abstract Response getGroupRoles(@Context HttpServletRequest request, @PathParam("group") String gid);

}
