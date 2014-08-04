package uk.ac.cam.cl.ticking.signups;

import java.util.Date;

public class Booking implements Comparable<Booking> {
    
    private String crsid;
    private String tickID;
    private Date startTime;
    private String sheetID;
    private String groupID;
    private String ticker;
    
    public Booking(String crsid, String tickID, Date startTime, String sheetID,
            String groupID, String ticker) {
        this.crsid = crsid;
        this.tickID = tickID;
        this.startTime = startTime;
        this.sheetID = sheetID;
        this.groupID = groupID;
        this.ticker = ticker;
    }

    public String getCrsid() {
        return crsid;
    }

    public String getTickID() {
        return tickID;
    }

    public Date getStartTime() {
        return startTime;
    }

    public String getSheetID() {
        return sheetID;
    }

    public String getGroupID() {
        return groupID;
    }

    public String getTicker() {
        return ticker;
    }

    @Override
    public int compareTo(Booking arg0) {
        return getStartTime().compareTo(arg0.getStartTime());
    }
    
}
