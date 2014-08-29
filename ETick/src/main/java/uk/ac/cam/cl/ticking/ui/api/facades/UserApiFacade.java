package uk.ac.cam.cl.ticking.ui.api.facades;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.dtg.teaching.exceptions.SerializableException;
import uk.ac.cam.cl.git.api.KeyException;
import uk.ac.cam.cl.git.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IUserApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.ToDoBean;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.DeadlineFirstComparator;
import uk.ac.cam.cl.ticking.ui.util.PermissionsManager;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class UserApiFacade implements IUserApiFacade {

	private static final Logger log = LoggerFactory
			.getLogger(UserApiFacade.class.getName());

	private IDataManager db;

	private ConfigurationLoader<Configuration> config;

	private WebInterface gitServiceProxy;
	
	private PermissionsManager permissions;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public UserApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config,
			WebInterface gitServiceProxy, PermissionsManager permissions) {
		this.db = db;
		this.config = config;
		this.gitServiceProxy = gitServiceProxy;
		this.permissions = permissions;
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
	public Response getUserFromCrsid(HttpServletRequest request, String crsid) {
		//Currently unused but may be needed in future for permissions checking
		@SuppressWarnings("unused")
		String myCrsid = (String) request.getSession().getAttribute(
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
		if (!permissions.isAdmin(myCrsid)) {
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
		List<Group> groups = new ArrayList<>();
		List<Grouping> groupings = db.getGroupings(crsid, true);
		
		for (Grouping grouping :groupings) {
			if (grouping.getRole()!=Role.ADMIN) {
				groups.add(db.getGroup(grouping.getGroup()));
			}
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
		Collections.sort(ticks);
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
			gitServiceProxy.addSSHKey(config.getConfig().getSecurityToken(), key, crsid);
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName().equals(IOException.class.getName())) {
				log.error("User " + crsid + " failed adding ssh key for " + crsid + "\nCause: "
						+ s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

			if (s.getClassName().equals(
					KeyException.class.getName())) {
				log.error("User " + crsid + " failed adding ssh key for " + crsid + "\nCause: "
						+ s.toString());
				
				log.error(s.getMessage());
				String[] badkeys = s.getMessage().trim().split(" ");
				for (String badkey: badkeys) {
					log.error(badkey);
					User badUser = db.getUser(badkey.split("\\.")[0]);
					try {
						gitServiceProxy.addSSHKey(config.getConfig().getSecurityToken(), badUser.getSsh(), crsid);
					} catch (InternalServerErrorException | IOException | KeyException e1) {
						badUser.setSsh(null);
						db.saveUser(badUser);
					}
				}
				if (s.getMessage().contains(crsid)) {
					return Response.status(Status.INTERNAL_SERVER_ERROR)
							.entity(Strings.BADKEY).build();
				}
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();

			} else {
				log.error("User " + crsid + " failed adding ssh key for " + crsid + "\nCause: "
						+ s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}
		} catch (IOException | KeyException e) {
			log.error("User " + crsid + " failed adding ssh key for " + crsid, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		}

		/* Add the key to the user object, save and return */
		user.setSsh(key);
		db.saveUser(user);
		return Response.status(Status.CREATED).entity(user).build();
	}
	
	@Override
	public Response getToDo(HttpServletRequest request) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		
		/* Get the user object, returning if not found */
		User user = db.getUser(crsid);

		if (user == null) {
			log.error("User " + crsid + " requested user " + crsid
					+ " to get todos, but they couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}
		
		Set<Tick> ticks = new HashSet<>();
		List<ToDoBean> todos = new ArrayList<>();
		
		for (Group group : db.getGroups(crsid, Role.SUBMITTER)) {
			for (String tickId : group.getTicks()) {
				Tick tick = db.getTick(tickId);
				ticks.add(tick);
			}
		}
		
		List<Tick> tickList = new ArrayList<>(ticks);
		Collections.sort(tickList, new DeadlineFirstComparator());
		
		for (Tick tick : tickList) {
			Fork fork = db.getFork(Fork.generateForkId(crsid, tick.getTickId()));
			todos.add(new ToDoBean(tick, fork));
		}
		
		return Response.ok(todos).build();
		
	}
}
