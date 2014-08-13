package uk.ac.cam.cl.ticking.ui.api;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import uk.ac.cam.cl.ticking.signups.TickSignups;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IForkApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupingApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.ISubmissionApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IUserApiFacade;
import uk.ac.cam.cl.ticking.ui.auth.LdapManager;
import uk.ac.cam.cl.ticking.ui.injection.GuiceConfigurationModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * This class registers the resteasy handlers. The name is important since it is
 * used as a String in HttpServletDispatcherV3
 * 
 * @author tl364
 * 
 */
public class ApplicationRegister extends Application {

	private Set<Object> singletons;

	/**
	 * Default constructor
	 */
	public ApplicationRegister() {
		singletons = new HashSet<Object>();
	}

	@Override
	public final Set<Object> getSingletons() {
		Injector injector = Guice
				.createInjector(new GuiceConfigurationModule());
		this.singletons.add(injector.getInstance(LdapManager.class));
		this.singletons.add(injector.getInstance(ITickApiFacade.class));
		this.singletons.add(injector.getInstance(IUserApiFacade.class));
		this.singletons.add(injector.getInstance(IGroupingApiFacade.class));
		this.singletons.add(injector.getInstance(IGroupApiFacade.class));
		this.singletons.add(injector.getInstance(ISubmissionApiFacade.class));
		this.singletons.add(injector.getInstance(IForkApiFacade.class));
		this.singletons.add(injector.getInstance(TickSignups.class));
		return this.singletons;
	}

	@Override
	public final Set<Class<?>> getClasses() {
		Set<Class<?>> result = new HashSet<Class<?>>();
		return result;
	}
}
