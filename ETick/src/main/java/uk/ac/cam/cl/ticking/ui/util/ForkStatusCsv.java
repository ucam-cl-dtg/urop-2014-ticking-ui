package uk.ac.cam.cl.ticking.ui.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.GroupApiFacade;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;

import com.google.inject.Inject;

public class ForkStatusCsv {

	private static final Logger log = LoggerFactory
			.getLogger(GroupApiFacade.class.getName());

	private IDataManager db;
	
	@Inject
	public ForkStatusCsv(IDataManager db) {
		this.db = db;
	}

	public File generateCsvFile(Group group) throws IOException {

		String groupId = group.getGroupId();
		List<String> tickIds = group.getTicks();
		List<User> submitters = db.getUsers(groupId, Role.SUBMITTER);

		File temp;
		FileWriter writer;
		
		DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/yyyy");

		temp = File.createTempFile(groupId, ".csv");
		writer = new FileWriter(temp);

		writer.append("Display Name");
		writer.append(",CRSid");
		writer.append(",College");
		for (String tickId : group.getTicks()) {
			writer.append(',' + db.getTick(tickId).getName());
		}

		writer.append('\n');

		for (User user : submitters) {
			writer.append(user.getDisplayName());
			writer.append(',' + user.getCrsid());
			writer.append(',' + user.getCollege());

			for (String tickId : tickIds) {
				Fork fork = db.getFork(Fork.generateForkId(user.getCrsid(),
						tickId));
				if (fork == null) {
					writer.append(',');
				} else {
					if (fork.getUnitPass()) {
						if (fork.getHumanPass()) {
							writer.append(",PASSED by "
									+ fork.getLastTickedBy() + " on "
									+ fork.getLastTickedOn().toString(dtf));
						} else {
							writer.append(",Unit passed");
						}
					} else {
						if (fork.isReportAvailable()) {
							writer.append(",Unit failed");
						} else {
							writer.append(",Initilialised");
						}
					}
				}

			}
			
			writer.append('\n');
			
		}
		
		writer.flush();
		writer.close();
		
		return temp;

	}
}
