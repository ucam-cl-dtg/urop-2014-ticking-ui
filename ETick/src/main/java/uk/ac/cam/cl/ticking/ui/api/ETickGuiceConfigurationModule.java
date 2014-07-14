package uk.ac.cam.cl.ticking.ui.api;

import uk.ac.cam.cl.ticking.ui.database.IDataManager;
import uk.ac.cam.cl.ticking.ui.database.Mongo;
import uk.ac.cam.cl.ticking.ui.database.MongoDataManager;

import com.google.inject.AbstractModule;
import com.mongodb.DB;

public class ETickGuiceConfigurationModule extends AbstractModule {

	@Override
	protected void configure() {
		this.configureDataPersistence();
		this.configureApplicationManagers();
	}
	
	private void configureDataPersistence() {
		bind(DB.class).toInstance(Mongo.getDB());
	}
	
	private void configureApplicationManagers() {
		bind(IDataManager.class).to(MongoDataManager.class);
	}

}
