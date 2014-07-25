package uk.ac.cam.cl.ticking.ui.ticks;

import java.util.Date;

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

	private String name;
	private String author;

	private String repo;
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
	 * @param repo
	 * @param deadline
	 * @param files
	 */
	@JsonCreator
	public Tick(@JsonProperty("name") String name,
			@JsonProperty("author") String author,
			@JsonProperty("repo") String repo,
			@JsonProperty("deadline") Date deadline) {

		this.setName(name);
		this.setAuthor(author);
		this.setRepo(repo);
		this.setDeadline(deadline);
		initTID();

	}

	/**
	 * Default constructor for Jackson JSON to POJO because java
	 */
	public Tick() {

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
	 * @return tid
	 */
	public String getTID() {
		return tid;
	}
	
	/**
	 * Initialises the TID field for the Tick object
	 */
	public void initTID() {
		this.tid = author + "," + name;
	}

	public static String replaceDelimeter(String tid) {
		return tid.replace(',', '/');
	}
}
