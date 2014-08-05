package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

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
	 * Deletes a user from the system as well as all associated groupings. If
	 * purge is specified, also removes all content they created, such as groups
	 * and ticks.
	 * 
	 * @param request
	 * @param crsid
	 * @param purge
	 * @return
	 */
	@DELETE
	@Path("/{crsid}")
	@Produces("application/json")
	public abstract Response deleteUser(@Context HttpServletRequest request,
			@PathParam("crsid") String crsid,
			@DefaultValue("false") @QueryParam("purge") boolean purge);

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
	 * @param groupId
	 * @return the list of roles that the current user has for the given group
	 */
	@GET
	@Path("/{groupId}/roles")
	@Produces("application/json")
	public abstract Response getGroupRoles(@Context HttpServletRequest request,
			@PathParam("groupId") String groupId);

	/**
	 * Uses the user's session to determine the crsid and then returns all
	 * groups for which the user has the specified role
	 * 
	 * @param request
	 * @param groupId
	 * @return the list of groups that the current user has the given role for
	 */
	@GET
	@Path("/{stringRole}")
	@Produces("application/json")
	public abstract Response getRoleGroups(@Context HttpServletRequest request,
			@PathParam("stringRole") String stringRole);

	/**
	 * @return All Tick objects authored by the user in the session
	 */
	@GET
	@Path("/ticks")
	@Produces("application/json")
	public abstract Response getMyTicks(@Context HttpServletRequest request);

}
