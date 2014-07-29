package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.git.api.RepositoryNotFoundException;

/**
 * A RESTful interface for requests regarding submissions. Many methods act as
 * abstractions over the testAPI, using the GitAPI to request repository URIS.
 * 
 * @author tl364
 *
 */
@Path("/submission")
@Produces("application/json")
public interface ISubmissionApiFacade {

	/**
	 * @param request
	 * @param tickId
	 * @return Response
	 * @throws RepositoryNotFoundException
	 */
	@POST
	@Path("/{tickId}/")
	@Produces("application/json")
	public abstract Response submit(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId) throws RepositoryNotFoundException;

	/**
	 * @param request
	 * @param tickId
	 * @return Status of the submission in the ticker from the test team
	 */
	@GET
	@Path("/{tickId}/running")
	@Produces("application/json")
	public abstract Response getStatus(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId);

	/**
	 * @param request
	 * @param tickId
	 * @return The most recent report in the Test database for the session
	 *         user's given tick
	 */
	@GET
	@Path("/{tickId}/last")
	@Produces("application/json")
	public abstract Response getLast(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId);

	/**
	 * @param request
	 * @param tickId
	 * @return All of the reports for the session user's given tick
	 */
	@GET
	@Path("/{tickId}")
	@Produces("application/json")
	public abstract Response getAll(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId);

}