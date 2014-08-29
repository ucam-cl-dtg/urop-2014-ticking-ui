package uk.ac.cam.cl.ticking.signups;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.dtg.teaching.exceptions.SerializableException;
import uk.ac.cam.cl.signups.api.Column;
import uk.ac.cam.cl.signups.api.Group;
import uk.ac.cam.cl.signups.api.Sheet;
import uk.ac.cam.cl.signups.api.SheetInfo;
import uk.ac.cam.cl.signups.api.Slot;
import uk.ac.cam.cl.signups.api.beans.BatchCreateBean;
import uk.ac.cam.cl.signups.api.beans.BatchDeleteBean;
import uk.ac.cam.cl.signups.api.beans.CreateColumnBean;
import uk.ac.cam.cl.signups.api.beans.GroupSheetBean;
import uk.ac.cam.cl.signups.api.beans.AddPermissionsBean;
import uk.ac.cam.cl.signups.api.beans.RemovePermissionsBean;
import uk.ac.cam.cl.signups.api.beans.SlotBookingBean;
import uk.ac.cam.cl.signups.api.beans.UpdateSheetBean;
import uk.ac.cam.cl.signups.api.exceptions.DuplicateNameException;
import uk.ac.cam.cl.signups.api.exceptions.ItemNotFoundException;
import uk.ac.cam.cl.signups.api.exceptions.NotAllowedException;
import uk.ac.cam.cl.signups.interfaces.SignupsWebInterface;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.util.PermissionsManager;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

/*
 * IMPORTANT
 * If you want to understand the code, it will be MUCH easier if you simply ignore
 * all blocks which begin by catching an InternalServerErrorException, and the
 * execution will run as intended. Catching InternalServerErrorExceptions is
 * necessary because of a bug with the exception-chains, which forces me to get
 * the InternalServerErrorException and work out what kind of exception it actually
 * is. Although I handle the different cases within the InternalServerErrorException
 * block, this handling is replicated outside the block, both to satisfy the compiler
 * and in case the bug is fixed.
 */

/* Note: this service treats all times as UTC */

/**
 * This class provides the ticking-specific functionality that would be inappropriate to
 * include in the generic signups project.
 * 
 * @author Isaac Dunn &lt;ird28@cam.ac.uk&gt;
 */
@Path("/signups")
public class TickSignups {
    /* For logging */
    Logger log = LoggerFactory.getLogger(TickSignups.class);
    
    private SignupsWebInterface service;
    private IDataManager db;
    private PermissionsManager permissions;
    private static Object synchLock = new Object(); // For synchronized blocks
    
    @Inject
    public TickSignups(IDataManager db, SignupsWebInterface service, PermissionsManager permissions) {
        this.service = service;
        this.db = db;
        this.permissions = permissions;
    }
    
    /* Below are the methods for the student workflow */
    
    /**
     * Lists the times for which the current raven user can sign up to get 
     * the given tick marked (on the given sheet).
     * @param HttpServletRequest (Supplied automatically)
     * @param tickID The ID of the tick which the user wants to sign up with
     * @param sheetID The ID of the sheet from which to list times
     * @return A list of the start times of free slots
     */
    @GET
    @Path("/sheets/{sheetID}/times/{tickID}")
    @Produces("application/json")
    public Response listAvailableTimes(@Context HttpServletRequest request,
            @PathParam("tickID") String tickID,
            @PathParam("sheetID") String sheetID) {
        /* Get current raven user */
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        try {
            /* 
             * Get the group of the given sheet - the generic signups service allows
             * each sheet to have any number of groups, but in this class we ensure
             * each sheet has precisely one group.
             */
            String groupID = getGroupID(sheetID);
            log.info("Listing available times using the following parameters...\ncrsid: "
                    + crsid + " tickID: " + tickID + " groupID: " + groupID + " sheetID: " + sheetID);
            
            /* Get slots from generic signups service */
            List<Date> freeTimes = service.listAllFreeStartTimes(crsid, tickID,
                    groupID, sheetID);
            /* Remove slots for which the student already has a slot booked */
            for (Slot s : service.listUserSlots(crsid)) {
                if (freeTimes.contains(s.getStartTime())) {
                    freeTimes.remove(s.getStartTime());
                }
            }
            return Response.ok(freeTimes).build();
        } catch(InternalServerErrorException e) { // Don't forget to ignore this block...
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.warn("Either the sheet was not found or something has gone very wrong", e1);
                return Response.status(Status.NOT_FOUND).entity("Not Found Error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) { // Here is all the information you need about the caught exception
            log.warn("Either the sheet was not found or something has gone very wrong", e);
            return Response.status(Status.NOT_FOUND).entity("Not Found Error: " + e.getMessage()).build();
        }
    }
    
    /**
     * Books the current raven user into a free slot at the specified
     * time. Only allowed if the student has permission to book a
     * slot for this tick and they haven't made a booking at the
     * same time already and they haven't already made a booking
     * for the same tick already.
     * 
     * Also ensures the student returns to the ticker who failed them,
     * if possible.
     * 
     * @return The ticker that the student has been signed up to
     * see at the given time.
     */
    @POST
    @Path("/sheets/{sheetID}/bookings")
    @Consumes("application/json")
    public Response makeBooking(@Context HttpServletRequest request,
            @PathParam("sheetID") String sheetID, MakeBookingBean bean) {
        /* Get current raven user */
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        String groupID = null;
        try {
            /* Each sheet has precisely one group - get it */
            groupID = getGroupID(sheetID);
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.warn("User " + crsid + " tried to book a slot but the sheet given was " +
                        "not found in the signups database");
                return Response.status(Status.NOT_FOUND)
                        .entity("The sheet was not found in the signups database").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e1) {
            log.warn("User " + crsid + " tried to book a slot but the sheet given (" + sheetID +
                    ")  was not found in the signups database");
            return Response.status(Status.NOT_FOUND)
                    .entity("The sheet was not found in the signups database").build();
        }
        log.info("Attempting to book slot for user " + crsid + " for tickID " + bean.getTickID() +
                " at time " + new Date(bean.getStartTime()) + " on sheet " + sheetID + " in group " + groupID);
        Date now = new Date();
        for (Slot slot : service.listUserSlots(crsid)) {
            /* Check user hasn't already booked a slot at the given time */
            if (slot.getStartTime().getTime() == bean.getStartTime()) {
                log.warn("The user already had a slot booked at the given time");
                return Response.status(Status.FORBIDDEN)
                        .entity(Strings.EXISTINGTIMEBOOKING).build();
            }
            /* Check user hasn't already booked a slot for the given tick */
            if (slot.getStartTime().after(now) && slot.getComment().equals(bean.getTickID())) {
                log.warn("The user " + crsid + " already had a slot booked for tick " + bean.getTickID());
                return Response.status(Status.FORBIDDEN)
                        .entity(Strings.EXISTINGTICKBOOKING).build();
            }
        }
        try {
            /* Get list of the tickers who are free at the given time */
            List<String> freeTickers = service.listColumnsWithFreeSlotsAt(sheetID, bean.getStartTime());
            /* If no tickers are free at the given time, tell the user */
            if (freeTickers.size() == 0) {
                log.info("No free slots were found at the given time");
                return Response.status(Status.NOT_FOUND)
                        .entity(Strings.NOFREESLOTS).build();
            }
            if (service.getPermissions(groupID, crsid).containsKey(bean.getTickID())) { // user has passed this tick
                /* Get the ticker they should ideally be signed up with (i.e. they have been failed by) */
                String ticker = service.getPermissions(groupID, crsid).get(bean.getTickID());
                if (ticker != null && !service.columnIsFullyBooked(sheetID, ticker)) {
                    /* 
                     * If they have been failed by a ticker for this tick in the past, and that ticker is not
                     * now fully booked, try to book a slot with this ticker.
                     */
                    service.book(sheetID, ticker, bean.getStartTime(), new SlotBookingBean(null, crsid, bean.getTickID()));
                    /* Update fork object */
                    Fork f = db.getFork(Fork.generateForkId(crsid, bean.getTickID()));
                    f.setSignedUp(true);
                    db.saveFork(f);
                    log.info("The booking was successfully made");
                    return Response.ok().entity(ticker).build();
                    /* If not successful, let the user know, don't automatically try a different ticker */
                } else {
                    /* User has not been failed by a ticker before, or if they have, then they are fully booked. */
                    freeTickers = service.listColumnsWithFreeSlotsAt(sheetID, bean.getStartTime());
                    for (int t = 0; t < freeTickers.size(); t++) { // for each free ticker
                        try { // try to book the user using that ticker
                            service.book(sheetID, freeTickers.get(t), bean.getStartTime(),
                                    new SlotBookingBean(null, crsid, bean.getTickID()));
                            /* If successful, update fork object and return success */
                            Fork f = db.getFork(Fork.generateForkId(crsid, bean.getTickID()));
                            f.setSignedUp(true);
                            db.saveFork(f);
                            log.info("The booking was successfully made");
                            return Response.ok().entity(freeTickers.get(t)).build();
                        } catch(InternalServerErrorException e) { // Ignore this block
                            try {
                                throwRealException(e);
                                log.error("Something went wrong when processing the InternalServerErrorException", e);
                                return Response.status(Status.INTERNAL_SERVER_ERROR)
                                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                                        .build();
                            } catch (NotAllowedException e1) { // booking failed
                                if (t+1 == freeTickers.size()) {
                                    throw e1; // No more free tickers at this time, report error
                                } else {
                                    // Do nothing; try and book with a different ticker instead
                                }
                            } catch (Throwable th) {
                                log.error("Something went wrong when processing the InternalServerErrorException", th);
                                return Response.status(Status.INTERNAL_SERVER_ERROR)
                                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                                        .build();
                            }
                        } catch (NotAllowedException e) { /* If booking failed */
                            if (t+1 == freeTickers.size()) {
                                throw e; /* No more free tickers at this time, report error */
                            } else {
                                /* Do nothing; there are other free tickers so try and
                                 * book with a different ticker instead */
                            }
                        }
                    }
                    log.warn("Booking unsuccessful after trying all possible tickers");
                    // I expect this to happen when freeTickers.size() == 0
                    return Response.status(Status.NOT_FOUND)
                            .entity(Strings.NOFREESLOTS).build();
                }
            } else {
                log.warn("The booking was not made - " + crsid + " does not have permission to sign up for tick"
                        + bean.getTickID());
                return Response.status(Status.FORBIDDEN)
                        .entity("Error: you do not have permission to book this slot - perhaps you have not " +
                                "passed the unit tests").build();
            }
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.warn("Something was not found the in database", e1);
                return Response.status(Status.NOT_FOUND)
                        .entity("Not found error: " + e1.getMessage()).build();
            } catch (NotAllowedException e1) {
                log.info("Permission to book the slot was denied", e1);
                return Response.status(Status.FORBIDDEN)
                        .entity("Not allowed: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.warn("Something was not found the in database", e);
            return Response.status(Status.NOT_FOUND)
                    .entity("Not found error: " + e.getMessage()).build();
        } catch (NotAllowedException e) {
            log.warn("Permission to book the slot was denied", e);
            return Response.status(Status.FORBIDDEN)
                    .entity("Booking unsuccessful: " + e.getMessage()).build();
        }
    }
    
    /**
     * Books the slot with the current raven user's crsid and the comment "Unavailable",
     * providing that the current raven user is a marker in the group of the given sheet.
     */
    @POST
    @Path("/sheets/{sheetID}/tickerbookings")
    public Response tickerReserveSlot(@Context HttpServletRequest request,
            @PathParam("sheetID") String sheetID, TickerReserveSlotBean bean) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        String groupID;
        try {
            groupID = getGroupID(sheetID);
        } catch (ItemNotFoundException e) {
            log.warn("The user " + crsid + " tried to reserve a the slot "
                    + " in the sheet of ID " + sheetID + " but the group for that sheet was not found");
            return Response.status(Status.NOT_FOUND).entity("Error: no group was found for that sheet").build();
        }
        /* Check current raven user is a marker in the group of the sheet */
        if (!permissions.hasRole(crsid, groupID, Role.MARKER)) {
            log.warn("The user " + crsid + " tried to reserve a the slot starting at "
                    + new Date(bean.getStartTime()) + " for ticker " + bean.getTicker()
                    + " in the sheet of ID " + sheetID + " but is not a marker in the group");
            return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
        }
        /* Tell the signups service to allow the booking */
        allowSignup(crsid, groupID, Strings.TICKERSLOT);
        try {
            /* Book slot */
            service.book(sheetID, bean.getTicker(), bean.getStartTime(),
                    new SlotBookingBean(null, crsid, Strings.TICKERSLOT, db.getAuthCode(sheetID)));
        } catch (ItemNotFoundException e) {
            log.error("Something was not found which definitely should have been; there is an inconsistency", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Server Error: inconsistent databases; "
                    + "reservation not made").build();
        } catch (NotAllowedException e) {
            log.error("The databases are inconsistent; the signups service rejected the auth code in the "
                    + "'front end' database", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Server Error: inconsistent databases; "
                    + "reservation not made").build();
        }
        log.info("Marker " + crsid + " has reserved a slot at " + new Date(bean.getStartTime())
        + " for ticker " + bean.getTicker()+ " in the sheet of ID " + sheetID);
        return Response.ok().build();
    }

    /**
     * Unbooks the current raven user from the slot they've booked for the specified tick.
     */
    @DELETE
    @Path("/bookings/{tickID}")
    public Response unbookSlot(@Context HttpServletRequest request, @PathParam("tickID") String tickID) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.info("The user " + crsid + " is trying to unbook their slot for tick " + tickID);
        return unbookSlot(crsid, tickID);
    }
    
    /**
     * Unbooks the student of the given crsid from the slot they've booked for the specified tick,
     * if the current raven user is a marker in the group the slot is booked in.
     */
    @DELETE
    @Path("/students/{crsid}/ticks/{tickID}")
    public Response tickerUnbookSlot(@Context HttpServletRequest request,
            @PathParam("crsid") String crsid, @PathParam("tickID") String tickID) {
        String callingCrsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.info("The user " + callingCrsid + " is trying to unbook the submitter " +
                crsid + "'s booking for tick " + tickID);
        Slot booking = null;
        Date now = new Date();
        /* Get the slot that the user has booked for the given tick */
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getComment().equals(tickID) // the comment stored in the slot in the generic signups database is the tickID
                    && slot.getStartTime().after(now)) {
                booking = slot;
            }
        }
        if (booking == null) { // booking for the tick not found
            log.warn("No booking was found for the specified tick (user: " + crsid + ", tick: " + tickID + ")");
            return Response.status(Status.NOT_FOUND).entity("No booking was found for this tick"
                    + " (bookings in the past cannot be changed)").build();
        }
        try {
            /* Check user is a marker in the group */
            if (!permissions.hasRole(callingCrsid, getGroupID(booking.getSheetID()), Role.MARKER)) {
                log.warn("The user " + callingCrsid + " is not a marker in the group");
                return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
            }
            /* Unbook slot */
            service.book(booking.getSheetID(), booking.getColumnName(),
                    booking.getStartTime().getTime(), new SlotBookingBean(crsid, null, null));
            /* Update fork object */
            Fork f = db.getFork(Fork.generateForkId(crsid, tickID));
            f.setSignedUp(false);
            db.saveFork(f);
            log.info("The slot was successfully unbooked (user: " + crsid + ", tick: " + tickID + ")");
            return Response.noContent().build();
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.error("The booking for the tick was found to simultaneously exist and not exist", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: There was an inconsistency in the signups database. "
                                + "See the logs for details.").build();
            } catch (NotAllowedException e1) {
                log.error("The unbooking should have been allowed but was not", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: There was a problem in removing the student. "
                                + "See the logs for details.").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.error("The booking for the tick was found to simultaneously exist and not exist", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: There was an inconsistency in the signups database. "
                            + "See the logs for details.").build();
        } catch (NotAllowedException e) {
            log.error("The unbooking should have been allowed but was not", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: There was a problem in removing the student. "
                            + "See the logs for details.").build();
        }
    }
    
    /**
     * Unbooks the student of the given crsid from the slot they've booked for the specified tick.
     */
    public Response unbookSlot(String crsid, String tickID) {
        Slot booking = null;
        Date now = new Date();
        /* Get the slot that the user has booked for the given tick */
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getComment().equals(tickID) // the comment stored in the slot in the generic signups database is the tickID
                    && slot.getStartTime().after(now)) {
                booking = slot;
            }
        }
        if (booking == null) { // booking for the tick not found
            log.warn("No booking was found for the specified tick (user: " + crsid + ", tick: " + tickID + ")");
            return Response.status(Status.NOT_FOUND).entity("No booking was found for this tick").build();
        }
        try {
            /* Unbook user from slot */
            service.book(booking.getSheetID(), booking.getColumnName(),
                    booking.getStartTime().getTime(), new SlotBookingBean(crsid, null, null));
            /* Update fork object */
            Fork f = db.getFork(Fork.generateForkId(crsid, tickID));
            f.setSignedUp(false);
            db.saveFork(f);
            log.info("The slot (user: " + crsid + ", tick: " + tickID + ") was successfully unbooked");
            return Response.noContent().build();
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.error("The booking for the tick was found to simultaneously exist and not exist", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: There was an inconsistency in the signups database. "
                                + "See the logs for details.").build();
            } catch (NotAllowedException e1) {
                log.error("The unbooking should have been allowed but was not", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: There was a problem in removing the student. "
                                + "See the logs for details.").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.error("The booking for the tick was found to simultaneously exist and not exist", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: There was an inconsistency in the signups database. "
                            + "See the logs for details.").build();
        } catch (NotAllowedException e) {
            log.error("The unbooking should have been allowed but was not", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: There was a problem in removing the student. "
                            + "See the logs for details.").build();
        }
    }

    /**
     * Returns a list of the bookings in the future made by the current raven user.
     */
    @GET
    @Path("/bookings")
    @Produces("application/json")
    public Response listStudentBookings(@Context HttpServletRequest request) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        List<BookingInfo> toReturn = new ArrayList<BookingInfo>();
        Date now = new Date();
        for (Slot s : service.listUserSlots(crsid)) {
            Date endTime = new Date(s.getStartTime().getTime() + s.getDuration());
            if (endTime.after(now)) {
                /* For each slot the user has booked that hasn't finished yet... */
                String groupName;
                try {
                    /* Gets the name of the group that the sheet belongs to */
                    groupName = db.getGroup(getGroupID(s.getSheetID())).getName();
                } catch(InternalServerErrorException e) { // Ignore this block
                    try {
                        throwRealException(e);
                        log.error("Something went wrong when processing the InternalServerErrorException", e);
                        return Response.status(Status.INTERNAL_SERVER_ERROR)
                                .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                                .build();
                    } catch (ItemNotFoundException e1) {
                        log.error("A booking appears to exist in a sheet that doesn't", e1);
                        return Response.status(Status.INTERNAL_SERVER_ERROR)
                                .entity("Server Error: You appear to have a booking in a sheet"
                                        + " that doesn't exist").build();
                    } catch (Throwable t) {
                        log.error("Something went wrong when processing the InternalServerErrorException", t);
                        return Response.status(Status.INTERNAL_SERVER_ERROR)
                                .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                                .build();
                    }
                } catch (ItemNotFoundException e) {
                    log.error("A booking appears to exist in a sheet that doesn't", e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: You appear to have a booking in a sheet"
                                    + " that doesn't exist").build();
                }
                /* ...include that booking in the list to be returned */
                toReturn.add(new BookingInfo(s, groupName));
            }
        }
        return Response.ok(toReturn).build();
    }
    
    /* Below are the methods for the ticker workflow */
    
    /**
     * Returns the list of the sheets in the given group whose end times are in the future.
     * If the query parameter includeHistoricSheets is given and is true, then ALL sheets
     * in the group are included.
     * 
     * The default is for the past sheets to be excluded because they just clutter - you can't
     * sign up for a slot on one etc.
     */
    @GET
    @Path("/groups/{groupID}")
    @Produces("application/json")
    public Response listSheets(@PathParam("groupID") String groupID,
            @DefaultValue("false") @QueryParam("includeHistoricSheets") boolean includeHistoricSheets) {
        try {
            /* List all sheets in the group */
            List<Sheet> sheets = service.listSheets(groupID, db.getAuthCode(groupID));
            if (!includeHistoricSheets) {
                /* Remove all sheets from the list whose end times are in the past */
                Date now = new Date();
                Iterator<Sheet> it = sheets.iterator();
                while (it.hasNext()) {
                    Sheet sheet = it.next();
                    if (sheet.getEndTime().before(now)) {
                        it.remove();
                    }
                }
            }
            return Response.ok(sheets).build();
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.warn("Probably the group was not found - investigate if something else", e1);
                return Response.status(Status.NOT_FOUND).entity("Not found error: " + e1.getMessage()).build();
            } catch (NotAllowedException e1) {
                log.error("AuthCode was rejected - the databases are inconsistent", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: authorisation code rejected; databases inconsistent").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.warn("Probably the group was not found - investigate if something else", e);
            return Response.status(Status.NOT_FOUND).entity("Not found error: " + e.getMessage()).build();
        } catch (NotAllowedException e1) {
            log.error("AuthCode was rejected - the databases are inconsistent", e1);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: authorisation code rejected; databases inconsistent").build();
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
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.warn("Probably the sheet was not found - investigate if something else", e1);
                return Response.status(Status.NOT_FOUND).entity("Not found error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.warn("Probably the sheet was not found - investigate if something else", e);
            return Response.status(Status.NOT_FOUND).entity("Not found error: " + e.getMessage()).build();
        }
    }
    
    /**
     * @return A list of the slots for the specified ticker in the specified sheet.
     */
    @GET
    @Path("/sheets/{sheetID}/tickers/{ticker}")
    @Produces("application/json")
    public Response listSlots(@PathParam("sheetID") String sheetID, @PathParam("ticker") String tickerName) {
        try {
            List<Slot> slots = service.listColumnSlots(sheetID, tickerName);
            return Response.ok(slots).build();
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.warn("Probably either the sheet or column was not found - investigate if something else", e1);
                return Response.status(Status.NOT_FOUND).entity("Not found error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.warn("Probably either the sheet or column was not found - investigate if something else", e);
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
        log.info("The user " + callingCRSID + " has requested the removal of all future " +
                "bookings of the submitter " + crsid + " for the sheet of ID " + sheetID);
        String groupID;
        try {
            groupID = getGroupID(sheetID);
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.warn("The sheet of ID " + sheetID + " was not found", e1);
                return Response.status(Status.NOT_FOUND)
                        .entity("Not found error: the sheet " + sheetID + "was not found").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.warn("The sheet of ID " + sheetID + " was not found", e);
            return Response.status(Status.NOT_FOUND)
                    .entity("Not found error: the sheet " + sheetID + "was not found").build();
        }
        /* Check calling user is a marker in the group */
        if (!permissions.hasRole(callingCRSID, groupID, Role.MARKER)) {
            log.warn("The user " + callingCRSID + " does not have permission to remove these bookings");
            return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
        }
        try {
            for (Slot slot : service.listUserSlots(crsid)) {
                /* Updating fork objects; not strictly necessary any more */
                Fork f = db.getFork(Fork.generateForkId(crsid, slot.getComment()));
                f.setSignedUp(false);
                db.saveFork(f);
            }
            service.removeAllUserBookings(sheetID, crsid, db.getAuthCode(sheetID));
            log.info("All future bookings of submitter " + crsid + " for sheet " + sheetID + " have been removed");
            return Response.noContent().build();
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (NotAllowedException e1) {
                log.error("AuthCode was rejected - the databases are inconsistent", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: authorisation code rejected; databases inconsistent").build();
            } catch (ItemNotFoundException e1) {
                log.error("Something was not found that should have been found", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something was not found in the database that should have been").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (NotAllowedException e) {
            log.error("AuthCode was rejected - the databases are inconsistent", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: authorisation code rejected; databases inconsistent").build();
        } catch (ItemNotFoundException e) {
            log.error("Something was not found that should have been found", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: Something was not found in the database that should have been").build();
        }
    }
    
    /*
     * Allows the user of the given crsid to sign up for the given tick in the given group.
     */
    public Response allowSignup(String crsid, String groupID, String tickID) {
        String groupAuthCode = db.getAuthCode(groupID);
        try {
            if (service.getPermissions(groupID, crsid).containsKey(tickID)) {
                /* If they have already passed this tick, do nothing */
                return Response.ok().build();
            }
            Map<String, String> map = new HashMap<String, String>();
            map.put(tickID, null); // null means any ticker is allowed
            service.addPermissions(groupID, crsid, new AddPermissionsBean(map, groupAuthCode));
            log.info(crsid + " is now allowed to sign up for tick " + tickID + " in group " + groupID);
            return Response.ok().build();
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (NotAllowedException e1) {
                log.error("The databases are inconsistent - the group authorisation code was rejected", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The signups database is inconsistent with the main database").build();
            } catch (ItemNotFoundException e1) {
                log.error("The databases are inconsistent - the group should exist in the signups database", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The signups database is inconsistent with the main database").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (NotAllowedException e) {
            log.error("The databases are inconsistent - the group authorisation code was rejected", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The signups database is inconsistent with the main database").build();
        } catch (ItemNotFoundException e) {
            log.error("The databases are inconsistent - the group should exist in the signups database", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The signups database is inconsistent with the main database").build();
        }
    }
    
    /*
     * Prevent the user from signing up for the given tick in the given group.
     */
    public Response disallowSignup(String crsid, String groupID, String tickID) {
        log.info("Removing submitter " + crsid + "'s permission to sign up for tick" +
                tickID + " in group " + groupID);
        List<String> toRemove = new ArrayList<>();
        toRemove.add(tickID);
        try {
            service.removePermissions(groupID, crsid, new RemovePermissionsBean(toRemove, db.getAuthCode(groupID)));
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (NotAllowedException e1) {
                log.error("The databases are inconsistent - the group authorisation code was rejected", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The signups database is inconsistent with the main database").build();
            } catch (ItemNotFoundException e1) {
                log.error("The databases are inconsistent - the group should exist in the signups database", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The signups database is inconsistent with the main database").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (NotAllowedException e) {
            log.error("The databases are inconsistent - the group authorisation code was rejected", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The signups database is inconsistent with the main database").build();
        } catch (ItemNotFoundException e) {
            log.error("The databases are inconsistent - the group should exist in the signups database", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The signups database is inconsistent with the main database").build();
        }
        return Response.ok().build();
    }
    
    public boolean studentHasBookingForTick(String crsid, String tickID) {
        Date now = new Date();
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getStartTime().after(now) && slot.getComment().equals(tickID)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * If the current raven user is a marker, ensures that the given student is assigned the given ticker
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
        log.info("User " + callerCRSID + " is requesting for user " + crsid + " to be allowed " +
                "to sign up for tick " + tickID + " in group " + groupID + " using " +
                (ticker == null ? "any ticker" : "ticker " + ticker + " only"));
        if (!permissions.hasRole(callerCRSID, groupID, Role.MARKER)) {
            log.warn("User " + callerCRSID + " is not a marker in this group and so "
                    + "was forbidden from changing signup permissions");
            return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
        }
        return assignTickerForTickForUser(crsid, groupID, tickID, ticker);
        
    }
    
    /**
     * Ensures that the given student is assigned the given ticker
     * (if possible) in the future for the specified tick.
     */
    public Response assignTickerForTickForUser(String crsid, String groupID, String tickID, String ticker) {
        log.info("Attempting to allow submitter " + crsid +
                " to sign up for tick " + tickID + " in group " + groupID + " using " +
                (ticker == null ? "any ticker" : "ticker " + ticker + " only"));
        String groupAuthCode = db.getAuthCode(groupID);
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put(tickID, ticker);
            service.addPermissions(groupID, crsid, new AddPermissionsBean(map, groupAuthCode));
            log.info("Permissions updated");
            return Response.ok().build();
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (NotAllowedException e1) {
                log.error("The databases are inconsistent - the group authorisation code was rejected", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The signups database is inconsistent with the main database").build();
            } catch (ItemNotFoundException e1) {
                log.error("The databases are inconsistent - the group should exist in the signups database", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The signups database is inconsistent with the main database").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (NotAllowedException e) {
            log.error("The databases are inconsistent - the group authorisation code was rejected", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The signups database is inconsistent with the main database").build();
        } catch (ItemNotFoundException e) {
            log.error("The databases are inconsistent - the group should exist in the signups database", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The signups database is inconsistent with the main database").build();
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
        log.info("User " + crsid + " has requested the creation of a sheet for " +
                "the group of ID " + bean.getGroupID() + ". Parameters follow:\n" + bean.toString());
        /* Check calling user is an author in the group */
        if (!permissions.hasRole(crsid, bean.getGroupID(), Role.AUTHOR)) {
            log.warn("Sheet creation failed: The user " + crsid + " is not an author in the group");
            return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
        }
        int millisecondsInOneMinute = 60000;
        long sheetLengthInMinutes = (bean.getEndTime() - bean.getStartTime())/millisecondsInOneMinute;
        /* Check the sheet has a positive length */
        if (sheetLengthInMinutes <= 0) {
            log.info("Sheet creation failed: the end time must be after the start time.\n" + bean.toString());
            return Response.status(Status.BAD_REQUEST).entity("The end time must be after "
                    + "the start time").build();
        }
        /* 
         * Check the slot length cleanly divides the sheet length. We force this because it is simple
         * for the user to work out what the correct start and end times they really want are, and means
         * we don't have to assert that they want the number of slots rounding either down or up.
         */
        if (sheetLengthInMinutes % bean.getSlotLengthInMinutes() != 0) {
            log.info("Sheet creation failed: There must be an integer number of slots in the sheet\n" + bean.toString());
            return Response.status(Status.BAD_REQUEST).entity("The difference in minutes "
                    + "between the start and end times should be an integer multiple of "
                    + "the length of the slots").build();
        }
        /* 
         * I don't see why anyone would want this many slots in a session. If they're desperate, they can
         * create more than one session.
         */
        if (sheetLengthInMinutes/bean.getSlotLengthInMinutes() > 500) {
            log.info("Sheet creation failed: Too many slots would have been created\n" + bean.toString());
            return Response.status(Status.FORBIDDEN).entity("This sheet would have a silly "
                    + "number of slots if created.").build();
        }
        /* Create new empty sheet object */
        Sheet newSheet = new Sheet(bean.getTitle(), bean.getDescription(), bean.getLocation());
        String id;
        String auth;
        try {
            /* Insert sheet into signups database */
            SheetInfo info = service.addSheet(newSheet);
            id = info.getSheetID();
            auth = info.getAuthCode();
            /* 
             * Store sheet authorisation code for this sheet into our database.
             * This code is needed for doing privileged things to the sheet. 
             */
            db.addAuthCode(id, auth);
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (DuplicateNameException e1) {
                log.info("The sheet seems to already exist");
                return Response.serverError().entity("This sheet already seems to exist").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (DuplicateNameException e) {
            log.warn("The sheet seems to already exist\n" + bean.toString());
            return Response.serverError().entity("This sheet already seems to exist").build();
        }
        log.info("The empty sheet was created\n" + bean.toString());
        for (String ticker : bean.getTickerNames()) {
            try {
                /* For each given ticker, add a new column (populated with slots) to the sheet */
                service.createColumn(id, new CreateColumnBean(ticker, auth, new Date(bean.getStartTime()),
                        new Date(bean.getEndTime()), bean.getSlotLengthInMinutes()));
            } catch(InternalServerErrorException e) { // Ignore this block
                try {
                    throwRealException(e);
                    log.error("Something went wrong when processing the InternalServerErrorException", e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                } catch (ItemNotFoundException e1) {
                    log.error("The sheet or column was not found, but we are creating them", e1);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: The sheet or a column was not found, "
                                    + "after it should have been created").build();
                } catch (NotAllowedException e1) {
                    log.error("AuthCode was rejected - the databases are inconsistent", e1);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: authorisation code rejected; databases inconsistent").build();
                } catch (DuplicateNameException e1) {
                    log.warn("A duplicate name exception was encountered when creating a sheet and ignored "
                            + "because two identical columns were probably entered. If it was a duplicate " +
                            "slot, there's a serious problem", e1);
                } catch (Throwable t) {
                    log.error("Something went wrong when processing the InternalServerErrorException", t);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                }
            } catch (ItemNotFoundException e1) {
                log.error("The sheet or column was not found, but we are creating them", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The sheet or a column was not found, "
                                + "after it should have been created").build();
            } catch (NotAllowedException e1) {
                log.error("AuthCode was rejected - the databases are inconsistent", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: authorisation code rejected; databases inconsistent").build();
            } catch (DuplicateNameException e1) {
                log.warn("A duplicate name exception was encountered when creating a sheet and ignored "
                        + "because two identical columns were probably entered. If it was a duplicate " +
                        "slot, there's a serious problem", e1);
            }
        }
        log.info("The sheet was populated with tickers and slots\n" + bean.toString());
        try {
            /* Add the sheet to the group */
            service.addSheetToGroup(bean.getGroupID(),
                    new GroupSheetBean(id, db.getAuthCode(bean.getGroupID()), auth));
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.error("The databases are inconsistent - the group should exist in the signups database", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The signups database is inconsistent with the main database").build();
            } catch (NotAllowedException e1) {
                log.error("AuthCode was rejected - the databases are inconsistent", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: authorisation code rejected; databases inconsistent").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e1) {
            log.error("The databases are inconsistent - the group should exist in the signups database", e1);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The signups database is inconsistent with the main database").build();
        } catch (NotAllowedException e1) {
            log.error("AuthCode was rejected - the databases are inconsistent", e1);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: authorisation code rejected; databases inconsistent").build();
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
     * any bookings made at those times).
     */
    @POST
    @Path("/sheets/{sheetID}")
    @Consumes("application/json")
    public Response editSheet(@Context HttpServletRequest request,
            @PathParam("sheetID") String sheetID, SheetBean bean) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.info("User " + crsid + " is attempting to edit the sheet of ID " + sheetID
                + ". Parameters follow.\n" + bean.toString());
        synchronized (synchLock) { // better safe than sorry
            Sheet sheet;
            /* Check user is an author in the group, and get the sheet from the signups database */
            try {
                if (!permissions.hasRole(crsid, getGroupID(sheetID),
                        Role.AUTHOR)) {
                    log.warn("The user " + crsid
                            + " is not an author in the group "
                            + getGroupID(sheetID));
                    return Response.status(Status.FORBIDDEN)
                            .entity(Strings.INVALIDROLE).build();
                }
                sheet = service.getSheet(sheetID, db.getAuthCode(sheetID));
            } catch (InternalServerErrorException e0) {
                try {
                    throwRealException(e0);
                    log.error(
                            "Something went wrong when processing the InternalServerErrorException",
                            e0);
                    return Response
                            .status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                } catch (ItemNotFoundException e) {
                    log.warn("Sheet not found", e);
                    return Response.status(Status.NOT_FOUND)
                            .entity("The sheet was not found").build();
                } catch (NotAllowedException e1) {
                    log.error("AuthCode was rejected - the databases are inconsistent", e1);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: authorisation code rejected; databases inconsistent").build();
                } catch (Throwable t) {
                    log.error(
                            "Something went wrong when processing the InternalServerErrorException",
                            t);
                    return Response
                            .status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                }
            } catch (ItemNotFoundException e) {
                log.info("Sheet not found", e);
                return Response.status(Status.NOT_FOUND)
                        .entity("The sheet was not found").build();
            } catch (NotAllowedException e1) {
                log.error("AuthCode was rejected - the databases are inconsistent", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: authorisation code rejected; databases inconsistent").build();
            }
            int millisecondsInOneMinute = 60000;
            long sheetLengthInMinutes = (bean.getEndTime() - bean
                    .getStartTime()) / millisecondsInOneMinute;
            /* Check sheet length is positive */
            if (sheetLengthInMinutes <= 0) {
                log.info("Sheet editing failed: the end time must be after the start time.\n"
                        + bean.toString());
                return Response
                        .status(Status.BAD_REQUEST)
                        .entity("The end time must be after "
                                + "the start time").build();
            }
            /* Check new start time is an integer number of slot lengths from old start time */
            if ((sheet.getStartTime().getTime() - bean.getStartTime())
                    % sheet.getSlotLengthInMinutes() != 0) {
                log.info("The new start time must be an integer multiple of slot lengths away from the "
                        + "old start time of "
                        + sheet.getStartTime().toString());
                return Response
                        .status(Status.FORBIDDEN)
                        .entity("The new start time must be an integer "
                                + "multiple of slot lengths away from the "
                                + "old start time of "
                                + sheet.getStartTime().toString()).build();
            }
            /* Check new end time is an integer number of slot lengths from old end time */
            if ((sheet.getEndTime().getTime() - bean.getEndTime())
                    % sheet.getSlotLengthInMinutes() != 0) {
                log.info("The new end time must be an integer multiple of slot lengths away from the "
                        + "old end time of " + sheet.getEndTime().toString());
                return Response
                        .status(Status.FORBIDDEN)
                        .entity("The new end time must be an integer "
                                + "multiple of slot lengths away from the "
                                + "old end time of "
                                + sheet.getEndTime().toString()).build();
            }
            /* Check reasonable number of slots. If desperate, just create another sheet */
            if (sheetLengthInMinutes / bean.getSlotLengthInMinutes() > 500) {
                log.info("Too many slots would have been created");
                return Response
                        .status(Status.FORBIDDEN)
                        .entity("This sheet would have a silly "
                                + "number of slots if changed.").build();
            }
            List<Column> oldTickers = sheet.getColumns();
            List<String> oldTickerNames = new ArrayList<String>();
            for (Column oldTicker : oldTickers) {
                oldTickerNames.add(oldTicker.getName());
            }
            try {
                for (String newTicker : bean.getTickerNames()) { // add new tickers
                    if (!oldTickerNames.contains(newTicker)) {
                        service.createColumn(
                                sheetID,
                                new CreateColumnBean(newTicker, db
                                        .getAuthCode(sheetID), new Date(bean
                                        .getStartTime()), new Date(bean
                                        .getEndTime()), sheet
                                        .getSlotLengthInMinutes()));
                    }
                }
                for (String oldTicker : oldTickerNames) { // delete removed tickers
                    if (!bean.getTickerNames().contains(oldTicker)) {
                        service.deleteColumn(sheetID, oldTicker,
                                db.getAuthCode(sheetID));
                    }
                }
                service.updateSheetInfo(sheetID,
                        new UpdateSheetBean(bean.getTitle(), bean.getLocation(),
                                bean.getDescription(), db.getAuthCode(sheetID)));
            } catch (InternalServerErrorException e0) { // Ignore this block
                try {
                    throwRealException(e0);
                    log.error(
                            "Something went wrong when processing the InternalServerErrorException",
                            e0);
                    return Response
                            .status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                } catch (ItemNotFoundException e) {
                    log.error(
                            "The sheet was not found, while earlier in this method it was",
                            e);
                    return Response
                            .status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Internal Server Error: some changes not made")
                            .build();
                } catch (NotAllowedException e) {
                    log.error(
                            "The auth code stored was for some reason not correct",
                            e);
                    return Response
                            .status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Internal Server Error: some changes not made")
                            .build();
                } catch (DuplicateNameException e) {
                    log.error(
                            "A new ticker was found to have the same name as an old ticker, "
                                    + "even though new tickers are only created when there are no old ones"
                                    + "with that name", e);
                    return Response
                            .status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Internal Server Error: some changes not made")
                            .build();
                } catch (Throwable t) {
                    log.error(
                            "Something went wrong when processing the InternalServerErrorException",
                            t);
                    return Response
                            .status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                }
            } catch (ItemNotFoundException e) {
                log.error(
                        "The sheet was not found, while earlier in this method it was",
                        e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Internal Server Error: some changes not made")
                        .build();
            } catch (NotAllowedException e) {
                log.error(
                        "The auth code stored was for some reason not correct",
                        e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Internal Server Error: some changes not made")
                        .build();
            } catch (DuplicateNameException e) {
                log.error(
                        "A new ticker was found to have the same name as an old ticker, "
                                + "even though new tickers are only created when there are no old ones"
                                + "with that name", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Internal Server Error: some changes not made")
                        .build();
            }
            try {
                Date newStart = new Date(bean.getStartTime());
                Date newEnd = new Date(bean.getEndTime());
                Date currStart = sheet.getStartTime();
                Date currEnd = sheet.getEndTime();
                String authCode = db.getAuthCode(sheetID);
                if (newStart.after(currEnd) || newEnd.before(currStart)) { /* No existing slots will remain */
                    service.deleteSlotsAfter(sheetID, new BatchDeleteBean(0L,
                            authCode)); // Delete all slots
                    service.createSlotsForAllColumns(
                            sheetID, // And create the new ones
                            new BatchCreateBean(newStart.getTime(), newEnd
                                    .getTime(), sheet.getSlotLengthInMinutes(),
                                    authCode));
                } else { /* Some slots will remain - preserve them */
                    if (newStart.after(currStart)) { // need to delete slots from start
                        service.deleteSlotsBefore(sheetID, new BatchDeleteBean(
                                newStart.getTime(), authCode));
                    }
                    if (newEnd.before(currEnd)) { // need to delete slots from end
                        service.deleteSlotsAfter(sheetID, new BatchDeleteBean(
                                newEnd.getTime(), authCode));
                    }
                    if (newStart.before(currStart)) { //  need to add slots to start
                        service.createSlotsForAllColumns(
                                sheetID,
                                new BatchCreateBean(newStart.getTime(),
                                        currStart.getTime(), sheet
                                                .getSlotLengthInMinutes(),
                                        authCode));
                    }
                    if (newEnd.after(currEnd)) { // need to add slots to end
                        service.createSlotsForAllColumns(
                                sheetID,
                                new BatchCreateBean(currEnd.getTime(), newEnd
                                        .getTime(), sheet
                                        .getSlotLengthInMinutes(), authCode));
                    }
                }
            } catch (InternalServerErrorException e0) { // Ignore this block
                try {
                    throwRealException(e0);
                    log.error(
                            "Something went wrong when processing the InternalServerErrorException",
                            e0);
                    return Response
                            .status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                } catch (ItemNotFoundException e) {
                    log.error(
                            "The sheet was not found, while earlier in this method it was, "
                                    + "or something else that should have been found wasn't",
                            e);
                    return Response
                            .status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Internal Server Error: some changes not made")
                            .build();
                } catch (NotAllowedException e) {
                    log.error(
                            "The auth code stored was for some reason not correct",
                            e);
                    return Response
                            .status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Internal Server Error: some changes not made")
                            .build();
                } catch (DuplicateNameException e) {
                    log.error(
                            "Slots that should not already exist were found to exist",
                            e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Internal Server Error").build();
                } catch (Throwable t) {
                    log.error(
                            "Something went wrong when processing the InternalServerErrorException",
                            t);
                    return Response
                            .status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                }
            } catch (ItemNotFoundException e) {
                log.error(
                        "The sheet was not found, while earlier in this method it was, "
                                + "or something else that should have been found wasn't",
                        e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Internal Server Error: some changes not made")
                        .build();
            } catch (NotAllowedException e) {
                log.error(
                        "The auth code stored was for some reason not correct",
                        e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Internal Server Error: some changes not made")
                        .build();
            } catch (DuplicateNameException e) {
                log.error(
                        "Slots that should not already exist were found to exist",
                        e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Internal Server Error").build();
            }
            log.info("Sheet updated");
            return Response.ok().build();
        }
    }
    
    /**
     * Removes the given sheet from the database, and irreversibly loses
     * all information associated with it, including bookings.
     * @param request
     * @param sheetID
     * @return
     */
    @DELETE
    @Path("/sheets/{sheetID}")
    public Response deleteSheet(@Context HttpServletRequest request,
            @PathParam("sheetID") String sheetID) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.info("User " + crsid + " has requested the deletion of sheet of ID " + sheetID);
        try {
            if (!permissions.hasRole(crsid, getGroupID(sheetID), Role.AUTHOR)) {
                log.warn("The user " + crsid + " is not an author in the group " + getGroupID(sheetID));
                return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
            }
            service.deleteSheet(sheetID, db.getAuthCode(sheetID));
            log.info("Sheet deleted");
            return Response.noContent().build();
        } catch(InternalServerErrorException e) { // Ignore this block
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.warn("The sheet of ID " + sheetID + " was not found", e1);
                return Response.status(Status.NOT_FOUND).entity("The given sheet was not found.").build();
            } catch (NotAllowedException e1) {
                log.error("AuthCode was rejected - the databases are inconsistent", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: authorisation code rejected; databases inconsistent").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e1) {
            log.warn("The sheet of ID " + sheetID + " was not found", e1);
            return Response.status(Status.NOT_FOUND).entity("The given sheet was not found.").build();
        } catch (NotAllowedException e1) {
            log.error("AuthCode was rejected - the databases are inconsistent", e1);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: authorisation code rejected; databases inconsistent").build();
        }
        
    }

    /**
     * Creates a group with the given ID in the signups database.
     * Also adds the new authorisation code to the front-end database.
     * @param groupID
     * @throws DuplicateNameException
     */
    public void createGroup(String groupID) throws DuplicateNameException {
        log.info("Creating new group in signups database with ID " + groupID);
        /* Create group in signups database */
        String groupAuthCode = service.addGroup(new Group(groupID));
        /* Store group authorisation code so we can have admin privileges for it */
        db.addAuthCode(groupID, groupAuthCode);
        log.info("Group created");
    }
    
    public void deleteGroup(String groupID) {
        log.info("Deleting all sheets belonging to group of ID " + groupID);
        try {
            for (Sheet sheet : service.listSheets(groupID, db.getAuthCode(groupID))) {
                /* Delete all signups sheets belonging to group */
                String id = sheet.get_id();
                try {
                    service.deleteSheet(id, db.getAuthCode(id));
                    db.removeAuthCodeCorrespondingTo(id);
                } catch (ItemNotFoundException e) {
                    log.error("A sheet was not found, although it was retrieved from a list of sheets");
                    // Do nothing though, because it doesn't exist, which is what was wanted
                } catch (NotAllowedException e) {
                    log.error("There was an inconsitency in the databases - the authCode was found to "
                            + "be incorrect");
                }
            }
            try {
                /* Delete group itself */
                service.deleteGroup(groupID, db.getAuthCode(groupID));
            } catch (NotAllowedException e) {
                log.error("There was an inconsitency in the databases - the authCode was found to "
                        + "be incorrect");
            }
            db.removeAuthCodeCorrespondingTo(groupID);
        } catch (ItemNotFoundException e) {
            log.error("The group was not found in the signups database even though it "
                    + "shouldn't have been deleted yet");
        } catch (NotAllowedException e1) {
            log.error("AuthCode was rejected - the databases are inconsistent", e1);
        }
        
    }
    
    public String getGroupID(String sheetID) throws ItemNotFoundException {
        List<String> groupIDs = service.getGroupIDs(sheetID);
        if (groupIDs.size() != 1) {
            log.error("There should be precisely one group associated with this "
                    + "sheet (" + sheetID + ", but there seems to be " + groupIDs.size() +
                    ". This should be impossible.");
            throw new RuntimeException("There should be precisely one group associated "
                            + "with this sheet, but there seems to be " + groupIDs.size());
        }
        return groupIDs.get(0);
    }

    /**
     * Extracts the real exception from the InternalServerErrorException passed to it, and
     * throws it.
     * @param e The wrapping exception
     * @throws Throwable The exception really desired
     */
    private void throwRealException(InternalServerErrorException e) throws Throwable {
        /* Extract SerializableException from the InternalServerErrorException */
        RemoteFailureHandler h = new RemoteFailureHandler();
        SerializableException s = h.readException(e);
        /* Extract class of the real exception */
        @SuppressWarnings("unchecked") // We know it must be throwable - it is an exception!
        Class<? extends Throwable> clazz = (Class<? extends Throwable>) Class.forName(s.getClassName());
        /* Get the constructor for the exception which takes a single string */
        Constructor<? extends Throwable> ctor = clazz.getConstructor(String.class);
        /* Construct instance of exception using the constructor */
        Throwable toThrow = ctor.newInstance(new Object[] { s.getMessage() });
        /* Throw the exception */
        throw toThrow;
    }
    
}
