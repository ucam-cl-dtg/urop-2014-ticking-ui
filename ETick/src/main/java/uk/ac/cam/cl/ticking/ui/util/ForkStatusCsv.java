package uk.ac.cam.cl.ticking.ui.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.joda.time.DateTime;
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
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

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
		writer.append(',');
		for (String tickId : group.getTicks()) {
			writer.append(',' + db.getTick(tickId).getName());
		}

		writer.append('\n');
		writer.append('\n');

		for (User user : submitters) {
			writer.append(user.getDisplayName());
			writer.append(',' + user.getCrsid());
			writer.append(',' + user.getCollege());
			writer.append(',');

			for (String tickId : tickIds) {
				Tick tick = db.getTick(tickId);
				Fork fork = db.getFork(Fork.generateForkId(user.getCrsid(),
						tickId));
				
				DateTime extension = tick.getExtensions().get(user.getCrsid());
				if (extension != null) {
					tick.setDeadline(extension);
				}
				
				if (fork == null) {
					
					if (tick.getDeadline()!=null&&tick.getDeadline().isBeforeNow()) {
						writer.append(","+Strings.FAILED);
					} else {
						writer.append(',');
					}
				} else {
					if (fork.getUnitPass()) {
						if (fork.getHumanPass()) {
							writer.append(","+Strings.PASSED+" (By "
									+ fork.getLastTickedBy() + " on "
									+ fork.getLastTickedOn().toString(dtf)+")");
						} else {
							if (tick.getDeadline()!=null&&tick.getDeadline().isBeforeNow()) {
								writer.append(","+Strings.FAILED+" ("+Strings.UNITPASSED+" "+fork.stats()+")");
							} else {
								writer.append(","+Strings.UNITPASSEDCODE+" ("+Strings.UNITPASSED+" "+fork.stats()+")");
							}
						}
					} else {
						if (fork.isReportAvailable()) {
							if (tick.getDeadline()!=null&&tick.getDeadline().isBeforeNow()) {
								writer.append(","+Strings.FAILED+" ("+Strings.UNITFAILED+" "+fork.stats()+")");
							} else {
								writer.append(","+Strings.UNITFAILEDCODE+" ("+Strings.UNITFAILED+" "+fork.stats()+")");
							}
						} else {
							if (tick.getDeadline()!=null&&tick.getDeadline().isBeforeNow()) {
								writer.append(","+Strings.FAILED+" ("+Strings.INITIALISED+" "+fork.stats()+")");
							} else {
								writer.append(","+Strings.INITCODE+" ("+Strings.INITIALISED+" "+fork.stats()+")");
							}
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
