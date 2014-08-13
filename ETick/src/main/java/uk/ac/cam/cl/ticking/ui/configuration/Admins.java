package uk.ac.cam.cl.ticking.ui.configuration;

import java.util.Arrays;

/**
 * This class contains the list of system admins by crsid
 * 
 * @author tl364
 *
 */
public class Admins implements ConfigurationFile {

	private String[] crsids = {"sac92","arb33","acr31"};

	/**
	 * @return crsid
	 */
	public String[] getCrsids() {
		return crsids;
	}

	/**
	 * @param crsid
	 */
	public void setCrsids(String[] crsids) {
		this.crsids = crsids;
	}

	public boolean isAdmin(String crsid) {
		return Arrays.asList(crsids).contains(crsid);
	}
}
