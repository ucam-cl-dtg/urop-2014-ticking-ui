package uk.ac.cam.cl.ticking.ui.api;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

@Provider
@Produces("application/json")
public class JodaDateConfig implements ContextResolver<ObjectMapper> {
	private final ObjectMapper objectMapper;

	public JodaDateConfig() throws Exception {
		objectMapper = new ObjectMapper()
				.registerModule(new JodaModule())
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
	}

	@Override
	public ObjectMapper getContext(Class<?> arg0) {
		return objectMapper;
	}
}
