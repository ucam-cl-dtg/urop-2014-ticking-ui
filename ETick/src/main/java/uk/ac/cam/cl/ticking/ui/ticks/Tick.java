package uk.ac.cam.cl.ticking.ui.ticks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import uk.ac.cam.cl.ticking.ui.actors.User;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class stores information regarding a tick.
 * 
 * @author tl364
 *
 */
public class Tick implements Comparable<Tick>{

	// FORMAT: 'author','name'
	@JsonProperty("_id")
	private String tickId;

	private String name;
	private String author;

	private String stubRepo, correctnessRepo;
	private DateTime deadline;
	private List<String> groups = new ArrayList<>();
	
	private Map<String, DateTime> extensions = new HashMap<>();
	
	private DateTime edited;

	/**
	 * 
	 * Create a new instance of the Tick object.
	 * 
	 * This will generate a tick identifier automatically and should only be
	 * used when a user is creating a new tick.
	 * 
	 * 
	 * @param name
	 * @param stubRepo
	 * @param deadline
	 * @param files
	 */
	@JsonCreator
	public Tick(@JsonProperty("name") String name,
			@JsonProperty("author") String author,
			@JsonProperty("deadline") DateTime deadline) {

		this.setName(name);
		this.setAuthor(author);
		this.setDeadline(deadline);
		initTickId();

	}

	/**
	 * Default constructor for Jackson JSON to POJO because java
	 */
	public Tick() {

	}

	/**
	 * @return stubRepo
	 */
	@JsonProperty("stubRepo")
	public String getStubRepo() {
		return stubRepo;
	}

	/**
	 * @param stubRepo
	 */
	@JsonProperty("stubRepo")
	public void setStubRepo(String stubRepo) {
		this.stubRepo = stubRepo;
	}
	
	/**
	 * @return correctnessRepo
	 */
	@JsonProperty("correctnessRepo")
	public String getCorrectnessRepo() {
		return correctnessRepo;
	}

	/**
	 * @param correctnessRepo
	 */
	@JsonProperty("correctnessRepo")
	public void setCorrectnessRepo(String correctnessRepo) {
		this.correctnessRepo = correctnessRepo;
	}

	/**
	 * @return deadline
	 */
	@JsonProperty("deadline")
	public DateTime getDeadline() {
		return deadline;
	}

	/**
	 * @param deadline
	 */
	@JsonProperty("deadline")
	public void setDeadline(DateTime deadline) {
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
	  * @return edited
	  */
	public DateTime getEdited() {
		return edited;
	}

	/**
	 * @param edited
	 */
	public void setEdited(DateTime edited) {
		this.edited = edited;
	}

	/**
	 * @return groups
	 */
	public List<String> getGroups() {
		return groups;
	}
	
	/**
	 * @param groups
	 */
	public void setGroups(List<String> groups) {
		this.groups = groups;
	}

	/**
	 * @param groupId
	 */
	public void addGroup(String groupId) {
		groups.add(groupId);
	}
	
	/**
	 * @param groupId
	 */
	public void removeGroup(String groupId) {
		groups.remove(groupId);
	}

	public Map<String, DateTime> getExtensions() {
		return extensions;
	}

	public void setExtensions(Map<String, DateTime> extensions) {
		this.extensions = extensions;
	}
	
	public void addExtension(String crsid, DateTime extension) {
		this.extensions.put(crsid, extension);
	}

	/**
	 * @return tickId
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
	
	@Override
	public int compareTo(Tick o) {
		return this.name.compareToIgnoreCase(o.name);
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
