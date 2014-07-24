package uk.ac.cam.cl.ticking.ui.api;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import uk.ac.cam.cl.ticking.ui.actors.Grouping;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupingApiFacade;
import uk.ac.cam.cl.ticking.ui.auth.RavenManager;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationFile;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.exceptions.DuplicateDataEntryException;
import uk.ac.cam.cl.ticking.ui.util.Strings;

import com.google.inject.Inject;

public class GroupingApiFacade implements IGroupingApiFacade {

	private IDataManager db;
	
	@SuppressWarnings("unused")
	// Currently not needed but these classes are still not final and it is
	// quite likely to be required in future
	private ConfigurationFile config;
	
	private RavenManager raven;

	/**
	 * @param db
	 * @param config
	 * @param raven
	 */
	@Inject
	public GroupingApiFacade(IDataManager db, ConfigurationFile config,
			RavenManager raven) {
		this.db = db;
		this.config = config;
		this.raven = raven;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.cam.cl.ticking.ui.api.public_interfaces.IGroupingApiFacade#addGrouping
	 * (javax.servlet.http.HttpServletRequest,
	 * uk.ac.cam.cl.ticking.ui.actors.Grouping)
	 */
	@Override
	public Response addGrouping(HttpServletRequest request, Grouping grouping) {
		String crsid = (String) request.getSession().getAttribute(
				"RavenRemoteUser");
		List<Role> roles = db.getRoles(grouping.getGroup(), crsid);
		if (!roles.contains(Role.AUTHOR)) {
			return Response.status(401).entity(Strings.INVALIDROLE).build();
		}
		try {
			db.insertUser(raven.ldapProduceUser(crsid));
		} catch (DuplicateDataEntryException e) {
			// Do nothing
			// The user is already in the database and so we don't need to add
			// them.
		}
		db.saveGrouping(grouping);
		return Response.status(201).entity(grouping).build();
	}

}
