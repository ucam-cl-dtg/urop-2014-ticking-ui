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
import uk.ac.cam.cl.git.api.RepositoryNotFoundException;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

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
	 * @param tid
	 * @return Response
	 * @throws RepositoryNotFoundException 
	 */
	@POST
	@Path("/{tid}/")
	@Produces("application/json")
	public abstract Response submit(@Context HttpServletRequest request, @PathParam("tid") String tid) throws RepositoryNotFoundException;

	/**
	 * @param request
	 * @param tid
	 * @return Status of the submission in the ticker from the test team
	 */
	@GET
	@Path("/{tid}/running")
	@Produces("application/json")
	public abstract Response getStatus(@Context HttpServletRequest request, @PathParam("tid") String tid);

	/**
	 * @param request
	 * @param tid
	 * @return The most recent report in the Test database for the session user's given tick
	 */
	@GET
	@Path("/{tid}/last")
	@Produces("application/json")
	public abstract Response getLast(@Context HttpServletRequest request, @PathParam("tid") String tid);
	
	/**
	 * @param request
	 * @param tid
	 * @return All of the reports for the session user's given tick
	 */
	@GET
	@Path("/{tid}")
	@Produces("application/json")
	public abstract Response getAll(@Context HttpServletRequest request, @PathParam("tid") String tid);
	
}