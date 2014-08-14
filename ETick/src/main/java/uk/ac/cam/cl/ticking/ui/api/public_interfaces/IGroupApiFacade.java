package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.GroupBean;

/**
 * A RESTful interface for requests regarding groups.
 * 
 * @author tl364
 *
 */
@Path("/group")
@Produces("application/json")
public interface IGroupApiFacade {

	/**
	 * 
	 * @param groupId
	 * @param byName
	 * @return the group object, found either by groupId or name as specified
	 */
	@GET
	@Path("/{groupId}")
	@Produces("application/json")
	public abstract Response getGroup(@PathParam("groupId") String groupId);
	
	/**
	 * 
	 * @param groupId
	 * @param byName
	 * @return the group object, found either by groupId or name as specified
	 */
	@GET
	@Path("/{groupId}")
	@Produces("text/plain")
	public abstract Response exportGroup(@PathParam("groupId") String groupId);

	/**
	 * Deletes the specified group, clearing up any dangling associations with
	 * groupings and ticks
	 * 
	 * @param request
	 * @param groupId
	 * @return the success of the request
	 */
	@DELETE
	@Path("/{groupId}")
	@Produces("application/json")
	public abstract Response deleteGroup(@Context HttpServletRequest request,
			@PathParam("groupId") String groupId);

	/**
	 * 
	 * @param groupId
	 * @return all users in the group, given by groupId
	 */
	@GET
	@Path("/{groupId}/users")
	@Produces("application/json")
	public abstract Response getUsers(@PathParam("groupId") String groupId);

	/**
	 * 
	 * @return all groups
	 */
	@GET
	@Path("/")
	@Produces("application/json")
	public abstract Response getGroups();

	/**
	 * Creates a new group object from the bean and commits it to the database.
	 * 
	 * @param request
	 * @param roles
	 * @param groupBean
	 * @return the group object committed
	 */
	@POST
	@Path("/")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response addGroup(@Context HttpServletRequest request,
			@DefaultValue("AUTHOR") @QueryParam("roles") List<String> roles,
			GroupBean groupBean);

	/**
	 * Updates the group object with the given groupId with the data in the
	 * bean. If there is no group object with the groupId, the bean is passed to
	 * the create method.
	 * 
	 * @param request
	 * @param groupId
	 * @param groupBean
	 * @return the group object updated
	 */
	@PUT
	@Path("/{groupId}")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response updateGroup(@Context HttpServletRequest request,
			@PathParam("groupId") String groupId, GroupBean groupBean);

	/**
	 * 
	 * @param request
	 * @param groupId
	 * @param members
	 * @param ticks
	 * @param groupBean
	 * @return the cloned group object
	 */
	@POST
	@Path("/{groupId}")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response cloneGroup(@Context HttpServletRequest request,
			@PathParam("groupId") String groupId,
			@DefaultValue("true") @QueryParam("members") boolean members,
			@DefaultValue("true") @QueryParam("ticks") boolean ticks,
			GroupBean groupBean);

}