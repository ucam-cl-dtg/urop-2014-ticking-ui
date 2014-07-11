package uk.ac.cam.cl.ticking.ui.ticks;

import java.util.Date;

import org.mongojack.ObjectId;

import uk.ac.cam.cl.ticking.ui.database.Database;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Submission {

	//FORMAT: 'group'_'name'_'author'
	@JsonProperty("_id")
	private String sid;
	
	private String group;

	private String author;
	
	private String repo;
	private Date submitted;
	private String tick;
	private boolean unitPass;
	private boolean humanPass;
	
	public Submission(@JsonProperty("author")String author,
							@JsonProperty("group")String group, 
							@JsonProperty("repo")String repo, 
							@JsonProperty("submitted")Date submitted, 
							@JsonProperty("tick")Tick tick, 
							@JsonProperty("unitPass")boolean unitPass) {
		
		this.sid = group+"_"+tick.getName()+"_"+author;
		this.setGroup(group);
		this.setAuthor(author);
		this.setRepo(repo);
		this.setSubmitted(submitted);
		this.setTick(tick);
		this.setUnitPass(unitPass);
		this.setHumanPass(false);
	}

	@JsonProperty("repo")
	public String getRepo() {
		return repo;
	}

	@JsonProperty("repo")
	public void setRepo(String repo) {
		this.repo = repo;
	}

	@JsonProperty("submitted")
	public Date getSubmitted() {
		return submitted;
	}

	@JsonProperty("submitted")
	public void setSubmitted(Date submitted) {
		this.submitted = submitted;
	}

	@JsonProperty("tick")
	public Tick getTick() {
		Database database = Database.get();
		return database.getTick(this.tick);
	}

	@JsonProperty("tick")
	public void setTick(Tick tick) {
		this.tick = tick.getTID();
	}

	@JsonProperty("unit_pass")
	public boolean getUnitPass() {
		return unitPass;
	}

	@JsonProperty("unit_pass")
	public void setUnitPass(boolean ePass) {
		this.unitPass = ePass;
	}

	@JsonProperty("human_pass")
	public boolean getHumanPass() {
		return humanPass;
	}

	@JsonProperty("human_pass")
	public void setHumanPass(boolean hPass) {
		this.humanPass = hPass;
	}

	@JsonProperty("author")
	public String getAuthor() {
		return author;
	}

	@JsonProperty("author")
	public void setAuthor(String author) {
		this.author = author;
	}

	@JsonProperty("group")
	public String getGroup() {
		return group;
	}

	@JsonProperty("group")
	public void setGroup(String group) {
		this.group = group;
	}
	
	public void save() {
		Database.get().saveSubmission(this);
	}
}
