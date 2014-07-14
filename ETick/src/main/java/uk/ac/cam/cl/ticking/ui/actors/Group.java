package uk.ac.cam.cl.ticking.ui.actors;

import java.util.ArrayList;
import java.util.List;

import org.mongojack.DBCursor;

import uk.ac.cam.cl.ticking.ui.dao.MongoDataManager;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.BasicDBObject;

/**
 * @author tl364
 * 
 *         This class stores the name and identifier for a group
 *
 */
public class Group {

	// FORMAT: 'author'_'name'
	@JsonProperty("_id")
	private String gid;

	private String name;

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
	public Group(@JsonProperty("name") String name) {
		this.setName(name);
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

	/*
	 * public void save() { MongoDataManager.get().saveGroup(this); }
	 * 
	 * public static Group getGroup(String gid) { return
	 * MongoDataManager.get().getGroup(gid); }
	 * 
	 * public List<Group> getGroups(User user) { return
	 * MongoDataManager.get().getGroups(user); }
	 * 
	 * public List<Group> getGroups(User user, Role role) { return
	 * MongoDataManager.get().getGroups(user, role); }
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object g) {
		return (g instanceof Group && this.gid.equals(((Group) g).gid));
	}
}
