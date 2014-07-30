package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
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

import org.joda.time.DateTime;

import uk.ac.cam.cl.git.api.DuplicateRepoNameException;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

/**
 * A RESTful interface for requests regarding ticks. Many methods act as
 * abstractions over the GitAPI, storing references to their stored repositories
 * in our own Database through Tick objects. Consistency is maintained by
 * ensuring any actions return successfully through the API before committing
 * them to our records.
 * 
 * @author tl364
 *
 */
@Path("/tick")
@Produces("application/json")
public interface ITickApiFacade {

	/**
	 * @param tick
	 * @return The Tick object with {tick} as it's tickId.
	 */
	@GET
	@Path("/{tickId}")
	@Produces("application/json")
	public abstract Response getTick(@PathParam("tickId") String tickId);

	/**
	 * @param group
	 * @return All Tick objects in {group} where {group} is a groupId
	 */
	@GET
	@Path("/list/{groupId}")
	@Produces("application/json")
	public abstract Response getTicks(@PathParam("groupId") String groupId);

	/**
	 * Commits the given tick object to the database, but only after a
	 * repository has been successfully created for it via the GitAPI. If a
	 * groupId is given as a queryparam, the Tick will also be added to that
	 * group via the addTick method.
	 * 
	 * @param request
	 * @param tick
	 * @return the tick object that has been committed to the database
	 * @throws IOException
	 * @throws DuplicateRepoNameException
	 * 
	 */
	@POST
	@Path("/")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response newTick(@Context HttpServletRequest request,
			@DefaultValue("") @QueryParam("groupId") String groupId, Tick tick)
			throws IOException, DuplicateRepoNameException;

	/**
	 * Adds a Tick to a Group given a tickId and groupId
	 * 
	 * @param request
	 * @param tick
	 * @return the group object that the tick has been added to
	 * @throws IOException
	 * @throws DuplicateRepoNameException
	 * 
	 */
	@POST
	@Path("/{tickId}/{groupId}")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response addTick(@Context HttpServletRequest request,
			@PathParam("groupId") String groupId,
			@PathParam("tickId") String tickId) throws IOException,
			DuplicateRepoNameException;

	/**
	 * @param request
	 * @param name
	 * @return URL of the new repository to clone
	 * @throws IOException
	 * @throws DuplicateRepoNameException
	 * 
	 */
	@POST
	@Path("/{tickId}")
	@Produces("application/json")
	public abstract Response forkTick(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId) throws IOException;

	@PUT
	@Path("/{tickId}/deadline")
	@Produces("application/json")
	public abstract Response setDeadline(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId, DateTime date);

}
