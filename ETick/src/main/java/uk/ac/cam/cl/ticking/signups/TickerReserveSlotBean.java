/* vim: set et ts=4 sts=4 sw=4 tw=72 : */
/* See the LICENSE file for the license of the project */
/**
 * 
 */
package uk.ac.cam.cl.ticking.signups;

/**
 * @author Isaac Dunn &lt;ird28@cam.ac.uk&gt;
 */
public class TickerReserveSlotBean {
    
    private String ticker;
    private Long startTime;
    
    public TickerReserveSlotBean(String ticker, Long startTime) {
        this.ticker = ticker;
        this.startTime = startTime;
    }
    
    public TickerReserveSlotBean() {
        // Default constructor
    }

    public String getTicker() {
        return ticker;
    }

    public Long getStartTime() {
        return startTime;
    }

    public void setTickID(String ticker) {
        this.ticker = ticker;
    }

    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

}
