/* vim: set et ts=4 sts=4 sw=4 tw=72 : */
/* See the LICENSE file for the license of the project */
/**
 * 
 */
package uk.ac.cam.cl.ticking.signups;

import java.util.Date;
import java.util.List;

/**
 * @author Isaac Dunn &lt;ird28@cam.ac.uk&gt;
 */
public class CreateSheetBean {
    private String title;
    private String description;
    private String location;
    private Date startTime;
    private int slotLengthInMinutes;
    private Date endTime;
    private List<String> tickerNames;
    private String groupID;
    
    public CreateSheetBean(String title, String description, String location,
            Date startTime, int slotLengthInMinutes, Date endTime,
            List<String> tickerNames, String groupID, String groupAuthCode) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.startTime = startTime;
        this.slotLengthInMinutes = slotLengthInMinutes;
        this.endTime = endTime;
        this.tickerNames = tickerNames;
        this.groupID = groupID;
    }
    
    public CreateSheetBean() {
        // Default constructor
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getLocation() {
        return location;
    }

    public Date getStartTime() {
        return startTime;
    }

    public int getSlotLengthInMinutes() {
        return slotLengthInMinutes;
    }

    public Date getEndTime() {
        return endTime;
    }

    public List<String> getTickerNames() {
        return tickerNames;
    }

    public String getGroupID() {
        return groupID;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setSlotLengthInMinutes(int slotLengthInMinutes) {
        this.slotLengthInMinutes = slotLengthInMinutes;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setTickerNames(List<String> tickerNames) {
        this.tickerNames = tickerNames;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }
    
    
    
}
