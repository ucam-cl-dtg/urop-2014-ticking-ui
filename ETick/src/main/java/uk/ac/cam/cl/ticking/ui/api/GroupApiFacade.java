package uk.ac.cam.cl.ticking.ui.api;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
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
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.UserRoleBean;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.ForkStatusCsv;
import uk.ac.cam.cl.ticking.ui.util.ForkStatusXls;
import uk.ac.cam.cl.ticking.ui.util.PermissionsManager;
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

	private PermissionsManager permissions;

	private ForkStatusCsv csv;
	private ForkStatusXls xls;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public GroupApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config,
			TickSignups tickSignupService, PermissionsManager permissions,
			ForkStatusCsv csv, ForkStatusXls xls) {
		this.db = db;
		this.config = config;
		this.tickSignupService = tickSignupService;
		this.permissions = permissions;
		this.csv = csv;
		this.xls = xls;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getGroup(String groupId) {
		Group group = db.getGroup(groupId);

		/* Get the group object, returning if not found */
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
	public Response exportGroup(String groupId) {
		Group group = db.getGroup(groupId);

		/* Get the group object, returning if not found */
		if (group == null) {
			log.error("Requested group " + groupId
					+ " but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		File temp;
		PrintWriter writer;

		try {
			temp = File.createTempFile(group.getGroupId(), ".txt");
			writer = new PrintWriter(temp);

		} catch (IOException e) {
			log.error("Tried exporting group " + groupId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(Strings.IDEMPOTENTRETRY).build();
		}

		for (Role role : Role.values()) {
			writer.println(role.name());
			for (User user : db.getUsers(groupId, role)) {
				writer.print(user.getCrsid() + " ");
			}
			writer.println();
		}

		writer.close();

		ResponseBuilder response = Response.ok((Object) temp);
		response.header("Content-Disposition", "attachment; filename=\""
				+ group.getName() + ".txt\"");
		response.header("Set-Cookie", "fileDownload=true; path=/");
		return response.build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response exportGroupForkStatusCsv(String groupId) {
		Group group = db.getGroup(groupId);

		/* Get the group object, returning if not found */
		if (group == null) {
			log.error("Requested group " + groupId
					+ " but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		File temp;
		try {
			temp = csv.generateCsvFile(group);
		} catch (IOException e) {
			log.error("Tried exporting group fork status" + groupId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(Strings.IDEMPOTENTRETRY).build();
		}

		ResponseBuilder response = Response.ok((Object) temp);
		response.header("Content-Disposition", "attachment; filename=\""
				+ group.getName() + ".csv\"");
		response.header("Set-Cookie", "fileDownload=true; path=/");
		return response.build();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response exportGroupForkStatusXls(String groupId) {
		Group group = db.getGroup(groupId);

		/* Get the group object, returning if not found */
		if (group == null) {
			log.error("Requested group " + groupId
					+ " but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		File temp;
		try {
			temp = xls.generateXlsFile(group);
		} catch (IOException e) {
			log.error("Tried exporting group fork status" + groupId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(Strings.IDEMPOTENTRETRY).build();
		}

		ResponseBuilder response = Response.ok((Object) temp);
		response.header("Content-Disposition", "attachment; filename=\""
				+ group.getName() + ".xls\"");
		response.header("Set-Cookie", "fileDownload=true; path=/");
		return response.build();
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
			log.error("User " + crsid + " requested group " + groupId
					+ " for deletion, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (!permissions.groupCreator(crsid, group)) {
			log.warn("User " + crsid + " tried to delete " + groupId
					+ " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Remove the group and return ok */
		db.removeGroup(groupId);
		return Response.ok(Strings.DELETED).build();
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
		return Response.ok(userBeans).build();
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
			log.error("User " + crsid + " requested user " + crsid
					+ " to add group, but they couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (permissions.isStudent(user)) {
			log.warn("User " + crsid
					+ " tried to create a group but was denied permission");
			return Response.status(Status.FORBIDDEN)
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
			log.error("User " + crsid
					+ " tried to insert group into database with groupId "
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

		/* Give admins the correct role for the group */
		for (User admin : db.getAdmins()) {
			Grouping grouping = new Grouping(group.getGroupId(),
					admin.getCrsid(), Role.ADMIN);
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
		if (!permissions.hasRole(crsid, groupId, Role.AUTHOR)) {
			log.warn("User " + crsid + " tried to update the group " + groupId
					+ " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Getting the original group object */
		Group prevGroup = db.getGroup(groupId);

		if (prevGroup != null) {
			/* Merge bean fields with group fields */
			prevGroup.setEdited(DateTime.now());
			prevGroup.setEditedBy(crsid);
			prevGroup.setInfo(groupBean.getInfo());
			prevGroup.setName(groupBean.getName());

			/* Save group and return it */
			db.saveGroup(prevGroup);
			return Response.status(Status.CREATED).entity(prevGroup).build();

		} else {
			/*
			 * There was no original group object, so create a new one from the
			 * same bean
			 */
			log.warn("User "
					+ crsid
					+ " requested group "
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

		/* Get the group object to be cloned and return if it doesn't exist */
		Group prevGroup = db.getGroup(groupId);

		if (prevGroup == null) {
			log.error("User " + crsid + " requested group " + groupId
					+ " for cloning, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (!permissions.hasRole(crsid, groupId, Role.AUTHOR)) {
			log.warn("User " + crsid + " tried to clone the group " + groupId
					+ " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Create the clone */
		Group group = new Group(groupBean.getName(), crsid);

		/*
		 * Insert the clone into the database, do this now due to potential _id
		 * clash from mongo
		 */
		try {
			db.insertGroup(group);
		} catch (DuplicateDataEntryException e) {
			log.error("User " + crsid
					+ " tried to insert group into database with groupId "
					+ group.getGroupId(), e);
			return Response.status(Status.CONFLICT)
					.entity(Strings.IDEMPOTENTRETRY).build();
		}

		/* Create mirror with the tick signup service */
		try {
			tickSignupService.createGroup(group.getGroupId());

		} catch (DuplicateNameException e) {
			log.warn(
					"GroupId clash with signups database, recursing to generate new Id",
					e);
			return cloneGroup(request, groupId, members, ticks, groupBean);
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

		/* Retain members in the clone if we wanted to */
		if (members) {
			for (Grouping grouping : db.getGroupings(groupId, false)) {
				db.saveGrouping(new Grouping(group.getGroupId(), grouping
						.getUser(), grouping.getRole()));
			}
		} else {
			/* Add the user as an author and all admins to the clone */
			db.saveGrouping(new Grouping(group.getGroupId(), crsid, Role.AUTHOR));

			for (User user : db.getAdmins()) {
				db.saveGrouping(new Grouping(group.getGroupId(), user
						.getCrsid(), Role.ADMIN));
			}
		}

		/* Retain ticks in the clone if we wanted to */
		if (ticks) {
			for (String tickId : prevGroup.getTicks()) {
				Tick tick = db.getTick(tickId);
				tick.addGroup(groupId);
				db.saveTick(tick);
			}
			group.setTicks(prevGroup.getTicks());
		}

		db.saveGroup(group);

		/* Return the clone */
		return Response.status(Status.CREATED).entity(group).build();
	}

}
