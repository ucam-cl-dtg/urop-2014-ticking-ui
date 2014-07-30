package uk.ac.cam.cl.ticking.ui.configuration;

import java.util.HashMap;

public class ConfigurationRegister {

	private static HashMap<Class<? extends ConfigurationFile>, ConfigurationLoader<?>> loaders = 
			new HashMap<Class<? extends ConfigurationFile>, ConfigurationLoader<?>>();

	static {
		loaders.put(Configuration.class,
				new ConfigurationLoader<Configuration>("configuration.json",
						Configuration.class));
		loaders.put(AcademicTemplate.class,
				new ConfigurationLoader<AcademicTemplate>("academic.json",
						AcademicTemplate.class));
	}

	public static ConfigurationLoader<? extends ConfigurationFile> getLoader(
			Class<? extends ConfigurationFile> clazz) {
		return loaders.get(clazz);
	}

}