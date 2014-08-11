/* vim: set et ts=4 sts=4 sw=4 tw=72 : */
/* See the LICENSE file for the license of the project */
/**
 * 
 */
package uk.ac.cam.cl.ticking.signups;


/**
 * @author Isaac Dunn &lt;ird28@cam.ac.uk&gt;
 */
public class AddColumnBean {
    
    private String name;
    private Long startTime;
    private Long endTime;
    private int slotLengthInMinutes;
    
    public AddColumnBean(String name, Long startTime, Long endTime,
            int slotLengthInMinutes) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotLengthInMinutes = slotLengthInMinutes;
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
    
    

}
