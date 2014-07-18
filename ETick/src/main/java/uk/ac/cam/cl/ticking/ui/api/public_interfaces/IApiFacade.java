package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.ticks.Tick;

@Path("/")
@Produces("application/json")
public interface IApiFacade {

	/**
	 * @param tick
	 * @return The Tick object with {tick} as it's tid.
	 */
	@GET
	@Path("tick/{tick}")
	@Produces("application/json")
	public abstract Response getTick(@PathParam("tick") String tick);

	/**
	 * @param group
	 * @return All Tick objects in {group} where {group} is a gid
	 */
	@GET
	@Path("tick/{group}")
	@Produces("application/json")
	public abstract Response getTicks(@PathParam("group") String group);

	/**
	 * @param request
	 * @param tick
	 * @return response
	 * @throws IOException
	 * 
	 */
	@POST
	@Path("tick/new")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response newTick(@Context HttpServletRequest request,
			Tick tick) throws IOException;

	/**
	 * @param request
	 * @param name
	 * @return URL of the new repository to clone
	 * @throws IOException
	 * 
	 */
	@GET
	@Path("tick/fork/{name}")
	@Produces("application/json")
	public abstract Response forkTick(@Context HttpServletRequest request,
			@PathParam("name") String name) throws IOException;

	/**
	 * Uses the user's session to determine the crsid and then returns all
	 * groups which that user is a member of.
	 * 
	 * @param request
	 * @return the list of groups that the current user is a member of
	 */
	@GET
	@Path("group/user")
	@Produces("application/json")
	public abstract Response getUserGroups(@Context HttpServletRequest request);

}
