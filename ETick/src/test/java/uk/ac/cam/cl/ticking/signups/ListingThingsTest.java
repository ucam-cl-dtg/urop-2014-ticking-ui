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

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.BeforeClass;
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
import uk.ac.cam.cl.signups.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.injection.GuiceConfigurationModule;

import com.google.inject.Guice;
import com.google.inject.Inject;

/**
 * @author Isaac Dunn &lt;ird28@cam.ac.uk&gt;
 */
public class ListingThingsTest {
    
    private TickSignups signups = new TickSignups();
    private WebInterface service;
    @Inject private ConfigurationLoader<Configuration> config;
    
    private String id;
    private String sauth;
    private String gauth;
    
    {
        Guice.createInjector(new GuiceConfigurationModule()).injectMembers(this);
        ResteasyClient client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(
                config.getConfig().getSignupsApiLocation()
                //"http://localhost:8080/UROP_SIGNUPS/rest/"
                );
        service = target.proxy(WebInterface.class);
    }
    
    @BeforeClass
    public void setUp() throws Exception {
        try {
            SheetInfo info = service.addSheet(new Sheet("Java 1A Week 5", "First year software practical session", "Intel lab"));
            id = info.getSheetID();
            sauth = info.getAuthCode();
            
            service.addColumn(id, new ColumnBean(new Column("Ticker A", new LinkedList<String>()), sauth));
            service.addSlot(id, "Ticker A", new SlotBean(
                    new Slot(id, "Ticker A", new Date(1420120800000L), 300000, "ird28", "Tick 11"), sauth));
            service.addSlot(id, "Ticker A", new SlotBean(
                    new Slot(id, "Ticker A", new Date(1420124400000L), 300000, "ird28", "Tick 13"), sauth));
            service.addSlot(id, "Ticker A", new SlotBean(new Slot(id, "Ticker A", new Date(1420128000000L), 300000), sauth));

            service.addColumn(id, new ColumnBean(new Column("Ticker B", new LinkedList<String>()), sauth));
            service.addSlot(id, "Ticker B", new SlotBean(new Slot(id, "Ticker B", new Date(1420120800000L), 300000), sauth));
            service.addSlot(id, "Ticker B", new SlotBean(new Slot(id, "Ticker B", new Date(1420124400000L), 300000), sauth));
            service.addSlot(id, "Ticker B", new SlotBean(
                    new Slot(id, "Ticker B", new Date(1420128000000L), 300000, "prv22", "Tick 11"), sauth));
            service.addSlot(id, "Ticker B", new SlotBean(new Slot(id, "Ticker B", new Date(1420131600000L), 300000), sauth));
            
            gauth = service.addGroup(new Group("1A Java"));
            service.addSheetToGroup("1A Java", new GroupSheetBean(id, gauth, sauth));
        } catch (javax.ws.rs.InternalServerErrorException e) {
            RemoteFailureHandler h = new RemoteFailureHandler();
            Object o = h.readException(e);
            System.out.println(o);
            throw e;
        } catch (DuplicateNameException e) {
            // Do nothing - stuff is already in the database
        }
        
        try {
            gauth = service.addGroup(new Group("1A Java"));
            service.addSheetToGroup("1A Java", new GroupSheetBean(id, gauth, sauth));
        } catch (javax.ws.rs.InternalServerErrorException e) {
            RemoteFailureHandler h = new RemoteFailureHandler();
            Object o = h.readException(e);
            System.out.println(o);
            throw e;
        } catch (DuplicateNameException e) {
            // Do nothing - stuff is already in the database
        }
    }
    

    @Test
    public void listAvailableTimes_success() {
        signups.assignTickerForTickForUser("ird28", "1A Java", "Tick 4", "Ticker A", gauth);
        Response r = signups.listAvailableTimes("ird28", "Tick 4", id);
        List<Date> times = (List<Date>) r.getEntity();
        List<Date> correctTimes = new LinkedList<Date>();
        correctTimes.add(arg0)
        assertEquals("The given times should be returned", )
        fail("Not yet implemented");
    }

    @Test
    public void listSheets_success() {
        fail("Not yet implemented");
    }
    
    @Test
    public void listTickers_success() {
        fail("Not yet implemented");
    }
    
    
    @Test
    public void listSlots_success() {
        fail("Not yet implemented");
    }
    
    @Test
    public void listStudentBookings_success() {
        fail("Not yet implemented");
    }

}
