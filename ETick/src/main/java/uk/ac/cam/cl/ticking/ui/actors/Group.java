package uk.ac.cam.cl.ticking.ui.actors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class stores the name and identifier for a group
 * 
 * @author tl364
 *
 */
public class Group implements Comparable<Group>{

	// FORMAT: 'creator'/'name'
	@JsonProperty("_id")
	private String gid;

	private String name, creator;

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
		this.gid = name;
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
	 * @param gid
	 */
	public void setGid(String gid) {
		this.gid = gid;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object g) {
		return (g instanceof Group && this.gid.equals(((Group) g).gid));
	}

	@Override
	public int compareTo(Group o) {
		return this.name.compareToIgnoreCase(o.name);
	}
}
