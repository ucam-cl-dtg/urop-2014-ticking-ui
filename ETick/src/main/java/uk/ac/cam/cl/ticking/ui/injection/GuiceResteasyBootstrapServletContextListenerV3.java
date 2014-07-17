package uk.ac.cam.cl.ticking.ui.injection;

import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.annotation.WebListener;

import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;

import com.google.inject.Module;

/**
 * 
 * @author acr31
 *
 */
@WebListener
public class GuiceResteasyBootstrapServletContextListenerV3 extends
		GuiceResteasyBootstrapServletContextListener {
	@Override
	protected List<? extends Module> getModules(ServletContext context) {
		return Arrays.asList(new Module[] { new ApplicationModule() });
	}
}
