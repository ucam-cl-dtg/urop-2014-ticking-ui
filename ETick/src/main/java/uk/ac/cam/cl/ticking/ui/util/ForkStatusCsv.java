package uk.ac.cam.cl.ticking.ui.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.ticking.ui.actors.Group;
import uk.ac.cam.cl.ticking.ui.actors.Role;
import uk.ac.cam.cl.ticking.ui.actors.User;
import uk.ac.cam.cl.ticking.ui.api.facades.GroupApiFacade;
import uk.ac.cam.cl.ticking.ui.dao.IDataManager;
import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

import com.google.inject.Inject;

public class ForkStatusCsv {

	// Not currently logging anything but could be used in future
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory
			.getLogger(GroupApiFacade.class.getName());

	private IDataManager db;

	@Inject
	public ForkStatusCsv(IDataManager db) {
		this.db = db;
	}

	public File generateCsvFile(Group group) throws IOException {

		String groupId = group.getGroupId();

		File temp = File.createTempFile(groupId, ".csv");
		FileWriter writer = new FileWriter(temp);

		generateCsv(writer, group);

		return temp;

	}

	public String generateCsvString(Group group) throws IOException {

		StringWriter writer = new StringWriter();

		generateCsv(writer, group);

		return writer.toString();

	}

	public void generateCsv(Writer writer, Group group) throws IOException {

		String groupId = group.getGroupId();

		List<Tick> ticks = new ArrayList<>();
		for (String tickId : group.getTicks()) {
			ticks.add(db.getTick(tickId));
		}

		Collections.sort(ticks);

		List<User> submitters = db.getUsers(groupId, Role.SUBMITTER);

		DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/yyyy");

		writer.append("Display Name");
		writer.append(",CRSid");
		writer.append(",College");
		writer.append(',');
		for (Tick tick : ticks) {
			String name = tick.getName().replace(',', ';');
			name = name.replaceAll("\\n", " ");
			String heading = ',' + name;
			writer.append(heading);
		}

		writer.append('\n');
		writer.append('\n');

		for (User user : submitters) {
			writer.append(user.getDisplayName());
			writer.append(',' + user.getCrsid());
			writer.append(',' + user.getCollege());
			writer.append(',');

			for (Tick tick : ticks) {
				Fork fork = db.getFork(Fork.generateForkId(user.getCrsid(),
						tick.getTickId()));

				DateTime extension = tick.getExtensions().get(user.getCrsid());
				if (extension != null) {
					tick.setDeadline(extension);
				}

				if (fork == null) {

					if (tick.getDeadline() != null
							&& tick.getDeadline().isBeforeNow()) {
						String code = tick.isStar() ? Strings.STARFAILED : Strings.FAILED;
						writer.append("," + code + " ("
								+ Strings.NOTSTARTED + ")");
					} else {
						if (tick.isStar()) {
							writer.append(",*");
						} else {
							writer.append(',');
						}
					}
				} else {
					if (fork.getUnitPass()) {
						if (fork.getHumanPass()) {
							String code = tick.isStar() ? Strings.STARPASSED : Strings.PASSED;
							writer.append("," + code + " (Ticked by "
									+ fork.getLastTickedBy() + " on "
									+ fork.getLastTickedOn().toString(dtf)
									+ " " + fork.stats() + ")");
						} else {
							if (fork.isSignedUp()) {
								String code = tick.isStar() ? Strings.STARSIGNEDUPCODE : Strings.SIGNEDUPCODE;
								writer.append("," + code + " ("
										+ Strings.SIGNEDUP + " " + fork.stats()
										+ ")");
							} else {
								if (tick.getDeadline() != null
										&& tick.getDeadline().isBeforeNow()) {
									String code = tick.isStar() ? Strings.STARFAILED : Strings.FAILED;
									writer.append("," + code + " ("
											+ Strings.UNITPASSED + " "
											+ fork.stats() + ")");
								} else {
									String code = tick.isStar() ? Strings.STARUNITPASSEDCODE : Strings.UNITPASSEDCODE;
									writer.append("," + code
											+ " (" + Strings.UNITPASSED + " "
											+ fork.stats() + ")");
								}
							}
						}
					} else {
						if (fork.isReportAvailable()) {
							if (tick.getDeadline() != null
									&& tick.getDeadline().isBeforeNow()) {
								String code = tick.isStar() ? Strings.STARFAILED : Strings.FAILED;
								writer.append("," + code + " ("
										+ Strings.UNITFAILED + " "
										+ fork.stats() + ")");
							} else {
								String code = tick.isStar() ? Strings.STARUNITFAILEDCODE : Strings.UNITFAILEDCODE;
								writer.append("," + code
										+ " (" + Strings.UNITFAILED + " "
										+ fork.stats() + ")");
							}
						} else {
							if (tick.getDeadline() != null
									&& tick.getDeadline().isBeforeNow()) {
								String code = tick.isStar() ? Strings.STARFAILED : Strings.FAILED;
								writer.append("," + code + " ("
										+ Strings.INITIALISED + " "
										+ fork.stats() + ")");
							} else {
								String code = tick.isStar() ? Strings.STARINITCODE : Strings.INITCODE;
								writer.append("," + code + " ("
										+ Strings.INITIALISED + " "
										+ fork.stats() + ")");
							}
						}
					}
				}

			}

			writer.append('\n');

		}

		writer.flush();
		writer.close();
	}
}
