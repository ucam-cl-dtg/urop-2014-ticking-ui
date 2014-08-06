package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.GroupingBean;

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
	@Path("/{groupId}")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response addGroupings(@Context HttpServletRequest request,
			@PathParam("groupId") String groupId, GroupingBean groupingBean);

	/**
	 * Deletes a grouping, effectively revoking a user's particular role in a
	 * group
	 * 
	 * @param request
	 * @param grouping
	 * @return the success of the request
	 */
	@DELETE
	@Path("/{groupId}")
	@Consumes("application/json")
	@Produces("application/json")
	public abstract Response deleteGroupings(
			@Context HttpServletRequest request,
			@PathParam("groupId") String groupId, GroupingBean groupingBean);

}
