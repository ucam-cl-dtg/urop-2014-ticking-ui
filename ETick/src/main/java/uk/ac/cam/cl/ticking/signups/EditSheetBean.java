/* vim: set et ts=4 sts=4 sw=4 tw=72 : */
/* See the LICENSE file for the license of the project */
/**
 * 
 */
package uk.ac.cam.cl.ticking.signups;

import java.util.List;

/**
 * @author Isaac Dunn &lt;ird28@cam.ac.uk&gt;
 */
public class EditSheetBean {
    
    private Long startTime;
    private Long endTime;
    private int slotLengthInMinutes;
    private List<String> tickerNames;
    private String title;
    private String description;
    private String location;
    
    public EditSheetBean(Long startTime, Long endTime, int slotLengthInMinutes,
            List<String> tickerNames, String title, String description,
            String location) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotLengthInMinutes = slotLengthInMinutes;
        this.tickerNames = tickerNames;
        this.title = title;
        this.description = description;
        this.location = location;
    }
    
    public EditSheetBean() {
        // Default constructor
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public int getSlotLengthInMinutes() {
        return slotLengthInMinutes;
    }

    public void setSlotLengthInMinutes(int slotLengthInMinutes) {
        this.slotLengthInMinutes = slotLengthInMinutes;
    }

    public List<String> getTickerNames() {
        return tickerNames;
    }

    public void setTickerNames(List<String> tickerNames) {
        this.tickerNames = tickerNames;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

}
