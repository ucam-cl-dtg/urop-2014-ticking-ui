package uk.ac.cam.cl.ticking.ui.dao;

import java.util.List;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.ticks.Submission;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

/**
 * This class manages data access from persistent storage
 * 
 * @author tl364
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
	 *            - Group object to be saved into storage. If the group exists
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
	 * @param cp
	 *            - User object to be saved into storage.
	 * @throws DuplicateDataEntryException 
	 */
	public void insertUser(User cp) throws DuplicateDataEntryException;

	/**
	 * @param t
	 *            - Tick object to be saved into storage.
	 * @throws DuplicateDataEntryException 
	 */
	public void insertTick(Tick t) throws DuplicateDataEntryException;

	/**
	 * @param m
	 *            - Submission object to be saved into storage.
	 * @throws DuplicateDataEntryException 
	 */
	public void insertSubmission(Submission m) throws DuplicateDataEntryException;

	/**
	 * @param g
	 *            - Group object to be saved into storage.
	 * @throws DuplicateDataEntryException 
	 */
	public void insertGroup(Group g) throws DuplicateDataEntryException;

	/**
	 * @param g
	 *            - Grouping object to be saved into storage.
	 * @throws DuplicateDataEntryException 
	 */
	public void insertGrouping(Grouping g) throws DuplicateDataEntryException;

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
	 * Goes to the configured storage and attempts to find all users who are
	 * students
	 * 
	 * @return List of all users who are students
	 */
	public List<User> getStudents();

	/**
	 * Goes to the configured storage and attempts to find the users belonging
	 * to the group with the specified gid
	 * 
	 * @param gid
	 * @return List of users belonging to given group
	 */
	public List<User> getUsers(String gid);

	/**
	 * Goes to the configured storage and attempts to find the users belonging
	 * to the group with the specified gid and having the specified role
	 * 
	 * @param gid
	 * @param role
	 * @return List of users belonging to given group with given role
	 */
	public List<User> getUsers(String gid, Role role);

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
	 * user with the specified crsid is in
	 * 
	 * @param crsid
	 * @return List of groups that the given user is in
	 */
	public List<Group> getGroups(String crsid);

	/**
	 * Goes to the configured storage and attempts to find all groups
	 * 
	 * @param user
	 * @return List of all groups
	 */
	public List<Group> getGroups();

	/**
	 * Goes to the configured storage and attempts to find the groups belonging
	 * to the user with the specified crsid and having the specified role
	 * 
	 * @param crsid
	 * @param role
	 * @return List of groups that the given user is in with the given role
	 */
	public List<Group> getGroups(String crsid, Role role);

	/**
	 * Goes to the configured storage and attempts to find the roles for the
	 * user with the specified crsid in the group with the specified gid
	 * 
	 * @param gid
	 * @param crsid
	 * @return List of roles that the given user has in the given group
	 */
	public List<Role> getRoles(String gid, String crsid);

	/**
	 * Goes to the configured storage and attempts to find the groupings
	 * containing the {field} with the specified {param}
	 * 
	 * @param param
	 * @param field
	 *            - true: 'User' false: 'Group'
	 * @return List of groupings that the given user is in
	 */
	public List<Grouping> getGroupings(String param, boolean field);

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
	 * Goes to the configured storage and finds all of the stored ticks
	 * 
	 * @param group
	 * @return List of all ticks
	 */
	public List<Tick> getTicks();

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
