package uk.ac.cam.cl.ticking.ui.actors;

import uk.ac.cam.cl.ticking.ui.database.Database;
import uk.ac.cam.cl.ticking.ui.ticks.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Student extends User
{
	private String college, tripos;
	private int yearOfStudy;

	@JsonCreator
	public Student(@JsonProperty("crsid")String crsid, @JsonProperty("name")String name, @JsonProperty("college")String college, @JsonProperty("tripos")String tripos, @JsonProperty("tripos_part")int triposPart) {
		super(crsid, name);
		this.college = college;
		this.tripos = tripos;
		this.yearOfStudy = triposPart;
	}


	@JsonProperty("college")
	public String getCollege()
	{
		return college;
	}
	
	@JsonProperty("college")
	public void setCollege(String college)
	{
		this.college = college;
	}
	
	@JsonProperty("tripos")
	public String getTripos()
	{
		return tripos;
	}
	
	@JsonProperty("tripos")
	public void setTripos(String tripos)
	{
		this.tripos = tripos;
	}
	
	@JsonProperty("tripos_part")
	public int getTriposPart()
	{
		return yearOfStudy;
	}
	
	@JsonProperty("tripos_part")
	public void setTriposPart(int triposPart)
	{
		this.yearOfStudy = triposPart;
	}

}
