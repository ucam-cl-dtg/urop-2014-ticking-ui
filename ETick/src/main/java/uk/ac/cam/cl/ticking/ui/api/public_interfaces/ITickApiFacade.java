package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.git.api.DuplicateRepoNameException;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.ExtensionBean;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.TickBean;

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
	 * Deletes a tick, removing all associations with groups.
	 * 
	 * @param request
	 * @param tickId
	 * @return the success of the request
	 */
	@DELETE
	@Path("/{tickId}")
	@Produces("application/json")
	public abstract Response deleteTick(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId);

	/**
	 * @param group
	 * @return All Tick objects in {group} where {group} is a groupId
	 */
	@GET
	@Path("/list/{groupId}")
	@Produces("application/json")
	public abstract Response getTicks(@Context HttpServletRequest request,
			@PathParam("groupId") String groupId);

	/**
	 * Creates a new tick from a bean, calling the respective APIs to create a
	 * stub repo, a correctness repo, and store the desired checkstyle options
	 * 
	 * @param request
	 * @param tickBean
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
			TickBean tickBean) throws IOException, DuplicateRepoNameException;

	/**
	 * Updates the tick object in the database with the information in the bean.
	 * If the tick object does not exist, the bean is passed to the creation
	 * method.
	 * 
	 * @param request
	 * @param tickId
	 * @param tickBean
	 * @return the created tick object
	 * @throws IOException
	 * @throws DuplicateRepoNameException
	 */
	@PUT
	@Path("/{tickId}")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response updateTick(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId, TickBean tickBean)
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
	public abstract Response addTick(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId,
			@PathParam("groupId") String groupId) throws IOException,
			DuplicateRepoNameException;

	/**
	 * Adds an extension to a tick in the database
	 * 
	 * @param request
	 * @param tickId
	 * @param extensionBean
	 * @return The updated tick object
	 */
	@PUT
	@Path("/{tickId}/extension")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response setExtension(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId, ExtensionBean extensionBean);
	
	/**
	 * Gets the extensions of a tick in the database
	 * 
	 * @param request
	 * @param tickId
	 * @return The extensions
	 */
	@GET
	@Path("/{tickId}/extension")
	@Produces("application/json")
	public abstract Response getExtensions(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId);

	/**
	 * 
	 * @param request
	 * @param tickId
	 * @param commitId
	 * @return the files for the stub repo of the tick object
	 */
	@GET
	@Path("/{tickId}/{commitId}/files")
	@Produces("application/json")
	public abstract Response getAllFiles(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId,
			@PathParam("commitId") String commitId);

}
