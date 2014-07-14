package uk.ac.cam.cl.ticking.ui.actors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mongojack.DBCursor;

import uk.ac.cam.cl.ticking.ui.database.MongoDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Submission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.BasicDBObject;

public class User {
	// FORMAT: crsid
	@JsonProperty("_id")
	private String crsid;

	private String name;

	private Map<Role, List<String>> groupSets;

	@JsonCreator
	public User() {

	}

	public User(String crsid, String name) {
		this.crsid = crsid;
		this.name = name;
	}

	/**
	 * GETTERS & SETTERS
	 * 
	 */

	public String getCrsid() {
		return crsid;
	}

	public void setCrsid(String crsid) {
		this.crsid = crsid;
	}

	public String getName() {
		return name;
	}

	@JsonProperty("name")
	public void setName(String name) {
		this.name = name;
	}

	public Map<Role, List<String>> getGroupSets() {
		return groupSets;
	}

	public void setGroupSets(Map<Role, List<String>> groupSets) {
		this.groupSets = groupSets;
	}

	public void add(Role role, String gid) {
		if (groupSets.get(role) == null) {
			groupSets.put(role, new ArrayList<String>());
		}
		groupSets.get(role).add(gid);
	}

	/*public void save() {
		MongoDataManager.get().saveUser(this);
	}

	public static User getUser(String crsid) {
		return MongoDataManager.get().getUser(crsid);
	}

	public static List<User> getUsers(Group group) {
		return MongoDataManager.get().getUsers(group);
	}

	public static List<User> getUsers(Group group, Role role) {
		return MongoDataManager.get().getUsers(group, role);
	}

	public List<Submission> getSubmissions() {
		MongoDataManager database = MongoDataManager.get();
		return database.getSubmissions(this.crsid);
	}*/

}
