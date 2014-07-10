package uk.ac.cam.tl364.database;

import java.util.List;

import uk.ac.cam.rds46.actors.Group;
import uk.ac.cam.rds46.actors.Grouping;
import uk.ac.cam.rds46.actors.Role;
import uk.ac.cam.rds46.actors.User;

public class DatabaseTest
{

	private final static Database db = Database.get();

	public static void testDB() {

		addEntry("Java 1A", "rds46", "Raahil", Role.OVERVIEW);
		addEntry("Java 1A", "rds46", "Raahil", Role.AUTHOR);
		addEntry("Java 1B", "rds46", "Raahil", Role.SUBMITTER);
		addEntry("Java 2", "rds46", "Raahil", Role.MARKER);
	}

	public static void addEntry(String group, String crsid, String name, Role r) {
		Group g = new Group(group);
		db.saveGroup(g);
		User u = new User(crsid, name);
		db.saveUser(u);
		Grouping gr = new Grouping(g, u, r);
		db.saveGrouping(gr);
	}
}
