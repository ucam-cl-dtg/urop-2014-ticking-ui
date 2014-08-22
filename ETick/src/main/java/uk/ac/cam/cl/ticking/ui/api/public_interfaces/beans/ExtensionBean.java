package uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans;

import java.util.List;

import org.joda.time.DateTime;

public class ExtensionBean {

	private List<String>  crsids;
	private DateTime deadline;
	
	public ExtensionBean() {
		//Default constructor for Jackson
	}

	public List<String> getCrsids() {
		return crsids;
	}

	public void setCrsids(List<String> crsids) {
		this.crsids = crsids;
	}

	public DateTime getDeadline() {
		return deadline;
	}

	public void setDeadline(DateTime deadline) {
		this.deadline = deadline;
	}
	
}
