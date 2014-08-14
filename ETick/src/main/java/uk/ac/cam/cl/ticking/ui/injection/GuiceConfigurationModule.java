package uk.ac.cam.cl.ticking.ui.injection;

import publicinterfaces.ITestService;
import uk.ac.cam.cl.git.interfaces.WebInterface;
import uk.ac.cam.cl.signups.interfaces.SignupsWebInterface;
import uk.ac.cam.cl.ticking.signups.TickSignups;
import uk.ac.cam.cl.ticking.ui.api.ForkApiFacade;
import uk.ac.cam.cl.ticking.ui.api.GroupApiFacade;
import uk.ac.cam.cl.ticking.ui.api.GroupingApiFacade;
import uk.ac.cam.cl.ticking.ui.api.SubmissionApiFacade;
import uk.ac.cam.cl.ticking.ui.api.TickApiFacade;
import uk.ac.cam.cl.ticking.ui.api.UserApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IForkApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupingApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IUserApiFacade;
import uk.ac.cam.cl.ticking.ui.api.remote.GitApi;
import uk.ac.cam.cl.ticking.ui.api.remote.SignupApi;
import uk.ac.cam.cl.ticking.ui.api.remote.TestApi;
import uk.ac.cam.cl.ticking.ui.auth.LdapManager;
import uk.ac.cam.cl.ticking.ui.configuration.AcademicTemplate;
import uk.ac.cam.cl.ticking.ui.configuration.Admins;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationRegister;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.dao.MongoDataManager;
import uk.ac.cam.cl.ticking.ui.database.Mongo;
import uk.ac.cam.cl.ticking.ui.util.PermissionsChecker;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
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
	private static UserApiFacade userApiFacade = null;
	private static GroupingApiFacade groupingApiFacade = null;
	private static GroupApiFacade groupApiFacade = null;
	private static SubmissionApiFacade submissionApiFacade = null;
	private static ForkApiFacade forkApiFacade = null;
	private static TickSignups tickSignups = null;
	private static LdapManager ldapManager = null;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure() {
		this.configureDataPersistence();
		this.configureApplicationManagers();
		this.configureFacades();
		this.configureConfiguration();
		this.configureRemoteApis();
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
		bind(IUserApiFacade.class).to(UserApiFacade.class);
		bind(IGroupingApiFacade.class).to(GroupingApiFacade.class);
		bind(IGroupApiFacade.class).to(GroupApiFacade.class);
		bind(ISubmissionApiFacade.class).to(SubmissionApiFacade.class);
		bind(IForkApiFacade.class).to(ForkApiFacade.class);
	}

	/**
	 * Deals with configuration files
	 */
	@SuppressWarnings("unchecked")
	private void configureConfiguration() {
		bind(new TypeLiteral<ConfigurationLoader<Configuration>>() {
		}).toInstance(
				(ConfigurationLoader<Configuration>) ConfigurationRegister
						.getLoader(Configuration.class));
		bind(new TypeLiteral<ConfigurationLoader<AcademicTemplate>>() {
		}).toInstance(
				(ConfigurationLoader<AcademicTemplate>) ConfigurationRegister
						.getLoader(AcademicTemplate.class));
		bind(new TypeLiteral<ConfigurationLoader<Admins>>() {
		}).toInstance(
				(ConfigurationLoader<Admins>) ConfigurationRegister
						.getLoader(Admins.class));
	}

	/**
	 * Deals with remote APIs
	 */
	private void configureRemoteApis() {
		bind(WebInterface.class).toInstance(GitApi.getWebInterface());
		bind(ITestService.class).toInstance(TestApi.getITestService());
		bind(SignupsWebInterface.class).toInstance(SignupApi.getWebInterface());
	}

	@Inject
	@Provides
	private static TickApiFacade getTickApiSingleton(IDataManager db,
			ConfigurationLoader<Configuration> config,
			ITestService testServiceProxy, WebInterface gitServiceProxy,
			PermissionsChecker permissions) {
		if (tickApiFacade == null) {
			tickApiFacade = new TickApiFacade(db, config, testServiceProxy,
					gitServiceProxy, permissions);
		}
		return tickApiFacade;
	}

	@Inject
	@Provides
	private static UserApiFacade getUserApiSingleton(IDataManager db,
			ConfigurationLoader<Configuration> config,
			WebInterface gitServiceProxy, PermissionsChecker permissions) {
		if (userApiFacade == null) {
			userApiFacade = new UserApiFacade(db, config, gitServiceProxy,
					permissions);
		}
		return userApiFacade;
	}

	@Inject
	@Provides
	private static GroupingApiFacade getGroupingApiSingleton(IDataManager db,
			ConfigurationLoader<Configuration> config, LdapManager raven,
			PermissionsChecker permissions) {
		if (groupingApiFacade == null) {
			groupingApiFacade = new GroupingApiFacade(db, config, raven,
					permissions);
		}
		return groupingApiFacade;
	}

	@Inject
	@Provides
	private static GroupApiFacade getGroupApiSingleton(IDataManager db,
			ConfigurationLoader<Configuration> config,
			TickSignups tickSignupService, PermissionsChecker permissions) {
		if (groupApiFacade == null) {
			groupApiFacade = new GroupApiFacade(db, config, tickSignupService,
					permissions);
		}
		return groupApiFacade;
	}

	@Inject
	@Provides
	private static SubmissionApiFacade getSubmissionApiSingleton(
			IDataManager db, ConfigurationLoader<Configuration> config,
			ITestService testServiceProxy, TickSignups tickSignupService,
			PermissionsChecker permissions) {
		if (submissionApiFacade == null) {
			submissionApiFacade = new SubmissionApiFacade(db, config,
					testServiceProxy, tickSignupService, permissions);
		}
		return submissionApiFacade;
	}

	@Inject
	@Provides
	private static ForkApiFacade getForkApiSingleton(IDataManager db,
			ConfigurationLoader<Configuration> config,
			ITestService testService, WebInterface gitService,
			TickSignups tickSignupService, PermissionsChecker permissions) {
		if (forkApiFacade == null) {
			forkApiFacade = new ForkApiFacade(db, config, testService,
					gitService, tickSignupService, permissions);
		}
		return forkApiFacade;
	}

	@Inject
	@Provides
	private static TickSignups getTickSignupsSingleton(IDataManager db,
			SignupsWebInterface signupServiceProxy) {
		if (tickSignups == null) {
			tickSignups = new TickSignups(db, signupServiceProxy);
		}
		return tickSignups;
	}

	@Inject
	@Provides
	private static LdapManager getLdapManager(IDataManager db,
			ConfigurationLoader<AcademicTemplate> academicConfig,
			ConfigurationLoader<Admins> adminConfig) {
		if (ldapManager == null) {
			ldapManager = new LdapManager(db, academicConfig, adminConfig);
		}
		return ldapManager;
	}

}
