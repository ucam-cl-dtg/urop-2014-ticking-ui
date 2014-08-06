package uk.ac.cam.cl.ticking.ui.ticks;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class stores information regarding a submission for a tickId
 * 
 * NOT CURRENTLY USED, RETAINING UNTIL CERTAIN THIS IS NOT REQUIRED
 * 
 * @author tl364
 *
 */
public class Fork {

	// FORMAT: 'group'_'name'_'author'
	@JsonProperty("_id")
	private String forkId;

	private String author;

	private String repo;

	private String tickId;

	private boolean unitPass = false;
	private boolean humanPass = false;
	private boolean signedUp = false;
	private boolean testing = false;

	/**
	 * Create a new instance of the Submission object.
	 * 
	 * This will generate a submission identifier automatically and should only
	 * be used when a user is creating a new submission.
	 * 
	 * @param author
	 * @param group
	 * @param repo
	 * @param submitted
	 * @param tickId
	 * @param unitPass
	 */
	public Fork(String author, String tickId, String repo) {

		this.forkId = author + "," + tickId;

		this.setAuthor(author);
		this.setRepo(repo);
		this.setTickId(tickId);
	}

	/**
	 * Default constructor for Jackson JSON to POJO because java
	 */
	public Fork() {

	}

	/**
	 * @return repo
	 */
	public String getRepo() {
		return repo;
	}

	/**
	 * @param repo
	 */
	public void setRepo(String repo) {
		this.repo = repo;
	}

	/**
	 * @response tickId
	 */
	public String getTickId() {
		return tickId;
	}

	/**
	 * @param tickId
	 */
	public void setTickId(String tickId) {
		this.tickId = tickId;
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
	 * @return author
	 */
	public String getAuthor() {
		return author;
	}

	/**
	 * @param author
	 */
	public void setAuthor(String author) {
		this.author = author;
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

	public static String generateForkId(String crsid, String tickId) {
			return crsid+","+tickId;
		}
	
	
}
