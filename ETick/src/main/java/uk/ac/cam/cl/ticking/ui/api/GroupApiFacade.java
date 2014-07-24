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
	// Currently not needed but these classes are still not final and it is
	// quite likely to be required in future
	private ConfigurationFile config;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public GroupApiFacade(IDataManager db, ConfigurationFile config) {
		this.db = db;
		this.config = config;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade#getGroup
	 * (java.lang.String, boolean)
	 */
	@Override
	public Response getGroup(String gid, boolean byName) {
		Group group = byName ? db.getGroupByName(gid) : db.getGroup(gid);
		return Response.ok(group).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade#getUsers
	 * (java.lang.String)
	 */
	@Override
	public Response getUsers(String gid) {
		List<User> users = db.getUsers(gid);
		Collections.sort(users);
		return Response.ok(users).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade#getGroups()
	 */
	@Override
	public Response getGroups() {
		List<Group> groups = db.getGroups();
		Collections.sort(groups);
		return Response.ok(groups).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade#addGroup
	 * (uk.ac.cam.cl.ticking.ui.actors.Group)
	 */
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
