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
	 * @return 	200 - success
	 * 			500 - repository creation via UROP-GIT failed
	 * 
	 */
	@POST
	@Path("tick/new")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response newTick(@Context HttpServletRequest request,
			Tick tick);

	/**
	 * @param request
	 * @param name
	 * @return 	200 - success 
	 * 			500 - repository creation via UROP-GIT failed
	 * 
	 */
	@GET
	@Path("tick/fork/{name}")
	@Produces("application/json")
	public abstract Response forkTick(@Context HttpServletRequest request,
			@PathParam("name") String name);

}
