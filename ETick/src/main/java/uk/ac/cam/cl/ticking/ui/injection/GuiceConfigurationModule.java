package uk.ac.cam.cl.ticking.ui.injection;

import uk.ac.cam.cl.ticking.ui.api.ApiFacade;
import uk.ac.cam.cl.ticking.ui.auth.RavenManager;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.dao.MongoDataManager;
import uk.ac.cam.cl.ticking.ui.database.Mongo;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.mongodb.DB;

/**
 * This class is responsible for injecting configuration values for persistence
 * related classes
 * 
 * @author tl364
 *
 */
public class GuiceConfigurationModule extends AbstractModule {
	
	private static ApiFacade  apiFacade = null;
	private static RavenManager  ravenManager = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.inject.AbstractModule#configure()
	 */
	@Override
	protected void configure() {
		this.configureDataPersistence();
		this.configureApplicationManagers();
	}

	/**
	 * Deals with persistence providers
	 */
	private void configureDataPersistence() {
		bind(DB.class).toInstance(Mongo.getDB());
	}

	/**
	 * Deals with application data managers
	 */
	private void configureApplicationManagers() {
		bind(IDataManager.class).to(MongoDataManager.class);
	}
	
	@Inject
	@Provides
	private static ApiFacade getApiSingleton(IDataManager db, RavenManager raven) {
		if (apiFacade == null) {
			apiFacade = new ApiFacade(db, raven);
		}
		return apiFacade;
	}
	
	@Inject
	@Provides
	private static RavenManager getRavenManager(IDataManager db) {
		if (ravenManager == null) {
			ravenManager = new RavenManager(db);
		}
		return ravenManager;
	}

}
