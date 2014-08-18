package uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans;

import java.util.List;

import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;

public class UserRoleBean {

	private User user;
	private List<Role> roles;
	
	public UserRoleBean() {
		//Default constructor for Jackson
	}
	
	public UserRoleBean(User user, List<Role> roles) {
		this.setUser(user);
		this.setRoles(roles);
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public List<Role> getRoles() {
		return roles;
	}

	public void setRoles(List<Role> roles) {
		this.roles = roles;
	}
}
