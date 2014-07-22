package uk.ac.cam.cl.ticking.ui.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import com.google.inject.Inject;

import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupingApiFacade;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationFile;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.util.Strings;

public class GroupingApiFacade implements IGroupingApiFacade {
	
	private IDataManager db;
	private ConfigurationFile config;

	@Inject
	public GroupingApiFacade(IDataManager db, ConfigurationFile config) {
		this.db = db;
		this.config = config;
	}

	@Override
	public Response addGrouping(HttpServletRequest request, Grouping grouping) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Role> roles = db.getRoles(grouping.getGroup(), crsid);
		if (!roles.contains(Role.AUTHOR)) {
			return Response.status(401).entity(Strings.INVALIDROLE).build();
		}
		db.saveGrouping(grouping);
		return Response.status(201).entity(grouping).build();
	}

}
