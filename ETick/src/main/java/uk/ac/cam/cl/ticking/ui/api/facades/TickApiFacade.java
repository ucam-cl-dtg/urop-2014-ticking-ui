package uk.ac.cam.cl.ticking.ui.api.facades;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import publicinterfaces.ITestService;
import publicinterfaces.TestIDNotFoundException;
import publicinterfaces.TickSettings;
import uk.ac.cam.cl.dtg.teaching.containers.api.exceptions.TestNotFoundException;
import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.dtg.teaching.exceptions.SerializableException;
import uk.ac.cam.cl.git.api.DuplicateRepoNameException;
import uk.ac.cam.cl.git.api.FileBean;
import uk.ac.cam.cl.git.api.IllegalCharacterException;
import uk.ac.cam.cl.git.api.RepoUserRequestBean;
import uk.ac.cam.cl.git.api.RepositoryNotFoundException;
import uk.ac.cam.cl.git.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.ExtensionBean;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.ExtensionReturnBean;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.TickBean;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.ToDoBean;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.PermissionsManager;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class TickApiFacade implements ITickApiFacade {

	private static final Logger log = LoggerFactory
			.getLogger(TickApiFacade.class.getName());

	private IDataManager db;
	private ConfigurationLoader<Configuration> config;

	private ITestService testServiceProxy;
	private WebInterface gitServiceProxy;

	private PermissionsManager permissions;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public TickApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config,
			ITestService testServiceProxy, WebInterface gitServiceProxy,
			PermissionsManager permissions) {
		this.db = db;
		this.config = config;
		this.testServiceProxy = testServiceProxy;
		this.gitServiceProxy = gitServiceProxy;
		this.permissions = permissions;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getTick(String tickId) {
		Tick tick = db.getTick(tickId);
		return Response.ok().entity(tick).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response deleteTick(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the tick object, returning if not found */
		Tick tick = db.getTick(tickId);

		if (tick == null) {
			log.error("User " + crsid + " requested tick " + crsid
					+ " for deletion, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (!permissions.tickCreator(crsid, tick)) {
			log.warn("User " + crsid
					+ " tried to delete a tick but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Call the git service to delete the repository */
		try {
			gitServiceProxy.deleteRepository(config.getConfig()
					.getSecurityToken(), Tick.replaceDelimeter(tickId));
			gitServiceProxy.deleteRepository(config.getConfig()
					.getSecurityToken(), Tick.replaceDelimeter(tickId)
					+ "/correctness");
			db.removeTick(tickId);
			/* Remove dangling group references */
			for (String groupId : tick.getGroups()) {
				Group group = db.getGroup(groupId);
				group.removeTick(tickId);
				db.saveGroup(group);
			}

		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName().equals(IOException.class.getName())) {
				log.error("User " + crsid + " failed deleting repository for "
						+ tickId + "\nCause: " + s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

			if (s.getClassName().equals(
					RepositoryNotFoundException.class.getName())) {
				/* We still want to remove the tick */
				db.removeTick(tickId);
				/* Remove dangling group references */
				for (String groupId : tick.getGroups()) {
					Group group = db.getGroup(groupId);
					group.removeTick(tickId);
					db.saveGroup(group);
				}

				log.warn("User " + crsid + " failed deleting repository for "
						+ tickId + "\nCause: " + s.toString());

			} else {
				log.error("User " + crsid + " failed deleting repository for "
						+ tickId + "\nCause: " + s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

		} catch (IOException | RepositoryNotFoundException e) {
			log.error("User " + crsid + " failed deleting repository for "
					+ tickId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		}

		return Response.ok().entity(Strings.DELETED).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getTicks(HttpServletRequest request, String groupId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		List<Fork> forks = new ArrayList<>();
		List<Tick> ticks = db.getGroupTicks(groupId);
		for (Tick tick : ticks) {
			DateTime extension = tick.getExtensions().get(crsid);
			if (extension != null) {
				tick.setDeadline(extension);
			}
		}

		Collections.sort(ticks);
		
		for (Tick tick: ticks) {
			Fork fork = db.getFork(Fork.generateForkId(crsid, tick.getTickId()));
			if (fork != null) {
				forks.add(fork);
			}
		}

		return Response.ok(new ToDoBean(ticks, forks)).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response newTick(HttpServletRequest request, TickBean tickBean) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Check permissions */
		if (!permissions.tickBeanGroupPermissions(crsid, tickBean)) {
			log.warn("User " + crsid + " tried to create a tick in groups "
					+ tickBean.getGroups().toString()
					+ " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Create the tick from the tick bean */
		Tick tick = new Tick(tickBean);
		tick.setAuthor(crsid);
		tick.initTickId();

		/* Has the tick been created previously? */
		Tick prev = db.getTick(tick.getTickId());
		if (prev != null) {
			log.error("User " + crsid + " failed creating tick with id "
					+ tick.getTickId());
			return Response.status(Status.CONFLICT)
					.entity(Strings.EXISTS).build();
		}

		/*
		 * Create the stub repository
		 */
		String repo;
		try {
			repo = gitServiceProxy.addRepository(config.getConfig()
					.getSecurityToken(), new RepoUserRequestBean(crsid + "/"
					+ tickBean.getName(), crsid));

		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName().equals(IOException.class.getName())) {

				log.error("User " + crsid
						+ " failed creating stub repository for "
						+ tick.getTickId() + "\nCause: " + s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

			if (s.getClassName().equals(
					DuplicateRepoNameException.class.getName())) {
				log.error("User " + crsid
						+ " failed creating stub repository for "
						+ tick.getTickId() + "\nCause: " + s.toString());
				return Response.status(Status.NOT_FOUND)
						.entity(Strings.IDEMPOTENTRETRY).build();

			}

			if (s.getClassName().equals(
					IllegalCharacterException.class.getName())) {

				log.error("User " + crsid
						+ " failed creating stub repository for "
						+ tick.getTickId() + "\nCause: " + s.toString());
				return Response.status(Status.BAD_REQUEST)
						.entity(s.getMessage()).build();

			} else {
				log.error("User " + crsid
						+ " failed creating stub repository for "
						+ tick.getTickId() + "\nCause: " + s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

		} catch (IOException | DuplicateRepoNameException
				| IllegalCharacterException e) {
			log.error(
					"User " + crsid + " failed to create stub repository for "
							+ tick.getTickId(), e.getCause());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
			// Due to exception chaining this shouldn't happen
		}

		tick.setStubRepo(repo);

		// Execution will only reach this point if there are no git errors else
		// IOException is thrown

		/* Save style checks with the test service */
		testServiceProxy.createNewTest(config.getConfig().getSecurityToken(),
				tick.getTickId(), tickBean.getCheckstyleOpts(),
				tickBean.getContainerId(), tickBean.getTestId());

		/* Register the tick with the required groups */
		for (String groupId : tick.getGroups()) {
			Group g = db.getGroup(groupId);
			g.addTick(tick.getTickId());
			db.saveGroup(g);
		}

		/* Save and return the tick */
		db.saveTick(tick);

		return Response.status(Status.CREATED).entity(tick).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response updateTick(HttpServletRequest request, String tickId,
			TickBean tickBean) throws IOException, DuplicateRepoNameException {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Check permissions */
		if (!permissions.tickBeanGroupPermissions(crsid, tickBean)) {
			log.warn("User " + crsid + " tried to update tick " + tickId
					+ " in groups " + tickBean.getGroups().toString()
					+ " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Get the tick to be updated */
		Tick prevTick = db.getTick(tickId);

		/* Does it exists? */
		if (prevTick != null) {

			/* Check for author permissions */
			if (!crsid.equals(prevTick.getAuthor())) {
				log.warn("User " + crsid + " tried to update tick " + tickId
						+ " but was denied permission");
				return Response.status(Status.FORBIDDEN)
						.entity(Strings.INVALIDROLE).build();
			}

			/* Call the test service to update the checkstyles */
			testServiceProxy.createNewTest(config.getConfig()
					.getSecurityToken(), tickId, tickBean.getCheckstyleOpts(),
					tickBean.getContainerId(), tickBean.getTestId());

			/* Timestamp the tick object */
			prevTick.setEdited(DateTime.now());

			/* remove it from it's original groups */
			for (String groupId : prevTick.getGroups()) {
				Group g = db.getGroup(groupId);
				g.removeTick(tickId);
				db.saveGroup(g);
			}

			/* Add it to the new desired groups */
			for (String groupId : tickBean.getGroups()) {
				Group g = db.getGroup(groupId);
				g.addTick(tickId);
				db.saveGroup(g);
			}

			prevTick.setGroups(tickBean.getGroups());

			/* Update the deadline, external resource, save and return */
			prevTick.setDeadline(tickBean.getDeadline());
			prevTick.setExternalReference(tickBean.getExternalReference());
			db.saveTick(prevTick);
			return Response.status(Status.CREATED).entity(prevTick).build();
		} else {
			/*
			 * There was no original tick object, so create a new one from the
			 * same bean
			 */
			log.warn("User "
					+ crsid
					+ " requested tick "
					+ tickId
					+ " for updating, but it couldn't be found, creating a new group instead");
			return newTick(request, tickBean);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response addTick(HttpServletRequest request, String tickId,
			String groupId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the group object and return if it doesn't exist */
		Group group = db.getGroup(groupId);

		if (group == null) {
			log.error("User " + crsid + " requested group " + groupId
					+ " to add a tick, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Get the tick object and return if it doesn't exist */
		Tick tick = db.getTick(tickId);

		if (tick == null) {
			log.error("User " + crsid + " requested tick " + tickId
					+ " to add to a group, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (!(permissions.hasRole(crsid, groupId, Role.AUTHOR) || (permissions
				.tickCreator(crsid, tick)))) {
			log.warn("User " + crsid + " tried to add tick " + tickId
					+ " to group " + groupId + " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		if (tick.getGroups().contains(groupId)) {
			log.warn("User " + crsid + " tried to add tick " + tickId
					+ " to group " + groupId
					+ " but it was already past of the group");
			return Response.status(Status.BAD_REQUEST)
					.entity(Strings.TICKISINGROUP).build();
		}

		/* Add the references and save */
		group.addTick(tickId);
		tick.addGroup(groupId);
		db.saveGroup(group);
		db.saveTick(tick);

		return Response.status(Status.CREATED).entity(group).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response setExtension(HttpServletRequest request, String tickId,
			ExtensionBean extensionBean) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the tick object and return if it doesn't exist */
		Tick tick = db.getTick(tickId);

		if (tick == null) {
			log.error("User " + crsid + " requested tick " + tickId
					+ " to add extensions, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (!permissions.tickCreator(crsid, tick)) {
			log.warn("User " + crsid + " tried to add an extension to tick "
					+ tickId + " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Update the extensions, save and return */
		for (String user : extensionBean.getCrsids()) {
			if (db.getUser(user) == null) {
				continue;
			}
			tick.addExtension(user, extensionBean.getDeadline());
		}
		db.saveTick(tick);

		List<ExtensionReturnBean> extensions = new ArrayList<>();
		for (Entry<String, DateTime> entry : tick.getExtensions().entrySet()) {
			extensions.add(new ExtensionReturnBean(db.getUser(entry.getKey()),
					entry.getValue()));
		}

		return Response.status(Status.CREATED).entity(extensions).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response removeExtension(HttpServletRequest request, String tickId,
			ExtensionBean extensionBean) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the tick object and return if it doesn't exist */
		Tick tick = db.getTick(tickId);

		if (tick == null) {
			log.error("User " + crsid + " requested tick " + tickId
					+ " to add extensions, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (!permissions.tickCreator(crsid, tick)) {
			log.warn("User " + crsid + " tried to add an extension to tick "
					+ tickId + " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Update the extensions, save and return */
		for (String user : extensionBean.getCrsids()) {
			tick.removeExtension(user);
		}
		db.saveTick(tick);

		List<ExtensionReturnBean> extensions = new ArrayList<>();
		for (Entry<String, DateTime> entry : tick.getExtensions().entrySet()) {
			extensions.add(new ExtensionReturnBean(db.getUser(entry.getKey()),
					entry.getValue()));
		}

		return Response.status(Status.CREATED).entity(extensions).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getExtensions(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the tick object and return if it doesn't exist */
		Tick tick = db.getTick(tickId);

		if (tick == null) {
			log.error("User " + crsid + " requested extensions for tick"
					+ tickId + " to update deadline, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (!permissions.tickCreator(crsid, tick)) {
			log.warn("User " + crsid + " tried get the extensions of tick "
					+ tickId + " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		List<ExtensionReturnBean> extensions = new ArrayList<>();
		for (Entry<String, DateTime> entry : tick.getExtensions().entrySet()) {
			extensions.add(new ExtensionReturnBean(db.getUser(entry.getKey()),
					entry.getValue()));
		}
		return Response.ok(extensions).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getAllFiles(HttpServletRequest request, String tickId,
			String commitId) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the tick object and return if it doesn't exist */
		Tick tick = db.getTick(tickId);

		if (tick == null) {
			log.error("User " + myCrsid + " requested tick " + tickId
					+ " to get files, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (!permissions.tickCreator(myCrsid, tick)) {
			log.warn("User " + myCrsid + " tried to get the files of tick "
					+ tickId + " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		/* Call the git service to get the files */
		List<FileBean> files;
		try {
			files = gitServiceProxy.getAllFiles(config.getConfig()
					.getSecurityToken(), Tick.replaceDelimeter(tickId),
					commitId);
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName().equals(IOException.class.getName())) {

				log.error("User " + myCrsid
						+ " failed to get repository files for " + tickId
						+ "\nCause: " + s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

			if (s.getClassName().equals(
					RepositoryNotFoundException.class.getName())) {
				log.error("User " + myCrsid
						+ " failed to get repository files for " + tickId
						+ "\nCause: " + s.toString());
				return Response.status(Status.NOT_FOUND)
						.entity(Strings.MISSING).build();

			} else {
				log.error("User " + myCrsid
						+ " failed to get repository files for " + tickId
						+ "\nCause: " + s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

		} catch (IOException | RepositoryNotFoundException e) {
			log.error("User " + myCrsid
					+ " failed to get repository files for " + tickId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		}

		return Response.ok(files).build();
	}

	@Override
	public Response getTestFiles(HttpServletRequest request) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		TickSettings settings;
		try {
			settings = testServiceProxy.getTestFiles(config.getConfig()
					.getSecurityToken());
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName().equals(IOException.class.getName())) {

				log.error("User " + myCrsid
						+ " failed to get default test settings" + "\nCause: "
						+ s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

			if (s.getClassName().equals(TestNotFoundException.class.getName())) {
				log.error("User " + myCrsid
						+ " failed to get default test settings" + "\nCause: "
						+ s.toString());
				return Response.status(Status.NOT_FOUND)
						.entity(Strings.MISSING).build();

			} else {
				log.error("User " + myCrsid
						+ " failed to get default test settings" + "\nCause: "
						+ s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}
		} catch (IOException | TestNotFoundException e) {
			log.error("User " + myCrsid
					+ " failed to get default test settings", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		}

		return Response.ok(settings).build();
	}

	@Override
	public Response getTestFiles(HttpServletRequest request, String tickId) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		/* Get the tick object and return if it doesn't exist */
		Tick tick = db.getTick(tickId);

		if (tick == null) {
			log.error("User " + myCrsid + " requested tick " + tickId
					+ " to get test settings, but it couldn't be found");
			return Response.status(Status.NOT_FOUND).entity(Strings.MISSING)
					.build();
		}

		/* Check permissions */
		if (!permissions.tickCreator(myCrsid, tick)) {
			log.warn("User " + myCrsid
					+ " tried to get the test settings of tick " + tickId
					+ " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}

		TickSettings settings;
		try {
			settings = testServiceProxy.getTestFiles(config.getConfig()
					.getSecurityToken(), tickId);
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName()
					.equals(TestIDNotFoundException.class.getName())) {

				log.error("User " + myCrsid
						+ " failed to get test settings for tick " + tickId
						+ "\nCause: " + s.toString());
				return Response.status(Status.NOT_FOUND)
						.entity(Strings.MISSING).build();
			}

			if (s.getClassName().equals(TestNotFoundException.class.getName())) {
				log.error("User " + myCrsid
						+ " failed to get test settings for tick " + tickId
						+ "\nCause: " + s.toString());
				return Response.status(Status.NOT_FOUND)
						.entity(Strings.MISSING).build();

			} else {
				log.error("User " + myCrsid
						+ " failed to get test settings for tick " + tickId
						+ "\nCause: " + s.toString());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}
		} catch (TestIDNotFoundException | TestNotFoundException e) {
			log.error("User " + myCrsid
					+ " failed to get default test settings", e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		}

		return Response.ok(settings).build();
	}
}