package uk.ac.cam.cl.ticking.ui.api.remote;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import publicinterfaces.ITestService;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationRegister;

public class TestApi {

	private static ITestService testService;

	static {
		Configuration config = (Configuration) ConfigurationRegister.getLoader(
				Configuration.class).getConfig();

		PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
		cm.setDefaultMaxPerRoute(config.getTestDefaultMaxPerRoute());
		cm.setMaxTotal(config.getTestMaxTotal());

		HttpClient httpClient = new DefaultHttpClient(cm);
		ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);

		ResteasyClient client = new ResteasyClientBuilder()
				.maxPooledPerRoute(config.getTestDefaultMaxPerRoute()).httpEngine(engine).build();
		ResteasyWebTarget target = client.target(config.getSecureTestApiLocation());

		testService = target.proxy(ITestService.class);
	}

	/**
	 * Provides a handle to the TestApi
	 * 
	 * @return testService handle
	 */
	public static ITestService getITestService() {
		return testService;
	}
}
