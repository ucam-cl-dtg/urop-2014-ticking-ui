package uk.ac.cam.cl.ticking.ui.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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

public class ForkStatusXls {

	private static final Logger log = LoggerFactory
			.getLogger(GroupApiFacade.class.getName());

	private IDataManager db;

	@Inject
	public ForkStatusXls(IDataManager db) {
		this.db = db;
	}

	public File generateXlsFile(Group group) throws IOException {

		String groupId = group.getGroupId();
		List<String> tickIds = group.getTicks();
		List<User> submitters = db.getUsers(groupId, Role.SUBMITTER);
		
		DateTimeFormatter dtf = DateTimeFormat.forPattern("dd/MM/yyyy");
		
		HSSFWorkbook workbook = new HSSFWorkbook();
		
		CreationHelper factory = workbook.getCreationHelper();
		
		HSSFSheet sheet = workbook.createSheet("Progress");
		
		sheet.createFreezePane(0, 1);
		
		int rownum = 0;
		int cellnum = 0;
		
		Row row = sheet.createRow(rownum++);
		
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
	    
	    for (String tickId : group.getTicks()) {
			row.createCell(cellnum++).setCellValue(db.getTick(tickId).getName());
		}
		
		for (User user : submitters) {
			row = sheet.createRow(rownum++);
			
			cellnum = 0;
			
			row.createCell(cellnum++).setCellValue(user.getDisplayName());
			row.createCell(cellnum++).setCellValue(user.getCrsid());
			rightCell = row.createCell(cellnum++);
			rightCell.setCellValue(user.getCollege());
			rightCell.setCellStyle(borderStyle);
			
			for (String tickId : tickIds) {
				Fork fork = db.getFork(Fork.generateForkId(user.getCrsid(),
						tickId));
				if (fork == null) {
					cellnum++;
				} else {
					if (fork.getUnitPass()) {
						if (fork.getHumanPass()) {
							Cell cell = row.createCell(cellnum++);
							cell.setCellValue("PASSED");
							CellStyle style = workbook.createCellStyle();
							style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
							style.setFillPattern(CellStyle.SOLID_FOREGROUND);
							cell.setCellStyle(style);
							
							Drawing drawing = sheet.createDrawingPatriarch();

						    // When the comment box is visible, have it show in a 1x3 space
						    ClientAnchor anchor = factory.createClientAnchor();
						    anchor.setCol1(cell.getColumnIndex());
						    anchor.setCol2(cell.getColumnIndex()+1);
						    anchor.setRow1(row.getRowNum());
						    anchor.setRow2(row.getRowNum()+3);

						    // Create the comment and set the text+author
						    Comment comment = drawing.createCellComment(anchor);
						    RichTextString str = factory.createRichTextString(fork.getLastTickedBy()+" "+fork.getLastTickedOn().toString(dtf));
						    comment.setString(str);
						    comment.setAuthor("System");

						    // Assign the comment to the cell
						    cell.setCellComment(comment);
						    
						} else {
							row.createCell(cellnum++).setCellValue("Unit passed");
						}
					} else {
						if (fork.isReportAvailable()) {
							row.createCell(cellnum++).setCellValue("Unit failed");
						} else {
							row.createCell(cellnum++).setCellValue("Initilialised");
						}
					}
				}

			}
			
		}
		
		for (int i = 0; i<100; i++) {
			sheet.autoSizeColumn(i);
		}
		
		File temp = File.createTempFile(groupId, ".xls");
		FileOutputStream out = new FileOutputStream(temp);

		workbook.write(out);
		
		out.close();
		
		return temp;

	}
}
