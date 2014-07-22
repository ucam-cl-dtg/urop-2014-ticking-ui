package uk.ac.cam.cl.ticking.ui.api;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IUserApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationFile;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;

import com.google.inject.Inject;

public class UserApiFacade implements IUserApiFacade {

	private IDataManager db;
	private ConfigurationFile config;

	@Inject
	public UserApiFacade(IDataManager db, ConfigurationFile config) {
		this.db = db;
		this.config = config;
	}
	
	@Override
	public Response getUser(HttpServletRequest request) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		User user = db.getUser(crsid);
		return Response.ok(user).build();
	}

	@Override
	public Response getGroups(HttpServletRequest request) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Group> groups = db.getGroups(crsid);
		Collections.sort(groups);
		return Response.ok(groups).build();
	}

	@Override
	public Response getGroupRoles(HttpServletRequest request, String gid) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Role> roles = db.getRoles(gid, crsid);
		return Response.ok(roles).build();
	}
}
