package uk.ac.cam.cl.ticking.ui.ticks;

import java.util.Date;

import uk.ac.cam.cl.ticking.ui.actors.User;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class stores information regarding a tick.
 * 
 * @author tl364
 *
 */
public class Tick {

	// FORMAT: 'author','name'
	@JsonProperty("_id")
	private String tickId;

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
		initTickId();

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
	public String getTickId() {
		return tickId;
	}

	/**
	 * Initialises the tickId field for the Tick object
	 */
	public void initTickId() {
		this.tickId = author + "," + name;
	}

	/**
	 * Takes , separated tickIds as they are stored in our system and turns them
	 * into / separated ids for use with the other APIs
	 * 
	 * @param tickId
	 * @return tickId in / separated format
	 */
	public static String replaceDelimeter(String tickId) {
		return tickId.replace(',', '/');
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Tick)) {
			return false;
		}
		return this.tickId == ((Tick) o).tickId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return tickId.hashCode();
	}
}
