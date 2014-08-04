package uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class stores the name and identifier for a group
 * 
 * @author tl364
 *
 */
public class GroupBean {

	private String name;
	
	private String info;

	private List<String> ticks = new ArrayList<String>();

	/**
	 * Create a new instance of a Group object
	 * 
	 * This will generate a group identifier automatically and should only be
	 * used when a user is creating a new group.
	 * 
	 * @param name
	 * @param info
	 */
	@JsonCreator
	public GroupBean(@JsonProperty("name") String name, @JsonProperty("info") String info) {
		this.setName(name);
		this.setInfo(info);
	}
	
	//Default constructor for Jackson
	public GroupBean() {
		
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
	 * @return ticks
	 */
	public List<String> getTicks() {
		return ticks;
	}

	/**
	 * @param tickId
	 */
	public void addTick(String tickId) {
		ticks.add(tickId);
	}
	
	/**
	 * @param tickId
	 */
	public void removeTick(String tickId) {
		ticks.remove(tickId);
	}

	/**
	 * @return info
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * @param info
	 */
	public void setInfo(String info) {
		this.info = info;
	}
}
