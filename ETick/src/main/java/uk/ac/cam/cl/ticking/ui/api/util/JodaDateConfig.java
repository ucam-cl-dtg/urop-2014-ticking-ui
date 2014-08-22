package uk.ac.cam.cl.ticking.ui.api.util;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

/**
 * This class allows Joda date objects to serialise to and from ISO8601 string
 * dates, making deadline passing between the back and front end standardised.
 * 
 * @author tl364
 *
 */
@Provider
@Produces("application/json")
public class JodaDateConfig implements ContextResolver<ObjectMapper> {
	private final ObjectMapper objectMapper;

	public JodaDateConfig() throws Exception {
		objectMapper = new ObjectMapper().registerModule(new JodaModule())
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
						false);
	}

	@Override
	public ObjectMapper getContext(Class<?> arg0) {
		return objectMapper;
	}
}
