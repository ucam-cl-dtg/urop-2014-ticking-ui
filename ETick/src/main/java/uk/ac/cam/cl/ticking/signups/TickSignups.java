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

import uk.ac.cam.cl.signups.api.Column;
import uk.ac.cam.cl.signups.api.Group;
import uk.ac.cam.cl.signups.api.Sheet;
import uk.ac.cam.cl.signups.api.SheetInfo;
import uk.ac.cam.cl.signups.api.Slot;
import uk.ac.cam.cl.signups.api.beans.BatchCreateBean;
import uk.ac.cam.cl.signups.api.beans.BatchDeleteBean;
import uk.ac.cam.cl.signups.api.beans.CreateColumnBean;
import uk.ac.cam.cl.signups.api.beans.GroupSheetBean;
import uk.ac.cam.cl.signups.api.beans.PermissionsBean;
import uk.ac.cam.cl.signups.api.beans.SlotBookingBean;
import uk.ac.cam.cl.signups.api.beans.UpdateSheetBean;
import uk.ac.cam.cl.signups.api.exceptions.DuplicateNameException;
import uk.ac.cam.cl.signups.api.exceptions.ItemNotFoundException;
import uk.ac.cam.cl.signups.api.exceptions.NotAllowedException;
import uk.ac.cam.cl.signups.interfaces.SignupsWebInterface;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
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
            log.debug("Listing available times using the following parameters...");
            String groupID = getGroupID(sheetID);
            log.debug("crsid: " + crsid);
            log.debug("tickID: " + tickID);
            log.debug("groupID: " + groupID);
            log.debug("sheetID: " + sheetID);
            return Response.ok(service.listAllFreeStartTimes(crsid, tickID, groupID, sheetID)).build();
        } catch (ItemNotFoundException e) {
            log.debug("Either the sheet was not found or something has gone very wrong", e);
            return Response.status(Status.NOT_FOUND).entity("Not Found Error: " + e.getMessage()).build();
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
        String groupID = null;
        try {
            groupID = getGroupID(sheetID);
        } catch (ItemNotFoundException e1) {
            log.debug("User " + crsid + " tried to book a slot but the sheet given was " +
                    "not found in the signups database");
            return Response.status(Status.NOT_FOUND)
                    .entity("The sheet was not found in the signups database").build();
        }
        log.debug("Attempting to book slot for user " + crsid + " for tickID " + bean.getTickID() +
                " at time " + new Date(bean.getStartTime()) + " on sheet " + sheetID + " in group " + groupID);
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getStartTime().equals(bean.getStartTime())) {
                log.debug("The user already had a slot booked at the given time");
                return Response.status(Status.FORBIDDEN)
                        .entity(Strings.EXISTINGTIMEBOOKING).build();
            }
            if (slot.getComment().equals(bean.getTickID())) {
                log.debug("The user already had a slot booked for the given tick");
                return Response.status(Status.FORBIDDEN)
                        .entity(Strings.EXISTINGTICKBOOKING).build();
            }
        }
        try {
            if (service.listColumnsWithFreeSlotsAt(sheetID, bean.getStartTime()).size() == 0) {
                log.debug("No free slots were found at the given time");
                return Response.status(Status.NOT_FOUND)
                        .entity(Strings.NOFREESLOTS).build();
            }
            if (service.getPermissions(groupID, crsid).containsKey(bean.getTickID())) { // have passed this tick
                String ticker = service.getPermissions(groupID, crsid).get(bean.getTickID());
                if (ticker == null) { // any ticker permitted
                    ticker = service.listColumnsWithFreeSlotsAt(sheetID, bean.getStartTime()).get(0);
                }
                service.book(sheetID, ticker, bean.getStartTime(), new SlotBookingBean(null, crsid, bean.getTickID()));
                Fork f = db.getFork(Fork.generateForkId(crsid, bean.getTickID()));
                f.setSignedUp(true);
                db.saveFork(f);
                log.debug("The booking was successfully made");
                return Response.ok().entity(ticker).build();
            } else {
                return Response.status(Status.FORBIDDEN)
                        .entity("Error: you do not have permission to book this slot - perhaps you have not " +
                                "passed the unit tests").build();
            }
        } catch (ItemNotFoundException e) {
            log.debug("Something was not found the in database", e);
            return Response.status(Status.NOT_FOUND)
                    .entity("Not found error: " + e.getMessage()).build();
        } catch (NotAllowedException e) {
            log.debug("Permission to book the slot was denied", e);
            return Response.status(Status.FORBIDDEN)
                    .entity("Not allowed: " + e.getMessage()).build();
        }
    }
       
    /**
     * Unbooks the given user from the given slot.
     */
    @DELETE
    @Path("/bookings/{tickID}")
    public Response unbookSlot(@Context HttpServletRequest request, @PathParam("tickID") String tickID) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.debug("The user " + crsid + " is trying to unbook their slot for tick " + tickID);
        Slot booking = null;
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getComment().equals(tickID)) {
                booking = slot;
            }
        }
        if (booking == null) {
            log.debug("No booking was found for the specified tick");
            return Response.status(Status.NOT_FOUND).entity("Error: no booking was found for this tick").build();
        }
        try {
            service.book(booking.getSheetID(), booking.getColumnName(),
                    booking.getStartTime().getTime(), new SlotBookingBean(crsid, null, null));
            Fork f = db.getFork(Fork.generateForkId(crsid, tickID));
            f.setSignedUp(false);
            db.saveFork(f);
            log.debug("The slot was successfully unbooked");
            return Response.ok().build();
        } catch (ItemNotFoundException e) {
            log.error("The booking for the tick was found to simultaneously exist and not exist", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The booking for this tick was found to exist and "
                            + "then not found to exist. See following exception:\n"
                            + e).build();
        } catch (NotAllowedException e) {
            log.error("The unbooking should have been allowed but was not", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The removal of the booking should have been allowed but "
                            + "for some reason was not. See following exception:\n"
                            + e).build();
        }
    }
    
    @DELETE
    @Path("/students/{crsid}/ticks/{tickID}")
    public Response tickerUnbookSlot(@Context HttpServletRequest request,
            @PathParam("crsid") String crsid, @PathParam("tickID") String tickID) {
        String callingCrsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.debug("The user " + crsid + " is trying to unbook the submitter " +
                crsid + "'s booking for tick " + tickID);
        Slot booking = null;
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getComment().equals(tickID)) {
                booking = slot;
            }
        }
        if (booking == null) {
            log.debug("No booking was found for the specified tick");
            return Response.status(Status.NOT_FOUND).entity("No booking was found for this tick").build();
        }
        try {
            if (!db.getRoles(getGroupID(booking.getSheetID()), callingCrsid).contains(Role.MARKER)) {
                log.debug("The user " + callingCrsid + " in not a marker in the group");
                return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
            }
            service.book(booking.getSheetID(), booking.getColumnName(),
                    booking.getStartTime().getTime(), new SlotBookingBean(crsid, null, null));
            Fork f = db.getFork(Fork.generateForkId(crsid, tickID));
            f.setSignedUp(false);
            db.saveFork(f);
            log.debug("The slot was successfully unbooked");
            return Response.ok().build();
        } catch (ItemNotFoundException e) {
            log.error("The booking for the tick was found to simultaneously exist and not exist", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The booking for this tick was found to exist and "
                            + "then not found to exist. See following exception:\n"
                            + e).build();
        } catch (NotAllowedException e) {
            log.error("The unbooking should have been allowed but was not", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The removal of the booking should have been allowed but "
                            + "for some reason was not. See following exception:\n"
                            + e).build();
        }
    }
    
    public Response unbookSlot(String crsid, String tickID) {
        Slot booking = null;
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getComment().equals(tickID)) {
                booking = slot;
            }
        }
        if (booking == null) {
            log.debug("No booking was found for the specified tick");
            return Response.status(Status.NOT_FOUND).entity("No booking was found for this tick").build();
        }
        try {
            service.book(booking.getSheetID(), booking.getColumnName(),
                    booking.getStartTime().getTime(), new SlotBookingBean(crsid, null, null));
            Fork f = db.getFork(Fork.generateForkId(crsid, tickID));
            f.setSignedUp(false);
            db.saveFork(f);
            log.debug("The slot was successfully unbooked");
            return Response.ok().build();
        } catch (ItemNotFoundException e) {
            log.error("The booking for the tick was found to simultaneously exist and not exist", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("The booking for this tick was found to exist and "
                            + "then not found to exist. See following exception:\n"
                            + e).build();
        } catch (NotAllowedException e) {
            log.error("The unbooking should have been allowed but was not", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("The removal of the booking should have been allowed but "
                            + "for some reason was not. See following exception:\n"
                            + e).build();
        }
    }
    
    /**
     * Returns a list of the bookings in the future made by one user.
     */
    @GET
    @Path("/bookings")
    @Produces("application/json")
    public Response listStudentBookings(@Context HttpServletRequest request) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.debug("Listing the future bookings for user" + crsid);
        List<BookingInfo> toReturn = new ArrayList<BookingInfo>();
        Date now = new Date();
        for (Slot s :service.listUserSlots(crsid)) {
            Date endTime = new Date(s.getStartTime().getTime() + s.getDuration());
            if (endTime.after(now)) {
                String groupName;
                try {
                    String sheetID = s.getSheetID();
                    String groupID = getGroupID(sheetID);
                    uk.ac.cam.cl.ticking.ui.actors.Group group = db.getGroup(groupID);
                    groupName = group.getName();
                } catch (ItemNotFoundException e) {
                    log.error("A booking appears to exist in a sheet that doesn't", e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("You appear to have a booking in a sheet that doesn't"
                                    + " exist").build();
                }
                toReturn.add(new BookingInfo(s, groupName));
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
            return Response.status(Status.NOT_FOUND).entity("Not found error: " + e.getMessage()).build();
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
            return Response.status(Status.NOT_FOUND).entity("Not found error: " + e.getMessage()).build();
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
            return Response.status(Status.NOT_FOUND).entity("Not found error: " + e.getMessage()).build();
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
            return Response.status(Status.NOT_FOUND).entity("Not found error: " + e.getMessage()).build();
        }
    }
    
    /**
     * Removes all bookings that haven't yet started for the given
     * user in the given sheet.
     */
    @DELETE
    @Path("/students/{crsid}/bookings/{sheetID}")
    public Response removeAllStudentBookings(@Context HttpServletRequest request,
            @PathParam("sheetID") String sheetID,
            @PathParam("crsid") String crsid) {
        String callingCRSID = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.debug("The user " + callingCRSID + " has requested the removal of all future " +
                "bookings of the submitter " + crsid + " for the sheet of ID " + sheetID);
        String groupID;
        try {
            groupID = getGroupID(sheetID);
        } catch (ItemNotFoundException e1) {
            log.debug("The sheet of ID " + sheetID + " was not found", e1);
            return Response.status(Status.NOT_FOUND)
                    .entity("Not found error: the sheet " + sheetID + "was not found").build();
        }
        if (!db.getRoles(groupID, callingCRSID).contains(Role.MARKER)) {
            log.debug("The user " + callingCRSID + " does not have permission to remove these bookings");
            return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
        }
        try {
            for (Slot slot : service.listUserSlots(crsid)) {
                Fork f = db.getFork(Fork.generateForkId(crsid, slot.getComment()));
                f.setSignedUp(false);
                db.saveFork(f);
            }
            service.removeAllUserBookings(sheetID, crsid, db.getAuthCode(sheetID));
            log.debug("All future bookings of submitter " + crsid + " for sheet " + sheetID + " have been removed");
            return Response.ok().build();
        } catch (NotAllowedException e) {
            log.debug("Action was not allowed", e);
            return Response.status(Status.FORBIDDEN).entity("Error: " + e.getMessage()).build();
        } catch (ItemNotFoundException e) {
            log.debug("Something was not found", e);
            return Response.status(Status.NOT_FOUND).entity("Not found error: " + e.getMessage()).build();
        }
    }
    
  
    
    public Response allowSignup(String crsid, String groupID, String tickID) {
        log.debug("Attempting to allow submitter " + crsid +
                " to sign up for the tick " + tickID + " in group " + groupID);
        try {
            service.listSheets(groupID); // to see if group exists
        } catch (ItemNotFoundException e) { // if it doesn't, create it
            log.debug("The group for some reason does not exist in the signups database - " + 
                    "attempting to create it");
            try {
                createGroup(groupID);
            } catch (DuplicateNameException e1) {
                log.error("When creating the group because it doesn't exist, it was " +
                        "found to exist", e1);
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
            log.debug(crsid + " is now allowed to sign up for " + tickID + " in group " + groupID);
            return Response.ok().build();
        } catch (NotAllowedException e) {
            log.debug("Permission was denied", e);
            return Response.status(Status.FORBIDDEN)
                    .entity("Not allowed error: " + e.getMessage()).build();
        } catch (ItemNotFoundException e) {
            log.debug("Something was not found", e);
            return Response.status(Status.NOT_FOUND)
                    .entity("Not found error: " + e.getMessage()).build();
        }
    }
    
    public Response disallowSignup(String crsid, String groupID, String tickID) {
        log.debug("Removing submitter " + crsid + "'s permission to sign up for tick" +
                tickID + " in group " + groupID);
        Map<String, String> map = new HashMap<String, String>();
        map.put(tickID, null);
        try {
            service.removePermissions(groupID, crsid, new PermissionsBean(map, db.getAuthCode(groupID)));
        } catch (NotAllowedException e) {
            log.error("Permission was denied - it shouldn't be", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Permission was denied - it shouldn't be.\n" + e).build();
        } catch (ItemNotFoundException e) {
            log.error("Something was not found that should have been", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal server error: " + e.getMessage()).build();
        }
        log.debug("Permission removed");
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
            @PathParam("ticker") String ticker) { // TODO: maybe put ticker in body to allow it to have spaces?
        String callerCRSID = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.debug("User " + callerCRSID + " is requesting for submitter " + crsid + " to be allowed " +
                "to sign up for tick " + tickID + " in group " + groupID + " using " +
                (ticker == null ? "any ticker" : "ticker " + ticker + " only"));
        if (!db.getRoles(groupID, callerCRSID).contains(Role.MARKER)) {
            log.debug("User " + callerCRSID + " does not have permission to do this");
            return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
        }
        String groupAuthCode = db.getAuthCode(groupID);
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put(tickID, ticker);
            service.addPermissions(groupID, crsid, new PermissionsBean(map, groupAuthCode));
            log.debug("Permissions updated");
            return Response.ok().build();
        } catch (NotAllowedException e) {
            log.debug("Permission denied", e);
            return Response.status(Status.FORBIDDEN)
                    .entity("Not allowed: " + e.getMessage()).build();
        } catch (ItemNotFoundException e) {
            log.debug("Something was not found", e);
            return Response.status(Status.NOT_FOUND)
                    .entity("Not found error: " + e.getMessage()).build();
        }
    }
    
    public Response assignTickerForTickForUser(String crsid, String groupID, String tickID, String ticker) {
        log.debug("Attempting to allow submitter " + crsid +
                " to sign up for tick " + tickID + " in group " + groupID + " using " +
                (ticker == null ? "any ticker" : "ticker " + ticker + " only"));
        String groupAuthCode = db.getAuthCode(groupID);
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put(tickID, ticker);
            service.addPermissions(groupID, crsid, new PermissionsBean(map, groupAuthCode));
            log.debug("Permissions updated");
            return Response.ok().build();
        } catch (NotAllowedException e) {
            log.debug("Permission denied", e);
            return Response.status(Status.FORBIDDEN)
                    .entity("Not allowed: " + e.getMessage()).build();
        } catch (ItemNotFoundException e) {
            log.debug("Something was not found", e);
            return Response.status(Status.NOT_FOUND)
                    .entity("Not found error: " + e.getMessage()).build();
        }
    }
    
    /* Below are the methods for the author workflow */
    
    /**
     * Creates a new sheet for the given group. All columns are filled
     * with regularly spaced empty slots between the start and end times,
     * of the specified length.
     */
    @POST
    @Path("/sheets")
    @Consumes("application/json")
    @Produces("application/json")
    public Response createSheet(@Context HttpServletRequest request, SheetBean bean) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.debug("User " + crsid + " has requested the creation of a sheet for " +
                "the group of ID " + bean.getGroupID());
        if (!db.getRoles(bean.getGroupID(), crsid).contains(Role.AUTHOR)) {
            log.debug("The user " + crsid + " in not an author in the group");
            return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
        }
        long sheetLengthInMinutes = (bean.getEndTime() - bean.getStartTime())/60000;
        if (sheetLengthInMinutes <= 0) {
            log.debug("The end time must be after the start time");
            return Response.status(Status.BAD_REQUEST).entity("The end time must be after "
                    + "the start time").build();
        }
        if (sheetLengthInMinutes % bean.getSlotLengthInMinutes() != 0) {
            log.debug("There must be an integer number of slots in the sheet");
            return Response.status(Status.BAD_REQUEST).entity("The difference in minutes "
                    + "between the start and end times should be an integer multiple of "
                    + "the length of the slots").build();
        }
        if (sheetLengthInMinutes/bean.getSlotLengthInMinutes() > 500) {
            log.debug("Too many slots would have been created");
            return Response.status(Status.FORBIDDEN).entity("This sheet would have a silly "
                    + "number of slots if created.").build();
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
            log.debug("The sheet seems to already exist");
            return Response.serverError().entity("This sheet already seems to exist").build();
        }
        log.debug("The empty sheet was created");
        for (String ticker : bean.getTickerNames()) {
            try {
                service.createColumn(id, new CreateColumnBean(ticker, auth, new Date(bean.getStartTime()),
                        new Date(bean.getEndTime()), bean.getSlotLengthInMinutes()*60000));
            } catch (ItemNotFoundException e) {
                log.error("The sheet or column was not found, but we are creating them", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The sheet or column was not found, but we are creating them").build();
            } catch (NotAllowedException e) {
                log.error("Permission was denied, it should not have been", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Permission to the sheet was denied, it should not have been").build();
            } catch (DuplicateNameException e) {
                log.warn("A duplicate name exception was encountered when creating a sheet" +
                        "and ignored because " +
                        "two identical columns were probably entered. If it was a duplicate " +
                        "slot, there's some problem.", e);
            }
        }
        log.debug("The sheet was populated with tickers and slots");
        try {
            service.addSheetToGroup(bean.getGroupID(),
                    new GroupSheetBean(id, db.getAuthCode(bean.getGroupID()), auth));
        } catch (ItemNotFoundException e) { // group doesn't yet exist: create and retry
            log.debug("The given group was not found in the signups database - attempting to create it");
            try {
                createGroup(bean.getGroupID());
                service.addSheetToGroup(bean.getGroupID(),
                        new GroupSheetBean(id, db.getAuthCode(bean.getGroupID()), auth));
            } catch (DuplicateNameException e1) {
                log.error("The group was found to not exist and then immediately exist " +
                        "in the signups database", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The group was found to both exist and not exist "
                                + "in the signups database").build();
            } catch (ItemNotFoundException e1) {
                log.error("Group still not found, even after attempted creation", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The group was not found in the signups database, we "
                                + "attempted to create it, but it still wasn't found."
                                + "\n"+e1).build();
            } catch (NotAllowedException e1) {
                log.error("The auth codes were found to be incorrect", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The group was not found in the signups database, "
                                + "we attempted to create it, but permission was not "
                                + "given - it should have been").build();
            }
        } catch (NotAllowedException e) {
            log.error("The auth codes stored in the " +
                    "database were not found to match those in the signups database", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The authorisation codes stored in the " +
                            "database were not found to match those in the signups database").build();
        }
        return Response.ok().build();
    }
    
    
    /**
     * Edits the given sheet. Title, location and description are updated. Any tickers
     * added to the list are created; any tickers removed from the list are deleted, along
     * with any bookings made to their column. A "rename" is treated as a deletion and an
     * insertion. The slot length cannot be edited. The start and end times can be changed,
     * but must remain consistent with the previous start/end times and the slot length. If
     * the sheet is extended, slots are added at the appropriate intervals; if the sheet is
     * made shorter, the slots outside the new start and end times are deleted (along with
     * any bookings made at those times). TODO: update fork objects when deletions are made
     */
    @POST
    @Path("/sheets/{sheetID}")
    @Consumes("application/json")
    public Response editSheet(@Context HttpServletRequest request,
            @PathParam("sheetID") String sheetID, SheetBean bean) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.debug("User " + crsid + " is attempting to edit the sheet of ID " + sheetID);
        Sheet sheet;
        try {
            if (!db.getRoles(getGroupID(sheetID), crsid).contains(Role.AUTHOR)) {
                log.debug("The user " + crsid + " is not an author in the group " + getGroupID(sheetID));
                return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
            }
            sheet = service.getSheet(sheetID);
        } catch (ItemNotFoundException e) {
            log.debug("Sheet not found", e);
            return Response.status(Status.NOT_FOUND).entity("The sheet was not found").build();
        }
        if ((sheet.getStartTime().getTime() - bean.getStartTime()) % sheet.getSlotLength() != 0) {
            log.debug("The new start time but be an integer multiple of slot lengths away from the " +
                    "old start time of " + sheet.getStartTime().toString());
            return Response.status(Status.FORBIDDEN).entity("The new start time but be an integer "
                    + "multiple of slot lengths away from the " +
                    "old start time of " + sheet.getStartTime().toString()).build();
        }
        if ((sheet.getEndTime().getTime() - bean.getEndTime()) % sheet.getSlotLength() != 0) {
            log.debug("The new end time but be an integer multiple of slot lengths away from the " +
                    "old end time of " + sheet.getEndTime().toString());
            return Response.status(Status.FORBIDDEN).entity("The new end time but be an integer "
                    + "multiple of slot lengths away from the " +
                    "old end time of " + sheet.getEndTime().toString()).build();
        }
        long sheetLengthInMinutes = (bean.getEndTime() - bean.getStartTime())/60000;
        if (sheetLengthInMinutes/bean.getSlotLengthInMinutes() > 500) {
            log.debug("Too many slots would have been created");
            return Response.status(Status.FORBIDDEN).entity("This sheet would have a silly "
                    + "number of slots if created.").build();
        }
        List<Column> oldTickers = sheet.getColumns();
        List<String> oldTickerNames = new ArrayList<String>();
        for (Column oldTicker : oldTickers) {
            oldTickerNames.add(oldTicker.getName());
        }
        try {
            for (String newTicker : bean.getTickerNames()) { // add new tickers
                if (!oldTickerNames.contains(newTicker)) {
                    service.createColumn(sheetID, new CreateColumnBean(newTicker,
                            db.getAuthCode(sheetID), new Date(bean.getStartTime()),
                            new Date(bean.getEndTime()), bean.getSlotLengthInMinutes()));
                    sheet = service.getSheet(sheetID); // TODO: check no silly concurrency problems
                }
            }
            for (String oldTicker : oldTickerNames) { // delete removed tickers
                if (!bean.getTickerNames().contains(oldTicker)) {
                    service.deleteColumn(sheetID, oldTicker, db.getAuthCode(sheetID));
                    sheet = service.getSheet(sheetID);
                }
            }
            sheet.setTitle(bean.getTitle());
            sheet.setDescription(bean.getDescription());
            sheet.setLocation(bean.getLocation());
            service.updateSheet(sheetID, new UpdateSheetBean(sheet, db.getAuthCode(sheetID)));
        } catch (ItemNotFoundException e) {
            log.error("The sheet was not found, while earlier in this method it was", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal Server Error: some changes not made").build();
        } catch (NotAllowedException e) {
            log.error("The auth code stored was for some reason not correct", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal Server Error: some changes not made").build();
        } catch (DuplicateNameException e) {
            log.error("A new ticker was found to have the same name as an old ticker, " +
                    "even though new tickers are only created when there are no old ones" +
                    "with that name", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal Server Error: some changes not made").build();
        }
        try {
            if (bean.getStartTime() < sheet.getStartTime().getTime()) { //  need to add slots to start
                service.createSlotsForAllColumns(sheetID,
                        new BatchCreateBean(bean.getStartTime(), sheet.getStartTime().getTime(),
                                sheet.getSlotLength(), db.getAuthCode(sheetID)));
            }
            if (bean.getEndTime() > sheet.getEndTime().getTime()) { // need to add slots to end
                service.createSlotsForAllColumns(sheetID,
                        new BatchCreateBean(sheet.getEndTime().getTime(), bean.getEndTime(),
                                sheet.getSlotLength(), db.getAuthCode(sheetID)));
            }
            if (bean.getStartTime() > sheet.getStartTime().getTime()) { // need to delete slots from start
                service.deleteSlotsBetween(sheetID,
                        new BatchDeleteBean(sheet.getStartTime().getTime(), bean.getStartTime(), db.getAuthCode(sheetID)));
            }
            if (bean.getEndTime() < sheet.getEndTime().getTime()) { // need to delete slots from end
                service.deleteSlotsBetween(sheetID,
                        new BatchDeleteBean(bean.getEndTime()+1L, sheet.getEndTime().getTime()+1L, db.getAuthCode(sheetID)));
                                        // +1L because the method includes the start time and excludes the end time TODO: change?
            }
        } catch (ItemNotFoundException e) {
            log.error("The sheet was not found, while earlier in this method it was, " +
                    "or something else that should have been found wasn't", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal Server Error: some changes not made").build();
        } catch (NotAllowedException e) {
            log.error("The auth code stored was for some reason not correct", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal Server Error: some changes not made").build();
        } catch (DuplicateNameException e) {
            log.error("Slots that should not already exist were found to exist", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal Server Error").build();
        }
        log.debug("Sheet updated");
        return Response.ok().build();
    }
    
    /**
     * Removes the given sheet from the database, and irreversibly loses
     * all information associated with it, including bookings. TODO: update fork objects
     * @param request
     * @param sheetID
     * @return
     */
    @DELETE
    @Path("/sheets/{sheetID}")
    public Response deleteSheet(@Context HttpServletRequest request,
            @PathParam("sheetID") String sheetID) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.debug("User " + crsid + " has requested the deletion of sheet of ID " + sheetID);
        try {
            if (!db.getRoles(getGroupID(sheetID), crsid).contains(Role.AUTHOR)) {
                log.debug("The user " + crsid + " is not an author in the group " + getGroupID(sheetID));
                return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
            }
            service.deleteSheet(sheetID, db.getAuthCode(sheetID));
            log.debug("Sheet deleted");
            return Response.ok().build();
        } catch (ItemNotFoundException e) {
            log.debug("The sheet of ID " + sheetID + " was not found", e);
            return Response.status(Status.NOT_FOUND).entity("The given sheet was not found.").build();
        } catch (NotAllowedException e) {
            log.error("The auth code for the sheet was found to be incorrect", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Server Error: "
                    + "the authCode seems to be wrong, but it never should be.").build();
        }
    }
    
    /**
     * Adds a new column to the sheet, filled with regularly spaced
     * empty slots. TODO: change bean - we want this column to conform to the others
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
    public Response addColumn(@Context HttpServletRequest request,
            @PathParam("sheetID") String sheetID,
            AddColumnBean bean) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.debug("The user " + crsid + " wants to add a ticker to the sheet " + sheetID);
        try {
            if (!db.getRoles(getGroupID(sheetID), crsid).contains(Role.MARKER)) {
                log.debug("The user " + crsid + " does not have permission to do this (is not a marker)");
                return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
            }
        } catch (ItemNotFoundException e1) {
            log.debug("The sheet was not found", e1);
            return Response.status(Status.NOT_FOUND).entity("The given signup "
                    + "sheet was not found").build();
        }
        long sheetLengthInMinutes = (bean.getEndTime() - bean.getStartTime())/60000;
        if (bean.getEndTime() <= bean.getStartTime()) {
            log.debug("The end time was not after the start time");
            return Response.status(Status.BAD_REQUEST).entity("The end time must be after "
                    + "the start time").build();
        }
        if (sheetLengthInMinutes % bean.getSlotLengthInMinutes() != 0) {
            log.debug("An integer number of slots is required");
            return Response.status(Status.BAD_REQUEST).entity("The difference in minutes "
                    + "between the start and end times should be an integer multiple of "
                    + "the length of the slots").build();
        }
        if (sheetLengthInMinutes/bean.getSlotLengthInMinutes() > 500) {
            log.debug("Too many slots");
            return Response.status(Status.BAD_REQUEST).entity("This sheet would have a silly "
                    + "number of slots if created.").build();
        }
        try {
            service.createColumn(sheetID, new CreateColumnBean(bean.getName(),
                    db.getAuthCode(sheetID), new Date(bean.getStartTime()), new Date(bean.getEndTime()),
                    bean.getSlotLengthInMinutes()*60000));
            log.debug("Ticker added");
            return Response.ok().build();
        } catch (ItemNotFoundException e) {
            log.debug("Something was not found", e);
            return Response.status(Status.NOT_FOUND).entity("Not found error: " + e.getMessage()).build();
        } catch (NotAllowedException e) {
            log.debug("Permission was denied for some reason", e);
            return Response.status(Status.FORBIDDEN).entity("Not allowed: " + e.getMessage()).build();
        } catch (DuplicateNameException e) {
            log.debug("Duplicate column or slot", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Server Error: "
                    + e.getMessage()).build();
        }
    }
    
    /**
     * Deletes the specified column from the sheet. This also deletes
     * all the bookings made for that column. TODO: update fork objects
     */
    @DELETE
    @Path("/sheets/{sheetID}/tickers/{ticker}")
    public Response deleteColumn(@Context HttpServletRequest request,
            @PathParam("sheetID") String sheetID,
            @PathParam("ticker") String ticker) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.debug("The user " + crsid + " has requested deletion of the ticker " + ticker +
                "from the sheet of ID " + sheetID);
        try {
            if (!db.getRoles(getGroupID(sheetID), crsid).contains(Role.MARKER)) {
                log.debug("The user " + crsid + " is not a marker and so permission was denied");
                return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
            }
        } catch (ItemNotFoundException e1) {
            log.debug("The sheet was not found", e1);
            return Response.status(Status.NOT_FOUND).entity("Error: The given signup "
                    + "sheet was not found").build();
        }
        try {
            service.deleteColumn(sheetID, ticker, db.getAuthCode(sheetID));
            log.debug("Ticker deleted");
            return Response.ok().build();
        } catch (NotAllowedException e) {
            log.debug("Permission denied", e);
            return Response.status(Status.FORBIDDEN).entity("Not allowed: " + e.getMessage()).build();
        } catch (ItemNotFoundException e) {
            log.debug("Something was not found", e);
            return Response.status(Status.NOT_FOUND).entity("Not found error: " + e.getMessage()).build();
        }
    }
    
    
    
    /**
     * Modifies bookings no matter what.
     * Commented out for now because not currently used and needs attention if it is to be used.
     */
    /*
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
            return Response.status(Status.NOT_FOUND).entity("Not found error: " + e.getMessage()).build();
        } catch (NotAllowedException e) {
            e.printStackTrace();
            return Response.status(Status.FORBIDDEN).entity("Not allowed: " + e.getMessage()).build();
        }
        return Response.ok().build();
    }
    */
    
    public void createGroup(String groupID) throws DuplicateNameException {
        log.debug("Creating new group in signups database with ID " + groupID);
        String groupAuthCode = service.addGroup(new Group(groupID));
        db.addAuthCode(groupID, groupAuthCode);
        log.debug("Group created");
    }
    
    public String getGroupID(String sheetID) throws ItemNotFoundException {
        List<String> groupIDs = service.getGroupIDs(sheetID);
        if (groupIDs.size() != 1) {
            throw new RuntimeException("There should be precisely one group associated "
                            + "with this sheet, but there seems to be " + groupIDs.size());
        }
        return groupIDs.get(0);
    }
    
}
