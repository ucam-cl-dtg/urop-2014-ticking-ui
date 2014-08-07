package uk.ac.cam.cl.ticking.ui.api.remote;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;

import uk.ac.cam.cl.git.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationRegister;

public class GitApi {

	private static WebInterface webInterface;

	static {
		Configuration config = (Configuration) ConfigurationRegister.getLoader(
				Configuration.class).getConfig();
		
		PoolingClientConnectionManager cm = new PoolingClientConnectionManager();
		cm.setDefaultMaxPerRoute(200);
		cm.setMaxTotal(200);
		HttpClient httpClient = new DefaultHttpClient(cm);
		ApacheHttpClient4Engine engine = new ApacheHttpClient4Engine(httpClient);
		
		ResteasyClient client = new ResteasyClientBuilder().httpEngine(engine).build();
		ResteasyWebTarget target = client.target(config.
				getGitApiLocation());

		webInterface = target.proxy(WebInterface.class);
	}

	/**
	 * Provides a handle to the GitApi
	 * 
	 * @return DB handle
	 */
	public static WebInterface getWebInterface() {
		return webInterface;
	}
	
}
