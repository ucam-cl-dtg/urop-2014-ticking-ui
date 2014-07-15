package uk.ac.cam.cl.ticking.ui.auth;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.dtg.ldap.LDAPObjectNotFoundException;
import uk.ac.cam.cl.dtg.ldap.LDAPQueryManager;
import uk.ac.cam.cl.dtg.ldap.LDAPUser;
import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.ETickGuiceConfigurationModule;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

@Path("/raven")
public class RavenLogin {
	
	private IDataManager db;
	
	@Inject
	public RavenLogin(IDataManager db) {
		this.db = db;
	}

	
	public Response stats(User user) {

		String html = "<html><head><title>User Info</title></head><body>";

		html += "<h1>User Login Test</h1>";
		html += "<h2>User Details</h2>";
		html += "<table style=\"width:500px\">" 
				+ "<tr><td>CRSID</td><td>"+ user.getCrsid() + "</td></tr>"
				+ "<tr><td>Name</td><td>"+ user.getDisplayName() + "</td></tr>"
				+ "<tr><td>Type</td><td>"+ (user.getIsStudent() ? "Student " : "Staff") + "</td></tr>"
				+ "<tr><td>College</td><td>" + user.getCollege() + "</td></tr>"
				+ "<tr><td>Surname</td><td>" + user.getSurname() + "</td></tr>"
				+ "<tr><td>Regname</td><td>" + user.getRegName() + "</td></tr>"
				+ "<tr><td>Email</td><td>" + user.getEmail() + "</td></tr>";
		
		for(String inst : user.getInstitutions()) {
			html += "<tr><td>" + inst + "</td></tr>";
		}
		
		html += "</td></tr>" + "</table>";

		html += "<hr>";

		List<Grouping> grps = db.getGroupings(user);

		html += "<h2>User Roles</h2>";
		html += "<table style=\"width:500px\">";
		for (Grouping grp : grps) {
			Group g = grp.fetchGroup();
			Role r = grp.getRole();
			html += "<tr><td>" + g.getName() + "</td><td>" + r.name()
					+ "</td></tr>";
		}
		html += "</table>";

		html += "</body></html>";
		return Response.status(200).entity(html).build();
	}

	@GET
	@Path("/login")
	public Response Login(@Context HttpServletRequest request) {

		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		User user = db.getUser(crsid);
		if (user == null) {
			LDAPUser u;
			try {
				u = LDAPQueryManager.getUser(crsid);
			} catch (LDAPObjectNotFoundException e) {
				return Response.status(600).entity(e.getMessage()).build();
			}
			boolean notStudent = u.getInstitutions().contains(Strings.LAB);
			user = new User(crsid, u.getSurname(), u.getRegName(),
					u.getDisplayName(), u.getEmail(),
					u.getInstitutions(), u.getCollegeName(), !notStudent);
			db.saveUser(user);
		}
		
		return stats(user);
	}

}
