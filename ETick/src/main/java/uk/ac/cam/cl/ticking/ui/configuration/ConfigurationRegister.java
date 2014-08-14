package uk.ac.cam.cl.ticking.ui.configuration;

import java.util.HashMap;

/**
 * A factory class for creating and registering loaders for specified files.
 * 
 * @author tl364
 *
 */
public class ConfigurationRegister {

	private static HashMap<Class<? extends ConfigurationFile>, ConfigurationLoader<?>> loaders = new HashMap<Class<? extends ConfigurationFile>, ConfigurationLoader<?>>();

	static {
		loaders.put(Configuration.class,
				new ConfigurationLoader<Configuration>("configuration.json",
						Configuration.class));
		loaders.put(AcademicTemplate.class,
				new ConfigurationLoader<AcademicTemplate>("academic.json",
						AcademicTemplate.class));
		loaders.put(Admins.class,
				new ConfigurationLoader<Admins>("admins.json",
						Admins.class));
	}

	public static ConfigurationLoader<? extends ConfigurationFile> getLoader(
			Class<? extends ConfigurationFile> clazz) {
		return loaders.get(clazz);
	}

}
