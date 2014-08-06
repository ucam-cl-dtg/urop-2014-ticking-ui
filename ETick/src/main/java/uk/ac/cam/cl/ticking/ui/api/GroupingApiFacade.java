package uk.ac.cam.cl.ticking.ui.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupingApiFacade#addGrouping
	 * (javax.servlet.http.HttpServletRequest, java.lang.String,
	 * java.lang.String, java.util.List)
	 */
	@Override
	public Response addGroupings(HttpServletRequest request, String groupId,
			GroupingBean groupingBean) {
		if (groupingBean.getRoles().isEmpty()) {
			return Response.status(Status.BAD_REQUEST)
					.entity(Strings.ATLEASTONEROLE).build();
		}
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Role> myRoles = db.getRoles(groupId, myCrsid);
		if (!myRoles.contains(Role.AUTHOR)) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}
		for (String crsid : groupingBean.getCrsids()) {
			try {
				db.insertUser(raven.ldapProduceUser(crsid));
			} catch (DuplicateDataEntryException e) {
				// Do nothing
				// The user is already in the database and so we don't need to
				// add
				// them.
			}
			for (Role r : groupingBean.getRoles()) {
				db.saveGrouping(new Grouping(groupId, crsid, r));
			}
		}
		List<User> users = db.getUsers(groupId);
		Collections.sort(users);
		return Response.status(Status.CREATED).entity(users).build();
	}

	@Override
	public Response deleteGroupings(HttpServletRequest request, String groupId,
			GroupingBean groupingBean) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Role> myRoles = db.getRoles(groupId, myCrsid);
		if (!myRoles.contains(Role.AUTHOR)) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		String output = "Users deleted";

		Group group = db.getGroup(groupId);
		for (String crsid : groupingBean.getCrsids()) {
			if (crsid.equals(group.getCreator())) {
				output = Strings.REMOVECREATOR;
			} else {
				List<Role> roles = groupingBean.getRoles();
				if (roles.isEmpty()) {
					roles = Arrays.asList(Role.values());
				}
				for (Role role : roles) {
					db.removeUserGroupRole(crsid, groupId, role);
				}
			}
		}
		return Response.ok().entity(output).build();
	}

}
