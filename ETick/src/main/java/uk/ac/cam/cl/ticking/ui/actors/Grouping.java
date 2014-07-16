package uk.ac.cam.cl.ticking.ui.actors;

import java.util.ArrayList;
import java.util.List;

import org.mongojack.DBCursor;
import org.mongojack.DBRef;

import uk.ac.cam.cl.ticking.ui.dao.MongoDataManager;
import uk.ac.cam.cl.ticking.ui.util.Strings;

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

	private DBRef<Group, String> group;
	private DBRef<User, String> user;
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
		this.setGroup(new DBRef<Group, String>(group.getGid(),
				Strings.GROUPSCOLLECTION));
		this.setUser(new DBRef<User, String>(user.getCrsid(),
				Strings.USERSCOLLECTION));
		this.setRole(role);
		this._id = group.getGid() + "_" + role.toString() + "_"
				+ user.getCrsid();
	}

	/**
	 * @return group
	 */
	@JsonProperty("group")
	public DBRef<Group, String> getGroup() {
		return group;
	}

	/**
	 * @param group
	 */
	@JsonProperty("group")
	public void setGroup(DBRef<Group, String> group) {
		this.group = group;
	}

	/**
	 * @return actual group object from DBRef
	 */
	public Group fetchGroup() {
		return group.fetch();
	}

	/**
	 * @return user
	 */
	@JsonProperty("user")
	public DBRef<User, String> getUser() {
		return user;
	}

	/**
	 * @param user
	 */
	@JsonProperty("user")
	public void setUser(DBRef<User, String> user) {
		this.user = user;
	}

	/**
	 * @return actual user object from DBRef
	 */
	public User fetchUser() {
		return user.fetch();
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

	/*
	 * public static List<Grouping> getGroupings(User user) { return
	 * MongoDataManager.get().getGroupings(user); }
	 * 
	 * public static List<Grouping> getGroupings(Group group) { return
	 * MongoDataManager.get().getGroupings(group); }
	 * 
	 * public static List<Grouping> getGroupings(Role role) { return
	 * MongoDataManager.get().getGroupings(role); }
	 * 
	 * public void save() { MongoDataManager.get().saveGrouping(this); }
	 */

}
