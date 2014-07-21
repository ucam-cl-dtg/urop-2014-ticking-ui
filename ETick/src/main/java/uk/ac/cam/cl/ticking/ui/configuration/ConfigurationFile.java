package uk.ac.cam.cl.ticking.ui.configuration;

/**
* @author kr2
* @author tl364 - changed for UROP_UI project
* 				- java retrieved 21/7/2014
*
*/
public class ConfigurationFile
{
	private String uiApiLoc = "http://urop2014.dtg.cl.cam.ac.uk/UROP_UI/api/";
    private String gitApiLoc = "http://urop2014.dtg.cl.cam.ac.uk/UROP_GIT/rest/";
    
    
	/**
	 * @return URL prefix of the UI api endpoints
	 */
	public String getUiApiLoc() {
		return uiApiLoc;
	}
	/**
	 * @param uiApiLoc
	 */
	public void setUiApiLoc(String uiApiLoc) {
		this.uiApiLoc = uiApiLoc;
	}
	/**
	 * @return URL prefix of the GIT api endpoints
	 */
	public String getGitApiLoc() {
		return gitApiLoc;
	}
	/**
	 * @param gitApiLoc
	 */
	public void setGitApiLoc(String gitApiLoc) {
		this.gitApiLoc = gitApiLoc;
	}
    
}


