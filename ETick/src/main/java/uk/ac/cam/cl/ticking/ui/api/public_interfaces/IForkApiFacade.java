package uk.ac.cam.cl.ticking.ui.api.public_interfaces;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.ForkBean;

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
@Path("/fork")
@Produces("application/json")
public interface IForkApiFacade {

	/**
	 * 
	 * @param request
	 * @param tickId
	 * @return the fork object
	 */
	@GET
	@Path("/{tickId}")
	@Produces("application/json")
	public abstract Response getFork(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId);

	/**
	 * @param request
	 * @param name
	 * @return the created fork object
	 * 
	 */
	@POST
	@Path("/{tickId}")
	@Produces("application/json")
	public abstract Response forkTick(@Context HttpServletRequest request,
			@PathParam("tickId") String tickId);

	/**
	 * 
	 * @param request
	 * @param tickId
	 * @param forkBean
	 * @return
	 */
	@PUT
	@Path("/{crsid}/{tickId}")
	@Produces("application/json")
	@Consumes("application/json")
	public abstract Response updateFork(@Context HttpServletRequest request,
			@PathParam("crsid") String crsid,
			@PathParam("tickId") String tickId, ForkBean forkBean);

}
