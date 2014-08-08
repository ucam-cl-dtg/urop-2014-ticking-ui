package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient4Engine;
import org.joda.time.DateTime;

import publicinterfaces.ITestService;
import uk.ac.cam.cl.dtg.teaching.exceptions.RemoteFailureHandler;
import uk.ac.cam.cl.dtg.teaching.exceptions.SerializableException;
import uk.ac.cam.cl.git.api.DuplicateRepoNameException;
import uk.ac.cam.cl.git.api.ForkRequestBean;
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
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class TickApiFacade implements ITickApiFacade {

	Logger log = Logger.getLogger(ConfigurationLoader.class.getName());
	private IDataManager db;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade#getTick(
	 * java.lang.String)
	 */
	@Override
	public Response getTick(String tickId) {
		Tick tick = db.getTick(tickId);
		return Response.ok().entity(tick).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade#deleteTick
	 * (javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public Response deleteTick(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		// TODO remove checkstyles?
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
		} catch (IOException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		} catch (RepositoryNotFoundException e) {
			db.removeTick(tickId);
			// Not finding the repo still indicates we want to delete the tick
			return Response.status(Status.NOT_FOUND).entity(e).build();
		}

		return Response.ok().build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade#getTicks
	 * (java.lang.String)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade#newTick(
	 * javax.servlet.http.HttpServletRequest, java.lang.String,
	 * uk.ac.cam.cl.ticking.ui.ticks.Tick)
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

		testServiceProxy.createNewTest(tick.getTickId(),
				tickBean.getCheckstyleOpts());

		String repo;
		/*
		 * Here we try to create two repositories. If the first create fails, we
		 * abort. If the second fails, we abort, however the first repository
		 * still exists. We throw this back to the user as a failure and they
		 * must try again. On this retry we will find that the first repository
		 * already exists as expected, hence we ignore the duplicate repository
		 * name exception thrown
		 */
		try {
			repo = gitServiceProxy.addRepository(new RepoUserRequestBean(crsid
					+ "/" + tickBean.getName(), crsid));
		} catch (IOException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		} catch (InternalServerErrorException e) {
			RemoteFailureHandler h = new RemoteFailureHandler();
			SerializableException s = h.readException(e);
			log.warn(s.getClass());
			repo = s.getMessage();

		} catch (DuplicateRepoNameException e) {
			try {
				repo = gitServiceProxy.getRepoURI(crsid + "/"
						+ tickBean.getName());
			} catch (RepositoryNotFoundException e1) {
				throw new RuntimeException("Schrodinger's repository");
				// The repo simultaneously does and doesn't exist
			}
			log.info(
					"Found a clashing repository name, assuming this is due to a previous error and continuing",
					e);
		}
		
		String correctnessRepo;
		try {
			correctnessRepo = gitServiceProxy
					.addRepository(new RepoUserRequestBean(crsid + "/"
							+ tickBean.getName() + "/correctness", crsid));
		} catch (IOException | DuplicateRepoNameException e) {
			/*
			 * Here if we encounter a duplicate repository name then the second
			 * create did not fail, and we should not be here again with the
			 * same repo name meaning, if we are, something has gone wrong.
			 */
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e)
					.build();
		}

		// Execution will only reach this point if there are no git errors else
		// IOException is thrown
		tick.setStubRepo(repo);
		tick.setCorrectnessRepo(correctnessRepo);

		for (String groupId : tick.getGroups()) {
			Group g = db.getGroup(groupId);
			g.addTick(tick.getTickId());
			db.saveGroup(g);
		}

		
		try {
			db.insertTick(tick);
		} catch (DuplicateDataEntryException de) {
			return Response.status(Status.CONFLICT).entity(Strings.EXISTS)
					.build();
		}

		return Response.status(Status.CREATED).entity(tick).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade#updateTick
	 * (javax.servlet.http.HttpServletRequest, java.lang.String,
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.TickBean)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade#addTick(
	 * javax.servlet.http.HttpServletRequest, java.lang.String,
	 * java.lang.String)
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade#forkTick
	 * (javax.servlet.http.HttpServletRequest, java.lang.String)
	 */
	@Override
	public Response forkTick(HttpServletRequest request, String tickId) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		Fork fork = db.getFork(crsid + "," + tickId);
		if (fork != null) {
			return Response.ok(fork).build();
		}

		String repo = null;
		String repoName = Tick.replaceDelimeter(tickId);
		try {
			repo = gitServiceProxy.forkRepository(new ForkRequestBean(null,
					crsid, repoName, null));
		} catch (DuplicateRepoNameException e) {
			repo = e.getMessage();
		} catch (IOException e) {
			// The repo that was being forked was empty, however, it has still
			// been forked thus continue

		}

		try {
			fork = new Fork(crsid, tickId, repo);
			db.insertFork(fork);
		} catch (DuplicateDataEntryException e) {
			throw new RuntimeException("Schrodinger's repository");
			// The fork simultaneously does and doesn't exist
		}

		// Execution will only reach this point if there are no git errors else
		// IOException is thrown
		return Response.status(Status.CREATED).entity(fork).build();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade#setDeadline
	 * (javax.servlet.http.HttpServletRequest, java.lang.String,
	 * org.joda.time.DateTime)
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
}