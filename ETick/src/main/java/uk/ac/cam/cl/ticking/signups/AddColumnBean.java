/* vim: set et ts=4 sts=4 sw=4 tw=72 : */
/* See the LICENSE file for the license of the project */
/**
 * 
 */
package uk.ac.cam.cl.ticking.signups;

import java.util.Date;

/**
 * @author Isaac Dunn &lt;ird28@cam.ac.uk&gt;
 */
public class AddColumnBean {
    
    private String name;
    private Date startTime;
    private Date endTime;
    private int slotLength; // in minutes
    
    public AddColumnBean(String name, Date startTime, Date endTime,
            int slotLength) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotLength = slotLength; // in minutes
    }
    
    public AddColumnBean() {
        // Default constructor
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public int getSlotLength() {
        return slotLength;
    }

    public void setSlotLength(int slotLength) {
        this.slotLength = slotLength;
    }
    
    

}
