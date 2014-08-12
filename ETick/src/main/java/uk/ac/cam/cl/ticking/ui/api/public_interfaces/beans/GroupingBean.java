package uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.cam.cl.ticking.ui.actors.Role;

public class GroupingBean {

	private List<String> crsids = new ArrayList<>();
	private List<Role> roles = new ArrayList<>();
	
	//Empty default constructor for Jackson
	public GroupingBean() {
		
	}

	/**
	 * 
	 * @return crsids
	 */
	public List<String> getCrsids() {
		return crsids;
	}

	/**
	 * 
	 * @param crsids
	 */
	public void setCrsids(List<String> crsids) {
		this.crsids = crsids;
	}

	/**
	 * 
	 * @return roles
	 */
	public List<Role> getRoles() {
		return roles;
	}

	/**
	 * 
	 * @param roles
	 */
	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}
	
}
