package uk.ac.cam.cl.ticking.ui.api.facades;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupingApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.GroupingBean;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.UserRoleBean;
import uk.ac.cam.cl.ticking.ui.auth.AuthManager;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.util.PermissionsManager;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class GroupingApiFacade implements IGroupingApiFacade {

	private static final Logger log = LoggerFactory
			.getLogger(GroupingApiFacade.class.getName());

	private IDataManager db;

	@SuppressWarnings("unused")
	// not currently used but could quite possibly be needed in the future, will
	// remove if not
	private ConfigurationLoader<Configuration> config;

	private AuthManager raven;
	
	private PermissionsManager permissions;

	@Inject
	public GroupingApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config, AuthManager raven, PermissionsManager permissions) {
		this.db = db;
		this.config = config;
		this.raven = raven;
		this.permissions = permissions;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response addGroupings(HttpServletRequest request, String groupId,
			GroupingBean groupingBean) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Return if we have not given any roles */
		if (groupingBean.getRoles().isEmpty()) {
			log.error("User "
					+ myCrsid
					+ " tried to add users to a group but neglected to supply desired roles");
			return Response.status(Status.BAD_REQUEST)
					.entity(Strings.ATLEASTONEROLE).build();
		}

		/* Check that the group we want to add members to exists */
		Group group = db.getGroup(groupId);

		if (group == null) {
			log.error("User " + myCrsid + " requested group " + groupId
					+ " to add members, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (!permissions.hasRole(myCrsid, groupId, Role.AUTHOR)) {
			log.warn("User " + myCrsid + " tried to add a member to group "
					+ groupId + " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/*
		 * Try inserting all users into the database in order to create those
		 * which do not exist
		 */
		for (String crsid : groupingBean.getCrsids()) {
			try {
				db.insertUser(raven.ldapProduceUser(crsid));
			} catch (DuplicateDataEntryException e) {
				/*
				 * Do nothing, the user is already in the database and so we
				 * don't need to add them.
				 */
			}

			/* Assign each user the requested roles for the group */
			for (Role r : groupingBean.getRoles()) {
				db.saveGrouping(new Grouping(groupId, crsid, r));
			}
		}

		/* Return the sorted list of users for the group */
		List<User> users = db.getUsers(groupId);
		
		/* Remove users who are only admins for the group */
		Iterator<User> i = users.iterator();
		while (i.hasNext()) {
			User user = i.next();
			List<Role> roles = db.getRoles(groupId, user.getCrsid());
			roles.remove(Role.ADMIN);
			if (roles.size() == 0) {
				i.remove();
			}
		}
		Collections.sort(users);
		
		List<UserRoleBean> userBeans = new ArrayList<>();
		for (User user : users) {
			userBeans.add(new UserRoleBean(user,db.getRoles(groupId, user.getCrsid())));
		}
		
		return Response.status(Status.CREATED).entity(userBeans).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response deleteGroupings(HttpServletRequest request, String groupId,
			GroupingBean groupingBean) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Check that the group we want to add members to exists */
		Group group = db.getGroup(groupId);

		if (group == null) {
			log.error("User " + myCrsid + " requested group " + groupId
					+ " to remove members, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (!permissions.hasRole(myCrsid, groupId, Role.AUTHOR)) {
			log.warn("User " + myCrsid + " tried to remove a member to group "
					+ groupId + " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Delete specified roles for the given group for each member */
		String output = Strings.REMOVEDUSERS;

		for (String crsid : groupingBean.getCrsids()) {
			/*
			 * Do not delete the group creator and alert the user that they
			 * tried to do this. However still continue with all other members
			 */
			if (crsid.equals(group.getCreator())&&!permissions.isAdmin(myCrsid)) {
				output = Strings.REMOVECREATOR;
			} else {
				/*
				 * Delete the roles, if the roles field in the group bean is the
				 * empty list, delete all roles
				 */
				List<Role> roles = groupingBean.getRoles();
				if (roles.isEmpty()) {
					roles = Arrays.asList(Role.values());
				}
				for (Role role : roles) {
					db.removeUserGroupRole(crsid, groupId, role);
				}
			}
		}

		/* Return the output message */
		return Response.ok().entity(output).build();
	}

}
