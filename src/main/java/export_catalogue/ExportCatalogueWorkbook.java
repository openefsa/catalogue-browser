package export_catalogue;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import catalogue.Catalogue;
import dcf_user.User;
import i18n_messages.CBMessages;
import ict_add_on.ICTInstaller;
import naming_convention.Headers;
import progress_bar.IProgressBar;

/**
 * Export all the database related to the current catalogue in a workbook.
 * 
 * @author avonva
 * @author shahaal
 */
public class ExportCatalogueWorkbook {

	private static final Logger LOGGER = LogManager.getLogger(ExportCatalogueWorkbook.class);

	private IProgressBar progressBar; // progress bar to show the export process to the user
	private static boolean extractXML; // flag used to add dump record if called from extract xml

	/**
	 * Set the progress bar if needed
	 * 
	 * @param progressBar
	 */
	public void setProgressBar(IProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	/**
	 * if (flag) -> Export the catalogue into a workbook formatted as .xlsx Four
	 * sheets are created: catalogue, hierarchy, attribute, term else -> Export only
	 * attributes and terms (ICT)
	 * 
	 * @author shahaal
	 * @param catalogue the catalogue we want to export
	 * @param filename
	 * @param flag
	 * @throws IOException
	 */
	public void exportCatalogue(Catalogue catalogue, String filename, Boolean flag) throws IOException {

		long startTime = System.currentTimeMillis();

		LOGGER.info("Starting export process...");

		// the workbook which will be created with the export
		SXSSFWorkbook workbook = new SXSSFWorkbook();

		// set that we want to maintain the temp files smaller
		workbook.setCompressTempFiles(true);

		// if exporting the info
		if (flag) {

			LOGGER.info("Exporting catalogue" + catalogue);

			// write the catalogue sheet
			ExportCatalogueSheet catSheet = new ExportCatalogueSheet(catalogue, workbook, Headers.CAT_SHEET_NAME);

			if (progressBar != null)
				catSheet.setProgressBar(progressBar, 20, CBMessages.getString("Export.CatalogueSheet"));

			catSheet.write();

			LOGGER.info("Exporting hierarchies");

			// write the hierarchy sheet
			ExportHierarchySheet hierarchySheet = new ExportHierarchySheet(catalogue, workbook,
					Headers.HIER_SHEET_NAME);

			if (progressBar != null)
				hierarchySheet.setProgressBar(progressBar, 40, CBMessages.getString("Export.HierarchySheet"));

			hierarchySheet.write();

			LOGGER.info("Exporting attributes");

			// write the attribute sheet
			ExportAttributeSheet attrSheet = new ExportAttributeSheet(catalogue, workbook, Headers.ATTR_SHEET_NAME,
					true);

			if (progressBar != null)
				attrSheet.setProgressBar(progressBar, 60, CBMessages.getString("Export.AttributeSheet"));

			attrSheet.write();

			LOGGER.info("Exporting terms");

			// write the term sheet
			ExportTermSheet termSheet = new ExportTermSheet(catalogue, workbook, Headers.TERM_SHEET_NAME, true);

			if (progressBar != null)
				termSheet.setProgressBar(progressBar, 80, CBMessages.getString("Export.TermSheet"));

			if (User.getInstance().isCatManager() && extractXML)
				termSheet.writeWithDump();
			else
				termSheet.write();

			LOGGER.info("Exporting release notes");

			// write the term sheet
			ExportReleaseNotesSheet noteSheet = new ExportReleaseNotesSheet(catalogue, workbook,
					Headers.NOTES_SHEET_NAME);

			if (progressBar != null)
				noteSheet.setProgressBar(progressBar, 95, CBMessages.getString("Export.NotesSheet"));

			noteSheet.write();

		} else {

			// if needed just the interpreting and checking tool info
			LOGGER.info("Exporting attributes");

			// write the attribute sheet
			ExportAttributeSheet attrSheet = new ExportAttributeSheet(catalogue, workbook, Headers.ATTR_SHEET_NAME,
					false);

			if (progressBar != null)
				attrSheet.setProgressBar(progressBar, 20, CBMessages.getString("Export.AttributeSheet"));

			attrSheet.write();

			LOGGER.info("Exporting terms");

			// write the term sheet
			ExportTermSheet termSheet = new ExportTermSheet(catalogue, workbook, Headers.TERM_SHEET_NAME, false);

			if (progressBar != null)
				termSheet.setProgressBar(progressBar, 95, CBMessages.getString("Export.TermSheet"));

			termSheet.write();

			// copy the new db into the ict main folder
			new ICTInstaller().createDatabase();
		}

		// last operation
		if (progressBar != null)
			progressBar.setLabel(CBMessages.getString("Export.WriteSheet"));

		LOGGER.info("Creating excel file");

		// write in the workbook
		OutputStream out = new FileOutputStream(filename);
		workbook.write(out);

		// close workbook and progress bar
		workbook.close();

		out.close();

		// fill progress bar
		if (progressBar != null)
			progressBar.fillToMax();

		if (progressBar != null)
			progressBar.close();

		LOGGER.info("Export finished, statistics: overall time = " + (System.currentTimeMillis() - startTime) / 1000.00
				+ " seconds");
	}
	
	/**
	 * method used for knowing if export from create xml
	 * 
	 * @author shahaal
	 * @param flag
	 */
	public static void setExtractXml() {
		extractXML = true;
	}
	
	/**
	 * method used for knowing if export from create xml
	 * 
	 * @author shahaal
	 * @param flag
	 */
	public static void resetExtractXml() {
		extractXML = false;
	}

	/*
	 * public static void main(String[] args) throws IOException {
	 * 
	 * ExportCatalogueWorkbook export = new ExportCatalogueWorkbook(); CatalogueDAO
	 * dao = new CatalogueDAO(); Catalogue landuse =
	 * dao.getLastVersionByCode("ACTFOR", DcfType.LOCAL); landuse.loadData();
	 * export.exportCatalogue(landuse, "landuse_export.xlsx", true); }
	 */

}