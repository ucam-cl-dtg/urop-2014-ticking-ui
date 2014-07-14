package uk.ac.cam.cl.ticking.ui.auth;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import com.google.inject.Guice;
import com.google.inject.Injector;

import uk.ac.cam.cl.dtg.ldap.LDAPObjectNotFoundException;
import uk.ac.cam.cl.dtg.ldap.LDAPQueryManager;
import uk.ac.cam.cl.dtg.ldap.LDAPUser;
import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.ETickGuiceConfigurationModule;
import uk.ac.cam.cl.ticking.ui.database.IDataManager;
import uk.ac.cam.cl.ticking.ui.database.MongoDataManager;
import uk.ac.cam.cl.ticking.ui.database.DatabaseTest;

@Path("/raven")
public class RavenViewTest
{
	@GET
	@Path("/")
	public Response getResponse(@Context HttpServletRequest request) {

		String html = "<html><head><title>User Info</title></head><body>";
		
		html += "<h1>User Login Test</h1>";
		
		DatabaseTest.testDB();

		String crsid = (String) request.getSession().getAttribute("RavenRemoteUser");

		try {
			LDAPUser u = LDAPQueryManager.getUser(crsid);
			html += "<h2>User Details</h2>";
			html += "<table style=\"width:500px\">"
					+ "<tr><td>CRSID</td><td>" + u.getID() + "</td></tr>"
					+ "<tr><td>Name</td><td>" + u.getDisplayName() + "</td></tr>"
					+ "<tr><td>Type</td><td>" + (u.isStudent() ? "Student " : "") + (u.isStaff() ? "Staff " : "") + "</td></tr>"
					+ "<tr><td>College</td><td>" + u.getCollegeName() + "</td></tr>"
					+ "<tr><td>Email</td><td>" + u.getEmail() + "</td></tr>"
					+ "</table>";

		}
		catch(LDAPObjectNotFoundException e){
			//Error getting user - do something
			html += e.getMessage();
		}
		
		html += "<hr>";
		
		User user = new User(crsid, "");

		Injector injector = Guice.createInjector(new ETickGuiceConfigurationModule());
		IDataManager db = injector.getInstance(IDataManager.class);

		List<Grouping> grps = db.getGroupings(user);
		
		html += "<h2>User Roles</h2>";
		html += "<table style=\"width:500px\">";
		for (Grouping grp : grps) {
			Group g = grp.fetchGroup();
			Role r = grp.getRole();
			html += "<tr><td>" + g.getName() + "</td><td>" + r.name() + "</td></tr>";
		}
		html += "</table>";

		html += "</body></html>";
		return Response.status(200).entity(html).build();
	}
}
