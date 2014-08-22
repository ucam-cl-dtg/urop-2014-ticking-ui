package uk.ac.cam.cl.ticking.ui.configuration;

/**
 * @author kr2
 * @author tl364 - changed for UROP_UI project - java retrieved 21/7/2014
 *
 */
public class Configuration implements ConfigurationFile {

	private String uiApiLocation = "http://urop2014.dtg.cl.cam.ac.uk:8080/UROP_UI/api/";
	private String gitApiLocation = "http://urop2014.dtg.cl.cam.ac.uk:8080/UROP_GIT/rest/";
	private String secureGitApiLocation = "http://urop2014.dtg.cl.cam.ac.uk:8080/UROP_GIT_SECURED/rest/";
	private String testApiLocation = "http://urop2014.dtg.cl.cam.ac.uk/UROP-TestingSystem/rest/";
	private String secureTestApiLocation = "http://urop2014.dtg.cl.cam.ac.uk/UROP-TestingSystem-secured/rest/";
	private String signupsApiLocation = "http://urop2014.dtg.cl.cam.ac.uk/UROP_SIGNUPS/rest/";
	private String uiMongoBroadcast = "localhost";
	private int uiMongoPort = 27017;
	
	private String securityToken;

	/**
	 * @return URL prefix of the UI api endpoints
	 */
	public String getUiApiLocation() {
		return uiApiLocation;
	}

	/**
	 * @param uiApiLoc
	 */
	public void setUiApiLocation(String uiApiLocation) {
		this.uiApiLocation = uiApiLocation;
	}

	/**
	 * @return URL prefix of the GIT api endpoints
	 */
	public String getGitApiLocation() {
		return gitApiLocation;
	}

	/**
	 * @param gitApiLoc
	 */
	public void setGitApiLocation(String gitApiLocation) {
		this.gitApiLocation = gitApiLocation;
	}

	/**
	 * @return where to find the MongoDB
	 */
	public String getUiMongoBroadcast() {
		return uiMongoBroadcast;
	}

	/**
	 * @param uiMongoBroadcast
	 */
	public void setUiMongoBroadcast(String uiMongoBroadcast) {
		this.uiMongoBroadcast = uiMongoBroadcast;
	}

	/**
	 * @return the port that the MongoDB is broadcasting on
	 */
	public int getUiMongoPort() {
		return uiMongoPort;
	}

	/**
	 * @param uiMongoPort
	 */
	public void setUiMongoPort(int uiMongoPort) {
		this.uiMongoPort = uiMongoPort;
	}

	/**
	 * @return testApiLocation
	 */
	public String getTestApiLocation() {
		return testApiLocation;
	}

	/**
	 * @param testApiLocation
	 */
	public void setTestApiLocation(String testApiLocation) {
		this.testApiLocation = testApiLocation;
	}

	/**
	 * 
	 * @return signupsApiLocation
	 */
	public String getSignupsApiLocation() {
		return signupsApiLocation;
	}

	/**
	 * 
	 * @param signupsApiLocation
	 */
	public void setSignupsApiLocation(String signupsApiLocation) {
		this.signupsApiLocation = signupsApiLocation;
	}

	public String getSecurityToken() {
		return securityToken;
	}

	public void setSecurityToken(String securityToken) {
		this.securityToken = securityToken;
	}

	public String getSecureGitApiLocation() {
		return secureGitApiLocation;
	}

	public void setSecureGitApiLocation(String secureGitApiLocation) {
		this.secureGitApiLocation = secureGitApiLocation;
	}

	public String getSecureTestApiLocation() {
		return secureTestApiLocation;
	}

	public void setSecureTestApiLocation(String secureTestApiLocation) {
		this.secureTestApiLocation = secureTestApiLocation;
	}

}
