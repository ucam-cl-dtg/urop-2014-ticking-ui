package uk.ac.cam.cl.ticking.ui.actors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class acts as a tuple storing a user, a group, and their role in that
 * group.
 * 
 * Multiple groupings can exist containing the same user and/or group references
 * providing their roles are different as one user may have multiple roles
 * within a group.
 * 
 * @author tl364
 *
 */
public class Grouping {

	@JsonProperty("_id")
	private String _id;

	private String group;
	private String user;
	private Role role;

	/**
	 * Empty default constructor exposed as a JsonCreator as MongoJack cannot
	 * use Grouping(Group, User, Role).
	 */
	@JsonCreator
	public Grouping() {

	}

	/**
	 * Create a new instance of a Grouping object
	 * 
	 * This will generate a grouping identifier automatically and should only be
	 * used when a user is being added to a group.
	 * 
	 * @param group
	 * @param user
	 * @param role
	 */
	public Grouping(Group group, User user, Role role) {
		this(group.getGid(), user.getCrsid(), role);
	}

	/**
	 * Create a new instance of a Grouping object
	 * 
	 * This will generate a grouping identifier automatically and should only be
	 * used when a user is being added to a group.
	 * 
	 * @param group
	 * @param user
	 * @param role
	 */
	public Grouping(String gid, String crsid, Role role) {
		this.setGroup(gid);
		this.setUser(crsid);
		this.setRole(role);
		this._id = gid + "_" + role.toString() + "_"
				+ crsid;
	}

	/**
	 * @return group
	 */
	@JsonProperty("group")
	public String getGroup() {
		return group;
	}

	/**
	 * @param group
	 */
	@JsonProperty("group")
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * @return user
	 */
	@JsonProperty("user")
	public String getUser() {
		return user;
	}

	/**
	 * @param user
	 */
	@JsonProperty("user")
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return role
	 */
	@JsonProperty("role")
	public Role getRole() {
		return role;
	}

	/**
	 * @param role
	 */
	@JsonProperty("role")
	public void setRole(Role role) {
		this.role = role;
	}

}
