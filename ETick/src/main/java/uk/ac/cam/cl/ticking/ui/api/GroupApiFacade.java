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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.signups.api.exceptions.DuplicateNameException;
import uk.ac.cam.cl.ticking.signups.TickSignups;
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

	private static final Logger log = LoggerFactory
			.getLogger(GroupApiFacade.class.getName());

	private IDataManager db;
	@SuppressWarnings("unused")
	// not currently used but could quite possibly be needed in the future, will
	// remove if not
	private ConfigurationLoader<Configuration> config;

	private TickSignups tickSignupService;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public GroupApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config,
			TickSignups tickSignupService) {
		this.db = db;
		this.config = config;
		this.tickSignupService = tickSignupService;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getGroup(String groupId) {
		Group group = db.getGroup(groupId);

		if (group == null) {
			log.error("Requested group " + groupId
					+ " but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		return Response.ok(group).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response deleteGroup(HttpServletRequest request, String groupId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the group object, returning if not found */
		Group group = db.getGroup(groupId);

		if (group == null) {
			log.error("Requested group " + groupId
					+ " for deletion, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (!crsid.equals(group.getCreator())) {
			log.warn("User " + crsid + " tried to delete " + groupId
					+ " but was denied permission");
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Remove the group and return ok */
		db.removeGroup(groupId);
		return Response.ok().build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getUsers(String groupId) {

		/* Get the group object, returning if not found */
		Group group = db.getGroup(groupId);

		if (group == null) {
			log.error("Requested group " + groupId
					+ " to find members, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Get the users in the group, sort and return them */
		List<User> users = db.getUsers(groupId);
		Collections.sort(users);
		return Response.ok(users).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getGroups() {
		List<Group> groups = db.getGroups();
		Collections.sort(groups);
		return Response.ok(groups).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response addGroup(HttpServletRequest request, List<String> roles,
			GroupBean groupBean) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the user object, returning if not found */
		User user = db.getUser(crsid);

		if (user == null) {
			log.error("Requested user " + crsid
					+ " to add group, but they couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (user.getIsStudent()) {
			log.warn("User " + crsid
					+ " tried to create a group but was denied permission");
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Create the group from the given bean */
		Group group = new Group(groupBean.getName(), crsid);

		/* Create mirror with the tick signup service */
		try {
			tickSignupService.createGroup(group.getGroupId());

		} catch (DuplicateNameException e) {
			log.warn(
					"GroupId clash with signups database, recursing to generate new Id",
					e);
			return addGroup(request, roles, groupBean);
			// The groupId clashed with the signups groups ids, try again
		}

		if (groupBean.getName().equalsIgnoreCase("xyzzy")) {
			return Response.status(Status.NOT_FOUND)
					.entity("Nothing happens...").build();
		}

		/* Set the group info from an escaped string */
		try {
			group.setInfo(URLDecoder.decode(groupBean.getInfo(),
					StandardCharsets.UTF_8.name()));

		} catch (UnsupportedEncodingException e) {
			log.error("UTF_8 URL decoding failed", e);
			// Hardcoded: known to be supported @see
			// http://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html#iana
		}

		/* Insert the group into the database */
		try {
			db.insertGroup(group);
		} catch (DuplicateDataEntryException e) {
			log.error("Tried to insert group into database with groupId "
					+ group.getGroupId(), e);
			return Response.status(Status.CONFLICT)
					.entity(Strings.IDEMPOTENTRETRY).build();
		}

		/* Set requested roles for yourself in the new group */
		for (String role : roles) {
			Grouping grouping = new Grouping(group.getGroupId(), crsid,
					Role.valueOf(role));
			db.saveGrouping(grouping);
		}

		/* return the created group object */
		return Response.status(Status.CREATED).entity(group).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response updateGroup(HttpServletRequest request, String groupId,
			GroupBean groupBean) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Check permissions */
		List<Role> myRoles = db.getRoles(groupId, crsid);
		
		if (!myRoles.contains(Role.AUTHOR)) {
			log.warn("User " + crsid + " tried to update the group " + groupId
					+ " but was denied permission");
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Getting the original group object */
		Group prevGroup = db.getGroup(groupId);
		
		if (prevGroup != null) {
			/*Merge bean fields with group fields*/
			prevGroup.setEdited(DateTime.now());
			prevGroup.setEditedBy(crsid);
			prevGroup.setInfo(groupBean.getInfo());
			prevGroup.setName(groupBean.getName());
			
			/*Save group and return it*/
			db.saveGroup(prevGroup);
			return Response.status(Status.CREATED).entity(prevGroup).build();
			
		} else {
			/*
			 * There was no original group object, so create a new one from the
			 * same bean
			 */
			log.warn("Requested group "
					+ groupId
					+ " for updating, but it couldn't be found, creating a new group instead");
			return addGroup(request, new ArrayList<String>(), groupBean);
		}

	}

	/**
	 * {@inheritDoc}
	 */
	public Response cloneGroup(HttpServletRequest request, String groupId,
			boolean members, boolean ticks, GroupBean groupBean) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		
		/*Get the group object to be cloned and return if it doesn't exist*/
		Group prevGroup = db.getGroup(groupId);
		
		if (prevGroup == null) {
			log.error("Requested group " + groupId
					+ " for cloning, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}
		
		/*Check permissions*/
		List<Role> myRoles = db.getRoles(groupId, crsid);
		if (!myRoles.contains(Role.AUTHOR)) {
			log.warn("User " + crsid + " tried to clone the group " + groupId
					+ " but was denied permission");
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}
		
		/*Create the clone*/
		Group group = new Group(groupBean.getName(), crsid);
		
		/* Set the group info from an escaped string */
		try {
			group.setInfo(URLDecoder.decode(groupBean.getInfo(),
					StandardCharsets.UTF_8.name()));
		} catch (UnsupportedEncodingException e) {
			log.error("UTF_8 URL decoding failed", e);
			// Hardcoded: known to be supported @see
			// http://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html#iana
		}
		
		/*Retain members in the clone if we wanted to*/
		if (members) {
			for (Grouping grouping : db.getGroupings(groupId, false)) {
				db.saveGrouping(new Grouping(group.getGroupId(), grouping
						.getUser(), grouping.getRole()));
			}
		}
		
		/*Retain ticks in the clone if we wanted to*/
		if (ticks) {
			for (String tickId : prevGroup.getTicks()) {
				Tick tick = db.getTick(tickId);
				tick.addGroup(groupId);
				db.saveTick(tick);
			}
			group.setTicks(prevGroup.getTicks());
		}
		
		/*Insert the clone into the database*/
		try {
			db.insertGroup(group);
		} catch (DuplicateDataEntryException e) {
			log.error("Tried to insert group into database with groupId "
					+ group.getGroupId(), e);
			return Response.status(Status.CONFLICT).build();
		}
		
		/*Return the clone*/
		return Response.status(Status.CREATED).entity(group).build();
	}

}
