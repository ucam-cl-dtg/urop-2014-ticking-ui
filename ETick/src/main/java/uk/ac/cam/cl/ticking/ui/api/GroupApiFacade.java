package uk.ac.cam.cl.ticking.ui.api;

import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationFile;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;

import com.google.inject.Inject;

public class GroupApiFacade implements IGroupApiFacade {

	private IDataManager db;
	private ConfigurationFile config;

	@Inject
	public GroupApiFacade(IDataManager db, ConfigurationFile config) {
		this.db = db;
		this.config = config;
	}

	@Override
	public Response getUserGroups(@Context HttpServletRequest request) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Group> groups = db.getGroups(db.getUser(crsid));
		Collections.sort(groups);
		return Response.ok(groups).build();
	}
}
