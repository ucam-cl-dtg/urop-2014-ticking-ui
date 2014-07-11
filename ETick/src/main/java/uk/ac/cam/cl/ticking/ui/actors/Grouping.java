package uk.ac.cam.cl.ticking.ui.actors;

import java.util.ArrayList;
import java.util.List;

import org.mongojack.DBCursor;
import org.mongojack.DBRef;

import uk.ac.cam.cl.ticking.ui.database.Database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Grouping {
	
	@JsonProperty("_id")
	private String _id;
	
	
	private DBRef<Group,String> group;
	private DBRef<User,String> user;
	private Role role;
	
	//Empty default constructor required for MongoJack as Grouping(Group, User, Role) cannot be used
	@JsonCreator
	public Grouping() {
		
	}
	
	public Grouping(Group g, User u, Role role) {
		this.setGroup(new DBRef<Group,String>(g.getGid(),"Groups"));
		this.setUser(new DBRef<User,String>(u.getCrsid(), "Users"));
		this.setRole(role);
		this._id = g.getGid()+"_"+role.toString()+"_"+u.getCrsid();
	}

	@JsonProperty("group")
	public DBRef<Group,String> getGroup() {
		return group;
	}

	@JsonProperty("group")
	public void setGroup(DBRef<Group,String> group) {
		this.group = group;
	}
	
	public Group fetchGroup() {
		return group.fetch();
	}

	@JsonProperty("user")
	public DBRef<User,String> getUser() {
		return user;
	}

	@JsonProperty("user")
	public void setUser(DBRef<User,String> user) {
		this.user = user;
	}
	
	public User fetchUser() {
		return user.fetch();
	}

	@JsonProperty("role")
	public Role getRole() {
		return role;
	}

	@JsonProperty("role")
	public void setRole(Role role) {
		this.role = role;
	}
	
	public static List<Grouping> getGroupings(User user) {
		return Database.get().getGroupings(user);
	}
	
	public static List<Grouping> getGroupings(Group group) {
		return Database.get().getGroupings(group);
	}
	
	public static List<Grouping> getGroupings(Role role) {
		return Database.get().getGroupings(role);
	}
	
	public void save() {
		Database.get().saveGrouping(this);
	}

}
