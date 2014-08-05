package uk.ac.cam.cl.ticking.signups;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import com.google.inject.Guice;
import com.google.inject.Inject;

import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.signups.api.*;
import uk.ac.cam.cl.signups.api.beans.CreateColumnBean;
import uk.ac.cam.cl.signups.api.beans.PermissionsBean;
import uk.ac.cam.cl.signups.api.beans.SlotBookingBean;
import uk.ac.cam.cl.signups.api.exceptions.DuplicateNameException;
import uk.ac.cam.cl.signups.api.exceptions.ItemNotFoundException;
import uk.ac.cam.cl.signups.api.exceptions.NotAllowedException;
import uk.ac.cam.cl.signups.interfaces.*;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.injection.GuiceConfigurationModule;

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
    
    public static void main(String[] args) {
        TickSignups ts = new TickSignups();
        try {
            System.out.println(ts.service.listColumns("dont_find_me"));
        } catch (InternalServerErrorException e) {
            RemoteFailureHandler h = new RemoteFailureHandler();
            Object o = h.readException(e);
            System.out.println(o);
        } catch (ItemNotFoundException e) {
            throw new Error("I don't believe you",e);
        }
    }
    
    /* Below are the methods for the student workflow */
    
    /**
     * Lists each time such that the time is the start time of
     * at least one free slot in the specified signup sheet.
     * @param sheetID The ID of the sheet whose free slots are needed.
     * @return A list of the start times of free slots
     * @throws ItemNotFoundException 
     * TODO: we don't want all free slots, we want all slots a student can use to sign up for a specific tick with
     */
    public Response listAvailableTimes(String sheetID) {
        try {
            List<Date> result = service.listAllFreeStartTimes(sheetID);
            return Response.ok(result).build();
        } catch (ItemNotFoundException e) {
            return Response.status(404).entity(e).build();
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
    public Response makeBooking(String crsid, String groupID,
            String sheetID, String tickID, Date startTime) {
        for (Slot slot : service.listUserSlots(crsid)) {
            if (slot.getStartTime().equals(startTime)) {
                return Response.status(Status.FORBIDDEN)
                        .entity("Student already has a booking at this time").build();
            }
            if (slot.getComment().equals(tickID)) {
                return Response.status(Status.FORBIDDEN)
                        .entity("Student already has a booking for this tick").build();
            }
        }
        try {
            if (service.listColumnsWithFreeSlotsAt(sheetID, startTime).size() == 0) {
                return Response.status(Status.NOT_FOUND)
                        .entity("There are no free slots at the given time").build();
            }
            if (service.getPermissions(groupID, crsid).containsKey(tickID)) {
                String ticker = service.getPermissions(groupID, crsid).get(tickID);
                if (ticker == null) { // any column permitted
                    ticker = service.listColumnsWithFreeSlotsAt(sheetID, startTime).get(0);
                }
                service.book(sheetID, ticker, startTime, new SlotBookingBean(null, crsid, tickID));
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
     * Equivalent to deleting the existing booking and making a
     * new booking at the given time.
     * @return The ticker that the student has been signed up to
     * see at the given time, or null if the booking was unsuccessful.
     */
    public Response modifyBooking(String crsid, String groupID,
            String sheetID, String tickID, Date oldStartTime, Date newStartTime) {
        Response unbookResponse = unbookSlot(crsid, groupID, sheetID, tickID, oldStartTime);
        if (unbookResponse.getStatus() != Status.OK.getStatusCode()) {
            return unbookResponse;
        }
        return makeBooking(crsid, groupID, sheetID, tickID, newStartTime);
    }
    
    /**
     * Deletes the specified booking (not slot).
     */
    public Response unbookSlot(String crsid, String groupID,
            String sheetID, String tickID, Date startTime) {
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
            service.book(sheetID, ticker, startTime, new SlotBookingBean(crsid, null, null));
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
    public Response listStudentBookings(String crsid) {
        return Response.ok(service.listUserSlots(crsid)).build();
    }
    
    /* Below are the methods for the ticker workflow */
    
    /**
     * @return A list of the sheetIDs for the given group.
     */
    public Response listSheets(String groupName) {
        try {
            return Response.ok(service.listSheetIDs(groupName)).build();
        } catch (ItemNotFoundException e) {
            return Response.status(404).entity(e).build();
        }
    }
    
    /**
     * @return A list of the ticker names for the given sheet.
     */
    public Response listTickers(String sheetID) {
        try {
            return Response.ok(service.listColumns(sheetID)).build();
        } catch (ItemNotFoundException e) {
            return Response.status(404).entity(e).build();
        }
    }
    
    /**
     * Returns a list of the slots for the specified ticker.
     */
    public Response listSlots(String sheetID, String tickerName) {
        try {
            return Response.ok(service.listColumnSlots(sheetID, tickerName)).build();
        } catch (ItemNotFoundException e) {
            return Response.status(404).entity(e).build();
        }
    }
    
    /**
     * @return Who has booked the slot (null if no one) and the tick
     * they have booked to do.
     */
    public Response getBooking(String sheetID, String tickerName, Date startTime) {
        try {
            return Response.ok(service.showBooking(sheetID, tickerName, startTime)).build();
        } catch (ItemNotFoundException e) {
            return Response.status(404).entity(e).build();
        }
    }
    
    /**
     * Removes all bookings that haven't yet started for the given
     * user in the given sheet.
     */
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
    public Response createSheet(String title, String description, String location,
            Date startTime, int slotLengthInMinutes, Date endTime, List<String> tickerNames) {
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
            }
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
    public Response deleteColumn(String sheetID, String columnName, String authCode) {
        try {
            service.deleteColumn(sheetID, columnName, authCode);
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
    
}
