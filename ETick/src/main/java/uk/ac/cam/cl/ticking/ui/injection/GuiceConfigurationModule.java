package uk.ac.cam.cl.ticking.ui.injection;

import uk.ac.cam.cl.ticking.ui.api.GroupApiFacade;
import uk.ac.cam.cl.ticking.ui.api.TickApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade;
import uk.ac.cam.cl.ticking.ui.auth.RavenManager;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationFile;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
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

	private static TickApiFacade tickApiFacade = null;
	private static GroupApiFacade groupApiFacade = null;
	private static RavenManager ravenManager = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.google.inject.AbstractModule#configure()
	 */
	@Override
	protected void configure() {
		this.configureDataPersistence();
		this.configureApplicationManagers();
		this.configureFacades();
		this.configureConfiguration();
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

	/**
	 * Deals with API
	 */
	private void configureFacades() {
		bind(ITickApiFacade.class).to(TickApiFacade.class);
		bind(IGroupApiFacade.class).to(GroupApiFacade.class);
	}
	
	/**
	 * Deals with configuration file
	 */
	private void configureConfiguration() {
		bind(ConfigurationFile.class).toInstance(ConfigurationLoader.getConfig());
	}

	@Inject
	@Provides
	private static TickApiFacade getTickApiSingleton(IDataManager db, ConfigurationFile config) {
		if (tickApiFacade == null) {
			tickApiFacade = new TickApiFacade(db, config);
		}
		return tickApiFacade;
	}
	
	@Inject
	@Provides
	private static GroupApiFacade getGroupApiSingleton(IDataManager db, ConfigurationFile config) {
		if (groupApiFacade == null) {
			groupApiFacade = new GroupApiFacade(db, config);
		}
		return groupApiFacade;
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
