package uk.ac.cam.cl.ticking.ui.api;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupingApiFacade;
import uk.ac.cam.cl.ticking.ui.auth.RavenManager;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class GroupingApiFacade implements IGroupingApiFacade {
	
	private IDataManager db;
	@SuppressWarnings("unused")
	// not currently used but could quite possibly be needed in the future, will
	// remove if not
	private ConfigurationLoader<Configuration> config;
	private RavenManager raven;

	@Inject
	public GroupingApiFacade(IDataManager db, ConfigurationLoader<Configuration> config, RavenManager raven) {
		this.db = db;
		this.config = config;
		this.raven = raven;
	}

	@Override
	public Response addGrouping(HttpServletRequest request, String crsid, String groupId, List<Role> roles) {
		if (roles.isEmpty()) {
			return Response.status(Status.BAD_REQUEST).entity(Strings.ATLEASTONEROLE).build();
		}
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Role> myRoles = db.getRoles(groupId, myCrsid);
		if (!myRoles.contains(Role.AUTHOR)) {
			return Response.status(Status.UNAUTHORIZED).entity(Strings.INVALIDROLE).build();
		}
		try {
			db.insertUser(raven.ldapProduceUser(crsid));
		} catch (DuplicateDataEntryException e) {
			//Do nothing
			//The user is already in the database and so we don't need to add them.
		}
		for (Role r : roles) {
			db.saveGrouping(new Grouping(groupId, crsid, r));
		}
		List<User> users = db.getUsers(groupId);
		Collections.sort(users);
		return Response.status(Status.CREATED).entity(users).build();
	}
	
	@Override
	public Response deleteGrouping(HttpServletRequest request,
			Grouping grouping) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Role> myRoles = db.getRoles(grouping.getGroup(), myCrsid);
		if (!myRoles.contains(Role.AUTHOR)) {
			return Response.status(Status.UNAUTHORIZED).entity(Strings.INVALIDROLE).build();
		}
		db.removeUserGroupRole(grouping.getUser(),grouping.getGroup(),grouping.getRole());
		return Response.ok().build();
	}

}
