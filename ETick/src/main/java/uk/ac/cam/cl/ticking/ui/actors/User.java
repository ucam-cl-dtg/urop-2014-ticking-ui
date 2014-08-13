package uk.ac.cam.cl.ticking.ui.actors;

import java.util.List;

import org.joda.time.DateTime;

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
	private String photo;

	private String ssh;

	private DateTime ldap;

	private boolean hasLogged;

	private boolean isStudent;

	public User(String crsid) {
		this.setCrsid(crsid);
		this.setLdap(null);
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
	public User(String crsid, String surname, String regName,
			String displayName, String email, List<String> institutions,
			String college, boolean isStudent) {
		this.setCrsid(crsid);
		this.setSurname(surname);
		this.setRegName(regName);
		this.setDisplayName(displayName);
		this.setEmail(email);
		this.setInstitutions(institutions);
		this.setCollege(college);
		this.setIsStudent(isStudent);
		this.setLdap(DateTime.now());
	}

	/**
	 * Empty default constructor for Jackson
	 */
	public User() {

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
	public String getSurname() {
		return surname;
	}

	/**
	 * @param surname
	 */
	public void setSurname(String surname) {
		this.surname = surname;
	}

	/**
	 * @return regName
	 */
	public String getRegName() {
		return regName;
	}

	/**
	 * @param regName
	 */
	public void setRegName(String regName) {
		this.regName = regName;
	}

	/**
	 * @return displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @param displayName
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return email
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * @param email
	 */
	public void setEmail(String email) {
		this.email = email;
	}

	/**
	 * @return institutions
	 */
	public List<String> getInstitutions() {
		return institutions;
	}

	/**
	 * @param institutions
	 */
	public void setInstitutions(List<String> institutions) {
		this.institutions = institutions;
	}

	/**
	 * @return college
	 */
	public String getCollege() {
		return college;
	}

	/**
	 * @param college
	 */
	public void setCollege(String college) {
		this.college = college;
	}

	/**
	 * Gets photo as an encoded base 64 jpeg To display in soy template, use
	 * <img src="data:image/jpeg;base64,{$user.photo}" /> or similar
	 * 
	 * @return String photo
	 */
	public String getPhoto() {
		return photo;
	}

	/**
	 * 
	 * @param photo
	 */
	public void setPhoto(String photo) {
		this.photo = photo;
	}

	/**
	 * 
	 * @return ssh
	 */
	public String getSsh() {
		return ssh;
	}

	/**
	 * 
	 * @param ssh
	 */
	public void setSsh(String ssh) {
		this.ssh = ssh;
	}

	/**
	 * @return isStudent
	 */
	public boolean getIsStudent() {
		return isStudent;
	}

	/**
	 * @param isStudent
	 */
	public void setIsStudent(boolean isStudent) {
		this.isStudent = isStudent;
	}

	/**
	 * @return ldap
	 */
	public DateTime getLdap() {
		return ldap;
	}

	/**
	 * @param ldap
	 */
	public void setLdap(DateTime ldap) {
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(User o) {
		String compareMe = (surname == null) ? crsid : surname;
		String compareThem = (o.surname == null) ? o.crsid : o.surname;
		return compareMe.compareToIgnoreCase(compareThem);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof User)) {
			return false;
		}
		return this.crsid == ((User) o).crsid;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return crsid.hashCode();
	}

}
