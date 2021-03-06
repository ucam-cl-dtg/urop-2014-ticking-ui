package uk.ac.cam.cl.ticking.ui.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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

public class ForkStatusXls {

	// Not currently logging anything but could be used in future
	@SuppressWarnings("unused")
	private static final Logger log = LoggerFactory
			.getLogger(GroupApiFacade.class.getName());

	private IDataManager db;

	@Inject
	public ForkStatusXls(IDataManager db) {
		this.db = db;
	}

	public File generateXlsFile(Group group) throws IOException {

		String groupId = group.getGroupId();
		List<Tick> ticks = new ArrayList<>();
		for (String tickId : group.getTicks()) {
			ticks.add(db.getTick(tickId));
		}

		Collections.sort(ticks);

		List<User> submitters = db.getUsers(groupId, Role.SUBMITTER);

		DateTimeFormatter dtf = DateTimeFormat.forPattern("yyyy-MM-dd");

		HSSFWorkbook workbook = new HSSFWorkbook();

		HSSFSheet sheet = workbook.createSheet("Progress");

		sheet.createFreezePane(0, 1);

		int rownum = 0;
		int cellnum = 0;

		Row row = sheet.createRow(rownum++);

		CellStyle passStyle = workbook.createCellStyle();
		passStyle.setFillForegroundColor(IndexedColors.GREEN.getIndex());
		passStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

		CellStyle failStyle = workbook.createCellStyle();
		failStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
		failStyle.setFillPattern(CellStyle.SOLID_FOREGROUND);

		CellStyle rowStyle = row.getRowStyle();
		if (rowStyle == null) {
			rowStyle = workbook.createCellStyle();
		}

		Font font = workbook.createFont();
		font.setBoldweight(HSSFFont.BOLDWEIGHT_BOLD);
		rowStyle.setFont(font);
		row.setRowStyle(rowStyle);

		row.createCell(cellnum++).setCellValue("DISPLAY NAME");
		row.createCell(cellnum++).setCellValue("CRSID");
		Cell rightCell = row.createCell(cellnum++);
		rightCell.setCellValue("COLLEGE");

		CellStyle borderStyle = workbook.createCellStyle();
		borderStyle.setBorderRight(CellStyle.BORDER_THICK);
		borderStyle.setRightBorderColor(IndexedColors.BLACK.getIndex());

		rightCell.setCellStyle(borderStyle);

		for (Tick tick : ticks) {
			String heading = tick.getName();
			heading += (tick.getDeadline() == null) ? "" : " "
					+ tick.getDeadline().toString(dtf);
			row.createCell(cellnum++).setCellValue(heading);
		}

		for (User user : submitters) {
			row = sheet.createRow(rownum++);

			cellnum = 0;

			row.createCell(cellnum++).setCellValue(user.getDisplayName());
			row.createCell(cellnum++).setCellValue(user.getCrsid());
			rightCell = row.createCell(cellnum++);
			rightCell.setCellValue(user.getCollege());
			rightCell.setCellStyle(borderStyle);

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
						Cell cell = row.createCell(cellnum++);
						cell.setCellValue(Strings.FAILED);
						cell.setCellStyle(failStyle);
						createComment(workbook, sheet, row, cell,
								Strings.NOTSTARTED);
					} else {
						cellnum++;
					}
				} else {
					if (fork.getUnitPass()) {
						if (fork.getHumanPass()) {
							String code = tick.isStar() ? Strings.STARPASSED : Strings.PASSED;
							Cell cell = row.createCell(cellnum++);
							cell.setCellValue(code);

							cell.setCellStyle(passStyle);

							String comment = fork.getLastTickedBy() + " "
									+ fork.getLastTickedOn().toString(dtf)
									+ " " + fork.stats();

							createComment(workbook, sheet, row, cell, comment);

						} else {
							if (fork.isSignedUp()) {
								Cell cell = row.createCell(cellnum++);
								cell.setCellValue(Strings.SIGNEDUP);
								createComment(workbook, sheet, row, cell,
										fork.stats());
							}
							if (tick.getDeadline() != null
									&& tick.getDeadline().isBeforeNow()) {
								Cell cell = row.createCell(cellnum++);
								cell.setCellValue(Strings.FAILED);
								cell.setCellStyle(failStyle);
								createComment(workbook, sheet, row, cell,
										Strings.UNITPASSED + " " + fork.stats());
							} else {
								Cell cell = row.createCell(cellnum++);
								cell.setCellValue(Strings.UNITPASSED);
								createComment(workbook, sheet, row, cell,
										fork.stats());
							}

						}
					} else {
						if (fork.isReportAvailable()) {
							if (tick.getDeadline() != null
									&& tick.getDeadline().isBeforeNow()) {
								Cell cell = row.createCell(cellnum++);
								cell.setCellValue(Strings.FAILED);
								cell.setCellStyle(failStyle);
								createComment(workbook, sheet, row, cell,
										Strings.UNITFAILED + " " + fork.stats());
							} else {
								Cell cell = row.createCell(cellnum++);
								cell.setCellValue(Strings.UNITFAILED);
								createComment(workbook, sheet, row, cell,
										fork.stats());
							}

						} else {
							if (tick.getDeadline() != null
									&& tick.getDeadline().isBeforeNow()) {
								Cell cell = row.createCell(cellnum++);
								cell.setCellValue(Strings.FAILED);
								cell.setCellStyle(failStyle);
								createComment(
										workbook,
										sheet,
										row,
										cell,
										Strings.INITIALISED + " "
												+ fork.stats());
							} else {
								Cell cell = row.createCell(cellnum++);
								cell.setCellValue(Strings.INITIALISED);
								createComment(workbook, sheet, row, cell,
										fork.stats());
							}
						}
					}
				}

			}

		}

		for (int i = 0; i < 100; i++) {
			sheet.autoSizeColumn(i);
		}

		File temp = File.createTempFile(groupId, ".xls");
		FileOutputStream out = new FileOutputStream(temp);

		workbook.write(out);

		out.close();

		return temp;

	}

	private void createComment(Workbook workbook, Sheet sheet, Row row,
			Cell cell, String stringComment) {

		CreationHelper factory = workbook.getCreationHelper();

		Drawing drawing = sheet.createDrawingPatriarch();

		// When the comment box is visible, have it show in a 3x3 space
		ClientAnchor anchor = factory.createClientAnchor();
		anchor.setCol1(cell.getColumnIndex());
		anchor.setCol2(cell.getColumnIndex() + 3);
		anchor.setRow1(row.getRowNum());
		anchor.setRow2(row.getRowNum() + 3);

		// Create the comment and set the text+author
		Comment comment = drawing.createCellComment(anchor);
		RichTextString str = factory.createRichTextString(stringComment);
		comment.setString(str);
		comment.setAuthor("System");

		// Assign the comment to the cell
		cell.setCellComment(comment);
	}
}
