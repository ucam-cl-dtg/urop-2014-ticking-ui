/* vim: set et ts=4 sts=4 sw=4 tw=72 : */
/* See the LICENSE file for the license of the project */
/**
 * 
 */
package uk.ac.cam.cl.ticking.signups;

import org.mongojack.Id;

import com.fasterxml.jackson.annotation.*;

/**
 * @author Isaac Dunn &lt;ird28@cam.ac.uk&gt;
 */
public class AuthCodeMap {
    
    private String _id;
    private String authCode;
    
    public AuthCodeMap(@Id String _id, @JsonProperty("authCode") String authCode) {
        this._id = _id;
        this.authCode = authCode;
    }

    public String get_id() {
        return _id;
    }

    public String getAuthCode() {
        return authCode;
    }
    
    

}
