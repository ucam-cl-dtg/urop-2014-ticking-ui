package uk.ac.cam.cl.ticking.ui.configuration;

import java.util.Arrays;

import uk.ac.cam.cl.dtg.ldap.LDAPUser;

public class AcademicTemplate implements ConfigurationFile {
	
	private String[] surname = {};
	private String[] regName = {};
	private String[] displayName = {};
	private String[] email = {};
	private String[] institutions = {};
	private String[] college = {};

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
		return (Arrays.asList(surname).contains(user.getSurname())
				|| Arrays.asList(regName).contains(user.getRegName())
				|| Arrays.asList(displayName).contains(user.getDisplayName())
				|| Arrays.asList(email).contains(user.getEmail())
				|| containsInstitutions
				|| Arrays.asList(college).contains(user.getCollegeName()));
	}
}

