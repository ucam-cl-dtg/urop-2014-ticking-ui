package uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class stores the user defined aspects of a group for use during creator.
 * 
 * @author tl364
 *
 */
public class GroupBean {

	private String name;

	private String info;

	private List<String> ticks = new ArrayList<String>();

	/**
	 * 
	 * @param name
	 * @param info
	 */
	public GroupBean(String name, String info) {
		this.setName(name);
		this.setInfo(info);
	}

	// Default constructor for Jackson
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
