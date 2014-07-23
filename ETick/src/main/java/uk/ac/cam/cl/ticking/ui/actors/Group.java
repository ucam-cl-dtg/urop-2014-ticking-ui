package uk.ac.cam.cl.ticking.ui.actors;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class stores the name and identifier for a group
 * 
 * @author tl364
 *
 */
public class Group implements Comparable<Group>{

	@JsonProperty("_id")
	private String gid;

	private String name, creator;
	
	private List<String> ticks = new ArrayList<String>();

	/**
	 * Create a new instance of a Group object
	 * 
	 * This will generate a group identifier automatically and should only be
	 * used when a user is creating a new group.
	 * 
	 * @param name
	 *            - desired name for the group
	 */
	@JsonCreator
	public Group(@JsonProperty("name") String name, @JsonProperty("creator") String creator) {
		this.setName(name);
		this.setCreator(creator);
		this.gid = ObjectId.get().toString();
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
	 * @return gid
	 */
	public String getGid() {
		return gid;
	}
	
	/**
	 * @return creator
	 */
	public String getCreator() {
		return creator;
	}

	/**
	 * @param creator
	 */
	public void setCreator(String creator) {
		this.creator = creator;
	}
	
	/**
	 * @return ticks
	 */
	public List<String> getTicks() {
		return ticks;
	}
	
	/**
	 * @param tid
	 */
	public void addTick(String tid) {
		ticks.add(tid);
	}

	@Override
	public int compareTo(Group o) {
		return this.name.compareToIgnoreCase(o.name);
	}
}
