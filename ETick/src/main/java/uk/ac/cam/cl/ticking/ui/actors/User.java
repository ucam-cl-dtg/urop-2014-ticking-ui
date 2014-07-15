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
 * This class stores information about a user.
 * 
 * @author tl364
 *
 */
public class User {

	// FORMAT: crsid
	@JsonProperty("_id")
	private String crsid;

	private String surname;
	private String regName;
	private String displayName;
	private String email;
	private List<String> institutions;
	private String college;

	private boolean isStudent;

	public User(String crsid) {
		this.setCrsid(crsid);
	}

	/**
	 * Creates a new instance of the User object
	 * 
	 * This uses the CRSID as a user identifier and should only be used when a a
	 * new user is being added to the system.
	 * 
	 * @param crsid
	 * @param surname
	 * @param regName
	 * @param displayName
	 * @param email
	 * @param institutions
	 * @param college
	 * @param status
	 */
	@JsonCreator
	public User(@JsonProperty("_id") String crsid,
			@JsonProperty("surname") String surname,
			@JsonProperty("reg_name") String regName,
			@JsonProperty("display_name") String displayName,
			@JsonProperty("email") String email,
			@JsonProperty("institutions") List<String> institutions,
			@JsonProperty("college") String college,
			@JsonProperty("is_student") boolean isStudent) {
		this.setCrsid(crsid);
		this.setSurname(surname);
		this.setRegName(regName);
		this.setDisplayName(displayName);
		this.setEmail(email);
		this.setInstitutions(institutions);
		this.setCollege(college);
		this.setIsStudent(isStudent);
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
	 * @return surname
	 */
	@JsonProperty("surname")
	public String getSurname() {
		return surname;
	}

	/**
	 * @param surname
	 */
	@JsonProperty("surname")
	public void setSurname(String surname) {
		this.surname = surname;
	}

	/**
	 * @return regName
	 */
	@JsonProperty("reg_name")
	public String getRegName() {
		return regName;
	}

	/**
	 * @param regName
	 */
	@JsonProperty("reg_name")
	public void setRegName(String regName) {
		this.regName = regName;
	}

	/**
	 * @return displayName
	 */
	@JsonProperty("display_name")
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @param displayName
	 */
	@JsonProperty("display_name")
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return email
	 */
	@JsonProperty("email")
	public String getEmail() {
		return email;
	}

	/**
	 * @param email
	 */
	@JsonProperty("email")
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return institutions
	 */
	@JsonProperty("institutions")
	public List<String> getInstitutions() {
		return institutions;
	}

	/**
	 * @param institutions
	 */
	@JsonProperty("institutions")
	public void setInstitutions(List<String> institutions) {
		this.institutions = institutions;
	}

	/**
	 * @return college
	 */
	@JsonProperty("college")
	public String getCollege() {
		return college;
	}

	/**
	 * @param college
	 */
	@JsonProperty("college")
	public void setCollege(String college) {
		this.college = college;
	}

	/**
	 * @return isStudent
	 */
	@JsonProperty("is_student")
	public boolean getIsStudent() {
		return isStudent;
	}

	/**
	 * @param isStudent
	 */
	@JsonProperty("is_student")
	public void setIsStudent(boolean isStudent) {
		this.isStudent = isStudent;
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
