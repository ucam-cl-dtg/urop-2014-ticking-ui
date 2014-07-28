package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

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
	 * @return The Tick object with {tick} as it's tid.
	 */
	@GET
	@Path("/{tid}")
	@Produces("application/json")
	public abstract Response getTick(@PathParam("tid") String tid);

	/**
	 * @param group
	 * @return All Tick objects in {group} where {group} is a gid
	 */
	@GET
	@Path("/{gid}")
	@Produces("application/json")
	public abstract Response getTicks(@PathParam("gid") String gid);

	/**
	 * Commits the given tick object to the database, but only after a
	 * repository has been successfully created for it via the GitAPI. If a gid
	 * is given as a queryparam, the Tick will also be added to that group via
	 * the addTick method.
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
			@DefaultValue("") @QueryParam("gid") String gid, Tick tick)
			throws IOException, DuplicateRepoNameException;

	/**
	 * Adds a Tick to a Group given a tid and gid
	 * 
	 * @param request
	 * @param tick
	 * @return the group object that the tick has been added to
	 * @throws IOException
	 * @throws DuplicateRepoNameException
	 * 
	 */
	@POST
	@Path("/{tid}/{gid}")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response addTick(@Context HttpServletRequest request,
			@PathParam("gid") String gid, @PathParam("tid") String tid)
			throws IOException, DuplicateRepoNameException;

	/**
	 * @param request
	 * @param name
	 * @return URL of the new repository to clone
	 * @throws IOException
	 * @throws DuplicateRepoNameException
	 * 
	 */
	@POST
	@Path("/{tid}")
	@Produces("application/json")
	public abstract Response forkTick(@Context HttpServletRequest request,
			@PathParam("tid") String tid) throws IOException;

}
