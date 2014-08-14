package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.dtg.teaching.exceptions.SerializableException;
import uk.ac.cam.cl.git.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IUserApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.Admins;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class UserApiFacade implements IUserApiFacade {

	private static final Logger log = LoggerFactory
			.getLogger(UserApiFacade.class.getName());

	private IDataManager db;

	@SuppressWarnings("unused")
	// Currently not needed but these classes are still not final and it is
	// quite likely to be required in future
	private ConfigurationLoader<Configuration> config;

	private ConfigurationLoader<Admins> adminConfig;

	private WebInterface gitServiceProxy;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public UserApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config,
			ConfigurationLoader<Admins> adminConfig,
			WebInterface gitServiceProxy) {
		this.db = db;
		this.config = config;
		this.adminConfig = adminConfig;
		this.gitServiceProxy = gitServiceProxy;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getUser(HttpServletRequest request) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the user object, returning if not found */
		User user = db.getUser(crsid);

		if (user == null) {
			log.error("User " + crsid + " requested user " + crsid
					+ " but they couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		return Response.ok(user).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response deleteUser(HttpServletRequest request, String crsid,
			boolean purge) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		
		/*Check permissions*/
		if (!adminConfig.getConfig().isAdmin(myCrsid)) {
			log.warn("User " + myCrsid + " tried to delete user "
					+ crsid + " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDPERMISSION).build();
		}

		/* Get the user object, returning if not found */
		User user = db.getUser(crsid);

		if (user == null) {
			log.error("User " + myCrsid + " requested user " + crsid
					+ " for deletion, but they couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		db.removeUser(crsid, purge);
		return Response.ok().entity(Strings.DELETED).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getGroups(HttpServletRequest request) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		User user = db.getUser(crsid);
		List<Group> groups = db.getGroups(crsid);

		if (user.isAdmin()) {
			groups = db.getGroups();
		}
		Collections.sort(groups);
		return Response.ok(groups).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getGroupRoles(HttpServletRequest request, String groupId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Role> roles = db.getRoles(groupId, crsid);
		return Response.ok(roles).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getRoleGroups(HttpServletRequest request, String stringRole) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		Role role = Role.valueOf(stringRole);
		List<Group> groups = db.getGroups(crsid, role);
		Collections.sort(groups);
		return Response.ok(groups).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getMyTicks(HttpServletRequest request) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Tick> ticks = db.getAuthorTicks(crsid);
		return Response.ok(ticks).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response addSSHKey(HttpServletRequest request, String key) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the user object, returning if not found */
		User user = db.getUser(crsid);

		if (user == null) {
			log.error("User " + crsid + " requested user " + crsid
					+ " to add a public ssh key, but they couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Call the git service */
		try {
			gitServiceProxy.addSSHKey(key, crsid);
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			log.error("User " + crsid + " tried adding ssh key for " + crsid,
					s.getCause(), s.getStackTrace());
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(Strings.IDEMPOTENTRETRY).build();
		} catch (IOException e) {
			log.error("User " + crsid + " tried adding ssh key for " + crsid, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		}

		/* Add the key to the user object, save and return */
		user.setSsh(key);
		db.saveUser(user);
		return Response.status(Status.CREATED).entity(user).build();
	}
}
