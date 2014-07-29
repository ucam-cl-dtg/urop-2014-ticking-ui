package uk.ac.cam.cl.ticking.ui.api;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IUserApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;

import com.google.inject.Inject;

public class UserApiFacade implements IUserApiFacade {

	private IDataManager db;

	@SuppressWarnings("unused")
	// Currently not needed but these classes are still not final and it is
	// quite likely to be required in future
	private Configuration config;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public UserApiFacade(IDataManager db, Configuration config) {
		this.db = db;
		this.config = config;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.IUserApiFacade#getUser(
	 * javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public Response getUser(HttpServletRequest request) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		User user = db.getUser(crsid);
		return Response.ok(user).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.IUserApiFacade#getGroups
	 * (javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public Response getGroups(HttpServletRequest request) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Group> groups = db.getGroups(crsid);
		Collections.sort(groups);
		return Response.ok(groups).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.IUserApiFacade#getGroupRoles
	 * (javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public Response getGroupRoles(HttpServletRequest request, String groupId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Role> roles = db.getRoles(groupId, crsid);
		return Response.ok(roles).build();
	}
}
