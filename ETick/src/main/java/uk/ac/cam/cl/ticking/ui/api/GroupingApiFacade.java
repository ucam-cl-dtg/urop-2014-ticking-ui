package uk.ac.cam.cl.ticking.ui.api;

import java.util.Arrays;
import java.util.Collections;
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
import uk.ac.cam.cl.ticking.ui.auth.RavenManager;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
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
	private RavenManager raven;

	@Inject
	public GroupingApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config, RavenManager raven) {
		this.db = db;
		this.config = config;
		this.raven = raven;
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
			log.error("Tried to add users to a group but neglected to supply desired roles");
			return Response.status(Status.BAD_REQUEST)
					.entity(Strings.ATLEASTONEROLE).build();
		}

		/* Check that the group we want to add members to exists */
		Group group = db.getGroup(groupId);

		if (group == null) {
			log.error("Requested group " + groupId
					+ " to add members, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		List<Role> myRoles = db.getRoles(groupId, myCrsid);
		if (!myRoles.contains(Role.AUTHOR)) {
			log.warn("User " + myCrsid + " tried to add a member to group "
					+ groupId + " but was denied permission");
			return Response.status(Status.UNAUTHORIZED)
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
		Collections.sort(users);
		return Response.status(Status.CREATED).entity(users).build();
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
			log.error("Requested group " + groupId
					+ " to remove members, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		List<Role> myRoles = db.getRoles(groupId, myCrsid);
		if (!myRoles.contains(Role.AUTHOR)) {
			log.warn("User " + myCrsid + " tried to remove a member to group "
					+ groupId + " but was denied permission");
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Delete specified roles for the given group for each member */
		String output = "Successfully removed users.";

		for (String crsid : groupingBean.getCrsids()) {
			/*
			 * Do not delete the group creator and alert the user that they
			 * tried to do this. However still continue with all other members
			 */
			if (crsid.equals(group.getCreator())) {
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
