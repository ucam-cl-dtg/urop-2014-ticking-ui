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

	private boolean unitPass = false;
	private boolean humanPass = false;
	private boolean signedUp = false;
	private boolean testing = false;

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
}
