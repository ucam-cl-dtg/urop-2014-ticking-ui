package uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;

import publicinterfaces.StaticOptions;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * This class stores the user defined aspects of a tick for use during creator.
 * 
 * @author tl364
 *
 */
public class TickBean {

	private String name;

	private DateTime deadline;
	private List<String> groups = new ArrayList<>();

	private Map<String, DateTime> extensions = new HashMap<>();

	private List<StaticOptions> checkstyleOpts = new ArrayList<>();

	/**
	 * @param name
	 * @param deadline
	 */
	@JsonCreator
	public TickBean(@JsonProperty("name") String name,
			@JsonProperty("deadline") DateTime deadline) {

		this.setName(name);
		this.setDeadline(deadline);

	}

	/**
	 * Default constructor for Jackson JSON to POJO because java
	 */
	public TickBean() {

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
	 * @return checkstyleOpts
	 */
	@JsonProperty("checkstyleOpts")
	public List<StaticOptions> getCheckstyleOpts() {
		return checkstyleOpts;
	}

	/**
	 * @param checkstyleOpts
	 */
	@JsonProperty("checkstyleOpts")
	public void setCheckstyleOpts(List<StaticOptions> checkstyleOpts) {
		this.checkstyleOpts = checkstyleOpts;
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

}
