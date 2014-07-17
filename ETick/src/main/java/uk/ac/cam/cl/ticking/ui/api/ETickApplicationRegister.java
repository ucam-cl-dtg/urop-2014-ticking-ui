package uk.ac.cam.cl.ticking.ui.api;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import uk.ac.cam.cl.dtg.teaching.exceptions.ExceptionHandler;
import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.ticking.ui.auth.RavenManager;
import uk.ac.cam.cl.ticking.ui.injection.ETickGuiceConfigurationModule;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * This class registers the resteasy handlers. The name is important since
 * it is used as a String in HttpServletDispatcherV3
 * 
 * @author tl364
 * 
 */
public class ETickApplicationRegister extends Application {

	private Set<Object> singletons;
	
	/**
	 * Default constructor
	 */
	public ETickApplicationRegister() {
		singletons = new HashSet<Object>();
	}
	
	@Override
	public final Set<Object> getSingletons() {
		Injector injector = Guice
				.createInjector(new ETickGuiceConfigurationModule());
		this.singletons.add(injector.getInstance(RavenManager.class));
		this.singletons.add(injector.getInstance(ETickApiFacade.class));
		return this.singletons;
	}
	
	@Override
	public final Set<Class<?>> getClasses() {
		Set<Class<?>> result = new HashSet<Class<?>>();
		return result;
	}
}
