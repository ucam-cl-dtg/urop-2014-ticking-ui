/*
 * This class has been written using code from cam.cl.raven.RavenValve, a Tomcat Valve, 
 * written by William Billingsley (whb21 at cam.ac.uk).
 * 
 * The main body of the code remains unchanged. All that has been added in version 1 is 
 * additional error handling and a simplified configuration.
 * 
 * RavenValve and RavenFilter achieve the same end result. 
 * 
 * The advantages of RavenFilter are that it can be used on any servlet container without
 * code modification but the Principal object containing the id of the authenticated user 
 * has to be obtained via an HttpSession attribute. 
 * 
 * RavenValve, is only usable within Tomcat.  However, it does allow the Raven user id
 * to be fetched from the standard Principal object available through HttpServletRequest.getRemoteUser(). 
 * 
 * 
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package uk.ac.cam.cl.ticking.ui.auth;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import uk.ac.cam.ucs.webauth.WebauthException;
import uk.ac.cam.ucs.webauth.WebauthRequest;
import uk.ac.cam.ucs.webauth.WebauthResponse;
import uk.ac.cam.ucs.webauth.WebauthValidator;

/**
 * A Servlet Filter which ensures a user is Raven authenticated.
 * 
 * <h1>Quick Configuration</h2>
 * 
 * <h3>Install Webauth package</h3>
 * 
 * Ensure you have the Raven Java Toolkit classes installed.
 * 
 * <h3>Install the Raven public key certificate</h3>
 * Download the Raven public key certificate from the <a
 * href="https://raven.cam.ac.uk/project/">Raven Project page</a>. Install into your web
 * application at <code>/WEB-INF/raven/pubkey2.crt</code>.
 * 
 * <h3>Configure web.xml</h3>
 * Add a filter definition:
 * 
 * <pre>
 *  &lt;filter&gt;
 *      &lt;filter-name&gt;ravenFilter&lt;/filter-name&gt;
 *      &lt;filter-class&gt;uk.ac.cam.ucs.webauth.RavenFilter&lt;/filter-class&gt;
 *  &lt;/filter&gt; 
 * </pre>
 * 
 * Add one or more filter-mapping's for your application. Eg:
 * 
 * <pre>
 *  &lt;filter-mapping&gt;
 *      &lt;filter-name&gt;ravenFilter&lt;/filter-name&gt;
 *      &lt;url-pattern&gt;/private&lt;/url-pattern&gt;
 *  &lt;/filter-mapping&gt; 
 * </pre>
 * 
 * <h3>Retrieve authenticated user name</h3>
 * 
 * Get the value of session or request attribute "RavenRemoteUser".
 * 
 * <p>
 * <code>String userId = request.getAttribute("RavenRemoteUser");</code>
 * </p>
 * 
 * 
 * <h1>Further Configuration</h2>
 * 
 * <h3>Filter init params</h3>
 * 
 * <table border="1">
 * <tr>
 * <th>Name</th>
 * <th>Default Value</th>
 * <th>Notes</th>
 * <tr>
 * <tr>
 * <td>authenticateUrl</td>
 * <td>https://raven.cam.ac.uk/auth/authenticate.html</td>
 * <td>Optional</td>
 * </tr>
 * <tr>
 * <td>certificatePath</td>
 * <td>/WEB-INF/raven/pubkey2.crt</td>
 * <td>Optional</td>
 * </tr>
 * </table><br/>
 * 
 * <h3>Error Codes</h3>
 * 
 * Use the following example entries for your web.xml if you wish to provide your own error pages.
 * The codes below are those given by WebauthResponse and passed on by RavenFilter to the servlet
 * container.
 * 
 * <pre>
 *  &lt;!-- 
 *  Raven related Error pages 
 *  --&gt;
 * 
 *  &lt;!-- Authentication cancelled at user's request --&gt;
 *  &lt;error-page&gt;&lt;error-code&gt;410&lt;/error-code&gt;&lt;location&gt;/ravenError.jsp&lt;/location&gt;&lt;/error-page&gt; 
 * 
 *  &lt;!-- No mutually acceptable types of authentication available --&gt;
 *  &lt;error-page&gt;&lt;error-code&gt;510&lt;/error-code&gt;&lt;location&gt;/ravenError.jsp&lt;/location&gt;&lt;/error-page&gt; 
 * 
 *  &lt;!-- Unsupported authentication protocol version --&gt;
 *  &lt;error-page&gt;&lt;error-code&gt;520&lt;/error-code&gt;&lt;location&gt;/ravenError.jsp&lt;/location&gt;&lt;/error-page&gt; 
 * 
 *  &lt;!-- Parameter error in authentication request --&gt;
 *  &lt;error-page&gt;&lt;error-code&gt;530&lt;/error-code&gt;&lt;location&gt;/ravenError.jsp&lt;/location&gt;&lt;/error-page&gt;
 * 
 *  &lt;!-- Interaction with the user would be required --&gt;
 *  &lt;error-page&gt;&lt;error-code&gt;540&lt;/error-code&gt;&lt;location&gt;/ravenError.jsp&lt;/location&gt;&lt;/error-page&gt;
 * 
 *  &lt;!--  Web server not authorised to use the authentication service --&gt;
 *  &lt;error-page&gt;&lt;error-code&gt;560&lt;/error-code&gt;&lt;location&gt;/ravenError.jsp&lt;/location&gt;&lt;/error-page&gt; 
 * 
 *  &lt;!-- Operation declined by the authentication service --&gt;
 *  &lt;error-page&gt;&lt;error-code&gt;570&lt;/error-code&gt;&lt;location&gt;/ravenError.jsp&lt;/location&gt;&lt;/error-page&gt; 
 * </pre>
 * 
 * @author whb21 William Billingsley (whb21 at cam.ac.uk)
 * @author pms52 Philip Shore
 * 
 * @version 1
 * @see <a href="https://raven.cam.ac.uk/project/waa2wls-protocol.txt">The Cambridge Web
 *      Authentication System: WAA->WLS communication protocol</a>
 * 
 */
public class DemoRavenFilter implements Filter
{
    static Log log = LogFactory.getLog(DemoRavenFilter.class);

    /** The request parameter name, if present, indicates a WLS Reponse that should be validated. */
    public static final String WLS_RESPONSE_PARAM = "WLS-Response";

    /** The session attribute name of the Raven WebauthRequest object */
    static final String SESS_RAVEN_REQ_KEY = "RavenReq";

    /**
     * The session attribute name of the RavenState object. This object additionally contains the
     * Principal used to identify the authenticated user.
     */
    static final String SESS_STORED_STATE_KEY = "RavenState";

    /** The name of the request and session attribute containing the authenticated user. */
    public static String ATTR_REMOTE_USER = "RavenRemoteUser";

    /** The default location of the raven public key certificate, relative to the web application */
    static final String DEFAULT_CERTIFICATE_PATH = "/WEB-INF/raven/pubkey901.crt";

    /** This is the default name for the raven public key */
    public static final String DEFAULT_KEYNAME = "webauth-pubkey901";

    /** The real path of the public key calculated from the cert init param */
    private String sCertRealPath = null;

    /**
     * The filter init-param param-name of the url to authenticate against. Optional. Defaults to
     * https://raven.cam.ac.uk/auth/authenticate.html
     */
    public static String INIT_PARAM_AUTHENTICATE_URL = "authenticateUrl";

    /**
     * The filter init-param param-name path to the certificate. Optional. Defaults to
     * /WEB-INF/raven/pubkey2.crt
     */
    public static String INIT_PARAM_CERTIFICATE_PATH = "certificatePath";

    /**
     * The url of the raven authenticate page. Optional.
     * 
     * Defaults to https://raven.cam.ac.uk/auth/authenticate.html
     * 
     * Use https://raven.cam.ac.uk/auth/authenticate.html or
     * https://demo.raven.cam.ac.uk/auth/authenticate.html
     */
    private String sRavenAuthenticatePage = "https://demo.raven.cam.ac.uk/auth/authenticate.html";

    /** KeyStore used by WebauthValidator class */
    protected KeyStore keyStore = null;

    protected WebauthValidator webauthValidator = null;

    public void init(FilterConfig config) throws ServletException
    {
        // check if a different authenticate page is configured.
        // eg https://demo.raven.cam.ac.uk/auth/authenticate.html
        String authenticatePage = config.getInitParameter(INIT_PARAM_AUTHENTICATE_URL);
        if (authenticatePage != null)
            sRavenAuthenticatePage = authenticatePage;

        // get the path to the raven certificate or use a default
        String sCertContextPath = config.getInitParameter(INIT_PARAM_CERTIFICATE_PATH);
        if (sCertContextPath == null)
            sCertContextPath = DEFAULT_CERTIFICATE_PATH;

        // calculate real path from web app relative version
        sCertRealPath = config.getServletContext().getRealPath(sCertContextPath);
        log.debug("Certificate will be loaded from: " + sCertRealPath);

        // ensure KeyStore is initialised.
        keyStore = getKeyStore();

        // ensure WebauthValidator is initialised.
        webauthValidator = getWebauthValidator();
    }

    /**
     * Gets a KeyStore and initialises if necessary.
     * 
     * The caller should ensure the KeyStore is persisted to a safe place.
     * 
     * @return An initialised KeyStore
     */
    protected KeyStore getKeyStore()
    {
        // init a new keystore with the Raven certificate,
        KeyStore keyStore;
        try
        {
            keyStore = KeyStore.getInstance("JKS");
            keyStore.load(null, new char[] {}); // Null InputStream, no password
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate cert = factory.generateCertificate(new FileInputStream(sCertRealPath));
            keyStore.setCertificateEntry(DEFAULT_KEYNAME, cert);
        }
        catch (KeyStoreException e)
        {
            log.error("Unable to setup KeyStore", e);
            throw new RuntimeException(e);
        }
        catch (NoSuchAlgorithmException e)
        {
            log.error("Unable to find crypto algorithm.", e);
            throw new RuntimeException(e);
        }
        catch (CertificateException e)
        {
            log.error("Unable to load certificate.", e);
            throw new RuntimeException(e);
        }
        catch (FileNotFoundException e)
        {
            log.error("Unable to load certificate file: " + sCertRealPath, e);
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            log.error("General IO problem.  Unable to initialised filter.", e);
            throw new RuntimeException(e);
        }

        return keyStore;

    }

    /**
     * Gets a WebauthValidator and initialises if necessary.
     * 
     * @return
     */
    protected WebauthValidator getWebauthValidator()
    {
        if (webauthValidator == null)
        {
            webauthValidator = new WebauthValidator(getKeyStore());
        }
        return webauthValidator;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#destroy()
     */
    public void destroy()
    {
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.Filter#doFilter(javax.servlet.ServletRequest,
     *      javax.servlet.ServletResponse, javax.servlet.FilterChain)
     */
    public void doFilter(ServletRequest servletReq, ServletResponse servletResp, FilterChain chain)
            throws IOException, ServletException
    {

        // Only process http requests.
        if ((servletReq instanceof HttpServletRequest) == false)
        {
            String msg = "Configuration Error.  RavenFilter can only handle Http requests. The rest of the filter chain will NOT be processed.";
            log.error(msg);
            return;
        }

        HttpServletRequest request = (HttpServletRequest) servletReq;
        HttpServletResponse response = (HttpServletResponse) servletResp;
        HttpSession session = request.getSession();
        
        // If we are in testing mode then we check to see if the requestor
        // has specified which user they would like to masquerade as
        String user = request
                .getParameter("raven_test_user");
        if (user == null) {
            // If its not specified then try and fish it out of the session
            // (to see if they set it in an earlier request)
            user = (String) session.getAttribute(ATTR_REMOTE_USER);
        }
        if (user == null) {
            // Otherwise default to the user 'test'
            user = "test";
        }
        servletReq.setAttribute(ATTR_REMOTE_USER, user);
        session.setAttribute(ATTR_REMOTE_USER, user);
        chain.doFilter(servletReq, servletResp);
        return;
    }

    class RavenPrincipal implements Principal
    {
        protected String name;

        public RavenPrincipal(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public String toString()
        {
            return "RavenPrincipal--" + name;
        }

    }// end inner class RavenPrincipal

    class RavenState
    {

        int status;

        String issue;

        long last;

        String life;

        String id;

        Principal principal;

        String aauth;

        String sso;

        String params;

        public RavenState(int status, String issue, String life, String id, Principal principal,
                String aauth, String sso, String params)
        {
            this.status = status;
            this.issue = issue;
            this.last = System.currentTimeMillis();
            this.life = life;
            this.id = id;
            this.principal = principal;
            this.aauth = aauth;
            this.sso = sso;
            this.params = params;
        }

        public String toString()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(" Status: ");
            sb.append(status);
            sb.append(" Issue: ");
            sb.append(issue);
            sb.append(" Last: ");
            sb.append(last);
            sb.append(" Life: ");
            sb.append(life);
            sb.append(" ID: ");
            sb.append(id);
            sb.append(" Principal: ");
            sb.append(principal);
            sb.append(" AAuth: ");
            sb.append(aauth);
            sb.append(" SSO: ");
            sb.append(sso);
            sb.append(" Params: ");
            sb.append(params);
            return sb.toString();
        }
    }// end inner class RavenState

}// end RavenFilter class
