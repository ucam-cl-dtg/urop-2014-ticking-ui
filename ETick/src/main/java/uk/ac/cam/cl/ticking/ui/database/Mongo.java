package uk.ac.cam.cl.ticking.ui.database;

import java.net.UnknownHostException;

import com.mongodb.DB;
import com.mongodb.MongoClient;

public class Mongo {

	private static DB db;
	
	static {
		try {
			MongoClient client = new MongoClient( "localhost" , 27017  );
			db = client.getDB("ETick");
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	
	public static DB getDB() {
		return db;
	}
}
