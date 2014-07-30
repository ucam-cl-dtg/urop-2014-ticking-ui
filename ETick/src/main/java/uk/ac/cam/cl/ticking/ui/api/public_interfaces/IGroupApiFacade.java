package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.actors.Group;

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
	public abstract Response getGroup(@PathParam("groupId") String groupId,
			@QueryParam("byName") boolean byName);

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
	 * Commits the given group object to the database
	 * 
	 * @param g
	 * @return the group object that has been committed to the database
	 */
	@POST
	@Path("/")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response addGroup(Group group);

}