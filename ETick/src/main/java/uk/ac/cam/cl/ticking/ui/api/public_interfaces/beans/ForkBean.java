package uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans;


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
	
	private String tickerComments;
	private String commitId;

	/**
	 * Default constructor for Jackson JSON to POJO because java
	 */
	public ForkBean() {

	}

	/**
	 * @return unitPass
	 */
	public boolean getUnitPass() {
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
	public boolean getHumanPass() {
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
	public boolean isSignedUp() {
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
	public boolean isTesting() {
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
}
