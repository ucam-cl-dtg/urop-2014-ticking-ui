package uk.ac.cam.cl.ticking.ui.database;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.mongojack.DBCursor;
import org.mongojack.JacksonDBCollection;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.ticks.Submission;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.MongoClient;

public class Database {

	private MongoClient mongoClient;
	private DB db;
	private JacksonDBCollection<Tick, String> tickColl;
	private JacksonDBCollection<User, String> userColl;
	private JacksonDBCollection<Group, String> groupColl;
	private JacksonDBCollection<Grouping, String> groupingColl;
	private JacksonDBCollection<Submission, String> subColl;
	public static Database database;

	private Database() throws UnknownHostException {
		mongoClient = new MongoClient("localhost", 27017);
		db = mongoClient.getDB("ETick");

		tickColl = JacksonDBCollection.wrap(
				db.getCollection(Strings.TICKSCOLLECTION), Tick.class,
				String.class);
		subColl = JacksonDBCollection.wrap(
				db.getCollection(Strings.SUBMISSIONSCOLLECTION),
				Submission.class, String.class);
		userColl = JacksonDBCollection.wrap(
				db.getCollection(Strings.USERSCOLLECTION), User.class,
				String.class);
		groupColl = JacksonDBCollection.wrap(
				db.getCollection(Strings.GROUPSCOLLECTION), Group.class,
				String.class);
		groupingColl = JacksonDBCollection.wrap(
				db.getCollection(Strings.GROUPINGSCOLLECTION), Grouping.class,
				String.class);
	}

	public static Database get() {
		if (database == null) {
			try {
				database = new Database();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
		return database;
	}

	public void saveUser(User cp) {
		userColl.save(cp);
	}

	public void saveTick(Tick t) {
		tickColl.save(t);
	}

	public void saveSubmission(Submission m) {
		subColl.save(m);
	}

	public void saveGroup(Group g) {
		groupColl.save(g);
	}

	public void saveGrouping(Grouping g) {
		groupingColl.save(g);
	}

	// People getters

	// Get person by UNIQUE crsid
	public User getUser(String crsid) {
		User p = null;
		p = userColl.findOne(new BasicDBObject("_id", crsid));
		return p;
	}

	public List<User> getUsers(Group group) {
		List<User> us = new ArrayList<User>();
		List<Grouping> grs = getGroupings(group);
		for (Grouping gr : grs) {
			us.add(gr.fetchUser());
		}
		return us;
	}

	public List<User> getUsers(Group group, Role role) {
		List<User> us = new ArrayList<User>();
		DBCursor<Grouping> cursor = groupingColl.find()
				.is("group.$id", group.getGid()).is("role", role);
		List<Grouping> grs = getGroupings(cursor);
		for (Grouping gr : grs) {
			us.add(gr.fetchUser());
		}
		return us;
	}

	// Group getters

	// Get group by UNIQUE group id
	public Group getGroup(String gid) {
		Group g = null;
		g = groupColl.findOne(new BasicDBObject("_id", gid));
		return g;
	}

	public List<Group> getGroups(User user) {
		List<Group> gs = new ArrayList<Group>();
		List<Grouping> grs = getGroupings(user);
		for (Grouping gr : grs) {
			gs.add(gr.fetchGroup());
		}
		return gs;

	}

	public List<Group> getGroups(User user, Role role) {
		List<Group> gs = new ArrayList<Group>();
		DBCursor<Grouping> cursor = groupingColl.find()
				.is("user.$id", user.getCrsid()).is("role", role);
		List<Grouping> grs = getGroupings(cursor);
		for (Grouping gr : grs) {
			gs.add(gr.fetchGroup());
		}
		return gs;
	}

	// Grouping getters

	public List<Grouping> getGroupings(User user) {
		DBCursor<Grouping> cursor = groupingColl.find().is("user.$id",
				user.getCrsid());
		return getGroupings(cursor);
	}

	public List<Grouping> getGroupings(Group group) {
		DBCursor<Grouping> cursor = groupingColl.find().is("group.$id",
				group.getGid());
		return getGroupings(cursor);
	}

	public List<Grouping> getGroupings(Role role) {
		DBCursor<Grouping> cursor = groupingColl.find().is("role", role);
		return getGroupings(cursor);
	}

	public List<Grouping> getGroupings(DBCursor<Grouping> cursor) {
		List<Grouping> gs = new ArrayList<Grouping>();
		while (cursor.hasNext()) {
			Grouping g = cursor.next();
			gs.add(g);
		}
		cursor.close();
		return gs;
	}

	// Tick getters

	// Get a tick by UNIQUE id
	public Tick getTick(String tid) {
		Tick t = null;
		t = tickColl.findOne(new BasicDBObject("_id", tid));
		return t;
	}

	// Get all ticks in a group by Unique group id
	public List<Tick> getGroupTicks(String group) {
		List<Tick> ts = new ArrayList<Tick>();
		DBCursor<Tick> cursor = tickColl.find().is("group", group);
		while (cursor.hasNext()) {
			Tick t = cursor.next();
			ts.add(t);
		}
		cursor.close();
		return ts;
	}

	/*
	 * public List<Tick> getAuthorTicks(String author) { List<Tick> ts = new
	 * ArrayList<Tick>(); DBCursor<Tick> cursor = tickColl.find().is("author",
	 * author); while (cursor.hasNext()) { Tick t = cursor.next(); ts.add(t); }
	 * return ts; }
	 */

	// Submission getters

	// Get all submissions in a group by UNIQUE group id
	public List<Submission> getGroupSubmissions(String group) {
		List<Submission> ss = new ArrayList<Submission>();
		DBCursor<Submission> cursor = subColl.find().is("group", group);
		while (cursor.hasNext()) {
			Submission s = cursor.next();
			ss.add(s);
		}
		cursor.close();
		return ss;
	}

	// Get all submissions by who submitted them by UNIQUE crsid
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
