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
* This just a simple class to load the file {@value fileName} in the
* current directory and convert it into a
* {@link uk.ac.cam.cl.git.configuration.ConfigurationFile}
* class.
*
* @author Kovacsics Robert &lt;rmk35@cam.ac.uk&gt;
*/
public class ConfigurationLoader
{
	
	// initialise log4j logger
    static Logger log = Logger.getLogger(ConfigurationLoader.class.getName());
    static public final String fileName = "configuration.json";
    static private File file = new File(fileName);
    static private ObjectMapper mapper = new ObjectMapper();
    static long mTime;
    static ConfigurationFile loadedConfig = new ConfigurationFile();

    static
    {
        /* ObjectMapper (JSON syntax) configuration */
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try
        {
            loadedConfig =
                mapper.readValue(file, ConfigurationFile.class);
            mTime = file.lastModified();
        }
        catch (IOException e)
        {
        	/* Exception thrown in getter */
            log.error("Error in loading configuration file, using defaults.\n Message: "
                    + e.getMessage());
        }
    }

    public static ConfigurationFile getConfig()
    {
        /* lastModified returns 0 if file not found */
        if (file.lastModified() > mTime)
        {
            try
            {
                loadedConfig = mapper.readValue(file, ConfigurationFile.class);
                mTime = file.lastModified();
            }
            catch (IOException e)
            {
            	log.error("Unable to load new configuration file!\n Message: "
                        + e.getMessage());
            }
        }
        return loadedConfig;
    }
}
