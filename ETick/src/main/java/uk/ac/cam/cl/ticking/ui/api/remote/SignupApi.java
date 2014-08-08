package uk.ac.cam.cl.ticking.ui.api.remote;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import publicinterfaces.ITestService;
import uk.ac.cam.cl.signups.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationRegister;

public class SignupApi {

	private static WebInterface signupService;

	static {
		Configuration config = (Configuration) ConfigurationRegister.getLoader(
				Configuration.class).getConfig();
		
		PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
		cm.setDefaultMaxPerRoute(200);
		cm.setMaxTotal(200);
		
		HttpClient httpClient = new DefaultHttpClient(cm);
		ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
		
		ResteasyClient client = new ResteasyClientBuilder().maxPooledPerRoute(200).httpEngine(engine).build();
		ResteasyWebTarget target = client.target(config.
				getSignupsApiLocation());

		signupService = target.proxy(WebInterface.class);
	}

	/**
	 * Provides a handle to the SignupApi
	 * 
	 * @return signupService handle
	 */
	public static WebInterface getWebInterface() {
		return signupService;
	}
}
