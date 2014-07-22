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

import uk.ac.cam.cl.git.api.DuplicateRepoNameException;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

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
	 * @param request
	 * @param tick
	 * @return response
	 * @throws IOException
	 * @throws DuplicateRepoNameException 
	 * 
	 */
	@POST
	@Path("/")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response newTick(@Context HttpServletRequest request,
			Tick tick) throws IOException, DuplicateRepoNameException;

	/**
	 * @param request
	 * @param name
	 * @return URL of the new repository to clone
	 * @throws IOException
	 * @throws DuplicateRepoNameException 
	 * 
	 */
	@POST
	@Path("/fork/{tid}")
	@Produces("application/json")
	public abstract Response forkTick(@Context HttpServletRequest request,
			@PathParam("tid") String tid) throws IOException, DuplicateRepoNameException;

}
