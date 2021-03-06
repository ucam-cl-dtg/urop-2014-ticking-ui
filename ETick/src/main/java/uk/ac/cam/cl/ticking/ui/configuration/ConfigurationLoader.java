package uk.ac.cam.cl.ticking.ui.configuration;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A configuration file loader class, set at compile time, loaded at
 * initialisation.
 *
 * This just a simple class to load the file {@value fileName} in the current
 * directory and convert it into a
 * {@link uk.ac.cam.cl.git.configuration.ConfigurationFile} implementing class.
 *
 * @author Kovacsics Robert &lt;rmk35@cam.ac.uk&gt;
 * @author tl364 - changed to allow multiple loaders, managed by register - java
 *         retrieved 21/7/2014
 *
 * @param <T>
 */
public class ConfigurationLoader<T extends ConfigurationFile> {

	// initialise log4j logger
	Logger log = Logger.getLogger(ConfigurationLoader.class.getName());
	public final String fileName;
	private File file;
	private ObjectMapper mapper = new ObjectMapper();
	private long mTime;

	private Class<T> configClass;
	private T loadedConfig;

	public ConfigurationLoader(String fileName, Class<T> configClass) {
		this.fileName = fileName;
		file = new File(fileName);

		this.configClass = configClass;
		try {
			this.loadedConfig = configClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			log.error(e);
		}
		load();
	}

	public void load() {
		/* ObjectMapper (JSON syntax) configuration */
		mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
				false);

		try {
			loadedConfig = mapper.readValue(file, configClass);
			mTime = file.lastModified();
		} catch (IOException e) {
			/* Exception thrown in getter */
			log.error("Error in loading configuration file, using defaults.\n Message: "
					+ e.getMessage());
		}
	}

	public T getConfig() {
		/* lastModified returns 0 if file not found */
		if (file.lastModified() > mTime) {
			try {
				loadedConfig = mapper.readValue(file, configClass);
				mTime = file.lastModified();
			} catch (IOException e) {
				log.error("Unable to load new configuration file!\n Message: "
						+ e.getMessage());
			}
		}
		return loadedConfig;
	}
}
