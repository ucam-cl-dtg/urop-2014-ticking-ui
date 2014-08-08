package uk.ac.cam.cl.ticking.signups;

import java.util.ArrayList;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.signups.api.Group;
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
import uk.ac.cam.cl.signups.interfaces.SignupsWebInterface;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

@Path("/signups")
public class TickSignups {
    /* For logging */
    Logger log = LoggerFactory.getLogger(TickSignups.class);
    
    private SignupsWebInterface service;
    private IDataManager db;
    
    @Inject
    public TickSignups(IDataManager db, SignupsWebInterface service) {
        this.service = service;
        this.db = db;
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
    public Response listAvailableTimes(@Context HttpServletRequest request,
            @PathParam("tickID") String tickID,
            @PathParam("sheetID") String sheetID) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        try {
            List<String> groupIDs = service.getGroupIDs(sheetID);
            if (groupIDs.size() != 1) {
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("There should be precisely one group associated "
                                + "with this sheet, but there seems to be " + groupIDs.size()).build();
            }
            String groupID = groupIDs.get(0);
            log.info("crsid: " + crsid);
            log.info("tickID: " + tickID);
            log.info("groupID: " + groupID);
            log.info("sheetID: " + sheetID);
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
    public Response makeBooking(@Context HttpServletRequest request,
            @PathParam("sheetID") String sheetID, MakeBookingBean bean) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        List<String> groupIDs;
        try {
            groupIDs = service.getGroupIDs(sheetID);
        } catch (ItemNotFoundException e) {
            return Response.status(Status.NOT_FOUND).entity(e).build();
        }
        if (groupIDs.size() != 1) {
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("There should be precisely one group associated "
                            + "with this sheet, but there seems to be " + groupIDs.size()).build();
        }
        String groupID = groupIDs.get(0);
        log.info("Attempting to book slot for user " + crsid + " for tickID " + bean.getTickID() +
                " at time " + new Date(bean.getStartTime()) + " on sheet " + sheetID + " in group " + groupID);
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getStartTime().equals(bean.getStartTime())) {
                return Response.status(Status.FORBIDDEN)
                        .entity(Strings.EXISTINGTIMEBOOKING).build();
            }
            if (slot.getComment().equals(bean.getTickID())) {
                return Response.status(Status.FORBIDDEN)
                        .entity(Strings.EXISTINGTICKBOOKING).build();
            }
        }
        try {
            if (service.listColumnsWithFreeSlotsAt(sheetID, bean.getStartTime()).size() == 0) {
                return Response.status(Status.NOT_FOUND)
                        .entity(Strings.NOFREESLOTS).build();
            }
            if (service.getPermissions(groupID, crsid).containsKey(bean.getTickID())) { // have passed this tick
                String ticker = service.getPermissions(groupID, crsid).get(bean.getTickID());
                if (ticker == null) { // any ticker permitted
                    ticker = service.listColumnsWithFreeSlotsAt(sheetID, bean.getStartTime()).get(0);
                }
                service.book(sheetID, ticker, bean.getStartTime(), new SlotBookingBean(null, crsid, bean.getTickID()));
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
                .entity("You do not have permission to book this slot").build();
    }
       
    /**
     * Unbooks the given user from the given slot.
     */
    @DELETE
    @Path("/bookings/{tickID}")
    public Response unbookSlot(@Context HttpServletRequest request, @PathParam("tickID") String tickID) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        Slot booking = null;
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getComment().equals("tickID")) {
                booking = slot;
            }
        }
        if (booking == null) {
            return Response.status(Status.NOT_FOUND).entity("No booking was found for this tick").build();
        }
        try {
            service.book(booking.getSheetID(), booking.getColumnName(),
                    booking.getStartTime().getTime(), new SlotBookingBean(crsid, null, null));
        } catch (ItemNotFoundException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("The booking for this tick was found to exist and "
                            + "then not found to exist. See following exception:\n"
                            + e).build();
        } catch (NotAllowedException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("The removal of the booking should have been allowed but "
                            + "for some reason was not. See following exception:\n"
                            + e).build();
        }
        return Response.ok().build();
    }
    
    /**
     * Returns a list of the bookings in the future made by one user.
     */
    @GET
    @Path("/bookings")
    @Produces("application/json")
    // TODO: check in order
    public Response listStudentBookings(@Context HttpServletRequest request) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        List<BookingInfo> toReturn = new ArrayList<BookingInfo>();
        for (Slot s :service.listUserSlots(crsid)) {
            Date endTime = new Date(s.getStartTime().getTime() + s.getDuration());
            if (endTime.after(new Date())) {
                toReturn.add(new BookingInfo(s));
            }
        }
        return Response.ok(toReturn).build();
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
    @Consumes("text/plain")
    public Response removeAllStudentBookings(@PathParam("sheetID") String sheetID,
            @PathParam("crsid") String crsid, String authCode) {
        try {
            service.removeAllUserBookings(sheetID, crsid, authCode);
            return Response.ok().build();
        } catch (NotAllowedException e) {
            return Response.status(Status.FORBIDDEN).entity(e).build();
        } catch (ItemNotFoundException e) {
            return Response.status(Status.NOT_FOUND).entity(e).build();
        }
    }
    
    public Response allowSignup(String crsid, String groupID, String tickID) {
        try {
            service.listSheets(groupID); // to see if group exists
        } catch (ItemNotFoundException e) {
            String groupAuthCode;
            try {
                groupAuthCode = service.addGroup(new Group(groupID));
                db.addAuthCode(groupID, groupAuthCode);
            } catch (DuplicateNameException e1) {
                e1.printStackTrace();
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("The group was found to both exist and not exist "
                                + "in the signups database, sorry.\n"+e1).build();
            }
        }
        String groupAuthCode = db.getAuthCode(groupID);
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put(tickID, null);
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
    
    public Response disallowSignup(String crsid, String groupID, String tickID) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(tickID, null);
        try {
            service.removePermissions(groupID, crsid, new PermissionsBean(map, db.getAuthCode(groupID)));
        } catch (NotAllowedException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Permission was denied - it shouldn't be.\n" + e).build();
        } catch (ItemNotFoundException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity(e).build();
        }
        return Response.ok().build();
    }
    
    /**
     * Ensures that the given student is assigned the given ticker
     * (if possible) in the future for the specified tick.
     */
    @POST
    @Path("/students/{crsid}/permissions/{groupID}/{tickID}/{ticker}")
    public Response assignTickerForTickForUser(@Context HttpServletRequest request,
            @PathParam("crsid") String crsid,
            @PathParam("groupID") String groupID,
            @PathParam("tickID") String tickID,
            @PathParam("ticker") String ticker) {
        String callerCRSID = (String) request.getSession().getAttribute("RavenRemoteUser");
        if (!db.getRoles(groupID, callerCRSID).contains(Role.MARKER)) {
            return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
        }
        String groupAuthCode = db.getAuthCode(groupID);
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put(tickID, ticker);
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
    
    public Response assignTickerForTickForUser(String crsid, String groupID, String tickID, String ticker) {
        String groupAuthCode = db.getAuthCode(groupID);
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put(tickID, ticker);
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
    public Response createSheet(@Context HttpServletRequest request, CreateSheetBean bean) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        if (!db.getRoles(bean.getGroupID(), crsid).contains(Role.AUTHOR)) {
            return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
        }
        long sheetLengthInMinutes = (bean.getEndTime().getTime() - bean.getStartTime().getTime())/60000;
        if (sheetLengthInMinutes % bean.getSlotLengthInMinutes() != 0) {
            return Response.status(Status.BAD_REQUEST).entity("The difference in minutes "
                    + "between the start and end times should be an integer multiple of "
                    + "the length of the slots").build();
        }
        Sheet newSheet = new Sheet(bean.getTitle(), bean.getDescription(), bean.getLocation());
        String id;
        String auth;
        try {
            SheetInfo info = service.addSheet(newSheet);
            id = info.getSheetID();
            auth = info.getAuthCode();
            db.addAuthCode(id, auth);
        } catch (DuplicateNameException e) {
            return Response.serverError().entity("This sheet already seems to exist").build();
        }
        for (String ticker : bean.getTickerNames()) {
            try {
                service.createColumn(id, new CreateColumnBean(ticker, auth, bean.getStartTime(),
                        bean.getEndTime(), bean.getSlotLengthInMinutes()));
            } catch (ItemNotFoundException e) {
                e.printStackTrace();
                throw new RuntimeException("This should only happen if the sheet or column is not found "
                        + "but we are creating them");
            } catch (NotAllowedException e) {
                e.printStackTrace();
                throw new RuntimeException("We should have permission to access the sheet we have just created");
            } catch (DuplicateNameException e) {
                /*
                 * Actually, this isn't the end of the world, we can just ignore it.
                 * e.printStackTrace();
                 * return Response.status(Status.BAD_REQUEST).entity("You must have specified two "
                 *      + "identical columns (or slots)").build();
                 */
            }
        }
        try {
            service.addSheetToGroup(bean.getGroupID(),
                    new GroupSheetBean(id, db.getAuthCode(bean.getGroupID()), auth));
        } catch (ItemNotFoundException e) { // group doesn't yet exist: create and retry
            String groupAuthCode;
            try {
                groupAuthCode = service.addGroup(new Group(bean.getGroupID()));
                service.addSheetToGroup(bean.getGroupID(),
                        new GroupSheetBean(id, db.getAuthCode(bean.getGroupID()), auth));
                db.addAuthCode(bean.getGroupID(), groupAuthCode);
            } catch (DuplicateNameException e1) {
                e1.printStackTrace();
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("The group was found to both exist and not exist "
                                + "in the signups database, sorry.\n"+e1).build();
            } catch (ItemNotFoundException e1) {
                e1.printStackTrace();
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("The group was not found in the signups database, we "
                                + "attempted to create it, but it still wasn't found."
                                + "\n"+e1).build();
            } catch (NotAllowedException e1) {
                e1.printStackTrace();
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("The group was not found in the signups database, "
                                + "we attempted to create it, but permission was not "
                                + "given - it should have been").build();
            }
        } catch (NotAllowedException e) {
            e.printStackTrace();
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("The auth codes stored by the database were for some "
                            + "reason found to be incorrect./n" + e).build();
        }
        return Response.ok().build();
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
    //TODO: argument needs to be a bean
    public Response addColumn(@PathParam("sheetID") String sheetID, String authCode, 
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
    @Consumes("text/plain")
    @Produces("application/json")
    public Response deleteColumn(@PathParam("sheetID") String sheetID,
            @PathParam("ticker") String ticker, String authCode) {
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
    // TODO: check arguments 
    // TODO: unify with normal modify booking and even perhaps make booking TODO: maybe not
    public Response forceModifyBooking(@PathParam("sheetID") String sheetID, String authCode, String tickID,
            @PathParam("sheetID") Long startTime, String currentlyBookedUser, String userToBook) {
        String ticker = null;
        for (Slot slot : service.listUserSlots(currentlyBookedUser)) {
            if (slot.getStartTime().getTime() == startTime &&
                    slot.getComment().equals(tickID) &&
                    slot.getSheetID().equals(sheetID)) {
                ticker = slot.getColumnName();
            }
        }
        if (ticker == null) {
            return Response.status(Status.NOT_FOUND).entity("The slot was not found").build();
        }
        try {
            service.book(sheetID, ticker, startTime, new SlotBookingBean(currentlyBookedUser, userToBook, tickID, authCode));
        } catch (ItemNotFoundException e) {
            e.printStackTrace();
            return Response.status(Status.NOT_FOUND).entity(e).build();
        } catch (NotAllowedException e) {
            e.printStackTrace();
            return Response.status(Status.FORBIDDEN).entity(e).build();
        }
        return Response.ok().build();
    }
    
    public void createGroup(String groupID) throws DuplicateNameException {
        service.addGroup(new Group(groupID));
    }
    
}
