package uk.ac.cam.cl.ticking.ui.database;

import java.net.UnknownHostException;

import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationFile;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.mongodb.DB;
import com.mongodb.MongoClient;

public class Mongo {

	private static DB db;

	static {
		ConfigurationFile config = ConfigurationLoader.getConfig();
		try {
			MongoClient client = new MongoClient(config.getUiMongoBroadcast(),
					config.getUiMongoPort());
			db = client.getDB(Strings.DBNAME);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Provides a handle to the local MongoDB instance
	 * 
	 * @return DB handle
	 */
	public static DB getDB() {
		return db;
	}
}
