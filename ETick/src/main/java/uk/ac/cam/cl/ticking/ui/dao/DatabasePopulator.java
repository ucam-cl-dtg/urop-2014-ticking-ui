package uk.ac.cam.cl.ticking.ui.dao;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.ETickGuiceConfigurationModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class DatabasePopulator {

	private static IDataManager db = null;

	public static void testDB() {

		Injector injector = Guice
				.createInjector(new ETickGuiceConfigurationModule());
		db = injector.getInstance(IDataManager.class);

		addEntry("Java 1A", "rds46", "Raahil", Role.OVERVIEW);
		addEntry("Java 1A", "rds46", "Raahil", Role.AUTHOR);
		addEntry("Java 1B", "rds46", "Raahil", Role.SUBMITTER);
		addEntry("Java 2", "rds46", "Raahil", Role.MARKER);
		addEntry("Java 2", "rds46", "Raahil", Role.MARKER);
		addEntry("Java 2", "rds46", "Raahil", Role.MARKER);
		addEntry("Being a grade A boss", "tl364", "Tom", Role.OVERVIEW);
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
