package uk.ac.cam.cl.ticking.ui.auth;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.dtg.ldap.LDAPObjectNotFoundException;
import uk.ac.cam.cl.dtg.ldap.LDAPQueryManager;
import uk.ac.cam.cl.dtg.ldap.LDAPUser;
import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.dao.DatabasePopulator;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

@Path("/raven")
public class RavenManager {

	private IDataManager db;

	@Inject
	public RavenManager(IDataManager db) {
		this.db = db;
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
		return Response.status(200).entity(html).build();
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
	public Response Login(@Context HttpServletRequest request) {

		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		User user = db.getUser(crsid);
		if (user == null) {
			try {
				user = ldapProduceUser(crsid);
			} catch (LDAPObjectNotFoundException e) {
				user = new User(crsid);
				e.printStackTrace();
			} finally {
				db.saveUser(user);
			}
			DatabasePopulator.testPopulate(user);
		}

		return Response.status(201).entity(user).build();
	}
	
	public User ldapProduceUser(String crsid) throws LDAPObjectNotFoundException {
		LDAPUser u;
		u = LDAPQueryManager.getUser(crsid);
		boolean notStudent = false;
		for (String inst : Strings.ACADEMICINSTITUTIONS) {
			notStudent = u.getInstitutions().contains(inst);
			if (notStudent) {
				break;
			}
		}

		return new User(crsid, u.getSurname(), u.getRegName(),
				u.getDisplayName(), u.getEmail(), u.getInstitutions(),
				u.getCollegeName(), !notStudent);
	}

}
