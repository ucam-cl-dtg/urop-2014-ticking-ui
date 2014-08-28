package uk.ac.cam.cl.ticking.ui.ticks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.TickBean;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class stores information regarding a tick.
 * 
 * @author tl364
 *
 */
public class Tick implements Comparable<Tick> {

	// FORMAT: 'author','name'
	@JsonProperty("_id")
	private String tickId;

	private String name;
	private String author;

	private String stubRepo;
	private DateTime deadline;
	private List<String> groups = new ArrayList<>();

	private Map<String, DateTime> extensions = new HashMap<>();

	private DateTime edited;
	
	private String externalReference;

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
	public Tick(String name, String author, DateTime deadline) {

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

	public Tick(TickBean bean) {
		this.setName(bean.getName());
		this.setDeadline(bean.getDeadline());
		this.setGroups(bean.getGroups());
		this.setExtensions(bean.getExtensions());
		this.setExternalReference(bean.getExternalReference());
	}

	/**
	 * @return stubRepo
	 */
	public String getStubRepo() {
		return stubRepo;
	}

	/**
	 * @param stubRepo
	 */
	public void setStubRepo(String stubRepo) {
		this.stubRepo = stubRepo;
	}

	/**
	 * @return deadline
	 */
	public DateTime getDeadline() {
		return deadline;
	}

	/**
	 * @param deadline
	 */
	public void setDeadline(DateTime deadline) {
		this.deadline = deadline;
	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
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

	public String getExternalReference() {
		return externalReference;
	}

	public void setExternalReference(String externalReference) {
		this.externalReference = externalReference;
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

	/**
	 * 
	 * @return extensions
	 */
	public Map<String, DateTime> getExtensions() {
		return extensions;
	}

	/**
	 * 
	 * @param extensions
	 */
	public void setExtensions(Map<String, DateTime> extensions) {
		this.extensions = extensions;
	}

	/**
	 * 
	 * @param crsid
	 * @param extension
	 */
	public void addExtension(String crsid, DateTime extension) {
		this.extensions.put(crsid, extension);
	}
	
	/**
	 * 
	 * @param crsid
	 */
	public void removeExtension(String crsid) {
		this.extensions.remove(crsid);
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
		String decodedName = name.replaceAll("[^a-zA-Z0-9\\s_-]", "");
		decodedName = decodedName.replaceAll("_", "__");
		decodedName = decodedName.replace(' ', '_');
		this.tickId = author + "," + decodedName;
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

	/**
	 * @param tickId
	 * @return The name of the tick extracted from the tick ID.
	 */
	public static String getNameFromID(String tickId) {
		return tickId.split(",")[1];
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Tick o) {
		return this.name.compareToIgnoreCase(o.name);
	}

	/**
	 * {@inheritDoc}
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return tickId.hashCode();
	}

}
