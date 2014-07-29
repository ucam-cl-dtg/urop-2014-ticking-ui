package uk.ac.cam.cl.ticking.ui.dao;

import java.util.ArrayList;
import java.util.List;

import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.ticks.Submission;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoException;

public class MongoDataManager implements IDataManager {

	private final JacksonDBCollection<Tick, String> tickColl;
	private final JacksonDBCollection<User, String> userColl;
	private final JacksonDBCollection<Group, String> groupColl;
	private final JacksonDBCollection<Grouping, String> groupingColl;
	private final JacksonDBCollection<Submission, String> subColl;

	@Inject
	public MongoDataManager(DB database) {
		tickColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.TICKSCOLLECTION), Tick.class,
				String.class);
		subColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.SUBMISSIONSCOLLECTION),
				Submission.class, String.class);
		userColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.USERSCOLLECTION), User.class,
				String.class);
		groupColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.GROUPSCOLLECTION), Group.class,
				String.class);
		groupingColl = JacksonDBCollection.wrap(
				database.getCollection(Strings.GROUPINGSCOLLECTION),
				Grouping.class, String.class);
	}

	@Override
	public void saveUser(User u) {
		userColl.save(u);
	}

	@Override
	public void saveTick(Tick t) {
		tickColl.save(t);
	}

	@Override
	public void saveSubmission(Submission s) {
		subColl.save(s);
	}

	@Override
	public void saveGroup(Group g) {
		// each group name must be unique
		groupColl.ensureIndex(new BasicDBObject("name", 1), null, true);
		groupColl.save(g);
	}

	@Override
	public void saveGrouping(Grouping g) {
		groupingColl.save(g);
	}

	@Override
	public void insertUser(User u) throws DuplicateDataEntryException {
		try {
			userColl.insert(u);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("User");
		}
	}

	@Override
	public void insertTick(Tick t) throws DuplicateDataEntryException {
		try {
			tickColl.insert(t);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("Tick");
		}
	}

	@Override
	public void insertSubmission(Submission s)
			throws DuplicateDataEntryException {
		try {
			subColl.insert(s);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("Submission");
		}
	}

	@Override
	public void insertGroup(Group g) throws DuplicateDataEntryException {
		try {
			// each group name must be unique
			groupColl.ensureIndex(new BasicDBObject("name", 1), null, true);
			groupColl.insert(g);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("Group");
		}
	}

	@Override
	public void insertGrouping(Grouping g) throws DuplicateDataEntryException {
		try {
			groupingColl.insert(g);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("Grouping");
		}
	}

	// People getters

	@Override
	public User getUser(String crsid) {
		User u = null;
		u = userColl.findOne(new BasicDBObject("_id", crsid));
		return u;
	}

	@Override
	public List<User> getUsers() {
		DBCursor<User> cursor = userColl.find();
		return getUsers(cursor);
	}

	@Override
	public List<User> getStudents() {
		DBCursor<User> cursor = userColl.find().is("isStudent", true);
		return getUsers(cursor);
	}

	private List<User> getUsers(DBCursor<User> cursor) {
		List<User> users = new ArrayList<User>();
		while (cursor.hasNext()) {
			User u = cursor.next();
			users.add(u);
		}
		cursor.close();
		return users;
	}

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

	// Group getters

	@Override
	public Group getGroup(String groupId) {
		Group group = null;
		group = groupColl.findOne(new BasicDBObject("_id", groupId));
		return group;
	}

	@Override
	public Group getGroupByName(String name) {
		Group group = null;
		group = groupColl.findOne(new BasicDBObject("name", name));
		return group;
	}

	@Override
	public List<Group> getGroups() {
		DBCursor<Group> cursor = groupColl.find();
		return getGroups(cursor);
	}

	private List<Group> getGroups(DBCursor<Group> cursor) {
		List<Group> groups = new ArrayList<Group>();
		while (cursor.hasNext()) {
			Group group = cursor.next();
			groups.add(group);
		}
		cursor.close();
		return groups;
	}

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

	// Role getters

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

	@Override
	public List<Grouping> getGroupings(String param, boolean field) {
		String sField = field ? "user" : "group";
		DBCursor<Grouping> cursor = groupingColl.find().is(sField, param);
		return getGroupings(cursor);
	}

	@Override
	public List<Grouping> getGroupings(Role role) {
		DBCursor<Grouping> cursor = groupingColl.find().is("role", role);
		return getGroupings(cursor);
	}

	private List<Grouping> getGroupings(DBCursor<Grouping> cursor) {
		List<Grouping> groupings = new ArrayList<Grouping>();
		while (cursor.hasNext()) {
			Grouping grouping = cursor.next();
			groupings.add(grouping);
		}
		cursor.close();
		return groupings;
	}

	// Tick getters

	@Override
	public Tick getTick(String tickId) {
		Tick tick = null;
		tick = tickColl.findOne(new BasicDBObject("_id", tickId));
		return tick;
	}

	@Override
	public List<Tick> getGroupTicks(String groupId) {
		Group group = getGroup(groupId);
		List<Tick> ticks = new ArrayList<Tick>();
		for (String tickId : group.getTicks()) {
			ticks.add(getTick(tickId));
		}
		return ticks;
	}

	@Override
	public List<Tick> getTicks() {
		DBCursor<Tick> cursor = tickColl.find();
		return getTicks(cursor);
	}

	private List<Tick> getTicks(DBCursor<Tick> cursor) {
		List<Tick> ticks = new ArrayList<Tick>();
		while (cursor.hasNext()) {
			Tick tick = cursor.next();
			ticks.add(tick);
		}
		cursor.close();
		return ticks;
	}

	// Submission getters

	@Override
	public List<Submission> getGroupSubmissions(String groupId) {
		List<Submission> submissions = new ArrayList<Submission>();
		DBCursor<Submission> cursor = subColl.find().is("group", groupId);
		while (cursor.hasNext()) {
			Submission submission = cursor.next();
			submissions.add(submission);
		}
		cursor.close();
		return submissions;
	}

	@Override
	public List<Submission> getSubmissions(String author) {
		List<Submission> submissions = new ArrayList<Submission>();
		DBCursor<Submission> cursor = subColl.find().is("author", author);
		while (cursor.hasNext()) {
			Submission submission = cursor.next();
			submissions.add(submission);
		}
		cursor.close();
		return submissions;
	}

}
