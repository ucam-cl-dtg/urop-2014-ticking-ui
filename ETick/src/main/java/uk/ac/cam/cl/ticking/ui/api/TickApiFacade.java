package uk.ac.cam.cl.ticking.ui.api;

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
import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.dtg.teaching.exceptions.SerializableException;
import uk.ac.cam.cl.git.api.DuplicateRepoNameException;
import uk.ac.cam.cl.git.api.FileBean;
import uk.ac.cam.cl.git.api.RepoUserRequestBean;
import uk.ac.cam.cl.git.api.RepositoryNotFoundException;
import uk.ac.cam.cl.git.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.ExtensionBean;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.ExtensionReturnBean;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.TickBean;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
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
			ITestService testServiceProxy, WebInterface gitServiceProxy, PermissionsManager permissions) {
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
			gitServiceProxy.deleteRepository(config.getConfig().getSecurityToken(), Tick.replaceDelimeter(tickId));
			gitServiceProxy.deleteRepository(config.getConfig().getSecurityToken(), Tick.replaceDelimeter(tickId)+"/correctness");
			db.removeTick(tickId);
			/*Remove dangling group references*/
			for (String groupId : tick.getGroups()) {
				Group group = db.getGroup(groupId);
				group.removeTick(tickId);
				db.saveGroup(group);
			}

		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName().equals(IOException.class.getName())) {
				log.error("User " + crsid + " tried deleting repository for "
						+ tickId, s.getCause(), s.getStackTrace());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

			if (s.getClassName().equals(
					RepositoryNotFoundException.class.getName())) {
				/* We still want to remove the tick */
				db.removeTick(tickId);
				/*Remove dangling group references*/
				for (String groupId : tick.getGroups()) {
					Group group = db.getGroup(groupId);
					group.removeTick(tickId);
					db.saveGroup(group);
				}

				log.warn("User " + crsid + " tried deleting repository for "
						+ tickId, s.getCause(), s.getStackTrace());

			} else {
				log.error("User " + crsid + " tried deleting repository for "
						+ tickId, s.getCause(), s.getStackTrace());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

		} catch (IOException | RepositoryNotFoundException e) {
			log.error("User " + crsid + " tried deleting repository for "
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

		List<Tick> ticks = db.getGroupTicks(groupId);
		for (Tick tick : ticks) {
			DateTime extension = tick.getExtensions().get(crsid);
			if (extension != null) {
				tick.setDeadline(extension);
			}
		}

		Collections.sort(ticks);

		return Response.ok().entity(ticks).build();
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

		/* Has the tick been unsuccessfully created previously? */
		Tick failed = db.getTick(tick.getTickId());
		if (failed != null && failed.getStubRepo() != null
				&& failed.getCorrectnessRepo() != null) {
			log.error("User " + crsid + " tried creating tick with id "
					+ tick.getTickId());
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(Strings.EXISTS).build();
		}

		/*
		 * If we haven't failed previously or we did and the failure was
		 * creating the stub repo
		 */
		if (failed == null || failed.getStubRepo() == null) {
			String repo;
			try {
				repo = gitServiceProxy.addRepository(config.getConfig().getSecurityToken(), new RepoUserRequestBean(
						crsid + "/" + tickBean.getName(), crsid));

			} catch (InternalServerErrorException e) {
				RemoteFailureHandler h = new RemoteFailureHandler();
				SerializableException s = h.readException(e);

				if (s.getClassName().equals(IOException.class.getName())) {

					log.error(
							"User " + crsid
									+ " tried creating stub repository for "
									+ tick.getTickId(), s.getCause(),
							s.getStackTrace());
					return Response.status(Status.INTERNAL_SERVER_ERROR)
							.entity(Strings.IDEMPOTENTRETRY).build();
				}

				if (s.getClassName().equals(
						DuplicateRepoNameException.class.getName())) {
					log.error(
							"User " + crsid
									+ " tried creating stub repository for "
									+ tick.getTickId(), s.getCause(),
							s.getStackTrace());
					return Response.status(Status.NOT_FOUND)
							.entity(Strings.IDEMPOTENTRETRY).build();

				} else {
					log.error(
							"User " + crsid
									+ " tried creating stub repository for "
									+ tick.getTickId(), s.getCause(),
							s.getStackTrace());
					return Response.status(Status.INTERNAL_SERVER_ERROR)
							.entity(Strings.IDEMPOTENTRETRY).build();
				}

			} catch (IOException | DuplicateRepoNameException e) {
				log.error(
						"User " + crsid
								+ " tried to create stub repository for "
								+ tick.getTickId(), e.getCause());
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
						.build();
				// Due to exception chaining this shouldn't happen
			}

			tick.setStubRepo(repo);
		} else {
			/* Else get the stub repo from the failure */
			tick.setStubRepo(failed.getStubRepo());
		}

		/*
		 * If we haven't failed previously or we did and the failure was
		 * creating the correctness repo
		 */
		if (failed == null || failed.getCorrectnessRepo() == null) {
			String correctnessRepo;
			try {
				correctnessRepo = gitServiceProxy
						.addRepository(config.getConfig().getSecurityToken(), new RepoUserRequestBean(crsid + "/"
								+ tickBean.getName() + "/correctness", crsid));
			} catch (InternalServerErrorException e) {
				RemoteFailureHandler h = new RemoteFailureHandler();
				SerializableException s = h.readException(e);

				if (s.getClassName().equals(IOException.class.getName())) {

					log.error("User " + crsid
							+ " tied creating correctness repository for "
							+ tick.getTickId(), s.getCause(), s.getStackTrace());
					return Response.status(Status.INTERNAL_SERVER_ERROR)
							.entity(Strings.IDEMPOTENTRETRY).build();
				}

				if (s.getClassName().equals(
						DuplicateRepoNameException.class.getName())) {
					log.error("User " + crsid
							+ " tried creating correctness repository for "
							+ tick.getTickId(), s.getCause(), s.getStackTrace());
					return Response.status(Status.NOT_FOUND)
							.entity(Strings.IDEMPOTENTRETRY).build();

				} else {
					log.error("User " + crsid
							+ " tried creating correctness repository for "
							+ tick.getTickId(), s.getCause(), s.getStackTrace());
					return Response.status(Status.INTERNAL_SERVER_ERROR)
							.entity(Strings.IDEMPOTENTRETRY).build();
				}

			} catch (IOException | DuplicateRepoNameException e) {
				log.error(
						"User "
								+ crsid
								+ " tried to create correctness repository for "
								+ tick.getTickId(), e.getCause());
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
						.build();
				// Due to exception chaining this shouldn't happen
			}
			tick.setCorrectnessRepo(correctnessRepo);

		} else {
			/* Else get the correctness repo from the failure */
			tick.setCorrectnessRepo(failed.getCorrectnessRepo());
		}

		// Execution will only reach this point if there are no git errors else
		// IOException is thrown

		/* Save style checks with the test service */
		testServiceProxy.createNewTest(tick.getTickId(),
				tickBean.getCheckstyleOpts());

		/* Register the tick with the required groups */
		for (String groupId : tick.getGroups()) {
			Group g = db.getGroup(groupId);
			g.addTick(tick.getTickId());
			db.saveGroup(g);
		}

		/* Ssave and return the tick */
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
			testServiceProxy
					.createNewTest(tickId, tickBean.getCheckstyleOpts());

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

			/* Update the deadline, save and return */
			prevTick.setDeadline(tickBean.getDeadline());
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
		if (!(permissions.hasRole(crsid, groupId, Role.AUTHOR)||(permissions.tickCreator(crsid, tick)))) {
			log.warn("User " + crsid + " tried to add tick " + tickId
					+ " to group " + groupId + " but was denied permission");
			return Response.status(Status.FORBIDDEN)
					.entity(Strings.INVALIDROLE).build();
		}
		
		if (tick.getGroups().contains(groupId)) {
			log.warn("User " + crsid + " tried to add tick " + tickId
					+ " to group " + groupId + " but it was already past of the group");
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
	public Response setExtension(HttpServletRequest request, String tickId, ExtensionBean extensionBean) {
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
			tick.addExtension(user, extensionBean.getDeadline());
		}
		db.saveTick(tick);
		
		List<ExtensionReturnBean> extensions = new ArrayList<>();
		for (Entry<String, DateTime> entry : tick.getExtensions().entrySet()) {
			extensions.add(new ExtensionReturnBean(db.getUser(entry.getKey()), entry.getValue()));
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
			log.error("User " + crsid + " requested extensions for tick" + tickId
					+ " to update deadline, but it couldn't be found");
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
			extensions.add(new ExtensionReturnBean(db.getUser(entry.getKey()), entry.getValue()));
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
			files = gitServiceProxy.getAllFiles(config.getConfig().getSecurityToken(), Tick.replaceDelimeter(tickId),
					commitId);
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);

			if (s.getClassName().equals(IOException.class.getName())) {

				log.error("User " + myCrsid
						+ " tried to get repository files for " + tickId,
						s.getCause(), s.getStackTrace());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

			if (s.getClassName().equals(
					RepositoryNotFoundException.class.getName())) {
				log.error("User " + myCrsid
						+ " tried to get repository files for " + tickId,
						s.getCause(), s.getStackTrace());
				return Response.status(Status.NOT_FOUND)
						.entity(Strings.MISSING).build();

			} else {
				log.error("User " + myCrsid
						+ " tried to get repository files for " + tickId,
						s.getCause(), s.getStackTrace());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();
			}

		} catch (IOException | RepositoryNotFoundException e) {
			log.error("User " + myCrsid + " tried to get repository files for "
					+ tickId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		}

		return Response.ok(files).build();
	}
}