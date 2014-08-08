/* vim: set et ts=4 sts=4 sw=4 tw=72 : */
/* See the LICENSE file for the license of the project */
/**
 * 
 */
package uk.ac.cam.cl.ticking.signups;

/**
 * @author Isaac Dunn &lt;ird28@cam.ac.uk&gt;
 */
public class MakeBookingBean {
    
    private String tickID;
    private Long startTime;
    
    public MakeBookingBean(String tickID, Long startTime) {
        this.tickID = tickID;
        this.startTime = startTime;
    }

    public String getTickID() {
        return tickID;
    }

    public Long getStartTime() {
        return startTime;
    }

}
