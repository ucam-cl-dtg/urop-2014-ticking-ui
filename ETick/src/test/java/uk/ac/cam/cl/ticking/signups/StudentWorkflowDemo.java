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

import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.signups.api.*;
import uk.ac.cam.cl.signups.api.beans.ColumnBean;
import uk.ac.cam.cl.signups.api.beans.CreateColumnBean;
import uk.ac.cam.cl.signups.api.beans.GroupSheetBean;
import uk.ac.cam.cl.signups.api.beans.SlotBean;
import uk.ac.cam.cl.signups.api.exceptions.DuplicateNameException;
import uk.ac.cam.cl.signups.api.exceptions.ItemNotFoundException;
import uk.ac.cam.cl.signups.api.exceptions.NotAllowedException;
import uk.ac.cam.cl.signups.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.injection.GuiceConfigurationModule;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * @author Isaac Dunn &lt;ird28@cam.ac.uk&gt;
 */
public class StudentWorkflowDemo {
    
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
        // Need to dump database before test is run
        try {
            SheetInfo info = service.addSheet(new Sheet("Java 1A Week 5", "First year software practical session", "Intel lab"));
            id = info.getSheetID();
            sauth = info.getAuthCode();
            service.addColumn(id, new ColumnBean(new Column("Ticker A", new LinkedList<String>()), sauth));
            service.addSlot(id, "Ticker A", new SlotBean(new Slot(id, "Ticker A", new Date(1420120800000L), 300000), sauth));
            service.addSlot(id, "Ticker A", new SlotBean(new Slot(id, "Ticker A", new Date(1420124400000L), 300000), sauth));
            service.addSlot(id, "Ticker A", new SlotBean(new Slot(id, "Ticker A", new Date(1420128000000L), 300000), sauth));
            gauth = service.addGroup(new Group("1A Java"));
            service.addSheet("1A Java", new GroupSheetBean(id, gauth, sauth));
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

    @Test
    public void SigningUp_success() {
        try {
            System.out.println();
            /* The student passes the unit test for tick 4 and is added to the whitelist */
            
            /* Check users database: should now map from 1A Java to Tick 4 to null */
            Response s = signups.assignTickerForTickForUser("ird28", "1A Java", "Tick 4", null, gauth);
            if (s.getStatus() == 200) {
                System.out.println("Assignment successful");
                System.out.println();
            }
            
            /* The student is displayed with a list of available times */
            System.out.println(signups.listAvailableTimes(id).getEntity());
            System.out.println();

            /* The student selects a free slot and signs up to it */
            Response r = signups.makeBooking("ird28", "1A Java", id, "Tick 4", new Date(1420120800000L));
            if (r.getStatus() == 200) {
                System.out.println("Booking successful - the ticker is " + r.getEntity());
            }
            System.out.println("\nTicker A slots are now: \n");
            for (Slot slot : (List<Slot>) signups.listSlots(id, "Ticker A").getEntity()) {
                System.out.println(slot.toString());
            }
        }  catch (javax.ws.rs.InternalServerErrorException e) {
            RemoteFailureHandler h = new RemoteFailureHandler();
            Object o = h.readException(e);
            System.out.println(o);
            throw e;
        }
    }

}