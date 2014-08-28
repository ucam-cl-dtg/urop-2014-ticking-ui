package uk.ac.cam.cl.ticking.ui.actors;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class stores the name, identifier, creator and info for a group as well
 * as stamps whenever editing takes place.
 * 
 * @author tl364
 *
 */
public class Group implements Comparable<Group> {

	@JsonProperty("_id")
	private String groupId;

	private String name, creator;

	private String info;

	private DateTime edited;
	private String editedBy;

	private List<String> ticks = new ArrayList<String>();

	/**
	 * Create a new instance of a Group object
	 * 
	 * This will generate a group identifier automatically and should only be
	 * used when a user is creating a new group.
	 * 
	 * @param name
	 * @param creator
	 */
	public Group(String name, String creator) {
		this.setName(name);
		this.setCreator(creator);
		this.groupId = ObjectId.get().toString();
	}

	// Default constructor for Jackson
	public Group() {

	}

	/**
	 * @return name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return groupId
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * @return creator
	 */
	public String getCreator() {
		return creator;
	}

	/**
	 * @param creator
	 */
	public void setCreator(String creator) {
		this.creator = creator;
	}

	/**
	 * @return ticks
	 */
	public List<String> getTicks() {
		return ticks;
	}

	/**
	 * @return ticks
	 */
	public void setTicks(List<String> ticks) {
		this.ticks = ticks;
	}

	/**
	 * @param tickId
	 */
	public void addTick(String tickId) {
		ticks.add(tickId);
	}

	/**
	 * @param tickId
	 */
	public void removeTick(String tickId) {
		ticks.remove(tickId);
	}

	/**
	 * @return info
	 */
	public String getInfo() {
		return info;
	}

	/**
	 * @param info
	 */
	public void setInfo(String info) {
		this.info = info;
	}

	/**
	 * @return edited
	 */
	public DateTime getEdited() {
		return edited;
	}

	/**
	 * @param edited
	 */
	public void setEdited(DateTime edited) {
		this.edited = edited;
	}

	/**
	 * @return editedBy
	 */
	public String getEditedBy() {
		return editedBy;
	}

	/**
	 * @param editedBy
	 */
	public void setEditedBy(String editedBy) {
		this.editedBy = editedBy;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int compareTo(Group o) {
		return this.name.compareToIgnoreCase(o.name);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof Group)) {
			return false;
		}
		return this.groupId.equals(((Group) o).groupId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return groupId.hashCode();
	}
}
