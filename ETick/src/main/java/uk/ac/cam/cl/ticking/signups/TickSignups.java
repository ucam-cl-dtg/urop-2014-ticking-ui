package uk.ac.cam.cl.ticking.signups;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
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

/*
 * VERY IMPORTANT
 * If you want to understand the code, it will be MUCH easier if you simply ignore
 * all blocks which begin by catching an InternalServerErrorException, and the
 * execution will run as intended. Catching InternalServerErrorExceptions is
 * necessary because of a bug with the exception-chains, which forces me to get
 * the InternalServerErrorException and work out what kind of exception it actually
 * is. Although I handle the different cases within the InternalServerErrorException
 * block, this handling is replicated outside the block, both to satisfy the compiler
 * and in case the bug is fixed.
 */


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
            log.info("Listing available times using the following parameters...");
            String groupID = getGroupID(sheetID);
            log.info("crsid: " + crsid);
            log.info("tickID: " + tickID);
            log.info("groupID: " + groupID);
            log.info("sheetID: " + sheetID);
            
            /* Convert all of the datetimes out of UTC before passing them on */
            List<Date> slots = service.listAllFreeStartTimes(crsid, tickID,
                    groupID, sheetID);
            List<Date> convertedSlots = new ArrayList<>();
            for (Date date : slots) {
                convertedSlots.add(convertToAssumedGMTXFromUTC(date));
            }
            return Response.ok(convertedSlots).build();
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.info("Either the sheet was not found or something has gone very wrong", e1);
                return Response.status(Status.NOT_FOUND).entity("Not Found Error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.info("Either the sheet was not found or something has gone very wrong", e);
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
        
        /* Convert the bean starttime to UTC */
        bean.setStartTime(convertToUTCViaAssumedGMTX(bean.getStartTime()));
        
        try {
            groupID = getGroupID(sheetID);
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.info("User " + crsid + " tried to book a slot but the sheet given was " +
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
            log.info("User " + crsid + " tried to book a slot but the sheet given was " +
                    "not found in the signups database");
            return Response.status(Status.NOT_FOUND)
                    .entity("The sheet was not found in the signups database").build();
        }
        log.info("Attempting to book slot for user " + crsid + " for tickID " + bean.getTickID() +
                " at time " + new Date(bean.getStartTime()) + " on sheet " + sheetID + " in group " + groupID);
        Date now = new Date();
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getStartTime().equals(bean.getStartTime())) {
                log.info("The user already had a slot booked at the given time");
                return Response.status(Status.FORBIDDEN)
                        .entity(Strings.EXISTINGTIMEBOOKING).build();
            }
            if (slot.getStartTime().after(now) && slot.getComment().equals(bean.getTickID())) {
                log.info("The user already had a slot booked for the given tick");
                return Response.status(Status.FORBIDDEN)
                        .entity(Strings.EXISTINGTICKBOOKING).build();
            }
        }
        try {
            if (service.listColumnsWithFreeSlotsAt(sheetID, bean.getStartTime()).size() == 0) {
                log.info("No free slots were found at the given time");
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
                log.info("The booking was successfully made");
                return Response.ok().entity(ticker).build();
            } else {
                return Response.status(Status.FORBIDDEN)
                        .entity("Error: you do not have permission to book this slot - perhaps you have not " +
                                "passed the unit tests").build();
            }
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.info("Something was not found the in database", e1);
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
            log.info("Something was not found the in database", e);
            return Response.status(Status.NOT_FOUND)
                    .entity("Not found error: " + e.getMessage()).build();
        } catch (NotAllowedException e) {
            log.info("Permission to book the slot was denied", e);
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
        log.info("The user " + crsid + " is trying to unbook their slot for tick " + tickID);
        Slot booking = null;
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getComment().equals(tickID)) { // the comment stored in the slot in the generic signups database is the tickID
                booking = slot;
            }
        }
        if (booking == null) {
            log.info("No booking was found for the specified tick");
            return Response.status(Status.NOT_FOUND).entity("Error: no booking was found for this tick").build();
        }
        try {
            service.book(booking.getSheetID(), booking.getColumnName(),
                    booking.getStartTime().getTime(), new SlotBookingBean(crsid, null, null));
            Fork f = db.getFork(Fork.generateForkId(crsid, tickID));
            f.setSignedUp(false);
            db.saveFork(f);
            log.info("The slot was successfully unbooked");
            return Response.ok().build();
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.error("The booking for the tick was found to simultaneously exist and not exist", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The booking for this tick was found to exist and "
                                + "then not found to exist. Seek help, something went very wrong.").build();
            } catch (NotAllowedException e1) {
                log.error("The unbooking should have been allowed but was not", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The removal of the booking should have been allowed but "
                                + "for some reason was not. Seek help.").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.error("The booking for the tick was found to simultaneously exist and not exist", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The booking for this tick was found to exist and "
                            + "then not found to exist. Seek help, something went very wrong.").build();
        } catch (NotAllowedException e) {
            log.error("The unbooking should have been allowed but was not", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Server Error: The removal of the booking should have been allowed but "
                            + "for some reason was not. Seek help.").build();
        }
    }
    
    @DELETE
    @Path("/students/{crsid}/ticks/{tickID}")
    public Response tickerUnbookSlot(@Context HttpServletRequest request,
            @PathParam("crsid") String crsid, @PathParam("tickID") String tickID) {
        String callingCrsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.info("The user " + crsid + " is trying to unbook the submitter " +
                crsid + "'s booking for tick " + tickID);
        Slot booking = null;
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getComment().equals(tickID)) {
                booking = slot;
            }
        }
        if (booking == null) {
            log.info("No booking was found for the specified tick");
            return Response.status(Status.NOT_FOUND).entity("No booking was found for this tick").build();
        }
        try {
            if (!db.getRoles(getGroupID(booking.getSheetID()), callingCrsid).contains(Role.MARKER)) {
                log.info("The user " + callingCrsid + " is not a marker in the group");
                return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
            }
            service.book(booking.getSheetID(), booking.getColumnName(),
                    booking.getStartTime().getTime(), new SlotBookingBean(crsid, null, null));
            Fork f = db.getFork(Fork.generateForkId(crsid, tickID));
            f.setSignedUp(false);
            db.saveFork(f);
            log.info("The slot was successfully unbooked");
            return Response.ok().build();
        } catch(InternalServerErrorException e) {
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
    
    public Response unbookSlot(String crsid, String tickID) {
        Slot booking = null;
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getComment().equals(tickID)) {
                booking = slot;
            }
        }
        if (booking == null) {
            log.info("No booking was found for the specified tick");
            return Response.status(Status.NOT_FOUND).entity("No booking was found for this tick").build();
        }
        try {
            service.book(booking.getSheetID(), booking.getColumnName(),
                    booking.getStartTime().getTime(), new SlotBookingBean(crsid, null, null));
            Fork f = db.getFork(Fork.generateForkId(crsid, tickID));
            f.setSignedUp(false);
            db.saveFork(f);
            log.info("The slot was successfully unbooked");
            return Response.ok().build();
        } catch(InternalServerErrorException e) {
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
     * Returns a list of the bookings in the future made by one user.
     */
    @GET
    @Path("/bookings")
    @Produces("application/json")
    public Response listStudentBookings(@Context HttpServletRequest request) {
        String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");
        log.info("Listing the future bookings for user" + crsid);
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
                } catch(InternalServerErrorException e) {
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
                /* Convert the starttime to GMTX */
                BookingInfo info = new BookingInfo(s, groupName);
                info.setStartTime(convertToAssumedGMTXFromUTC(info
                        .getStartTime()));
                toReturn.add(info);
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
            List<Sheet> sheets = service.listSheets(groupID);
            for (Sheet sheet : sheets) {
                /* Convert the starttime to GMTX */
                sheet.setStartTime(convertToAssumedGMTXFromUTC(sheet
                        .getStartTime()));
                sheet.setEndTime(convertToAssumedGMTXFromUTC(sheet.getEndTime()));
            }
            return Response.ok(sheets).build();
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.warn("Probably the group was not found", e1);
                return Response.status(Status.NOT_FOUND).entity("Not found error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.warn("Probably the group was not found", e);
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
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.warn("Probably the sheet was not found", e1);
                return Response.status(Status.NOT_FOUND).entity("Not found error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.warn("Probably the sheet was not found", e);
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
            List<Slot> slots = service.listColumnSlots(sheetID, tickerName);
            List<Slot> convertedSlots = new ArrayList<>();
            for (Slot slot : slots) {
                /*Convert to GMTX*/
                Slot converted = new Slot(slot.getSheetID(),
                        slot.getColumnName(),
                        convertToAssumedGMTXFromUTC(slot.getStartTime()),
                        slot.getDuration(), slot.getBookedUser(), slot.getComment());
                convertedSlots.add(converted);
            }
            return Response.ok(convertedSlots).build();
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.warn("Probably either the sheet or column was not found", e1);
                return Response.status(Status.NOT_FOUND).entity("Not found error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.warn("Probably either the sheet or column was not found", e);
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
        /*Convert to UTC*/
        startTime = convertToUTCViaAssumedGMTX(startTime);
        try {
            return Response.ok(service.showBooking(sheetID, tickerName, startTime.getTime())).build();
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.warn("The booking was not found", e1);
                return Response.status(Status.NOT_FOUND).entity("Not found error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.warn("The booking was not found", e);
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
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.info("The sheet of ID " + sheetID + " was not found", e1);
                return Response.status(Status.NOT_FOUND)
                        .entity("Not found error: the sheet " + sheetID + "was not found").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.info("The sheet of ID " + sheetID + " was not found", e);
            return Response.status(Status.NOT_FOUND)
                    .entity("Not found error: the sheet " + sheetID + "was not found").build();
        }
        if (!db.getRoles(groupID, callingCRSID).contains(Role.MARKER)) {
            log.info("The user " + callingCRSID + " does not have permission to remove these bookings");
            return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
        }
        try {
            for (Slot slot : service.listUserSlots(crsid)) {
                Fork f = db.getFork(Fork.generateForkId(crsid, slot.getComment()));
                f.setSignedUp(false);
                db.saveFork(f);
            }
            service.removeAllUserBookings(sheetID, crsid, db.getAuthCode(sheetID));
            log.info("All future bookings of submitter " + crsid + " for sheet " + sheetID + " have been removed");
            return Response.ok().build();
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (NotAllowedException e1) {
                log.info("Action was not allowed", e1);
                return Response.status(Status.FORBIDDEN).entity("Error: " + e1.getMessage()).build();
            } catch (ItemNotFoundException e1) {
                log.info("Something was not found", e1);
                return Response.status(Status.NOT_FOUND).entity("Not found error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (NotAllowedException e) {
            log.info("Action was not allowed", e);
            return Response.status(Status.FORBIDDEN).entity("Error: " + e.getMessage()).build();
        } catch (ItemNotFoundException e) {
            log.info("Something was not found", e);
            return Response.status(Status.NOT_FOUND).entity("Not found error: " + e.getMessage()).build();
        }
    }
        
    public Response allowSignup(String crsid, String groupID, String tickID) { // TODO: only if they haven't already passed...
        log.info("Attempting to allow submitter " + crsid +
                " to sign up for the tick " + tickID + " in group " + groupID);
        try {
            service.listSheets(groupID); // to see if group exists
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException f) { // if it doesn't, create it
                log.info("The group for some reason does not exist in the signups database - " + 
                        "attempting to create it");
                try {
                    createGroup(groupID);
                } catch(InternalServerErrorException e1) { // this means stuff has gone seriously wrong
                    try {
                        throwRealException(e1);
                        log.error("Something went wrong when processing the InternalServerErrorException", e1);
                        return Response.status(Status.INTERNAL_SERVER_ERROR)
                                .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                                .build();
                    } catch (DuplicateNameException e2) {
                        log.error("When creating the group because it doesn't exist, it was " +
                                "found to exist", e2);
                        return Response.status(Status.INTERNAL_SERVER_ERROR)
                                .entity("The group was found to both exist and not exist "
                                        + "in the signups database, sorry.\n"+e2).build();
                    } catch (Throwable t) {
                        log.error("Something went wrong when processing the InternalServerErrorException", t);
                        return Response.status(Status.INTERNAL_SERVER_ERROR)
                                .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                                .build();
                    }
                } catch (DuplicateNameException e1) {
                    log.error("When creating the group because it doesn't exist, it was " +
                            "found to exist", e1);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("The group was found to both exist and not exist "
                                    + "in the signups database, sorry.\n"+e1).build();
                }
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) { // if it doesn't, create it
            log.info("The group for some reason does not exist in the signups database - " + 
                    "attempting to create it");
            try {
                createGroup(groupID);
            } catch(InternalServerErrorException e1) {
                try {
                    throwRealException(e1);
                    log.error("Something went wrong when processing the InternalServerErrorException", e1);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                } catch (DuplicateNameException e2) {
                    log.error("When creating the group because it doesn't exist, it was " +
                            "found to exist", e2);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("The group was found to both exist and not exist "
                                    + "in the signups database, sorry.\n"+e2).build();
                } catch (Throwable t) {
                    log.error("Something went wrong when processing the InternalServerErrorException", t);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                }
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
            log.info(crsid + " is now allowed to sign up for " + tickID + " in group " + groupID);
            return Response.ok().build();
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (NotAllowedException e1) {
                log.info("Permission was denied", e1);
                return Response.status(Status.FORBIDDEN)
                        .entity("Not allowed error: " + e1.getMessage()).build();
            } catch (ItemNotFoundException e1) {
                log.info("Something was not found", e1);
                return Response.status(Status.NOT_FOUND)
                        .entity("Not found error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (NotAllowedException e) {
            log.info("Permission was denied", e);
            return Response.status(Status.FORBIDDEN)
                    .entity("Not allowed error: " + e.getMessage()).build();
        } catch (ItemNotFoundException e) {
            log.info("Something was not found", e);
            return Response.status(Status.NOT_FOUND)
                    .entity("Not found error: " + e.getMessage()).build();
        }
    }
    
    public Response disallowSignup(String crsid, String groupID, String tickID) {
        log.info("Removing submitter " + crsid + "'s permission to sign up for tick" +
                tickID + " in group " + groupID);
        Map<String, String> map = new HashMap<String, String>();
        map.put(tickID, null);
        try {
            service.removePermissions(groupID, crsid, new PermissionsBean(map, db.getAuthCode(groupID)));
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (NotAllowedException e1) {
                log.error("Permission was denied - it shouldn't be", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Permission was denied - it shouldn't be.").build();
            } catch (ItemNotFoundException e1) {
                log.error("Something was not found that should have been", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Internal server error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (NotAllowedException e) {
            log.error("Permission was denied - it shouldn't be", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Permission was denied - it shouldn't be.\n" + e).build();
        } catch (ItemNotFoundException e) {
            log.error("Something was not found that should have been", e);
            return Response.status(Status.INTERNAL_SERVER_ERROR)
                    .entity("Internal server error: " + e.getMessage()).build();
        }
        log.info("Permission removed");
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
        log.info("User " + callerCRSID + " is requesting for submitter " + crsid + " to be allowed " +
                "to sign up for tick " + tickID + " in group " + groupID + " using " +
                (ticker == null ? "any ticker" : "ticker " + ticker + " only"));
        if (!db.getRoles(groupID, callerCRSID).contains(Role.MARKER)) {
            log.info("User " + callerCRSID + " does not have permission to do this");
            return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
        }
        String groupAuthCode = db.getAuthCode(groupID);
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put(tickID, ticker);
            service.addPermissions(groupID, crsid, new PermissionsBean(map, groupAuthCode));
            log.info("Permissions updated");
            return Response.ok().build();
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (NotAllowedException e1) {
                log.info("Permission denied", e1);
                return Response.status(Status.FORBIDDEN)
                        .entity("Not allowed error: " + e1.getMessage()).build();
            } catch (ItemNotFoundException e1) {
                log.info("Something was not found", e1);
                return Response.status(Status.NOT_FOUND)
                        .entity("Not found error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (NotAllowedException e) {
            log.info("Permission denied", e);
            return Response.status(Status.FORBIDDEN)
                    .entity("Not allowed: " + e.getMessage()).build();
        } catch (ItemNotFoundException e) {
            log.info("Something was not found", e);
            return Response.status(Status.NOT_FOUND)
                    .entity("Not found error: " + e.getMessage()).build();
        }
    }
    
    public Response assignTickerForTickForUser(String crsid, String groupID, String tickID, String ticker) {
        log.info("Attempting to allow submitter " + crsid +
                " to sign up for tick " + tickID + " in group " + groupID + " using " +
                (ticker == null ? "any ticker" : "ticker " + ticker + " only"));
        String groupAuthCode = db.getAuthCode(groupID);
        try {
            Map<String, String> map = new HashMap<String, String>();
            map.put(tickID, ticker);
            service.addPermissions(groupID, crsid, new PermissionsBean(map, groupAuthCode));
            log.info("Permissions updated");
            return Response.ok().build();
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (NotAllowedException e1) {
                log.info("Permission denied", e1);
                return Response.status(Status.FORBIDDEN)
                        .entity("Not allowed: " + e1.getMessage()).build();
            } catch (ItemNotFoundException e1) {
                log.info("Something was not found", e1);
                return Response.status(Status.NOT_FOUND)
                        .entity("Not found error: " + e1.getMessage()).build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (NotAllowedException e) {
            log.info("Permission denied", e);
            return Response.status(Status.FORBIDDEN)
                    .entity("Not allowed: " + e.getMessage()).build();
        } catch (ItemNotFoundException e) {
            log.info("Something was not found", e);
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
        log.info("User " + crsid + " has requested the creation of a sheet for " +
                "the group of ID " + bean.getGroupID());
        
        /*Convert to UTC*/
        bean.setStartTime(convertToUTCViaAssumedGMTX(bean.getStartTime()));
        bean.setEndTime(convertToUTCViaAssumedGMTX(bean.getEndTime()));
        
        if (!db.getRoles(bean.getGroupID(), crsid).contains(Role.AUTHOR)) {
            log.info("The user " + crsid + " in not an author in the group");
            return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
        }
        long sheetLengthInMinutes = (bean.getEndTime() - bean.getStartTime())/60000;
        if (sheetLengthInMinutes <= 0) {
            log.info("The end time must be after the start time");
            return Response.status(Status.BAD_REQUEST).entity("The end time must be after "
                    + "the start time").build();
        }
        if (sheetLengthInMinutes % bean.getSlotLengthInMinutes() != 0) {
            log.info("There must be an integer number of slots in the sheet");
            return Response.status(Status.BAD_REQUEST).entity("The difference in minutes "
                    + "between the start and end times should be an integer multiple of "
                    + "the length of the slots").build();
        }
        if (sheetLengthInMinutes/bean.getSlotLengthInMinutes() > 500) {
            log.info("Too many slots would have been created");
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
        } catch(InternalServerErrorException e) {
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
            log.info("The sheet seems to already exist");
            return Response.serverError().entity("This sheet already seems to exist").build();
        }
        log.info("The empty sheet was created");
        for (String ticker : bean.getTickerNames()) {
            try {
                service.createColumn(id, new CreateColumnBean(ticker, auth, new Date(bean.getStartTime()),
                        new Date(bean.getEndTime()), bean.getSlotLengthInMinutes()));
            } catch(InternalServerErrorException e) {
                try {
                    throwRealException(e);
                    log.error("Something went wrong when processing the InternalServerErrorException", e);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                } catch (ItemNotFoundException e1) {
                    log.error("The sheet or column was not found, but we are creating them", e1);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: The sheet or column was not found, but we are creating them").build();
                } catch (NotAllowedException e1) {
                    log.error("Permission was denied, it should not have been", e1);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Permission to the sheet was denied, it should not have been").build();
                } catch (DuplicateNameException e1) {
                    log.warn("A duplicate name exception was encountered when creating a sheet" +
                            "and ignored because " +
                            "two identical columns were probably entered. If it was a duplicate " +
                            "slot, there's some problem.", e1);
                } catch (Throwable t) {
                    log.error("Something went wrong when processing the InternalServerErrorException", t);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                }
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
        log.info("The sheet was populated with tickers and slots");
        try {
            service.addSheetToGroup(bean.getGroupID(),
                    new GroupSheetBean(id, db.getAuthCode(bean.getGroupID()), auth));
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException ee) { // group doesn't yet exist: create and retry
                log.info("The given group was not found in the signups database - attempting to create it");
                try {
                    createGroup(bean.getGroupID());
                    service.addSheetToGroup(bean.getGroupID(),
                            new GroupSheetBean(id, db.getAuthCode(bean.getGroupID()), auth));
                } catch(InternalServerErrorException e0) {
                    try {
                        throwRealException(e0);
                        log.error("Something went wrong when processing the InternalServerErrorException", e0);
                        return Response.status(Status.INTERNAL_SERVER_ERROR)
                                .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                                .build();
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
                                        + "attempted to create it, but it still wasn't found.").build();
                    } catch (NotAllowedException e1) {
                        log.error("The auth codes were found to be incorrect", e1);
                        return Response.status(Status.INTERNAL_SERVER_ERROR)
                                .entity("Server Error: The group was not found in the signups database, "
                                        + "we attempted to create it, but permission was not "
                                        + "given - it should have been").build();
                    } catch (Throwable t) {
                        log.error("Something went wrong when processing the InternalServerErrorException", t);
                        return Response.status(Status.INTERNAL_SERVER_ERROR)
                                .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                                .build();
                    }
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
            } catch (NotAllowedException e1) {
                log.error("The auth codes stored in the " +
                        "database were not found to match those in the signups database", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: The authorisation codes stored in the " +
                                "database were not found to match those in the signups database").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) { // group doesn't yet exist: create and retry
            log.info("The given group was not found in the signups database - attempting to create it");
            try {
                createGroup(bean.getGroupID());
                service.addSheetToGroup(bean.getGroupID(),
                        new GroupSheetBean(id, db.getAuthCode(bean.getGroupID()), auth));
            } catch(InternalServerErrorException e0) {
                try {
                    throwRealException(e0);
                    log.error("Something went wrong when processing the InternalServerErrorException", e0);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
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
                                    + "attempted to create it, but it still wasn't found.").build();
                } catch (NotAllowedException e1) {
                    log.error("The auth codes were found to be incorrect", e1);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: The group was not found in the signups database, "
                                    + "we attempted to create it, but permission was not "
                                    + "given - it should have been").build();
                } catch (Throwable t) {
                    log.error("Something went wrong when processing the InternalServerErrorException", t);
                    return Response.status(Status.INTERNAL_SERVER_ERROR)
                            .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                            .build();
                }
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
        log.info("User " + crsid + " is attempting to edit the sheet of ID " + sheetID);
        
        /*Convert to UTC*/
        bean.setStartTime(convertToUTCViaAssumedGMTX(bean.getStartTime()));
        bean.setEndTime(convertToUTCViaAssumedGMTX(bean.getEndTime()));
        
        Sheet sheet;
        try {
            if (!db.getRoles(getGroupID(sheetID), crsid).contains(Role.AUTHOR)) {
                log.info("The user " + crsid + " is not an author in the group " + getGroupID(sheetID));
                return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
            }
            sheet = service.getSheet(sheetID);
        } catch(InternalServerErrorException e0) {
            try {
                throwRealException(e0);
                log.error("Something went wrong when processing the InternalServerErrorException", e0);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e) {
                log.info("Sheet not found", e);
                return Response.status(Status.NOT_FOUND).entity("The sheet was not found").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e) {
            log.info("Sheet not found", e);
            return Response.status(Status.NOT_FOUND).entity("The sheet was not found").build();
        }
        if ((sheet.getStartTime().getTime() - bean.getStartTime()) % sheet.getSlotLengthInMinutes() != 0) {
            log.info("The new start time must be an integer multiple of slot lengths away from the " +
                    "old start time of " + sheet.getStartTime().toString());
            return Response.status(Status.FORBIDDEN).entity("The new start time but be an integer "
                    + "multiple of slot lengths away from the " +
                    "old start time of " + sheet.getStartTime().toString()).build();
        }
        if ((sheet.getEndTime().getTime() - bean.getEndTime()) % sheet.getSlotLengthInMinutes() != 0) {
            log.info("The new end time must be an integer multiple of slot lengths away from the " +
                    "old end time of " + sheet.getEndTime().toString());
            return Response.status(Status.FORBIDDEN).entity("The new end time but be an integer "
                    + "multiple of slot lengths away from the " +
                    "old end time of " + sheet.getEndTime().toString()).build();
        }
        long sheetLengthInMinutes = (bean.getEndTime() - bean.getStartTime())/60000;
        if (sheetLengthInMinutes/bean.getSlotLengthInMinutes() > 500) {
            log.info("Too many slots would have been created");
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
                            new Date(bean.getEndTime()), sheet.getSlotLengthInMinutes()));
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
        } catch(InternalServerErrorException e0) {
            try {
                throwRealException(e0);
                log.error("Something went wrong when processing the InternalServerErrorException", e0);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
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
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
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
            Date newStart = new Date(bean.getStartTime());
            Date newEnd = new Date(bean.getEndTime());
            Date currStart = sheet.getStartTime();
            Date currEnd = sheet.getEndTime();
            String authCode = db.getAuthCode(sheetID);
            if (newStart.after(currEnd) || newEnd.before(currStart)) { /* No existing slots will remain */
                service.deleteSlotsAfter(sheetID, new BatchDeleteBean(0L, authCode)); // Delete all slots
                service.createSlotsForAllColumns(sheetID, // And create the new ones
                        new BatchCreateBean(newStart.getTime(), newEnd.getTime(),
                                sheet.getSlotLengthInMinutes(), authCode));
            } else { /* Some slots will remain - preserve them */
                if (newStart.after(currStart)) { // need to delete slots from start
                    service.deleteSlotsBefore(sheetID,
                            new BatchDeleteBean(newStart.getTime(), authCode));
                }
                if (newEnd.before(currEnd)) { // need to delete slots from end
                    service.deleteSlotsAfter(sheetID,
                            new BatchDeleteBean(newEnd.getTime(), authCode));
                }
                if (newStart.before(currStart)) { //  need to add slots to start
                    service.createSlotsForAllColumns(sheetID,
                            new BatchCreateBean(newStart.getTime(), currStart.getTime(),
                                    sheet.getSlotLengthInMinutes(), authCode));
                }
                if (newEnd.after(currEnd)) { // need to add slots to end
                    service.createSlotsForAllColumns(sheetID,
                            new BatchCreateBean(currEnd.getTime(), newEnd.getTime(),
                                    sheet.getSlotLengthInMinutes(), authCode));
                }
            }
        } catch(InternalServerErrorException e0) {
            try {
                throwRealException(e0);
                log.error("Something went wrong when processing the InternalServerErrorException", e0);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
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
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
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
        log.info("Sheet updated");
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
        log.info("User " + crsid + " has requested the deletion of sheet of ID " + sheetID);
        try {
            if (!db.getRoles(getGroupID(sheetID), crsid).contains(Role.AUTHOR)) {
                log.info("The user " + crsid + " is not an author in the group " + getGroupID(sheetID));
                return Response.status(Status.FORBIDDEN).entity(Strings.INVALIDROLE).build();
            }
            service.deleteSheet(sheetID, db.getAuthCode(sheetID));
            log.info("Sheet deleted");
            return Response.ok().build();
        } catch(InternalServerErrorException e) {
            try {
                throwRealException(e);
                log.error("Something went wrong when processing the InternalServerErrorException", e);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            } catch (ItemNotFoundException e1) {
                log.info("The sheet of ID " + sheetID + " was not found", e1);
                return Response.status(Status.NOT_FOUND).entity("The given sheet was not found.").build();
            } catch (NotAllowedException e1) {
                log.error("The auth code for the sheet was found to be incorrect", e1);
                return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Server Error: "
                        + "the authCode seems to be wrong, but it never should be.").build();
            } catch (Throwable t) {
                log.error("Something went wrong when processing the InternalServerErrorException", t);
                return Response.status(Status.INTERNAL_SERVER_ERROR)
                        .entity("Server Error: Something went wrong when processing the InternalServerErrorException")
                        .build();
            }
        } catch (ItemNotFoundException e1) {
            log.info("The sheet of ID " + sheetID + " was not found", e1);
            return Response.status(Status.NOT_FOUND).entity("The given sheet was not found.").build();
        } catch (NotAllowedException e1) {
            log.error("The auth code for the sheet was found to be incorrect", e1);
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Server Error: "
                    + "the authCode seems to be wrong, but it never should be.").build();
        }
        
    }

    public void createGroup(String groupID) throws DuplicateNameException {
        log.info("Creating new group in signups database with ID " + groupID);
        String groupAuthCode = service.addGroup(new Group(groupID));
        db.addAuthCode(groupID, groupAuthCode);
        log.info("Group created");
    }
    
    public String getGroupID(String sheetID) throws ItemNotFoundException {
        List<String> groupIDs = service.getGroupIDs(sheetID);
        if (groupIDs.size() != 1) {
            log.error("There should be precisely one group associated with this "
                    + "sheet (" + sheetID + ", but there seems to be " + groupIDs.size());
            throw new RuntimeException("There should be precisely one group associated "
                            + "with this sheet, but there seems to be " + groupIDs.size());
        }
        return groupIDs.get(0);
    }
    
    private Date convertToUTCViaAssumedGMTX(Date date) {
        DateTime input = new DateTime(date);
        DateTime gmtx = input.withZoneRetainFields(DateTimeZone.getDefault());
        DateTime utc = gmtx.withZone(DateTimeZone.UTC);

        return utc.toDate();

    }

    private Date convertToAssumedGMTXFromUTC(Date date) {
        DateTime input = new DateTime(date);
        DateTime gmtx = input.withZone(DateTimeZone.getDefault());

        return gmtx.toDate();
    }

    private Long convertToUTCViaAssumedGMTX(Long date) {
        DateTime input = new DateTime(date);
        DateTime gmtx = input.withZoneRetainFields(DateTimeZone.getDefault());
        DateTime utc = gmtx.withZone(DateTimeZone.UTC);

        return utc.getMillis();

    }

    private Long convertToAssumedGMTXFromUTC(Long date) {
        DateTime input = new DateTime(date);
        DateTime gmtx = input.withZone(DateTimeZone.getDefault());

        return gmtx.getMillis();
    }
    
    private void throwRealException(InternalServerErrorException e) throws Throwable {
        RemoteFailureHandler h = new RemoteFailureHandler();
        SerializableException s = h.readException(e);
        Class<? extends Throwable> clazz = (Class<? extends Throwable>) Class.forName(s.getClassName());
        Constructor<? extends Throwable> ctor = clazz.getConstructor(String.class);
        Throwable toThrow = ctor.newInstance(new Object[] { s.getMessage() });
        throw toThrow;
    }
    
}
