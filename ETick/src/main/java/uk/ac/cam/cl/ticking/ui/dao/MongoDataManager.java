package uk.ac.cam.cl.ticking.ui.dao;

import java.util.ArrayList;
import java.util.List;

import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;
import org.mongojack.internal.MongoJackModule;

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
				database.getCollection(Strings.FORKSCOLLECTION),
				Fork.class, String.class, objectMapper);
		userColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.USERSCOLLECTION), User.class,
				String.class, objectMapper);
		groupColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.GROUPSCOLLECTION), Group.class,
				String.class, objectMapper);
		groupingColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.GROUPINGSCOLLECTION),
				Grouping.class, String.class, objectMapper);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#saveUser(uk.ac.cam.cl.ticking
	 * .ui.actors.User)
	 */
	@Override
	public void saveUser(User u) {
		userColl.save(u);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#saveTick(uk.ac.cam.cl.ticking
	 * .ui.ticks.Tick)
	 */
	@Override
	public void saveTick(Tick t) {
		tickColl.save(t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#saveFork(uk.ac.cam.cl.
	 * ticking.ui.ticks.Fork)
	 */
	@Override
	public void saveFork(Fork s) {
		forkColl.save(s);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#saveGroup(uk.ac.cam.cl.ticking
	 * .ui.actors.Group)
	 */
	@Override
	public void saveGroup(Group g) {
		// each group name must be unique
		groupColl.save(g);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#saveGrouping(uk.ac.cam.cl.ticking
	 * .ui.actors.Grouping)
	 */
	@Override
	public void saveGrouping(Grouping g) {
		groupingColl.save(g);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#insertUser(uk.ac.cam.cl.ticking
	 * .ui.actors.User)
	 */
	@Override
	public void insertUser(User u) throws DuplicateDataEntryException {
		try {
			userColl.insert(u);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("User");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#insertTick(uk.ac.cam.cl.ticking
	 * .ui.ticks.Tick)
	 */
	@Override
	public void insertTick(Tick t) throws DuplicateDataEntryException {
		try {
			tickColl.insert(t);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("Tick");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#insertFork(uk.ac.cam.cl
	 * .ticking.ui.ticks.Fork)
	 */
	@Override
	public void insertFork(Fork s)
			throws DuplicateDataEntryException {
		try {
			forkColl.insert(s);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("Fork");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#insertGroup(uk.ac.cam.cl.ticking
	 * .ui.actors.Group)
	 */
	@Override
	public void insertGroup(Group g) throws DuplicateDataEntryException {
		try {
			groupColl.insert(g);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("Group");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#insertGrouping(uk.ac.cam.cl.
	 * ticking.ui.actors.Grouping)
	 */
	@Override
	public void insertGrouping(Grouping g) throws DuplicateDataEntryException {
		try {
			groupingColl.insert(g);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("Grouping");
		}
	}

	// People

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#removeUser(java.lang.String,
	 * boolean)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.cam.cl.ticking.ui.dao.IDataManager#getUser(java.lang.String)
	 */
	@Override
	public User getUser(String crsid) {
		User u = null;
		u = userColl.findOne(new BasicDBObject("_id", crsid));
		return u;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.cam.cl.ticking.ui.dao.IDataManager#getUsers()
	 */
	@Override
	public List<User> getUsers() {
		DBCursor<User> cursor = userColl.find();
		return getUsers(cursor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.cam.cl.ticking.ui.dao.IDataManager#getStudents()
	 */
	@Override
	public List<User> getStudents() {
		DBCursor<User> cursor = userColl.find().is("isStudent", true);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.cam.cl.ticking.ui.dao.IDataManager#getUsers(java.lang.String)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.cam.cl.ticking.ui.dao.IDataManager#getUsers(java.lang.String,
	 * uk.ac.cam.cl.ticking.ui.actors.Role)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#removeGroup(java.lang.String)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#removeUserGroup(java.lang.String
	 * , java.lang.String)
	 */
	@Override
	public void removeUserGroup(String crsid, String groupId) {
		groupingColl.remove(new BasicDBObject().append("group", groupId)
				.append("user", crsid));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.cam.cl.ticking.ui.dao.IDataManager#getGroup(java.lang.String)
	 */
	@Override
	public Group getGroup(String groupId) {
		Group group = null;
		group = groupColl.findOne(new BasicDBObject("_id", groupId));
		return group;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.cam.cl.ticking.ui.dao.IDataManager#getGroups()
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.cam.cl.ticking.ui.dao.IDataManager#getGroups(java.lang.String)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.cam.cl.ticking.ui.dao.IDataManager#getGroups(java.lang.String,
	 * uk.ac.cam.cl.ticking.ui.actors.Role)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#removeUserGroupRole(java.lang
	 * .String, java.lang.String, uk.ac.cam.cl.ticking.ui.actors.Role)
	 */
	@Override
	public void removeUserGroupRole(String crsid, String groupId, Role role) {
		groupingColl.remove(new BasicDBObject().append("user", crsid)
				.append("group", groupId).append("role", role));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.cam.cl.ticking.ui.dao.IDataManager#getRoles(java.lang.String,
	 * java.lang.String)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#getGroupings(java.lang.String,
	 * boolean)
	 */
	@Override
	public List<Grouping> getGroupings(String param, boolean field) {
		String sField = field ? "user" : "group";
		DBCursor<Grouping> cursor = groupingColl.find().is(sField, param);
		return getGroupings(cursor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#getGroupings(uk.ac.cam.cl.ticking
	 * .ui.actors.Role)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#removeTick(java.lang.String)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.cam.cl.ticking.ui.dao.IDataManager#getTick(java.lang.String)
	 */
	@Override
	public Tick getTick(String tickId) {
		Tick tick = null;
		tick = tickColl.findOne(new BasicDBObject("_id", tickId));
		return tick;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#getAuthorTicks(java.lang.String)
	 */
	@Override
	public List<Tick> getAuthorTicks(String crsid) {
		DBCursor<Tick> cursor = tickColl.find().is("author", crsid);
		return getTicks(cursor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#getGroupTicks(java.lang.String)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.cam.cl.ticking.ui.dao.IDataManager#getTicks()
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#getFork(java.lang
	 * .String)
	 */
	@Override
	public Fork getFork(String forkId) {
		Fork fork = null;
		fork = forkColl.findOne(new BasicDBObject("_id", forkId));
		return fork;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.dao.IDataManager#getForks(java.lang.String)
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

}
