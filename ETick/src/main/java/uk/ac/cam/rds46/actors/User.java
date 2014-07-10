package uk.ac.cam.rds46.actors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mongojack.DBCursor;

import uk.ac.cam.tl364.database.Database;
import uk.ac.cam.tl364.ticks.Submission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.BasicDBObject;

public class User
{
	//FORMAT: crsid
	@JsonProperty("_id")
	private String crsid;
	
	private String name;
	
	private Map<Role,List<String>> groupSets;

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
	
	public String getCrsid()
	{
		return crsid;
	}

	public void setCrsid(String crsid)
	{
		this.crsid = crsid;
	}

	public String getName()
	{
		return name;
	}

	@JsonProperty("name")
	public void setName(String name)
	{
		this.name = name;
	}
	
	public Map<Role,List<String>> getGroupSets() {
		return groupSets;
	}

	public void setGroupSets(Map<Role,List<String>> groupSets) {
		this.groupSets = groupSets;
	}
	
	public void add(Role role, String gid) {
		if (groupSets.get(role) == null) groupSets.put(role, new ArrayList<String>());
		groupSets.get(role).add(gid);
	}
	
	public void save() {
		Database.get().saveUser(this);
	}
	
	public static User getUser(String crsid) {
		return Database.get().getUser(crsid);
	}
	
	public static List<User> getUsers(Group group) {
		return Database.get().getUsers(group);
	}
	
	public static List<User> getUsers(Group group, Role role) {
		return Database.get().getUsers(group, role);
	}
	
	public List<Submission> getSubmissions() {
		Database database = Database.get();
		return database.getSubmissions(this.crsid);
	}

}
