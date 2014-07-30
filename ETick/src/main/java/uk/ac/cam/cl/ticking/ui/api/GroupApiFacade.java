package uk.ac.cam.cl.ticking.ui.api;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;

import com.google.inject.Inject;

public class GroupApiFacade implements IGroupApiFacade {

	private IDataManager db;
	@SuppressWarnings("unused")
	// not currently used but could quite possibly be needed in the future, will
	// remove if not
	private ConfigurationLoader<Configuration> config;

	@Inject
	public GroupApiFacade(IDataManager db, ConfigurationLoader<Configuration> config) {
		this.db = db;
		this.config = config;
	}

	@Override
	public Response getGroup(String groupId, boolean byName) {
		Group group = byName ? db.getGroupByName(groupId) : db.getGroup(groupId);
		return Response.ok(group).build();
	}

	@Override
	public Response getUsers(String groupId) {
		List<User> users = db.getUsers(groupId);
		Collections.sort(users);
		return Response.ok(users).build();
	}

	@Override
	public Response getGroups() {
		List<Group> groups = db.getGroups();
		Collections.sort(groups);
		return Response.ok(groups).build();
	}

	@Override
	public Response addGroup(HttpServletRequest request, String name) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		Group group = new Group(name, crsid);
		try {
			db.insertGroup(group);
		} catch (DuplicateDataEntryException de) {
			return Response.status(Status.CONFLICT).build();
		}
		Grouping grouping = new Grouping(group.getGroupId(), crsid, Role.AUTHOR);
		db.saveGrouping(grouping);
		return Response.status(Status.CREATED).entity(group).build();
	}

}
