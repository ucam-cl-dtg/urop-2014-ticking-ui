package uk.ac.cam.cl.ticking.ui.configuration;

import java.util.Arrays;

import uk.ac.cam.cl.dtg.ldap.LDAPUser;

/**
 * This class is a template for an academic. If any of field in the user match
 * any of the array elements for the respective fields as defined by a json
 * object, that user becomes an academic
 * 
 * @author tl364
 *
 */
public class AcademicTemplate implements ConfigurationFile {

	private String[] crsid = {};
	private String[] surname = {};
	private String[] regName = {};
	private String[] displayName = {};
	private String[] email = {};
	private String[] institutions = {};
	private String[] college = {};

	/**
	 * @return crsid
	 */
	public String[] getCrsid() {
		return crsid;
	}

	/**
	 * @param crsid
	 */
	public void setCrsid(String[] crsid) {
		this.crsid = crsid;
	}

	/**
	 * @return possible surnames
	 */
	public String[] getSurname() {
		return surname;
	}

	/**
	 * @param surname
	 */
	public void setSurname(String[] surname) {
		this.surname = surname;
	}

	/**
	 * @return possible regNames
	 */
	public String[] getRegName() {
		return regName;
	}

	/**
	 * @param regName
	 */
	public void setRegName(String[] regName) {
		this.regName = regName;
	}

	/**
	 * @return possible displayNames
	 */
	public String[] getDisplayName() {
		return displayName;
	}

	/**
	 * @param displayName
	 */
	public void setDisplayName(String[] displayName) {
		this.displayName = displayName;
	}

	/**
	 * @return possible emails
	 */
	public String[] getEmail() {
		return email;
	}

	/**
	 * @param email
	 */
	public void setEmail(String[] email) {
		this.email = email;
	}

	/**
	 * @return possible institutions
	 */
	public String[] getInstitutions() {
		return institutions;
	}

	/**
	 * @param institutions
	 */
	public void setInstitutions(String[] institutions) {
		this.institutions = institutions;
	}

	/**
	 * @return possible colleges
	 */
	public String[] getCollege() {
		return college;
	}

	/**
	 * @param college
	 */
	public void setCollege(String[] college) {
		this.college = college;
	}

	/**
	 * @param user
	 * @return whether the user fits the academic template
	 */
	public boolean represents(LDAPUser user) {
		boolean containsInstitutions = false;
		for (String inst : institutions) {
			if (user.getInstitutions().contains(inst)) {
				containsInstitutions = true;
				break;
			}
		}
		return (Arrays.asList(crsid).contains(user.getID())
				|| Arrays.asList(surname).contains(user.getSurname())
				|| Arrays.asList(regName).contains(user.getRegName())
				|| Arrays.asList(displayName).contains(user.getDisplayName())
				|| Arrays.asList(email).contains(user.getEmail())
				|| containsInstitutions || Arrays.asList(college).contains(
				user.getCollegeName()));
	}
}
