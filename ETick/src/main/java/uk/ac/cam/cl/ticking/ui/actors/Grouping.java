package uk.ac.cam.cl.ticking.ui.actors;

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
	 * Empty default constructor for Jackson
	 */
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
		this(group.getGroupId(), user.getCrsid(), role);
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
	public Grouping(String groupId, String crsid, Role role) {
		this.setGroup(groupId);
		this.setUser(crsid);
		this.setRole(role);
		this._id = groupId + "_" + role.toString() + "_" + crsid;
	}

	/**
	 * @return group
	 */
	public String getGroup() {
		return group;
	}

	/**
	 * @param group
	 */
	public void setGroup(String group) {
		this.group = group;
	}

	/**
	 * @return user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return role
	 */
	public Role getRole() {
		return role;
	}

	/**
	 * @param role
	 */
	public void setRole(Role role) {
		this.role = role;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Grouping)) {
			return false;
		}
		return this._id.equals(((Grouping) o)._id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return _id.hashCode();
	}

}
