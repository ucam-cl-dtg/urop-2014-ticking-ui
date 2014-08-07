package uk.ac.cam.cl.ticking.ui.api;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.joda.time.DateTime;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.GroupBean;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class GroupApiFacade implements IGroupApiFacade {

	private IDataManager db;
	@SuppressWarnings("unused")
	// not currently used but could quite possibly be needed in the future, will
	// remove if not
	private ConfigurationLoader<Configuration> config;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public GroupApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config) {
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
	public Response getGroup(String groupId) {
		Group group = db.getGroup(groupId);
		return Response.ok(group).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade#deleteGroup
	 * (javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public Response deleteGroup(HttpServletRequest request, String groupId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		Group group = db.getGroup(groupId);
		if (!crsid.equals(group.getCreator())) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}
		db.removeGroup(groupId);
		return Response.ok().build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade#getUsers
	 * (java.lang.String)
	 */
	@Override
	public Response getUsers(String groupId) {
		List<User> users = db.getUsers(groupId);
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
	 * (javax.servlet.http.HttpServletRequest, java.util.List,
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.GroupBean)
	 */
	@Override
	public Response addGroup(HttpServletRequest request, List<String> roles,
			GroupBean groupBean) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		User user = db.getUser(crsid);
		if (user.getIsStudent()) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}
		Group group = new Group(groupBean.getName(), crsid);
		if (groupBean.getName().equalsIgnoreCase("xyzzy")) {
			return Response.status(Status.NOT_FOUND)
					.entity("Nothing happens...").build();
		}
		try {
			group.setInfo(URLDecoder.decode(groupBean.getInfo(),
					StandardCharsets.UTF_8.name()));
		} catch (UnsupportedEncodingException e) {
			// Hardcoded: known to be supported @see
			// http://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html#iana
		}
		try {
			db.insertGroup(group);
		} catch (DuplicateDataEntryException de) {
			return Response.status(Status.CONFLICT).build();
		}
		for (String role : roles) {
			Grouping grouping = new Grouping(group.getGroupId(), crsid,
					Role.valueOf(role));
			db.saveGrouping(grouping);
		}
		return Response.status(Status.CREATED).entity(group).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade#updateGroup
	 * (javax.servlet.http.HttpServletRequest, java.lang.String,
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.GroupBean)
	 */
	@Override
	public Response updateGroup(HttpServletRequest request, String groupId,
			GroupBean groupBean) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Role> myRoles = db.getRoles(groupId, crsid);
		if (!myRoles.contains(Role.AUTHOR)) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}
		Group prevGroup = db.getGroup(groupId);
		if (prevGroup != null) {
			prevGroup.setEdited(DateTime.now());
			prevGroup.setEditedBy(crsid);
			prevGroup.setInfo(groupBean.getInfo());
			prevGroup.setName(groupBean.getName());
			db.saveGroup(prevGroup);
			return Response.status(Status.CREATED).entity(prevGroup).build();
		} else {
			// TODO should this behave like so?
			return addGroup(request, new ArrayList<String>(), groupBean);
		}

	}

	public Response cloneGroup(HttpServletRequest request, String groupId,
			boolean members, boolean ticks, GroupBean groupBean) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		Group prevGroup = db.getGroup(groupId);
		List<Role> myRoles = db.getRoles(groupId, crsid);
		if (!myRoles.contains(Role.AUTHOR)) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}
		Group group = new Group(groupBean.getName(), crsid);
		try {
			group.setInfo(URLDecoder.decode(groupBean.getInfo(),
					StandardCharsets.UTF_8.name()));
		} catch (UnsupportedEncodingException e) {
			// Hardcoded: known to be supported @see
			// http://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html#iana
		}
		if (members) {
			for (Grouping grouping : db.getGroupings(groupId, false)) {
				db.saveGrouping(new Grouping(group.getGroupId(), grouping
						.getUser(), grouping.getRole()));
			}
		}
		if (ticks) {
			for (String tickId : prevGroup.getTicks()) {
				Tick tick = db.getTick(tickId);
				tick.addGroup(groupId);
				db.saveTick(tick);
			}
			group.setTicks(prevGroup.getTicks());
		}
		try {
			db.insertGroup(group);
		} catch (DuplicateDataEntryException de) {
			return Response.status(Status.CONFLICT).build();
		}
		return Response.status(Status.CREATED).entity(group).build();
	}

}
