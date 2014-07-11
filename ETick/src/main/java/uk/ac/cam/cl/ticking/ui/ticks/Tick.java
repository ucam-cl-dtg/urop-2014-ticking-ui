package uk.ac.cam.cl.ticking.ui.ticks;

import java.util.Date;
import java.util.List;

import org.mongojack.ObjectId;

import uk.ac.cam.cl.ticking.ui.database.Database;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Tick {

	//FORMAT: 'group'_'name'
	@JsonProperty("_id")
	private String tid;
	
	private String group;
	
	private String repo, name;
	private Date deadline;
	private List<String> files;
	
	public Tick(@JsonProperty("name")String name, 
						@JsonProperty("group")String group, 
						@JsonProperty("repo")String repo, 
						@JsonProperty("deadline")Date deadline, 
						@JsonProperty("files")List<String> files) {
		
		this.tid = group+"_"+name;
		this.setName(name);
		this.setGroup(group);
		this.setRepo(repo);
		this.setDeadline(deadline);
		this.setFiles(files);
		
	}

	@JsonProperty("repo")
	public String getRepo() {
		return repo;
	}

	@JsonProperty("repo")
	public void setRepo(String repo) {
		this.repo = repo;
	}

	@JsonProperty("deadline")
	public Date getDeadline() {
		return deadline;
	}

	@JsonProperty("deadline")
	public void setDeadline(Date deadline) {
		this.deadline = deadline;
	}

	@JsonProperty("files")
	public List<String> getFiles() {
		return files;
	}

	@JsonProperty("files")
	public void setFiles(List<String> files) {
		this.files = files;
	}

	@JsonProperty("name")
	public String getName() {
		return name;
	}

	@JsonProperty("name")
	public void setName(String name) {
		this.name = name;
	}

	@JsonProperty("group")
	public String getGroup() {
		return group;
	}

	@JsonProperty("group")
	public void setGroup(String group) {
		this.group = group;
	}
	
	public String getTID() {
		return tid;
	}
	
	public void save() {
		Database.get().saveTick(this);
	}
}
