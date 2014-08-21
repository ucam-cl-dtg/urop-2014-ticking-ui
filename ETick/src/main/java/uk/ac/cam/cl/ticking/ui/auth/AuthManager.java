package uk.ac.cam.cl.ticking.ui.auth;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.ac.cam.cl.dtg.ldap.LDAPObjectNotFoundException;
import uk.ac.cam.cl.dtg.ldap.LDAPQueryManager;
import uk.ac.cam.cl.dtg.ldap.LDAPUser;
import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.configuration.AcademicTemplate;
import uk.ac.cam.cl.ticking.ui.configuration.Admins;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;

import com.google.inject.Inject;

@Path("/raven")
public class AuthManager {

	private IDataManager db;
	private ConfigurationLoader<AcademicTemplate> academicConfig;
	private ConfigurationLoader<Admins> adminConfig;

	/**
	 * @param db
	 */
	@Inject
	public AuthManager(IDataManager db,
			ConfigurationLoader<AcademicTemplate> academicConfig,
			ConfigurationLoader<Admins> adminConfig) {
		this.db = db;
		this.academicConfig = academicConfig;
		this.adminConfig = adminConfig;
	}

	@GET
	@Path("/")
	public Response session(@Context HttpServletRequest request) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		if (crsid == null) {
			return Response.status(Status.NOT_FOUND).build();
		}
		return Response.ok().build();
	}

	/**
	 * Displays the information we have concerning the user as HTML.
	 * 
	 * @param request
	 * @return response
	 */
	@GET
	@Path("/stats")
	public Response stats(@Context HttpServletRequest request) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		User user = db.getUser(crsid);

		String html = "<html><head><title>User Info</title></head><body>";

		html += "<h1>User Login Test</h1>";
		html += "<h2>User Details</h2>";
		html += "<table style=\"width:500px\">" + "<tr><td>CRSID</td><td>"
				+ user.getCrsid() + "</td></tr>" + "<tr><td>Name</td><td>"
				+ user.getDisplayName() + "</td></tr>"
				+ "<tr><td>Type</td><td>"
				+ (user.getIsStudent() ? "Student " : "Staff") + "</td></tr>"
				+ "<tr><td>College</td><td>" + user.getCollege() + "</td></tr>"
				+ "<tr><td>Surname</td><td>" + user.getSurname() + "</td></tr>"
				+ "<tr><td>Regname</td><td>" + user.getRegName() + "</td></tr>"
				+ "<tr><td>Email</td><td>" + user.getEmail() + "</td></tr>";

		for (String inst : user.getInstitutions()) {
			html += "<tr><td>" + inst + "</td></tr>";
		}

		html += "</td></tr>" + "</table>";

		html += "<hr>";

		List<Grouping> grps = db.getGroupings(user.getCrsid(), true);

		html += "<h2>User Roles</h2>";
		html += "<table style=\"width:500px\">";
		for (Grouping grp : grps) {
			Group g = db.getGroup(grp.getGroup());
			Role r = grp.getRole();
			html += "<tr><td>" + g.getName() + "</td><td>" + r.name()
					+ "</td></tr>";
		}
		html += "</table>";

		html += "</body></html>";
		return Response.ok(html).build();
	}

	/**
	 * If the user does not exist in our database then create an object for them
	 * using information from LDAP and store it.
	 * 
	 * Student/Academic is determined by checking for the presence of any of the
	 * Strings in Strings.ACADEMICINSTITUTIONS in the user's list of
	 * institutions.
	 * 
	 * This is a GET request as no sensitive information is passed in this
	 * request. The function is to add a user to the database if this is their
	 * first time visiting and so caching is allowed as it is idempotent due to
	 * the mongo save call.
	 * 
	 * @param request
	 * @return response
	 */
	@GET
	@Path("/login")
	@Produces("application/json")
	public Response login(@Context HttpServletRequest request) {

		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		if (crsid == null) {
			return Response.status(Status.NOT_FOUND).build();
		}

		User user = db.getUser(crsid);
		if (user == null || user.getLdap() == null
				|| user.getLdap().plusDays(1).isBeforeNow()) {
			String ssh = null;
			if (user != null) {
				ssh = user.getSsh();
			}
			user = ldapProduceUser(crsid);
			user.setSsh(ssh);
			db.saveUser(user);
			return Response.status(Status.CREATED).entity(user).build();
		}
		user.setHasLogged(true);
		db.saveUser(user);
		return Response.ok(user).build();
	}

	/**
	 * @param request
	 * @return
	 */
	@DELETE
	@Path("/logout")
	@Produces("application/json")
	public Response logout(@Context HttpServletRequest request) {
		request.getSession().invalidate();
		return Response.ok().build();

	}

	/**
	 * Queries LDAP for user information, if a connection to LDAP is not
	 * available, returns a user object with only a crsid and the ldap flag set
	 * to false.
	 * 
	 * @param crsid
	 * @return A potentially populated user object
	 */
	public User ldapProduceUser(String crsid) {
		LDAPUser u;
		User user;
		try {
			// TODO async
			u = LDAPQueryManager.getUser(crsid);
			boolean notStudent = academicConfig.getConfig().represents(u);
			boolean admin = adminConfig.getConfig().isAdmin(crsid);
			user = new User(crsid, u.getSurname(), u.getRegName(),
					u.getDisplayName(), u.getEmail(), u.getInstitutions(),
					u.getCollegeName(), !notStudent, admin);
			List<String> photos = u.getPhotos();
			if (photos != null) {
				user.setPhoto(photos.get(photos.size() - 1));
			}
			if (admin) {
				for (Group group : db.getGroups()) {
					db.saveGrouping(new Grouping(group.getGroupId(), user.getCrsid(), Role.ADMIN));
				}
			}
		} catch (LDAPObjectNotFoundException e) {
			user = new User(crsid);
		}
		return user;
	}

}
