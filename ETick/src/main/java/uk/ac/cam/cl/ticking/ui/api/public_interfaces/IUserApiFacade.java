package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;

@Path("/user")
@Produces("application/json")
public interface IUserApiFacade {
	
	/**
	 * Returns the User object for the current user
	 * 
	 * @param request
	 * @return the current user
	 */
	@GET
	@Path("/")
	@Produces("application/json")
	public abstract Response getUser(@Context HttpServletRequest request);
	
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

	/**
	 * Uses the user's session to determine the crsid and then returns all roles
	 * which that user has for the specified group
	 * 
	 * @param request
	 * @param gid
	 * @return the list of roles that the current user has for the given group
	 */
	@GET
	@Path("/{gid}/roles")
	@Produces("application/json")
	public abstract Response getGroupRoles(@Context HttpServletRequest request,
			@PathParam("gid") String gid);

}
