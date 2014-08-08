/* vim: set et ts=4 sts=4 sw=4 tw=72 : */
/* See the LICENSE file for the license of the project */
/**
 * 
 */
package uk.ac.cam.cl.ticking.signups;

import java.util.Date;

import uk.ac.cam.cl.signups.api.Slot;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

/**
 * @author Isaac Dunn &lt;ird28@cam.ac.uk&gt;
 */
public class BookingInfo {
    
    String ticker;
    String tickName;
    String tickID;
    Date startTime;
    
    public BookingInfo(Slot slot) {
        ticker = slot.getColumnName();
        tickID = slot.getComment();
        tickName = Tick.getNameFromID(tickID);
        startTime = slot.getStartTime();
    }

    public String getTicker() {
        return ticker;
    }

    public String getTickName() {
        return tickName;
    }

    public String getTickID() {
        return tickID;
    }

    public Date getStartTime() {
        return startTime;
    }    

}
