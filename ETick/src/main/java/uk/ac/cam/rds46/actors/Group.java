package uk.ac.cam.rds46.actors;


import java.util.ArrayList;
import java.util.List;

import org.mongojack.DBCursor;

import uk.ac.cam.tl364.database.Database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.BasicDBObject;

public class Group {
	
	//FORMAT: 'author'_'name'
	@JsonProperty("_id")
	private String gid;
	
	private String name;
	
	@JsonCreator
	public Group(@JsonProperty("name")String name) {
		this.setName(name);
		this.gid = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getGid() {
		return gid;
	}

	public void setGid(String gid) {
		this.gid = gid;
	}
	
	public void save() {
		Database.get().saveGroup(this);
	}
	
	public static Group getGroup(String gid) {
		return Database.get().getGroup(gid);
	}
	
	public List<Group> getGroups(User user) {
		return Database.get().getGroups(user);
	}
	
	public List<Group> getGroups(User user, Role role) {
		return Database.get().getGroups(user, role);
	}
	
	@Override
	public boolean equals(Object g) {
		return (g instanceof Group && this.gid.equals(((Group)g).gid));
	}
}
