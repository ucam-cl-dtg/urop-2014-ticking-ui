package uk.ac.cam.cl.ticking.ui.dao;

import java.util.List;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
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
	public void saveUser(User u);

	/**
	 * @param t
	 *            - Tick object to be saved into storage. If the tick exists it
	 *            will be updated, else it will be created
	 */
	public void saveTick(Tick t);

	/**
	 * @param m
	 *            - Fork object to be saved into storage. If the
	 *            fork exists it will be updated, else it will be created
	 */
	public void saveFork(Fork s);

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
	public void saveGrouping(Grouping gr);

	/**
	 * @param cp
	 *            - User object to be saved into storage.
	 * @throws DuplicateDataEntryException
	 */
	public void insertUser(User u) throws DuplicateDataEntryException;

	/**
	 * @param t
	 *            - Tick object to be saved into storage.
	 * @throws DuplicateDataEntryException
	 */
	public void insertTick(Tick t) throws DuplicateDataEntryException;

	/**
	 * @param m
	 *            - Fork object to be saved into storage.
	 * @throws DuplicateDataEntryException
	 */
	public void insertFork(Fork s)
			throws DuplicateDataEntryException;

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
	public void insertGrouping(Grouping gr) throws DuplicateDataEntryException;

	/**
	 * Removes the user from the database, if purge is set, also removes all
	 * content created by that user.
	 * 
	 * @param crsid
	 * @param purge
	 */
	public void removeUser(String crsid, boolean purge);

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
	 * to the group with the specified groupId
	 * 
	 * @param groupId
	 * @return List of users belonging to given group
	 */
	public List<User> getUsers(String groupId);

	/**
	 * Goes to the configured storage and attempts to find the users belonging
	 * to the group with the specified groupId and having the specified role
	 * 
	 * @param groupId
	 * @param role
	 * @return List of users belonging to given group with given role
	 */
	public List<User> getUsers(String groupId, Role role);

	/**
	 * Removes the group from the database, also clearing up associated
	 * groupings and links to ticks.
	 * 
	 * @param groupId
	 */
	public void removeGroup(String groupId);

	/**
	 * Removes the user from that group
	 * 
	 * @param crsid
	 * @param groupId
	 */
	public void removeUserGroup(String crsid, String groupId);

	/**
	 * Goes to the configured storage and attempts to find a group with the
	 * specified id
	 * 
	 * @param groupId
	 * @return Group with given groupId
	 */
	public Group getGroup(String groupId);

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
	 * Removes the grouping formed from the given parameters, effectively
	 * revoking that role for that user in that group
	 * 
	 * @param crsid
	 * @param groupId
	 * @param role
	 */
	public void removeUserGroupRole(String crsid, String groupId, Role role);

	/**
	 * Goes to the configured storage and attempts to find the roles for the
	 * user with the specified crsid in the group with the specified groupId
	 * 
	 * @param groupId
	 * @param crsid
	 * @return List of roles that the given user has in the given group
	 */
	public List<Role> getRoles(String groupId, String crsid);

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
	 * Removes the tick from the database also clearing up any assocaitions with
	 * groups.
	 * 
	 * @param tickId
	 */
	public void removeTick(String tickId);

	/**
	 * Goes to the configured storage and attempts to find a tick with the
	 * specified tickId
	 * 
	 * @param tickId
	 * @return Tick with given tickId
	 */
	public Tick getTick(String tickId);

	/**
	 * Goes to the configured storage and attempts to find the ticks belonging
	 * to the specified group
	 * 
	 * @param group
	 * @return List of ticks in the given group
	 */
	public List<Tick> getGroupTicks(String group);

	/**
	 * 
	 * @param crsid
	 * @return all ticks authored by the user with the given crsid
	 */
	public List<Tick> getAuthorTicks(String crsid);

	/**
	 * Goes to the configured storage and finds all of the stored ticks
	 * 
	 * @param group
	 * @return List of all ticks
	 */
	public List<Tick> getTicks();

	/**
	 * Goes to the configured storage and attempts to find the fork with the specified id
	 * 
	 * @param forkId
	 * @return fork with the gievn Id
	 */
	public Fork getFork(String forkId);

	/**
	 * Goes to the configured storage and attempts to find the forks
	 * belonging to the specified user
	 * 
	 * @param author
	 * @return List of forks from the given author
	 */
	public List<Fork> getForks(String author);
	
	/**
	 * Stores the id/authCode pair.
	 * @param id
	 * @param authCode
	 */
	public void addAuthCode(String id, String authCode);
	
	/**
	 * 
	 * @param id
	 * @return The authCode corresponding to the given id. Null if none exists.
	 */
	public String getAuthCode(String id);

}
