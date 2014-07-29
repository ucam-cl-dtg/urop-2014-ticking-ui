package uk.ac.cam.cl.ticking.signups;

import java.util.Date;
import java.util.List;

import uk.ac.cam.cl.signups.api.BookingInfo;

public interface TickSignupInterface {
    
    /*
     * TODO: are these methods called directly by the front end?
     */
    
    /* Below are the methods for the student workflow */
    
    /**
     * Lists each time such that the time is the start time of
     * at least one free slot in the specified signup sheet.
     * @return A List of the start times of free slots
     */
    public List<Date> listAvailableTimes(/* some args */);
    
    /**
     * Books the given student into a free slot at the specified
     * time. Only allowed if the student has permission to book a
     * slot for this tick and they haven't make a booking at the
     * same time already.
     * @return The ticker that the student has been signed up to
     * see at the given time, or null if the booking was unsuccessful.
     */
    public String makeBooking(/* some args*/);
    
    /**
     * Equivalent to deleting the existing booking and making a
     * new booking at the given time.
     * @return The ticker that the student has been signed up to
     * see at the given time, or null if the booking was unsuccessful.
     */
    public String modifyBooking(/* some args*/);
    
    /**
     * Deletes the specified booking.
     */
    public void deleteBooking(/* some args*/);
    
    /**
     * Returns a list of the bookings made by one user.
     */
    public List<Object> listBookings(/* some args */);
    
    /* Below are the methods for the ticker workflow */
    
    /**
     * @return A list of the sheetIDs for the given group.
     */
    public List<String> listSheets(/* some args */);
    
    /**
     * @return A list of the column names for the given sheet.
     */
    public List<String> listColumns(/* some args */);
    
    /**
     * Returns a list of the slots in the specified column.
     */
    public List<Object> listSlots(/* some args */);
    
    /**
     * @return Who has booked the slot (null if no one) and the tick
     * they have booked to do.
     */
    public BookingInfo getBooking(/* some args */);
    
    /**
     * Removes all bookings that haven't yet started for the given
     * user in the given sheet.
     */
    public void removeAllStudentBookings(/* some args */);
    
    /**
     * Ensures that the given student is assigned the given ticker
     * (if possible) in the future for the specified tick.
     * TODO: modify signups api to allow booking in other columns if specified one is full
     */
    public void assignTickerForTickForUser(/* some args */);
    
    /* Below are the methods for the author workflow */
    
    /**
     * Creates a new sheet for the given group.
     * @return TODO
     */
    public Object createSheet(/* some args */);
    
    /**
     * Creates a new column for the sheet.
     */
    public void addColumn(/* some args */);
    
    /**
     * Deletes the specified column from the sheet. This also deletes
     * all the bookings made for that column.
     */
    public void deleteColumn(/* some args */);
    
    
    /**
     * Modifies bookings no matter what.
     */
    public void forceModifyBooking(/* some args */);
    
}
