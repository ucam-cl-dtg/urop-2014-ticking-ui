package uk.ac.cam.cl.ticking.ui.actors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.mongojack.DBCursor;

import uk.ac.cam.cl.ticking.ui.dao.MongoDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Submission;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mongodb.BasicDBObject;

/**
 * @author tl364
 * 
 *         This class stores information about a user.
 * 
 * 
 *
 */
public class User {

	// FORMAT: crsid
	@JsonProperty("_id")
	private String crsid;

	private String name;

	/**
	 * Empty default constructor exposed as a JSonCreator as MongoJack requires
	 * this due to subclass Student
	 */
	@JsonCreator
	public User() {

	}

	/**
	 * Creates a new instance of the User object
	 * 
	 * This uses the CRSID as a user identifier and should only be
	 * used when a a new user is being added to the system.
	 * 
	 * @param crsid
	 * @param name
	 */
	public User(String crsid, String name) {
		this.crsid = crsid;
		this.name = name;
	}

	/**
	 * @return crsid
	 */
	public String getCrsid() {
		return crsid;
	}

	/**
	 * @param crsid
	 */
	public void setCrsid(String crsid) {
		this.crsid = crsid;
	}

	/**
	 * @return name
	 */
	@JsonProperty("name")
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 */
	@JsonProperty("name")
	public void setName(String name) {
		this.name = name;
	}

	/*
	 * public void save() { MongoDataManager.get().saveUser(this); }
	 * 
	 * public static User getUser(String crsid) { return
	 * MongoDataManager.get().getUser(crsid); }
	 * 
	 * public static List<User> getUsers(Group group) { return
	 * MongoDataManager.get().getUsers(group); }
	 * 
	 * public static List<User> getUsers(Group group, Role role) { return
	 * MongoDataManager.get().getUsers(group, role); }
	 * 
	 * public List<Submission> getSubmissions() { MongoDataManager database =
	 * MongoDataManager.get(); return database.getSubmissions(this.crsid); }
	 */

}
