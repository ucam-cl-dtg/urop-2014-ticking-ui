/* vim: set et ts=4 sts=4 sw=4 tw=72 : */
/* See the LICENSE file for the license of the project */
/**
 * 
 */
package uk.ac.cam.cl.ticking.signups;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.signups.api.Column;
import uk.ac.cam.cl.signups.api.Group;
import uk.ac.cam.cl.signups.api.Sheet;
import uk.ac.cam.cl.signups.api.SheetInfo;
import uk.ac.cam.cl.signups.api.Slot;
import uk.ac.cam.cl.signups.api.beans.ColumnBean;
import uk.ac.cam.cl.signups.api.beans.GroupSheetBean;
import uk.ac.cam.cl.signups.api.beans.SlotBean;
import uk.ac.cam.cl.signups.api.exceptions.DuplicateNameException;
import uk.ac.cam.cl.signups.api.exceptions.ItemNotFoundException;
import uk.ac.cam.cl.signups.api.exceptions.NotAllowedException;
import uk.ac.cam.cl.signups.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.injection.GuiceConfigurationModule;

import com.google.inject.Guice;
import com.google.inject.Inject;

/**
 * @author Isaac Dunn &lt;ird28@cam.ac.uk&gt;
 */
public class TickSignupsTest {
    
    private TickSignups signups = new TickSignups();
    private WebInterface service;
    @Inject private ConfigurationLoader<Configuration> config;
    
    {
        Guice.createInjector(new GuiceConfigurationModule()).injectMembers(this);
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(
                config.getConfig().getSignupsApiLocation()
                //"http://localhost:8080/UROP_SIGNUPS/rest/"
                );

        service = target.proxy(WebInterface.class);
    }
    
    private String id;
    private String sauth;
    private String gauth;
    
    @Before
    public void setUp() throws DuplicateNameException, ItemNotFoundException, NotAllowedException, Exception {
        try {
            SheetInfo info = service.addSheet(new Sheet("Java 1A Week 5", "First year software practical session", "Intel lab"));
            id = info.getSheetID();
            sauth = info.getAuthCode();
            
            service.addColumn(id, new ColumnBean(new Column("Ticker A", new LinkedList<String>()), sauth));
            service.addSlot(id, "Ticker A", new SlotBean(new Slot(id, "Ticker A", new Date(1420120800000L), 300000), sauth));
            service.addSlot(id, "Ticker A", new SlotBean(new Slot(id, "Ticker A", new Date(1420124400000L), 300000), sauth));
            service.addSlot(id, "Ticker A", new SlotBean(new Slot(id, "Ticker A", new Date(1420128000000L), 300000), sauth));

            service.addColumn(id, new ColumnBean(new Column("Ticker B", new LinkedList<String>()), sauth));
            service.addSlot(id, "Ticker B", new SlotBean(new Slot(id, "Ticker B", new Date(1420120800000L), 300000), sauth));
            service.addSlot(id, "Ticker B", new SlotBean(new Slot(id, "Ticker B", new Date(1420124400000L), 300000), sauth));
            service.addSlot(id, "Ticker B", new SlotBean(new Slot(id, "Ticker B", new Date(1420128000000L), 300000), sauth));
            service.addSlot(id, "Ticker B", new SlotBean(new Slot(id, "Ticker B", new Date(1420131600000L), 300000), sauth));
            
            gauth = service.addGroup(new Group("1A Java"));
            service.addSheetToGroup("1A Java", new GroupSheetBean(id, gauth, sauth));
        } catch (javax.ws.rs.InternalServerErrorException e) {
            RemoteFailureHandler h = new RemoteFailureHandler();
            Object o = h.readException(e);
            System.out.println(o);
            throw e;
        } catch (Exception e) {
            System.out.println("Should not reach here\n");
            e.printStackTrace();
            throw e;
        }
    }
    
    @After
    public void tearDown() throws Exception {
        service.deleteSheet(id, sauth);
        service.deleteGroup("1A Java", gauth);
    }

    @Test
    public void makeBookings_success() {
        try {
            signups.assignTickerForTickForUser("ird28", "1A Java", "Tick 4", null, gauth);
            signups.assignTickerForTickForUser("ird28", "1A Java", "Tick 5", null, gauth);
            String ticker1 = (String) signups.makeBooking("ird28", "1A Java", id, "Tick 4", new Date(1420120800000L)).getEntity();
            String ticker2 = (String) signups.makeBooking("ird28", "1A Java", id, "Tick 5", new Date(1420131600000L)).getEntity();
            assertEquals("Second should be booked to ticker B", "Ticker B", ticker2);
            List<Date> freeTimes = (List<Date>) signups.listAvailableTimes("ird28", "Tick 4", id).getEntity();
            assertTrue("Should be another free slot", freeTimes.contains(new Date(1420120800000L)));
            assertFalse("Only 1 slot should now be booked", freeTimes.contains(new Date(1420131600000L)));
            List<Slot> bookings = (List<Slot>) signups.listStudentBookings("ird28").getEntity();
            for (Slot slot : bookings) {
                assertTrue((slot.getStartTime().equals(new Date(1420120800000L)) && slot.getComment().equals("Tick 4"))
                        || (slot.getStartTime().equals(new Date(1420131600000L)) && slot.getComment().equals("Tick 5")));
            }
        } catch (javax.ws.rs.InternalServerErrorException e) {
            RemoteFailureHandler h = new RemoteFailureHandler();
            Object o = h.readException(e);
            System.out.println(o);
            throw e;
        }
    }
    
    @Test
    public void makeBookings_forbidden_alreadyHasBookingForTick() {
        signups.assignTickerForTickForUser("ird28", "1A Java", "Tick 4", null, gauth);
        Response r1 = signups.makeBooking("ird28", "1A Java", id, "Tick 4", new Date(1420120800000L));
        Response r2 = signups.makeBooking("ird28", "1A Java", id, "Tick 4", new Date(1420131600000L));
        assertEquals("First booking should be allowed", Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("Second booking should be forbidden", Status.FORBIDDEN.getStatusCode(), r2.getStatus());
        
        List<Slot> tickerBslots = (List<Slot>) signups.listSlots(id, "Ticker B").getEntity();
        for (Slot slot : tickerBslots) {
            assertFalse("All ticker B slots should be unbooked still", slot.isBooked());
        }

        List<Slot> bookings = (List<Slot>) signups.listStudentBookings("ird28").getEntity();
        assertEquals("Only one slot should be booked", 1, bookings.size());
        assertEquals("Booking should be to first time", new Date(1420120800000L), bookings.get(0).getStartTime());
    }
    
    @Test
    public void makeBookings_forbidden_alreadyHasBookingForTime() {
        signups.assignTickerForTickForUser("ird28", "1A Java", "Tick 4", null, gauth);
        signups.assignTickerForTickForUser("ird28", "1A Java", "Tick 5", null, gauth);
        Response r1 = signups.makeBooking("ird28", "1A Java", id, "Tick 4", new Date(1420120800000L));
        Response r2 = signups.makeBooking("ird28", "1A Java", id, "Tick 5", new Date(1420120800000L));
        assertEquals("First booking should be allowed", Status.OK.getStatusCode(), r1.getStatus());
        assertEquals("Second booking should be forbidden", Status.FORBIDDEN.getStatusCode(), r2.getStatus());
        
        List<Slot> tickerBslots = (List<Slot>) signups.listSlots(id, "Ticker B").getEntity();
        for (Slot slot : tickerBslots) {
            assertFalse("All ticker B slots should be unbooked still", slot.isBooked());
        }

        List<Slot> bookings = (List<Slot>) signups.listStudentBookings("ird28").getEntity();
        assertEquals("Only one slot should be booked", 1, bookings.size());
        assertEquals("Booking should be to first time", new Date(1420120800000L), bookings.get(0).getStartTime());
    }

}
