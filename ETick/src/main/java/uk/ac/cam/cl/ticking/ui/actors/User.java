package uk.ac.cam.cl.ticking.ui.actors;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class stores information about a user.
 * 
 * @author tl364
 *
 */
public class User implements Comparable<User> {

	// FORMAT: crsid
	@JsonProperty("_id")
	private String crsid;

	private String surname;
	private String regName;
	private String displayName;
	private String email;
	private List<String> institutions;
	private String college;

	private boolean ldap;
	
	private boolean hasLogged;

	private boolean isStudent;

	public User(String crsid) {
		this.setCrsid(crsid);
		this.setLdap(false);
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
			@JsonProperty("regName") String regName,
			@JsonProperty("displayName") String displayName,
			@JsonProperty("email") String email,
			@JsonProperty("institutions") List<String> institutions,
			@JsonProperty("college") String college,
			@JsonProperty("isStudent") boolean isStudent) {
		this.setCrsid(crsid);
		this.setSurname(surname);
		this.setRegName(regName);
		this.setDisplayName(displayName);
		this.setEmail(email);
		this.setInstitutions(institutions);
		this.setCollege(college);
		this.setIsStudent(isStudent);
		this.setLdap(true);
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
	@JsonProperty("regName")
	public String getRegName() {
		return regName;
	}

	/**
	 * @param regName
	 */
	@JsonProperty("regName")
	public void setRegName(String regName) {
		this.regName = regName;
	}

	/**
	 * @return displayName
	 */
	@JsonProperty("displayName")
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @param displayName
	 */
	@JsonProperty("displayName")
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
	@JsonProperty("isStudent")
	public boolean getIsStudent() {
		return isStudent;
	}

	/**
	 * @param isStudent
	 */
	@JsonProperty("isStudent")
	public void setIsStudent(boolean isStudent) {
		this.isStudent = isStudent;
	}

	/**
	 * @return ldap
	 */
	public boolean isLdap() {
		return ldap;
	}

	/**
	 * @param ldap
	 */
	public void setLdap(boolean ldap) {
		this.ldap = ldap;
	}

	/**
	 * @return hasLogged
	 */
	public boolean isHasLogged() {
		return hasLogged;
	}

	/**
	 * @param hasLogged
	 */
	public void setHasLogged(boolean hasLogged) {
		this.hasLogged = hasLogged;
	}

	@Override
	public int compareTo(User o) {
		String compareMe = (surname == null) ? crsid : surname;
		String compareThem = (o.surname == null) ? o.crsid : o.surname;
		return compareMe.compareToIgnoreCase(compareThem);
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof User)) {
			return false;
		}
		return this.crsid == ((User)o).crsid;
	}
	
	@Override
	public int hashCode() {
		return crsid.hashCode();
	}

}
