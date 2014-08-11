package uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans;

import org.joda.time.DateTime;


/**
 * This class stores information regarding a submission for a tickId
 * 
 * NOT CURRENTLY USED, RETAINING UNTIL CERTAIN THIS IS NOT REQUIRED
 * 
 * @author tl364
 *
 */
public class ForkBean {

	private Boolean unitPass = false;
	private Boolean humanPass = false;
	private Boolean signedUp = false;
	private Boolean testing = false;
	private Boolean reportAvailable = false;
	
	private String ticker;
	private String tickerComments;
	private String commitId;
	
	private DateTime reportDate;

	/**
	 * Default constructor for Jackson JSON to POJO because java
	 */
	public ForkBean() {

	}

	/**
	 * @return unitPass
	 */
	public Boolean getUnitPass() {
		return unitPass;
	}

	/**
	 * @param unitPass
	 */
	public void setUnitPass(boolean unitPass) {
		this.unitPass = unitPass;
	}

	/**
	 * @return humanPass
	 */
	public Boolean getHumanPass() {
		return humanPass;
	}

	/**
	 * @param humanPass
	 */
	public void setHumanPass(boolean humanPass) {
		this.humanPass = humanPass;
	}

	/**
	 * 
	 * @return signedUp
	 */
	public Boolean isSignedUp() {
		return signedUp;
	}

	/**
	 * 
	 * @param signedUp
	 */
	public void setSignedUp(boolean signedUp) {
		this.signedUp = signedUp;
	}

	/**
	 * 
	 * @return testing
	 */
	public Boolean isTesting() {
		return testing;
	}

	/**
	 * 
	 * @param testing
	 */
	public void setTesting(boolean testing) {
		this.testing = testing;
	}
	
	/**
	 * @return reportAvailable
	 */
	public Boolean getReportAvailable() {
		return reportAvailable;
	}

	/**
	 * @param reportAvailable
	 */
	public void setReportAvailable(Boolean reportAvailable) {
		this.reportAvailable = reportAvailable;
	}

	/**
	 * 
	 * @return tickerComments
	 */
	public String getTickerComments() {
		return tickerComments;
	}

	/**
	 * 
	 * @param tickerComments
	 */
	public void setTickerComments(String tickerComments) {
		this.tickerComments = tickerComments;
	}

	/**
	 * 
	 * @return commitId
	 */
	public String getCommitId() {
		return commitId;
	}

	/**
	 * 
	 * @param commitId
	 */
	public void setCommitId(String commitId) {
		this.commitId = commitId;
	}

	/**
	 * 
	 * @return ticker
	 */
	public String getTicker() {
		return ticker;
	}

	/**
	 * 
	 * @param ticker
	 */
	public void setTicker(String ticker) {
		this.ticker = ticker;
	}

	/**
	 * 
	 * @return reportDate
	 */
	public DateTime getReportDate() {
		return reportDate;
	}

	/**
	 * 
	 * @param reportDate
	 */
	public void setReportDate(DateTime reportDate) {
		this.reportDate = reportDate;
	}
}
