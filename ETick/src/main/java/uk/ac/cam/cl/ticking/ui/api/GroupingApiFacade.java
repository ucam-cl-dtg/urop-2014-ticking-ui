package uk.ac.cam.cl.ticking.ui.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupingApiFacade;
import uk.ac.cam.cl.ticking.ui.auth.RavenManager;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationFile;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class GroupingApiFacade implements IGroupingApiFacade {
	
	private IDataManager db;
	@SuppressWarnings("unused")
	// not currently used but could quite possibly be needed in the future, will
	// remove if not
	private ConfigurationFile config;
	private RavenManager raven;

	@Inject
	public GroupingApiFacade(IDataManager db, ConfigurationFile config, RavenManager raven) {
		this.db = db;
		this.config = config;
		this.raven = raven;
	}

	@Override
	public Response addGrouping(HttpServletRequest request, String crsid, String gid, List<Role> roles) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Role> myRoles = db.getRoles(gid, myCrsid);
		if (!myRoles.contains(Role.AUTHOR)) {
			return Response.status(401).entity(Strings.INVALIDROLE).build();
		}
		try {
			db.insertUser(raven.ldapProduceUser(crsid));
		} catch (DuplicateDataEntryException e) {
			//Do nothing
			//The user is already in the database and so we don't need to add them.
		}
		for (Role r : roles) {
			db.saveGrouping(new Grouping(gid, crsid, r));
		}
		List<User> users = db.getUsers(gid);
		return Response.status(201).entity(users).build();
	}

}
