package uk.ac.cam.cl.ticking.ui.dao;

import java.util.ArrayList;
import java.util.List;

import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;
import org.mongojack.internal.MongoJackModule;

import uk.ac.cam.cl.ticking.signups.AuthCodeMap;
import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoException;

public class MongoDataManager implements IDataManager {

	private final JacksonDBCollection<Tick, String> tickColl;
	private final JacksonDBCollection<User, String> userColl;
	private final JacksonDBCollection<Group, String> groupColl;
	private final JacksonDBCollection<Grouping, String> groupingColl;
	private final JacksonDBCollection<Fork, String> forkColl;
	private final JacksonDBCollection<AuthCodeMap, String> authCodeColl;

	/**
	 * @param database
	 */
	@Inject
	public MongoDataManager(DB database) {
		ObjectMapper objectMapper = new ObjectMapper().registerModule(
				new JodaModule()).configure(
				SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		MongoJackModule.configure(objectMapper);

		tickColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.TICKSCOLLECTION), Tick.class,
				String.class, objectMapper);
		forkColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.FORKSCOLLECTION), Fork.class,
				String.class, objectMapper);
		userColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.USERSCOLLECTION), User.class,
				String.class, objectMapper);
		groupColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.GROUPSCOLLECTION), Group.class,
				String.class, objectMapper);
		groupingColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.GROUPINGSCOLLECTION),
				Grouping.class, String.class, objectMapper);
		authCodeColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.AUTHCODESCOLLECTION),
				AuthCodeMap.class, String.class, objectMapper);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void saveUser(User u) {
		userColl.save(u);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void saveTick(Tick t) {
		tickColl.save(t);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void saveFork(Fork s) {
		forkColl.save(s);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void saveGroup(Group g) {
		// each group name must be unique
		groupColl.save(g);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void saveGrouping(Grouping g) {
		groupingColl.save(g);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void insertUser(User u) throws DuplicateDataEntryException {
		try {
			userColl.insert(u);
		} catch (MongoException.DuplicateKey duplicate) {
			throw new DuplicateDataEntryException("User");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void insertTick(Tick t) throws DuplicateDataEntryException {
		try {
			tickColl.insert(t);
		} catch (MongoException.DuplicateKey duplicate) {
			throw new DuplicateDataEntryException("Tick");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void insertFork(Fork s) throws DuplicateDataEntryException {
		try {
			forkColl.insert(s);
		} catch (MongoException.DuplicateKey duplicate) {
			throw new DuplicateDataEntryException("Fork");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void insertGroup(Group g) throws DuplicateDataEntryException {
		try {
			groupColl.insert(g);
		} catch (MongoException.DuplicateKey duplicate) {
			throw new DuplicateDataEntryException("Group");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void insertGrouping(Grouping g) throws DuplicateDataEntryException {
		try {
			groupingColl.insert(g);
		} catch (MongoException.DuplicateKey duplicate) {
			throw new DuplicateDataEntryException("Grouping");
		}
	}

	// People

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeUser(String crsid, boolean purge) {
		userColl.remove(new BasicDBObject().append("_id", crsid));
		groupingColl.remove(new BasicDBObject().append("user", crsid));
		if (purge) {
			tickColl.remove(new BasicDBObject().append("author", crsid));
			groupColl.remove(new BasicDBObject().append("creator", crsid));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public User getUser(String crsid) {
		User u = null;
		u = userColl.findOne(new BasicDBObject("_id", crsid));
		return u;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<User> getUsers() {
		DBCursor<User> cursor = userColl.find();
		return getUsers(cursor);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<User> getStudents() {
		DBCursor<User> cursor = userColl.find().is("isStudent", true);
		return getUsers(cursor);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<User> getAdmins() {
		DBCursor<User> cursor = userColl.find().is("admin", true);
		return getUsers(cursor);
	}

	/**
	 * @param cursor
	 * @return
	 */
	private List<User> getUsers(DBCursor<User> cursor) {
		List<User> users = new ArrayList<User>();
		while (cursor.hasNext()) {
			User u = cursor.next();
			users.add(u);
		}
		cursor.close();
		return users;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<User> getUsers(String groupId) {
		List<User> users = new ArrayList<User>();
		List<Grouping> groupings = getGroupings(groupId, false);
		List<String> added = new ArrayList<String>();
		for (Grouping grouping : groupings) {
			String crsid = grouping.getUser();
			if (!added.contains(crsid)) {
				added.add(crsid);
				users.add(getUser(crsid));
			}
		}
		return users;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<User> getUsers(String groupId, Role role) {
		List<User> users = new ArrayList<User>();
		DBCursor<Grouping> cursor = groupingColl.find().is("group", groupId)
				.is("role", role);
		List<Grouping> groupings = getGroupings(cursor);
		for (Grouping grouping : groupings) {
			users.add(getUser(grouping.getUser()));
		}
		return users;
	}

	// Group

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeGroup(String groupId) {
		Group group = getGroup(groupId);
		for (String tickId : group.getTicks()) {
			Tick tick = getTick(tickId);
			tick.removeGroup(groupId);
			saveTick(tick);
		}

		groupColl.remove(new BasicDBObject().append("_id", groupId));
		groupingColl.remove(new BasicDBObject().append("group", groupId));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeUserGroup(String crsid, String groupId) {
		groupingColl.remove(new BasicDBObject().append("group", groupId)
				.append("user", crsid));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Group getGroup(String groupId) {
		Group group = null;
		group = groupColl.findOne(new BasicDBObject("_id", groupId));
		return group;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Group> getGroups() {
		DBCursor<Group> cursor = groupColl.find();
		return getGroups(cursor);
	}

	/**
	 * @param cursor
	 * @return
	 */
	private List<Group> getGroups(DBCursor<Group> cursor) {
		List<Group> groups = new ArrayList<Group>();
		while (cursor.hasNext()) {
			Group group = cursor.next();
			groups.add(group);
		}
		cursor.close();
		return groups;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Group> getGroups(String crsid) {
		List<Group> groups = new ArrayList<Group>();
		List<Grouping> groupings = getGroupings(crsid, true);
		List<String> added = new ArrayList<String>();
		for (Grouping grouping : groupings) {
			String groupId = grouping.getGroup();
			if (!added.contains(groupId)) {
				added.add(groupId);
				groups.add(getGroup(groupId));
			}
		}
		return groups;

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Group> getGroups(String crsid, Role role) {
		List<Group> groups = new ArrayList<Group>();
		DBCursor<Grouping> cursor = groupingColl.find().is("user", crsid)
				.is("role", role);
		List<Grouping> groupings = getGroupings(cursor);
		for (Grouping grouping : groupings) {
			groups.add(getGroup(grouping.getGroup()));
		}
		return groups;
	}

	// Roles

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeUserGroupRole(String crsid, String groupId, Role role) {
		groupingColl.remove(new BasicDBObject().append("user", crsid)
				.append("group", groupId).append("role", role));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Role> getRoles(String groupId, String crsid) {
		List<Role> roles = new ArrayList<Role>();
		DBCursor<Grouping> cursor = groupingColl.find().is("user", crsid)
				.is("group", groupId);
		List<Grouping> groupings = getGroupings(cursor);
		for (Grouping grouping : groupings) {
			roles.add(grouping.getRole());
		}
		return roles;
	}

	// Grouping getters

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Grouping> getGroupings(String param, boolean field) {
		String sField = field ? "user" : "group";
		DBCursor<Grouping> cursor = groupingColl.find().is(sField, param);
		return getGroupings(cursor);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Grouping> getGroupings(Role role) {
		DBCursor<Grouping> cursor = groupingColl.find().is("role", role);
		return getGroupings(cursor);
	}

	/**
	 * @param cursor
	 * @return
	 */
	private List<Grouping> getGroupings(DBCursor<Grouping> cursor) {
		List<Grouping> groupings = new ArrayList<Grouping>();
		while (cursor.hasNext()) {
			Grouping grouping = cursor.next();
			groupings.add(grouping);
		}
		cursor.close();
		return groupings;
	}

	// Tick

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeTick(String tickId) {
		Tick tick = getTick(tickId);
		for (String groupId : tick.getGroups()) {
			Group group = getGroup(groupId);
			group.removeTick(tickId);
			saveGroup(group);
		}
		tickColl.remove(new BasicDBObject().append("_id", tickId));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Tick getTick(String tickId) {
		Tick tick = null;
		tick = tickColl.findOne(new BasicDBObject("_id", tickId));
		return tick;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Tick> getAuthorTicks(String crsid) {
		DBCursor<Tick> cursor = tickColl.find().is("author", crsid);
		return getTicks(cursor);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Tick> getGroupTicks(String groupId) {
		Group group = getGroup(groupId);
		List<Tick> ticks = new ArrayList<Tick>();
		for (String tickId : group.getTicks()) {
			ticks.add(getTick(tickId));
		}
		return ticks;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Tick> getTicks() {
		DBCursor<Tick> cursor = tickColl.find();
		return getTicks(cursor);
	}

	/**
	 * @param cursor
	 * @return
	 */
	private List<Tick> getTicks(DBCursor<Tick> cursor) {
		List<Tick> ticks = new ArrayList<Tick>();
		while (cursor.hasNext()) {
			Tick tick = cursor.next();
			ticks.add(tick);
		}
		cursor.close();
		return ticks;
	}

	// Fork getters

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Fork getFork(String forkId) {
		Fork fork = null;
		fork = forkColl.findOne(new BasicDBObject("_id", forkId));
		return fork;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Fork> getForks(String author) {
		List<Fork> forks = new ArrayList<Fork>();
		DBCursor<Fork> cursor = forkColl.find().is("author", author);
		while (cursor.hasNext()) {
			Fork fork = cursor.next();
			forks.add(fork);
		}
		cursor.close();
		return forks;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addAuthCode(String id, String authCode) {
		authCodeColl.save(new AuthCodeMap(id, authCode));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getAuthCode(String id) {
		try {
			return authCodeColl.findOneById(id).getAuthCode();
		} catch (MongoException e) {
			return null; // as per javadoc
		}
	}
	
	@Override
	public void removeAuthCodeCorrespondingTo(String id) {
	    authCodeColl.removeById(id);
	}

}
