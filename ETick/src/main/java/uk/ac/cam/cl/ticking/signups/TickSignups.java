package uk.ac.cam.cl.ticking.signups;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import uk.ac.cam.cl.signups.api.Sheet;
import uk.ac.cam.cl.signups.api.SheetInfo;
import uk.ac.cam.cl.signups.api.Slot;
import uk.ac.cam.cl.signups.api.beans.CreateColumnBean;
import uk.ac.cam.cl.signups.api.beans.GroupSheetBean;
import uk.ac.cam.cl.signups.api.beans.PermissionsBean;
import uk.ac.cam.cl.signups.api.beans.SlotBookingBean;
import uk.ac.cam.cl.signups.api.exceptions.DuplicateNameException;
import uk.ac.cam.cl.signups.api.exceptions.ItemNotFoundException;
import uk.ac.cam.cl.signups.api.exceptions.NotAllowedException;
import uk.ac.cam.cl.signups.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.injection.GuiceConfigurationModule;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Guice;
import com.google.inject.Inject;

@Path("/signups")
public class TickSignups {
    
    private WebInterface service;
    @Inject private ConfigurationLoader<Configuration> config;
    
    public TickSignups() {
        Guice.createInjector(new GuiceConfigurationModule()).injectMembers(this);
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(config.getConfig()
                .getSignupsApiLocation());
        service = target.proxy(WebInterface.class);
    }
    
    /* Below are the methods for the student workflow */
    
    /**
     * Lists each time such that the time is the start time of
     * at least one free slot in the specified signup sheet.
     * @param sheetID The ID of the sheet whose free slots are needed.
     * @return A list of the start times of free slots
     * @throws ItemNotFoundException 
     */
    @GET
    @Path("/sheets/{sheetID}/times/{tickID}")
    @Produces("application/json")
    public Response listAvailableTimes(HttpServletRequest request,
            @PathParam("tickID") String tickID,
            @PathParam("sheetID") String sheetID) {
        String crsid = (String) request.getSession().getAttribute(
                "RavenRemoteUser");
        try {
            List<String> groupIDs = service.getGroupIDs(sheetID);
            if (groupIDs.size() != 1) {
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("There should be precisely one group associated "
                                + "with this sheet, but there seems to be " + groupIDs.size()).build();
            }
            String groupID = groupIDs.get(0);
            return Response.ok(service.listAllFreeStartTimes(crsid, tickID, groupID, sheetID)).build();
        } catch (ItemNotFoundException e) {
            return Response.status(Status.NOT_FOUND).entity(e).build();
        }
    }
    
    /**
     * Books the given student into a free slot at the specified
     * time. Only allowed if the student has permission to book a
     * slot for this tick and they haven't made a booking at the
     * same time already and they haven't already made a booking
     * for the same tick already.
     * @return The ticker that the student has been signed up to
     * see at the given time.
     */
    @POST
    @Path("/sheets/{sheetID}/bookings")
    @Consumes("application/json")    
    public Response makeBooking(String crsid, String groupID,
            String sheetID, String tickID, Date startTime) {
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getStartTime().equals(startTime)) {
                return Response.status(Status.FORBIDDEN)
                        .entity(Strings.EXISTINGTIMEBOOKING).build();
            }
            if (slot.getComment().equals(tickID)) {
                return Response.status(Status.FORBIDDEN)
                        .entity(Strings.EXISTINGTICKBOOKING).build();
            }
        }
        try {
            if (service.listColumnsWithFreeSlotsAt(sheetID, startTime.getTime()).size() == 0) {
                return Response.status(Status.NOT_FOUND)
                        .entity(Strings.NOFREESLOTS).build();
            }
            if (service.getPermissions(groupID, crsid).containsKey(tickID)) { // have passed this tick
                String ticker = service.getPermissions(groupID, crsid).get(tickID);
                if (ticker == null) { // any ticker permitted
                    ticker = service.listColumnsWithFreeSlotsAt(sheetID, startTime.getTime()).get(0);
                }
                service.book(sheetID, ticker, startTime.getTime(), new SlotBookingBean(null, crsid, tickID));
                return Response.ok().entity(ticker).build();
            }
        } catch (ItemNotFoundException e) {
            e.printStackTrace();
            return Response.status(Status.NOT_FOUND)
                    .entity(e).build();
        } catch (NotAllowedException e) {
            e.printStackTrace();
            return Response.status(Status.FORBIDDEN)
                    .entity(e).build();
        }
        return Response.status(Status.FORBIDDEN)
                .entity("Student does not have permission to book this slot").build();
    }
       
    /**
     * Unbooks the given user from the given slot.
     */
    @DELETE
    @Path("/sheets/{sheetID}/bookings")
    @Consumes("application/json")
    public Response unbookSlot(String crsid, String groupID,
            @PathParam("sheetID") String sheetID, String tickID, Date startTime) {
        String ticker = null;
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getStartTime().equals(startTime) &&
                    slot.getComment().equals(tickID) &&
                    slot.getSheetID().equals(sheetID)) {
                ticker = slot.getColumnName();
            }
        }
        if (ticker == null) {
            return Response.status(Status.NOT_FOUND).entity("The slot was not found").build();
        }
        try {
            service.book(sheetID, ticker, startTime.getTime(), new SlotBookingBean(crsid, null, null));
        } catch (ItemNotFoundException e) {
            e.printStackTrace();
            return Response.status(Status.NOT_FOUND)
                    .entity(e).build();
        } catch (NotAllowedException e) {
            e.printStackTrace();
            return Response.status(Status.FORBIDDEN)
                    .entity(e).build();
        }
        return Response.ok().build();
    }
    
    /**
     * Returns a list of the bookings made by one user.
     */
    @GET
    @Path("/students/{crsid}/bookings")
    @Produces("application/json")
    public Response listStudentBookings(@PathParam("crsid") String crsid) {
        return Response.ok(service.listUserSlots(crsid)).build();
    }
    
    /* Below are the methods for the ticker workflow */
    
    /**
     * @return The list of the sheets in the given group.
     */
    @GET
    @Path("/groups/{groupID}")
    @Produces("application/json")
    public Response listSheets(@PathParam("groupID") String groupID) {
        try {
            return Response.ok(service.listSheets(groupID)).build();
        } catch (ItemNotFoundException e) {
            return Response.status(404).entity(e).build();
        }
    }
    
    /**
     * @return A list of the ticker names for the given sheet.
     */
    @GET
    @Path("/sheets/{sheetID}")
    @Produces("application/json")
    public Response listTickers(@PathParam("sheetID") String sheetID) {
        try {
            return Response.ok(service.listColumns(sheetID)).build();
        } catch (ItemNotFoundException e) {
            return Response.status(404).entity(e).build();
        }
    }
    
    /**
     * Returns a list of the slots for the specified ticker.
     */
    @GET
    @Path("/sheets/{sheetID}/tickers/{ticker}")
    @Produces("application/json")
    public Response listSlots(@PathParam("sheetID") String sheetID, @PathParam("ticker") String tickerName) {
        try {
            return Response.ok(service.listColumnSlots(sheetID, tickerName)).build();
        } catch (ItemNotFoundException e) {
            return Response.status(Status.NOT_FOUND).entity(e).build();
        }
    }
    
    /**
     * @return Response whose body is has booked the slot (null if no one) and the tick
     * they have booked to do.
     */
    @GET
    @Path("/sheets/{sheetID}/tickers/{ticker}/{startTime}")
    @Produces("application/json")
    public Response getBooking(@PathParam("sheetID") String sheetID,
            @PathParam("ticker") String tickerName,
            @PathParam("startTime") Date startTime) {
        try {
            return Response.ok(service.showBooking(sheetID, tickerName, startTime.getTime())).build();
        } catch (ItemNotFoundException e) {
            return Response.status(Status.NOT_FOUND).entity(e).build();
        }
    }
    
    /**
     * Removes all bookings that haven't yet started for the given
     * user in the given sheet.
     */
    @DELETE
    @Path("/students/{crsid}/bookings/{sheetID}")
    @Consumes("application/json")
    public Response removeAllStudentBookings(String sheetID, String crsid, String authCode) {
        try {
            service.removeAllUserBookings(sheetID, crsid, authCode);
            return Response.ok().build();
        } catch (NotAllowedException e) {
            return Response.status(Status.FORBIDDEN).entity(e).build();
        } catch (ItemNotFoundException e) {
            return Response.status(Status.NOT_FOUND).entity(e).build();
        }
    }
    
    /**
     * Ensures that the given student is assigned the given ticker
     * (if possible) in the future for the specified tick.
     */
    @POST
    @Path("/students/{crsid}/permissions")
    @Consumes("application/json")
    public Response assignTickerForTickForUser(String crsid, String groupID,
            String tickID, String ticker, String groupAuthCode) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(tickID, ticker);
        try {
            service.addPermissions(groupID, crsid, new PermissionsBean(map, groupAuthCode));
            return Response.ok().build();
        } catch (NotAllowedException e) {
            e.printStackTrace();
            return Response.status(Status.FORBIDDEN)
                    .entity(e).build();
        } catch (ItemNotFoundException e) {
            e.printStackTrace();
            return Response.status(Status.NOT_FOUND)
                    .entity(e).build();
        }
    }
    
    /* Below are the methods for the author workflow */
    
    /**
     * Creates a new sheet for the given group.
     */
    @POST
    @Path("/sheets")
    @Consumes("application/json")
    @Produces("application/json")
    public Response createSheet(String title, String description, String location,
            Date startTime, int slotLengthInMinutes, Date endTime, List<String> tickerNames,
            String groupID, String groupAuthCode) {
        long sheetLengthInMinutes = (endTime.getTime() - startTime.getTime())/60000;
        if (sheetLengthInMinutes % slotLengthInMinutes != 0) {
            return Response.status(Status.BAD_REQUEST).entity("The difference in minutes "
                    + "between the start and end times should be an integer multiple of "
                    + "the length of the slots").build();
        }
        Sheet newSheet = new Sheet(title, description, location);
        String id;
        String auth;
        try {
            SheetInfo info = service.addSheet(newSheet);
            id = info.getSheetID();
            auth = info.getAuthCode();
        } catch (DuplicateNameException e) {
            return Response.serverError().entity("This sheet already seems to exist").build();
        }
        for (String ticker : tickerNames) {
            try {
                service.createColumn(id, new CreateColumnBean(ticker, auth, startTime, endTime, slotLengthInMinutes));
            } catch (ItemNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException("This should only happen if the sheet or column is not found "
                        + "but we are creating them");
            } catch (NotAllowedException e) {
                e.printStackTrace();
                throw new RuntimeException("We should have permission to access the sheet we have just created");
            } catch (DuplicateNameException e) {
                e.printStackTrace();
                return Response.status(Status.BAD_REQUEST).entity("You must have specified two "
                        + "identical columns (or slots)").build();
                // TODO: delete sheet?
            }
        }
        try {
            service.addSheetToGroup(groupID, new GroupSheetBean(id, groupAuthCode, auth));
        } catch (ItemNotFoundException e) {
            // TODO Create group?
            e.printStackTrace();
            return Response.status(Status.NOT_IMPLEMENTED).build();
        } catch (NotAllowedException e) {
            // Store auth codes in database and check user permissions before providing them?
            e.printStackTrace();
            return Response.status(Status.NOT_IMPLEMENTED).build();
        }
        return Response.ok().entity(/*TODO*/"TODO").build();
    }
    
    /**
     * Adds a new column to the sheet, filled with regularly spaced
     * empty slots.
     * @param sheetID
     * @param authCode
     * @param name
     * @param startTime Time of first slot
     * @param numberOfSlots
     * @param slotLength In minutes
     */
    @POST
    @Path("/sheets/{sheetID}/tickers")
    @Consumes("application/json")
    @Produces("application/json")
    public Response addColumn(String sheetID, String authCode, 
            String name, Date startTime, Date endTime,
            int slotLength /* in minutes */) {
        try {
            service.createColumn(sheetID, new CreateColumnBean(name, authCode, startTime, endTime, slotLength));
        } catch (ItemNotFoundException e) {
            e.printStackTrace();
            return Response.status(Status.NOT_FOUND).entity(e).build();
        } catch (NotAllowedException e) {
            e.printStackTrace();
            return Response.status(Status.FORBIDDEN).entity(e).build();
        } catch (DuplicateNameException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
        return Response.ok().build();
    }
    
    /**
     * Deletes the specified column from the sheet. This also deletes
     * all the bookings made for that column.
     */
    @DELETE
    @Path("/sheets/{sheetID}/tickers/{ticker}")
    @Consumes("application/json")
    @Produces("application/json")
    public Response deleteColumn(String sheetID, String ticker, String authCode) {
        try {
            service.deleteColumn(sheetID, ticker, authCode);
        } catch (NotAllowedException e) {
            e.printStackTrace();
            return Response.status(Status.FORBIDDEN).entity(e).build();
        } catch (ItemNotFoundException e) {
            e.printStackTrace();
            return Response.status(Status.NOT_FOUND).entity(e).build();
        }
        return Response.ok().build();
    }
    
    
    /**
     * Modifies bookings no matter what.
     */
    @POST
    @Path("/sheets/{sheetID}/bookings/{startTime}")
    @Consumes("application/json")
    // TODO: unify with normal modify booking and even perhaps make booking
    public Response forceModifyBooking(String sheetID, String authCode, String tickID,
            Date startTime, String currentlyBookedUser, String userToBook) {
        String ticker = null;
        for (Slot slot : service.listUserSlots(currentlyBookedUser)) {
            if (slot.getStartTime().equals(startTime) &&
                    slot.getComment().equals(tickID) &&
                    slot.getSheetID().equals(sheetID)) {
                ticker = slot.getColumnName();
            }
        }
        if (ticker == null) {
            return Response.status(Status.NOT_FOUND).entity("The slot was not found").build();
        }
        try {
            service.book(sheetID, ticker, startTime.getTime(), new SlotBookingBean(currentlyBookedUser, userToBook, tickID, authCode));
        } catch (ItemNotFoundException e) {
            e.printStackTrace();
            return Response.status(Status.NOT_FOUND).entity(e).build();
        } catch (NotAllowedException e) {
            e.printStackTrace();
            return Response.status(Status.FORBIDDEN).entity(e).build();
        }
        return Response.ok().build();
    }
    
}
