package uk.ac.cam.cl.ticking.ui.dao;

import java.util.List;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.ticks.Submission;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

/**
 * @author tl364
 * 
 *         This class manages data access from persistent storage
 *
 */
public interface IDataManager {

	/**
	 * @param cp
	 *            - User object to be saved into storage. If the user exists it
	 *            will be updated, else it will be created
	 */
	public void saveUser(User cp);

	/**
	 * @param t
	 *            - Tick object to be saved into storage. If the tick exists it
	 *            will be updated, else it will be created
	 */
	public void saveTick(Tick t);

	/**
	 * @param m
	 *            - Submission object to be saved into storage. If the
	 *            submission exists it will be updated, else it will be created
	 */
	public void saveSubmission(Submission m);

	/**
	 * @param g
	 *            - Grou[ object to be saved into storage. If the group exists
	 *            it will be updated, else it will be created
	 */
	public void saveGroup(Group g);

	/**
	 * @param g
	 *            - Grouping object to be saved into storage. If the grouping
	 *            exists it will be updated, else it will be created
	 */
	public void saveGrouping(Grouping g);

	/**
	 * Goes to the configured storage and attempts to find a user with the
	 * specified id
	 * 
	 * @param crsid
	 * @return User with given crsid
	 */
	public User getUser(String crsid);
	
	/**
	 * Goes to the configured storage and attempts to find all users
	 * 
	 * @return List of all users
	 */
	public List<User> getUsers();
	
	/**
	 * Goes to the configured storage and attempts to find all users who are students
	 * 
	 * @return List of all users who are students
	 */
	public List<User> getStudents();
	
	
	/**
	 * Goes to the configured storage and attempts to find the users belonging
	 * to the specified group
	 * 
	 * @param group
	 * @return List of users belonging to given group
	 */
	public List<User> getUsers(Group group);

	/**
	 * Goes to the configured storage and attempts to find the users belonging
	 * to the specified group with the specified role
	 * 
	 * @param group
	 * @param role
	 * @return List of users belonging to given group with given role
	 */
	public List<User> getUsers(Group group, Role role);

	/**
	 * Goes to the configured storage and attempts to find a group with the
	 * specified id
	 * 
	 * @param gid
	 * @return Group with given gid
	 */
	public Group getGroup(String gid);

	/**
	 * Goes to the configured storage and attempts to find the groups that the
	 * specified user is in
	 * 
	 * @param user
	 * @return List of groups that the given user is in
	 */
	public List<Group> getGroups(User user);
	
	/**
	 * Goes to the configured storage and attempts to find all groups
	 * 
	 * @param user
	 * @return List of all groups
	 */
	public List<Group> getGroups();

	/**
	 * Goes to the configured storage and attempts to find the groups belonging
	 * to the specified group with the specified role
	 * 
	 * @param user
	 * @param role
	 * @return List of groups that the given user is in with the given role
	 */
	public List<Group> getGroups(User user, Role role);

	// Grouping getters

	/**
	 * Goes to the configured storage and attempts to find the groupings
	 * containing the specified user
	 * 
	 * @param user
	 * @return List of groupings that the given user is in
	 */
	public List<Grouping> getGroupings(User user);

	/**
	 * Goes to the configured storage and attempts to find the groupings
	 * containing the specified group
	 * 
	 * @param group
	 * @return List of groupings that the given group is in
	 */
	public List<Grouping> getGroupings(Group group);

	/**
	 * Goes to the configured storage and attempts to find the groupings
	 * containing the specified role
	 * 
	 * @param role
	 * @return List of groupings that the given role is in
	 */
	public List<Grouping> getGroupings(Role role);

	/**
	 * Goes to the configured storage and attempts to find a tick with the
	 * specified tid
	 * 
	 * @param tid
	 * @return Tick with given tid
	 */
	public Tick getTick(String tid);

	/**
	 * Goes to the configured storage and attempts to find the ticks belonging
	 * to the specified group
	 * 
	 * @param group
	 * @return List of ticks in the given group
	 */
	public List<Tick> getGroupTicks(String group);

	/**
	 * Goes to the configured storage and attempts to find the submissions
	 * belonging to the specified group
	 * 
	 * @param group
	 * @return List of submissions in the given group
	 */
	public List<Submission> getGroupSubmissions(String group);

	/**
	 * Goes to the configured storage and attempts to find the submissions
	 * belonging to the specified user
	 * 
	 * @param author
	 * @return List of submissions from the given author
	 */
	public List<Submission> getSubmissions(String author);

}
