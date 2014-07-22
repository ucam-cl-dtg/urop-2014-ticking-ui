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
	public void saveUser(User cp) {
		userColl.save(cp);
	}

	@Override
	public void saveTick(Tick t) {
		tickColl.save(t);
	}

	@Override
	public void saveSubmission(Submission m) {
		subColl.save(m);
	}

	@Override
	public void saveGroup(Group g) {
		groupColl.save(g);
	}

	@Override
	public void saveGrouping(Grouping g) {
		groupingColl.save(g);
	}
	
	@Override
	public void insertUser(User cp) throws DuplicateDataEntryException {
		try {
			userColl.insert(cp);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("User");
		}
	}

	@Override
	public void insertTick(Tick t) throws DuplicateDataEntryException {
		try{
			tickColl.save(t);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("Tick");
		}
	}

	@Override
	public void insertSubmission(Submission m) throws DuplicateDataEntryException {
		try {
			subColl.save(m);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("Submission");
		}
	}

	@Override
	public void insertGroup(Group g) throws DuplicateDataEntryException {
		try {
			groupColl.save(g);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("Group");
		}
	}

	@Override
	public void insertGrouping(Grouping g) throws DuplicateDataEntryException {
		try {
			groupingColl.save(g);
		} catch (MongoException duplicate) {
			throw new DuplicateDataEntryException("Grouping");
		}
	}

	// People getters

	@Override
	public User getUser(String crsid) {
		User p = null;
		p = userColl.findOne(new BasicDBObject("_id", crsid));
		return p;
	}

	@Override
	public List<User> getUsers() {
		DBCursor<User> cursor = userColl.find();
		return getUsers(cursor);
	}

	@Override
	public List<User> getStudents() {
		DBCursor<User> cursor = userColl.find().is("is_student", true);
		return getUsers(cursor);
	}

	private List<User> getUsers(DBCursor<User> cursor) {
		List<User> us = new ArrayList<User>();
		while (cursor.hasNext()) {
			User u = cursor.next();
			us.add(u);
		}
		cursor.close();
		return us;
	}

	@Override
	public List<User> getUsers(String gid) {
		List<User> us = new ArrayList<User>();
		List<Grouping> grs = getGroupings(gid, false);
		List<String> ss = new ArrayList<String>();
		for (Grouping gr : grs) {
			String u = gr.getUser();
			if (!ss.contains(u)) {
				ss.add(u);
				us.add(getUser(u));
			}
		}
		return us;
	}

	@Override
	public List<User> getUsers(String gid, Role role) {
		List<User> us = new ArrayList<User>();
		DBCursor<Grouping> cursor = groupingColl.find()
				.is("group", gid).is("role", role);
		List<Grouping> grs = getGroupings(cursor);
		for (Grouping gr : grs) {
			us.add(getUser(gr.getUser()));
		}
		return us;
	}

	// Group getters

	@Override
	public Group getGroup(String gid) {
		Group g = null;
		g = groupColl.findOne(new BasicDBObject("_id", gid));
		return g;
	}

	@Override
	public List<Group> getGroups() {
		DBCursor<Group> cursor = groupColl.find();
		return getGroups(cursor);
	}

	private List<Group> getGroups(DBCursor<Group> cursor) {
		List<Group> gs = new ArrayList<Group>();
		while (cursor.hasNext()) {
			Group g = cursor.next();
			gs.add(g);
		}
		cursor.close();
		return gs;
	}

	@Override
	public List<Group> getGroups(String crsid) {
		List<Group> gs = new ArrayList<Group>();
		List<String> ss = new ArrayList<String>();
		List<Grouping> grs = getGroupings(crsid, true);
		for (Grouping gr : grs) {
			String g = gr.getGroup();
			if (!ss.contains(g)) {
				ss.add(g);
				gs.add(getGroup(g));
			}
		}
		return gs;

	}

	@Override
	public List<Group> getGroups(String crsid, Role role) {
		List<Group> gs = new ArrayList<Group>();
		DBCursor<Grouping> cursor = groupingColl.find()
				.is("user", crsid).is("role", role);
		List<Grouping> grs = getGroupings(cursor);
		for (Grouping gr : grs) {
			gs.add(getGroup(gr.getGroup()));
		}
		return gs;
	}

	// Role getters

	@Override
	public List<Role> getRoles(String gid, String crsid) {
		List<Role> rs = new ArrayList<Role>();
		DBCursor<Grouping> cursor = groupingColl.find()
				.is("user", crsid).is("group", gid);
		List<Grouping> grs = getGroupings(cursor);
		for (Grouping gr :grs) {
			rs.add(gr.getRole());
		}
		return rs;
	}

	// Grouping getters

	@Override
	public List<Grouping> getGroupings(String param, boolean field) {
		String sField = field ? "user" : "group";
		DBCursor<Grouping> cursor = groupingColl.find().is(sField,
				param);
		return getGroupings(cursor);
	}

	@Override
	public List<Grouping> getGroupings(Role role) {
		DBCursor<Grouping> cursor = groupingColl.find().is("role", role);
		return getGroupings(cursor);
	}

	private List<Grouping> getGroupings(DBCursor<Grouping> cursor) {
		List<Grouping> gs = new ArrayList<Grouping>();
		while (cursor.hasNext()) {
			Grouping g = cursor.next();
			gs.add(g);
		}
		cursor.close();
		return gs;
	}

	// Tick getters

	@Override
	public Tick getTick(String tid) {
		Tick t = null;
		t = tickColl.findOne(new BasicDBObject("_id", tid));
		return t;
	}

	@Override
	public List<Tick> getGroupTicks(String gid) {
		List<Tick> ts = new ArrayList<Tick>();
		DBCursor<Tick> cursor = tickColl.find().is("group", gid);
		while (cursor.hasNext()) {
			Tick t = cursor.next();
			ts.add(t);
		}
		cursor.close();
		return ts;
	}

	// Submission getters

	@Override
	public List<Submission> getGroupSubmissions(String gid) {
		List<Submission> ss = new ArrayList<Submission>();
		DBCursor<Submission> cursor = subColl.find().is("group", gid);
		while (cursor.hasNext()) {
			Submission s = cursor.next();
			ss.add(s);
		}
		cursor.close();
		return ss;
	}

	@Override
	public List<Submission> getSubmissions(String author) {
		List<Submission> ss = new ArrayList<Submission>();
		DBCursor<Submission> cursor = subColl.find().is("author", author);
		while (cursor.hasNext()) {
			Submission s = cursor.next();
			ss.add(s);
		}
		cursor.close();
		return ss;
	}

}
