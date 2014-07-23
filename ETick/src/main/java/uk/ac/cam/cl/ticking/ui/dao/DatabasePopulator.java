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
public class DatabasePopulator {

	private static IDataManager db = null;

	public static void testPopulate(User u) {

		Injector injector = Guice
				.createInjector(new GuiceConfigurationModule());
		db = injector.getInstance(IDataManager.class);
		Group g;

		g = new Group("TestS", "tl364");
		addEntry(u, g, Role.SUBMITTER);
		
		g = new Group("TestM", "tl364");
		addEntry(u, g, Role.MARKER);
		
		g = new Group("TestMA", "tl364");
		addEntry(u, g, Role.MARKER);
		addEntry(u, g, Role.AUTHOR);
		
		g = new Group("TestMO", "tl364");
		addEntry(u, g, Role.MARKER);
		addEntry(u, g, Role.OVERVIEW);
		
		g = new Group("TestMAO", "tl364");
		addEntry(u, g, Role.MARKER);
		addEntry(u, g, Role.AUTHOR);
		addEntry(u, g, Role.OVERVIEW);
		
		g = new Group("TestA", "tl364");
		addEntry(u, g, Role.AUTHOR);
		
		g = new Group("TestAO", "tl364");
		addEntry(u, g, Role.AUTHOR);
		addEntry(u, g, Role.OVERVIEW);
		
		g = new Group("TestO", "tl364");
		addEntry(u, g, Role.OVERVIEW);
	}

	public static void addEntry(User u, Group group, Role r) {
		db.saveGroup(group);
		Grouping gr = new Grouping(group, u, r);
		db.saveGrouping(gr);
	}
}
