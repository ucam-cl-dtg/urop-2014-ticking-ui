package uk.ac.cam.cl.ticking.ui.api;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.joda.time.DateTime;

import uk.ac.cam.cl.git.api.DuplicateRepoNameException;
import uk.ac.cam.cl.git.api.ForkRequestBean;
import uk.ac.cam.cl.git.api.RepoUserRequestBean;
import uk.ac.cam.cl.git.interfaces.WebInterface;
import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.ITickApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.Configuration;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class TickApiFacade implements ITickApiFacade {

	private IDataManager db;
	private ConfigurationLoader<Configuration> config;

	/**
	 * @param db
	 * @param config
	 */
	@Inject
	public TickApiFacade(IDataManager db,
			ConfigurationLoader<Configuration> config) {
		this.db = db;
		this.config = config;
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
			if (extension !=null) {
				tick.setDeadline(extension);
			}
		}
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
	public Response newTick(HttpServletRequest request, Tick tick)
			 {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(config.getConfig()
				.getGitApiLocation());

		if (!validatePermissions(tick.getGroups(), crsid)) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		WebInterface proxy = target.proxy(WebInterface.class);
		String repo;
		//TODO not this \/
		try {
			repo = proxy.addRepository(new RepoUserRequestBean(crsid + "/"
						+ tick.getName(), crsid));
		} catch (IOException | DuplicateRepoNameException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
		}
		String correctnessRepo;
		try {
			correctnessRepo = proxy.addRepository(new RepoUserRequestBean(
					crsid + "/" + tick.getName() + "/correctness", crsid));
		} catch (IOException | DuplicateRepoNameException e) {
			return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e).build();
		}

		// Execution will only reach this point if there are no git errors else
		// IOException is thrown
		tick.setAuthor(crsid);
		tick.setStubRepo(repo);
		tick.setCorrectnessRepo(correctnessRepo);
		tick.initTickId();

		for (String groupId : tick.getGroups()) {
			Group g = db.getGroup(groupId);
			g.addTick(tick.getTickId());
			db.saveGroup(g);
		}

		try {
			db.insertTick(tick);
		} catch (DuplicateDataEntryException de) {
			return Response.status(Status.CONFLICT).build();
		}

		return Response.status(Status.CREATED).entity(tick).build();
	}

	@Override
	public Response updateTick(HttpServletRequest request, Tick tick)
			throws IOException, DuplicateRepoNameException {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		if (!validatePermissions(tick.getGroups(), crsid)) {
			return Response.status(Status.UNAUTHORIZED)
					.entity(Strings.INVALIDROLE).build();
		}

		Tick prevTick = db.getTick(tick.getTickId());

		if (prevTick != null) {

			if (!crsid.equals(prevTick.getAuthor())) {
				return Response.status(Status.UNAUTHORIZED)
						.entity(Strings.INVALIDROLE).build();
			}

			prevTick.setEdited(DateTime.now());
			for (String groupId : prevTick.getGroups()) {
				Group g = db.getGroup(groupId);
				g.removeTick(tick.getTickId());
				db.saveGroup(g);
			}
			for (String groupId : tick.getGroups()) {
				Group g = db.getGroup(groupId);
				g.addTick(tick.getTickId());
				db.saveGroup(g);
			}
			prevTick.setGroups(tick.getGroups());
			db.saveTick(prevTick);
			return Response.status(Status.CREATED).entity(prevTick).build();
		} else {
			return newTick(request, tick);
		}
	}

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
	public Response forkTick(HttpServletRequest request, String tickId)
			throws IOException {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");

		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(config.getConfig()
				.getGitApiLocation());
		WebInterface proxy = target.proxy(WebInterface.class);
		String output;
		String repoName = Tick.replaceDelimeter(tickId);
		try {
			output = proxy.forkRepository(new ForkRequestBean(null, crsid,
					repoName, null));
		} catch (DuplicateRepoNameException e) {
			output = e.getMessage() + Strings.FORKED;
		}

		// Execution will only reach this point if there are no git errors else
		// IOException is thrown
		return Response.status(Status.CREATED).entity(output).build();
	}

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