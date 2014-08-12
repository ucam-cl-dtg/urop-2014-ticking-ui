package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

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
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.TickBean;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class TickApiFacade implements ITickApiFacade {

	private static final Logger log = LoggerFactory
			.getLogger(TickApiFacade.class.getName());

	private IDataManager db;
	// not currently used but could quite possibly be needed in the future, will
	// remove if not
	@SuppressWarnings("unused")
	private ConfigurationLoader<Configuration> config;

	private ITestService testServiceProxy;
	private WebInterface gitServiceProxy;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public TickApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config,
			ITestService testServiceProxy, WebInterface gitServiceProxy) {
		this.db = db;
		this.config = config;
		this.testServiceProxy = testServiceProxy;
		this.gitServiceProxy = gitServiceProxy;
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
		Tick tick = db.getTick(tickId);
		if (!crsid.equals(tick.getAuthor())) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		try {
			gitServiceProxy.deleteRepository(Tick.replaceDelimeter(tickId)); // throws
																				// the
			// exceptions
			db.removeTick(tickId);
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);
			log.error("Tried deleting repository for " + tickId, s.getCause());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		} catch (IOException e) {
			log.error("Tried deleting repository for " + tickId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		} catch (RepositoryNotFoundException e) {
			log.error("Tried deleting repository for " + tickId, e);
			db.removeTick(tickId);
			// Not finding the repo still indicates we want to delete the tick
			return Response.status(Status.NOT_FOUND).entity(e).build();
		}

		return Response.ok().build();
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

		if (!validatePermissions(tickBean.getGroups(), crsid)) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		Tick tick = new Tick(tickBean);
		tick.setAuthor(crsid);
		tick.initTickId();

		Tick failed = db.getTick(tick.getTickId());
		if (failed != null && failed.getStubRepo() != null
				&& failed.getCorrectnessRepo() != null) {
			return Response.status(Status.INTERNAL_SERVER_ERROR)
					.entity(Strings.EXISTS).build();
		}

		if (failed == null || failed.getStubRepo() == null) {
			String repo;
			try {
				repo = gitServiceProxy.addRepository(new RepoUserRequestBean(
						crsid + "/" + tickBean.getName(), crsid));
			} catch (InternalServerErrorException e) {
				RemoteFailureHandler h = new RemoteFailureHandler();
				SerializableException s = h.readException(e);
				log.error(s.getMessage(), e.getCause());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();

			} catch (IOException | DuplicateRepoNameException e) {
				log.error(
						"Tried to create stub repository for "
								+ tick.getTickId(), e.getCause());
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
						.build();
				// Due to exception chaining this shouldn't happen
			}
			tick.setStubRepo(repo);
		} else {
			tick.setStubRepo(failed.getStubRepo());
		}

		if (failed == null || failed.getCorrectnessRepo() == null) {
			String correctnessRepo;
			try {
				correctnessRepo = gitServiceProxy
						.addRepository(new RepoUserRequestBean(crsid + "/"
								+ tickBean.getName() + "/correctness", crsid));
			} catch (InternalServerErrorException e) {
				RemoteFailureHandler h = new RemoteFailureHandler();
				SerializableException s = h.readException(e);
				log.error(s.getMessage(), e.getCause());
				return Response.status(Status.INTERNAL_SERVER_ERROR)
						.entity(Strings.IDEMPOTENTRETRY).build();

			} catch (IOException | DuplicateRepoNameException e) {
				log.error(
						"Tried to create correctness repository for "
								+ tick.getTickId(), e.getCause());
				return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
						.build();
				// Due to exception chaining this shouldn't happen
			}
			tick.setCorrectnessRepo(correctnessRepo);
		} else {
			tick.setCorrectnessRepo(failed.getCorrectnessRepo());
		}

		// Execution will only reach this point if there are no git errors else
		// IOException is thrown

		testServiceProxy.createNewTest(tick.getTickId(),
				tickBean.getCheckstyleOpts());

		for (String groupId : tick.getGroups()) {
			Group g = db.getGroup(groupId);
			g.addTick(tick.getTickId());
			db.saveGroup(g);
		}

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

		if (!validatePermissions(tickBean.getGroups(), crsid)) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		Tick prevTick = db.getTick(tickId);

		if (prevTick != null) {

			if (!crsid.equals(prevTick.getAuthor())) {
				return Response.status(Status.UNAUTHORIZED)
						.entity(Strings.INVALIDROLE).build();
			}

			testServiceProxy
					.createNewTest(tickId, tickBean.getCheckstyleOpts());

			prevTick.setEdited(DateTime.now());
			for (String groupId : prevTick.getGroups()) {
				Group g = db.getGroup(groupId);
				g.removeTick(tickId);
				db.saveGroup(g);
			}
			for (String groupId : tickBean.getGroups()) {
				Group g = db.getGroup(groupId);
				g.addTick(tickId);
				db.saveGroup(g);
			}
			prevTick.setGroups(tickBean.getGroups());
			prevTick.setDeadline(tickBean.getDeadline());
			db.saveTick(prevTick);
			return Response.status(Status.CREATED).entity(prevTick).build();
		} else {
			// TODO should this behave like so?
			return newTick(request, tickBean);
		}
	}

	/**
	 * 
	 * @param groupIds
	 * @param crsid
	 * @return whether the user has author permissions for all of the supplied
	 *         groups
	 */
	private boolean validatePermissions(List<String> groupIds, String crsid) {
		for (String groupId : groupIds) {
			List<Role> roles = db.getRoles(groupId, crsid);
			if (!roles.contains(Role.AUTHOR)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response addTick(HttpServletRequest request, String tickId,
			String groupId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Role> roles = db.getRoles(groupId, crsid);
		Tick t = db.getTick(tickId);
		if (!roles.contains(Role.AUTHOR) || !(t.getAuthor().equals(crsid))) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}
		Group g = db.getGroup(groupId);
		g.addTick(tickId);
		t.addGroup(groupId);
		db.saveGroup(g);
		db.saveTick(t);
		return Response.status(Status.CREATED).entity(g).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response setDeadline(HttpServletRequest request, String tickId,
			DateTime date) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		Tick tick = db.getTick(tickId);
		if (!tick.getAuthor().equals(crsid)) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}
		tick.setDeadline(date);
		db.saveTick(tick);
		return Response.status(Status.CREATED).entity(tick).build();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Response getAllFiles(HttpServletRequest request, String tickId,
			String commitId) {
		String myCrsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		if (!myCrsid.equals(db.getTick(tickId).getAuthor())) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}
		List<FileBean> files;
		try {
			files = gitServiceProxy.getAllFiles(Tick.replaceDelimeter(tickId),
					commitId);
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);
			log.error("Tried to get repository files for " + tickId,
					s.getCause());
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		} catch (IOException | RepositoryNotFoundException e) {
			log.error("Tried to get repository files for " + tickId, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		}
		return Response.ok(files).build();
	}
}