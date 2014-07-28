package uk.ac.cam.cl.ticking.ui.ticks;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class stores information regarding a submission for a tick
 * 
 * NOT CURRENTLY USED, RETAINING UNTIL CERTAIN THIS IS NOT REQUIRED
 * 
 * @author tl364
 *
 */
public class Submission {

	// FORMAT: 'group'_'name'_'author'
	@JsonProperty("_id")
	private String sid;

	private String group;

	private String author;

	private String repo;
	private Date submitted;
	private String tick;
	private boolean unitPass;
	private boolean humanPass;

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
	 * @param tick
	 * @param unitPass
	 */
	public Submission(@JsonProperty("author") String author,
			@JsonProperty("group") String group,
			@JsonProperty("repo") String repo,
			@JsonProperty("submitted") Date submitted,
			@JsonProperty("tick") Tick tick,
			@JsonProperty("unit_pass") boolean unitPass) {

		this.sid = group + "_" + tick.getName() + "_" + author;
		this.setGroup(group);
		this.setAuthor(author);
		this.setRepo(repo);
		this.setSubmitted(submitted);
		this.setTick(tick);
		this.setUnitPass(unitPass);
		this.setHumanPass(false);
	}

	/**
	 * Default constructor for Jackson JSON to POJO because java
	 */
	public Submission() {

	}

	/**
	 * @return repo
	 */
	@JsonProperty("repo")
	public String getRepo() {
		return repo;
	}

	/**
	 * @param repo
	 */
	@JsonProperty("repo")
	public void setRepo(String repo) {
		this.repo = repo;
	}

	/**
	 * @return submitted
	 */
	@JsonProperty("submitted")
	public Date getSubmitted() {
		return submitted;
	}

	/**
	 * @param submitted
	 */
	@JsonProperty("submitted")
	public void setSubmitted(Date submitted) {
		this.submitted = submitted;
	}

	/**
	 * @response tick
	 */
	@JsonProperty("tick")
	public String getTick() {
		return tick;
	}

	/**
	 * @param tick
	 */
	@JsonProperty("tick")
	public void setTick(Tick tick) {
		this.tick = tick.getTID();
	}

	/**
	 * @return unitPass
	 */
	@JsonProperty("unit_pass")
	public boolean getUnitPass() {
		return unitPass;
	}

	/**
	 * @param unitPass
	 */
	@JsonProperty("unit_pass")
	public void setUnitPass(boolean unitPass) {
		this.unitPass = unitPass;
	}

	/**
	 * @return humanPass
	 */
	@JsonProperty("human_pass")
	public boolean getHumanPass() {
		return humanPass;
	}

	/**
	 * @param humanPass
	 */
	@JsonProperty("human_pass")
	public void setHumanPass(boolean humanPass) {
		this.humanPass = humanPass;
	}

	/**
	 * @return author
	 */
	@JsonProperty("author")
	public String getAuthor() {
		return author;
	}

	/**
	 * @param author
	 */
	@JsonProperty("author")
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * @return group
	 */
	@JsonProperty("group")
	public String getGroup() {
		return group;
	}

	/**
	 * @param group
	 */
	@JsonProperty("group")
	public void setGroup(String group) {
		this.group = group;
	}
}
