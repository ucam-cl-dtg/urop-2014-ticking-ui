package uk.ac.cam.cl.ticking.ui.actors;

import uk.ac.cam.cl.ticking.ui.dao.MongoDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author tl364
 * 
 *         This class stores information about a student.
 * 
 *         Students extend regular users as further information regarding
 *         college, tripos and year is required for filtering which is not
 *         needed for academics.
 *
 */
public class Student extends User {
	private String college, tripos;
	private int yearOfStudy;

	/**
	 * 
	 * Creates a new instance of a Student object.
	 * 
	 * This uses the CRSID as a student identifier and should only be
	 * used when a a new student is being added to the system.
	 * 
	 * @param crsid
	 * @param name
	 * @param college
	 * @param tripos
	 * @param triposPart
	 */
	@JsonCreator
	public Student(@JsonProperty("crsid") String crsid,
			@JsonProperty("name") String name,
			@JsonProperty("college") String college,
			@JsonProperty("tripos") String tripos,
			@JsonProperty("year_of_study") int triposPart) {
		super(crsid, name);
		this.college = college;
		this.tripos = tripos;
		this.yearOfStudy = triposPart;
	}

	/**
	 * @return college
	 */
	@JsonProperty("college")
	public String getCollege() {
		return college;
	}

	/**
	 * @param college
	 */
	@JsonProperty("college")
	public void setCollege(String college) {
		this.college = college;
	}

	/**
	 * @return tripos
	 */
	@JsonProperty("tripos")
	public String getTripos() {
		return tripos;
	}

	/**
	 * @param tripos
	 */
	@JsonProperty("tripos")
	public void setTripos(String tripos) {
		this.tripos = tripos;
	}

	/**
	 * @return yearOfStudy
	 */
	@JsonProperty("year_of_study")
	public int getYearOfStudy() {
		return yearOfStudy;
	}

	/**
	 * @param yearOfStudy
	 */
	@JsonProperty("year_of_study")
	public void setYearOfStudy(int yearOfStudy) {
		this.yearOfStudy = yearOfStudy;
	}

}
