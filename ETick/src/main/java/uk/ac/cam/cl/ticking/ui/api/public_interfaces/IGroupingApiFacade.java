package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;

/**
 * A RESTful interface for requests regarding groupings of Users, Groups and
 * Roles.
 * 
 * @author tl364
 *
 */
@Path("/grouping")
@Produces("application/json")
public interface IGroupingApiFacade {

	/**
	 * Stores the given Grouping object in our database iff the invoking user is
	 * an 'author' for the group specified by the grouping.
	 * 
	 * The User specified by the grouping will be created in the database if
	 * they do not already exist, using LDAP information if possible.
	 * 
	 * @param request
	 * @param grouping
	 * @return the stored grouping object
	 */
	@POST
	@Path("/")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response addGrouping(@Context HttpServletRequest request,
			@QueryParam("crsid") String crsid, @QueryParam("gid") String gid,
			List<Role> roles);

	/**
	 * Deletes a grouping, effectively revoking a user's particular role in a
	 * group
	 * 
	 * @param request
	 * @param grouping
	 * @return the success of the request
	 */
	@DELETE
	@Path("/")
	@Consumes("application/json")
	@Produces("application/json")
	public abstract Response deleteGrouping(
			@Context HttpServletRequest request, Grouping grouping);

}
