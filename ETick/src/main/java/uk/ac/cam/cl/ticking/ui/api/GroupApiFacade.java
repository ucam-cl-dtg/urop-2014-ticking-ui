package uk.ac.cam.cl.ticking.ui.api;

import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.actors.User;
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
	public Response getUsers(String gid) {
		List<User> users = db.getUsers(gid);
		Collections.sort(users);
		return Response.ok(users).build();
	}

}