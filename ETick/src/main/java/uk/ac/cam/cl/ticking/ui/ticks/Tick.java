package uk.ac.cam.cl.ticking.ui.ticks;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class stores information regarding a tick.
 * 
 * @author tl364
 *
 */
public class Tick {

	// FORMAT: 'group'_'name'
	@JsonProperty("_id")
	private String tid;

	private String group;
	private String author;

	private String repo, name;
	private Date deadline;

	/**
	 * 
	 * Create a new instance of the Tick object.
	 * 
	 * This will generate a tick identifier automatically and should only be
	 * used when a user is creating a new tick.
	 * 
	 * 
	 * @param name
	 * @param group
	 * @param repo
	 * @param deadline
	 * @param files
	 */
	@JsonCreator
	public Tick(@JsonProperty("name") String name,
			@JsonProperty("group") String group,
			@JsonProperty("author") String author,
			@JsonProperty("repo") String repo,
			@JsonProperty("deadline") Date deadline) {

		this.tid = group + "_" + name;
		this.setName(name);
		this.setGroup(group);
		this.setAuthor(author);
		this.setRepo(repo);
		this.setDeadline(deadline);

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
	 * @return deadline
	 */
	@JsonProperty("deadline")
	public Date getDeadline() {
		return deadline;
	}

	/**
	 * @param deadline
	 */
	@JsonProperty("deadline")
	public void setDeadline(Date deadline) {
		this.deadline = deadline;
	}

	/**
	 * @return name
	 */
	@JsonProperty("name")
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 */
	@JsonProperty("name")
	public void setName(String name) {
		this.name = name;
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
		this.author = author;;
	}

	/**
	 * @return tid
	 */
	public String getTID() {
		return tid;
	}

	/*
	 * public void save() { MongoDataManager.get().saveTick(this); }
	 */
}
