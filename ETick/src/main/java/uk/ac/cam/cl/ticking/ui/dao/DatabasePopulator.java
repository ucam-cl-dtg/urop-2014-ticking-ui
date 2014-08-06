package uk.ac.cam.cl.ticking.ui.dao;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.injection.GuiceConfigurationModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Test class used for populating the database with groupings
 * 
 * @author tl364
 *
 */
@Deprecated
public class DatabasePopulator {

	private static IDataManager db = null;

	public static void testPopulate(User u) {

		Injector injector = Guice
				.createInjector(new GuiceConfigurationModule());
		db = injector.getInstance(IDataManager.class);

		addEntry(u, "TestS", Role.SUBMITTER);

		addEntry(u, "TestM", Role.MARKER);

		addEntry(u, "TestMA", Role.MARKER);
		addEntry(u, "TestMA", Role.AUTHOR);

		addEntry(u, "TestMO", Role.MARKER);
		addEntry(u, "TestMO", Role.OVERVIEW);

		addEntry(u, "TestMAO", Role.MARKER);
		addEntry(u, "TestMAO", Role.AUTHOR);
		addEntry(u, "TestMAO", Role.OVERVIEW);

		addEntry(u, "TestA", Role.AUTHOR);

		addEntry(u, "TestAO", Role.AUTHOR);
		addEntry(u, "TestAO", Role.OVERVIEW);

		addEntry(u, "TestO", Role.OVERVIEW);
	}

	public static void addEntry(User u, String g, Role r) {
		Group group = null;
		//group = db.getGroupByName(g);
		//Method getGroupByName no longer exists
		if (group == null) {
			group = new Group(g, "tl364");
			db.saveGroup(group);
		}
		Grouping gr = new Grouping(group, u, r);
		db.saveGrouping(gr);
	}
}
