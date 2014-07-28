package uk.ac.cam.cl.ticking.ui.api;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationFile;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;

import com.google.inject.Inject;

public class GroupApiFacade implements IGroupApiFacade {

	private IDataManager db;
	@SuppressWarnings("unused")
	// not currently used but could quite possibly be needed in the future, will
	// remove if not
	private ConfigurationFile config;

	@Inject
	public GroupApiFacade(IDataManager db, ConfigurationFile config) {
		this.db = db;
		this.config = config;
	}

	@Override
	public Response getGroup(String gid, boolean byName) {
		Group group = byName ? db.getGroupByName(gid) : db.getGroup(gid);
		return Response.ok(group).build();
	}

	@Override
	public Response getUsers(String gid) {
		List<User> users = db.getUsers(gid);
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
	public Response addGroup(Group group) {
		try {
			db.insertGroup(group);
		} catch (DuplicateDataEntryException de) {
			return Response.status(409).build();
		}
		return Response.status(201).entity(group).build();
	}

}
