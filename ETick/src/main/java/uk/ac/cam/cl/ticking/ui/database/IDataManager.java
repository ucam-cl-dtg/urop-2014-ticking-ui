package uk.ac.cam.cl.ticking.ui.database;

import java.util.List;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.ticks.Submission;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

public interface IDataManager {

	public void saveUser(User cp);

	public void saveTick(Tick t);

	public void saveSubmission(Submission m);

	public void saveGroup(Group g);

	public void saveGrouping(Grouping g);

	// People getters

	// Get person by UNIQUE crsid
	public User getUser(String crsid);

	public List<User> getUsers(Group group);

	public List<User> getUsers(Group group, Role role);

	// Group getters

	// Get group by UNIQUE group id
	public Group getGroup(String gid);

	public List<Group> getGroups(User user);

	public List<Group> getGroups(User user, Role role);

	// Grouping getters

	public List<Grouping> getGroupings(User user);

	public List<Grouping> getGroupings(Group group);

	public List<Grouping> getGroupings(Role role);

	// Tick getters

	// Get a tick by UNIQUE id
	public Tick getTick(String tid);

	// Get all ticks in a group by Unique group id
	public List<Tick> getGroupTicks(String group);

	// Submission getters

	// Get all submissions in a group by UNIQUE group id
	public List<Submission> getGroupSubmissions(String group);

	// Get all submissions by who submitted them by UNIQUE crsid
	public List<Submission> getSubmissions(String author);

}
