package uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans;

import java.util.List;

import org.joda.time.DateTime;

import uk.ac.cam.cl.ticking.ui.actors.User;

public class ExtensionReturnBean {

	private User user;
	private DateTime deadline;
	
	public ExtensionReturnBean() {
		//Default constructor for Jackson
	}
	
	public ExtensionReturnBean(User user, DateTime deadline) {
		this.user = user;
		this.deadline = deadline;
	}

	public User getUser() {
		return user;
	}

	public void setCrsids(User user) {
		this.user = user;
	}

	public DateTime getDeadline() {
		return deadline;
	}

	public void setDeadline(DateTime deadline) {
		this.deadline = deadline;
	}
	
}
