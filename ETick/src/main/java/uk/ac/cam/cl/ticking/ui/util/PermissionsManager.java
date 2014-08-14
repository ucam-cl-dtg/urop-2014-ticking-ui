package uk.ac.cam.cl.ticking.ui.util;

import java.util.List;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans.TickBean;
import uk.ac.cam.cl.ticking.ui.configuration.Admins;
import uk.ac.cam.cl.ticking.ui.configuration.ConfigurationLoader;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

import com.google.inject.Inject;

public class PermissionsManager {

	private IDataManager db;

	private ConfigurationLoader<Admins> adminConfig;
	
	@Inject
	public PermissionsManager(IDataManager db, ConfigurationLoader<Admins> adminConfig) {
		this.db = db;
		this.adminConfig = adminConfig;
	}
	
	public boolean isStudent(User user) {
		return user.getIsStudent()&&!user.isAdmin();
	}
	
	public boolean isAdmin(String crsid) {
		return adminConfig.getConfig().isAdmin(crsid);
	}

	public boolean tickRole(String crsid, String tickId, Role role) {

		if (isAdmin(crsid)) {
			return true;
		}

		boolean isRole = false;
		List<String> groupIds = db.getTick(tickId).getGroups();

		for (String groupId : groupIds) {
			List<Role> roles = db.getRoles(groupId, crsid);
			if (roles.contains(role)) {
				isRole = true;
			}
		}

		return isRole;
	}
	
	public boolean tickBeanGroupPermissions(String crsid, TickBean tickBean) {
		
		if (isAdmin(crsid)) {
			return true;
		}
		
		List<String> groupIds = tickBean.getGroups();
		for (String groupId : groupIds) {
			List<Role> roles = db.getRoles(groupId, crsid);
			if (!roles.contains(Role.AUTHOR)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean hasRole(String crsid, String groupId, Role role) {
		List<Role> myRoles = db.getRoles(groupId, crsid);
		return (isAdmin(crsid))||myRoles.contains(role);
	}

	public boolean forkCreator(String myCrsid, String crsid, String tickId) {
		return (isAdmin(crsid))
				|| (myCrsid.equals(db.getFork(
						Fork.generateForkId(crsid, tickId)).getAuthor()));
	}

	public boolean groupCreator(String crsid, Group group) {
		return (isAdmin(crsid))
				|| (crsid.equals(group.getCreator()));
	}
	
	public boolean tickCreator(String crsid, Tick tick) {
		return (isAdmin(crsid))||(crsid.equals(tick.getAuthor()));
	}

}
