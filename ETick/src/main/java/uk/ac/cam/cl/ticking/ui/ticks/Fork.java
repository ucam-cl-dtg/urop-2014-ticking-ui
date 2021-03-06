package uk.ac.cam.cl.ticking.ui.ticks;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
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
	
	private boolean forking = false;

	private boolean unitPass = false;
	private boolean humanPass = false;
	private boolean signedUp = false;
	private boolean testing = false;
	private boolean reportAvailable = false;
	
	private int unitFails = 0;
	private int unitPasses = 0;
	private int humanFails = 0;

	private String lastTickedBy;
	private DateTime lastTickedOn;
	
	private DateTime lastReport;

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
	 * 
	 * @return forkId
	 */
	public String getForkId() {
		return forkId;
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
	 * @return lastTickedBy
	 */
	public String getLastTickedBy() {
		return lastTickedBy;
	}

	/**
	 * 
	 * @param lastTickedBy
	 */
	public void setLastTickedBy(String lastTickedBy) {
		this.lastTickedBy = lastTickedBy;
	}

	public boolean isForking() {
		return forking;
	}

	public void setForking(boolean forking) {
		this.forking = forking;
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
	 * @param signedUpr
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
	 * @return reportAvailable
	 */
	public boolean isReportAvailable() {
		return reportAvailable;
	}

	/**
	 * @param reportAvailable
	 */
	public void setReportAvailable(boolean reportAvailable) {
		this.reportAvailable = reportAvailable;
	}

	/**
	 * 
	 * @return lastTimedOn
	 */
	public DateTime getLastTickedOn() {
		return lastTickedOn;
	}

	/**
	 * 
	 * @param lastTickedOn
	 */
	public void setLastTickedOn(DateTime lastTickedOn) {
		this.lastTickedOn = lastTickedOn;
	}

	public int getUnitFails() {
		return unitFails;
	}

	public void setUnitFails(int unitFails) {
		this.unitFails = unitFails;
	}

	public int getUnitPasses() {
		return unitPasses;
	}

	public void setUnitPasses(int unitPasses) {
		this.unitPasses = unitPasses;
	}

	public int getHumanFails() {
		return humanFails;
	}

	public void setHumanFails(int humanFails) {
		this.humanFails = humanFails;
	}
	
	public DateTime getLastReport() {
		return lastReport;
	}

	public void setLastReport(DateTime lastReport) {
		this.lastReport = lastReport;
	}

	public void incrementUnitFails() {
		unitFails++;
	}
	
	public void incrementUnitPasses() {
		unitPasses++;
	}
	
	public void incrementHumanFails() {
		humanFails++;
	}
	
	public String stats() {
		return humanFails+"|"+unitPasses+"|"+unitFails;
	}
	

	/**
	 * 
	 * @param crsid
	 * @param tickId
	 * @return the fork id a fork object with the given crsid and tickId will
	 *         have been given
	 */
	public static String generateForkId(String crsid, String tickId) {
		return crsid + "," + tickId;
	}

}
