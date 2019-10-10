package sas_remote_procedures;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import catalogue.Catalogue;
import catalogue_browser_dao.CatalogueEntityDAO;
import catalogue_generator.ThreadFinishedListener;
import export_catalogue.ExportActions;
import export_catalogue.ExportCatalogueWorkbook;
import i18n_messages.CBMessages;
import progress_bar.FormProgressBar;
import progress_bar.IProgressBar;
import utilities.GlobalUtil;

/**
 * This class is used to compute the differences between the local catalogue and
 * the official version of the catalogue (storing them in a .xml file), in order
 * to be able to update the changes in a second step with an {@link UploadData}
 * action. In particular, the .xml file contains several instructions which can
 * be used to upload the official catalogue directly.
 * 
 * @author avonva
 *
 */
public class XmlUpdatesFactory {

	private static final Logger LOGGER = LogManager.getLogger(XmlUpdatesFactory.class);

	private CatalogueEntityDAO<XmlUpdateFile> dao;
	private IProgressBar progressBar;
	private Listener abortListener;
	private Listener doneListener;

	public XmlUpdatesFactory(CatalogueEntityDAO<XmlUpdateFile> dao) {
		this.dao = dao;
	}

	/**
	 * Ask to the sas server to create the .xml file which contains the differences
	 * between {@code catalogue} and its official version hosted on the dcf. The
	 * .xml file will be created on the remote folder, which is defined in
	 * {@link #XML_UPDATES_CREATOR_PATH}.
	 * 
	 * @param catalogue
	 */
	public void createXml(final Catalogue catalogue) {

		LOGGER.info("Starting xml creation procedure for " + catalogue);

		// delete the old input if present, since it is not more useful
		// because we are requesting a new xml related to the same catalogue
		deleteOldInput(catalogue);

		final String filename = createUniqueCode(catalogue);

		final String startFilename = filename + XmlChangesService.START_FORMAT;

		// export the catalogue into the start file
		// and make actions when it has finished
		ExportActions export = new ExportActions();
		export.setProgressBar(progressBar);
		// flag in order to create dump string
		ExportCatalogueWorkbook.setExtractXml();
		// export first the excel
		export.exportAsync(catalogue, startFilename, true, new ThreadFinishedListener() {

			@Override
			public void finished(Thread thread, int code, Exception exception) {

				if (code != ThreadFinishedListener.OK) {
					abort(CBMessages.getString("XmlChangesCreator.ExportAbort"));
					// reset flag
					ExportCatalogueWorkbook.resetExtractXml();
					return;
				}

				// base remote path of the sas procedure
				String inputFolder = SasRemotePaths.XML_UPDATES_CREATOR_INPUT_FOLDER;

				// .start file which is created by the local application
				// which contains the catalogue export .xlsx hidden in a .start file
				File startFile = new File(startFilename);

				// path where the local .start file should be copied
				File remoteStartFile = new File(inputFolder + startFilename);

				// name which will be given to the .start file when
				// it is copied into the remote folder. In particular, we
				// will rename it into .xlsx once the .start is successfully
				// copied into the remote folder (avoid file system problems)
				File remoteXlsxFile = new File(inputFolder + filename + XmlChangesService.LOCAL_EXPORT_FORMAT);

				// .end file which is used as green semaphore, i.e., to warn
				// the sas procedure that it can start processing the .xlsx file
				File remoteEndFile = new File(inputFolder + filename + XmlChangesService.END_FORMAT);

				// copy the start file into the remote folder
				// where the sas procedure can read the file
				try {
					GlobalUtil.copyFile(startFile, remoteStartFile);
				} catch (IOException e) {
					LOGGER.error("Cannot copy " + startFile + " into " + remoteStartFile, e);
					abort(e.getMessage());
					// reset flag
					ExportCatalogueWorkbook.resetExtractXml();
					return;
				}

				// delete the local .start file since we
				// do not need it anymore
				startFile.delete();

				// change the extension in .xlsx
				if (!rename(remoteStartFile, remoteXlsxFile))
					return;

				// create the .end file in the remote folder
				if (!createEndFile(remoteEndFile))
					return;

				// save the xml filename into the database
				saveXmlFilename(catalogue, filename);

				LOGGER.info("Create updates xml process finished");

				// reset flag
				ExportCatalogueWorkbook.resetExtractXml();

				// call the done listener
				if (doneListener != null) {
					doneListener.handleEvent(null);
				}
			}
		});
		
	}

	/**
	 * Delete a previous {@value #LOCAL_EXPORT_FORMAT} file from the remote folder
	 * if the sas procedure did not started the conversion of that file.
	 * 
	 * @param filename
	 * @return
	 */
	private boolean deleteOldInput(Catalogue catalogue) {

		XmlUpdateFile xmlUp = dao.getById(catalogue.getId());

		if (xmlUp == null)
			return false;

		// set the name of the base filename
		String filename = SasRemotePaths.XML_UPDATES_CREATOR_UPDATE_FOLDER + xmlUp.getXmlFilename();

		boolean lockDeleted = false;
		boolean dataDeleted = false;

		// check if the remote lock exists or not
		File remoteStartProcFile = new File(filename + XmlChangesService.REMOTE_START_FORMAT);

		// if it exists, the procedure was started
		// therefore we cannot do anything
		if (remoteStartProcFile.exists())
			return lockDeleted;

		// otherwise we delete both the local export file and its
		// green semaphore
		File remoteEndFile = new File(filename + XmlChangesService.END_FORMAT);
		File remoteXlsxFile = new File(filename + XmlChangesService.LOCAL_EXPORT_FORMAT);

		if (remoteEndFile.exists())
			lockDeleted = remoteEndFile.delete();

		if (remoteXlsxFile.exists())
			dataDeleted = remoteXlsxFile.delete();

		if (lockDeleted)
			LOGGER.info("old " + remoteEndFile + " deleted");

		if (dataDeleted)
			LOGGER.info("old " + remoteXlsxFile + " deleted");

		return lockDeleted && dataDeleted;
	}

	/**
	 * Create a unique code for the current catalogue, in order to be able to
	 * correctly retrieve the correct xml file
	 * 
	 * @param catalogue
	 * @return
	 */
	private String createUniqueCode(Catalogue catalogue) {

		StringBuilder builder = new StringBuilder();
		builder.append(catalogue.getCode());
		builder.append("_");
		builder.append(catalogue.getVersion());
		builder.append("_");

		// use today date as encoding to use a unique id
		Date date = Calendar.getInstance().getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_hhmmss");
		String encodedDate = sdf.format(date);

		builder.append(encodedDate);
		builder.append("_");
		builder.append("BR");

		return builder.toString();
	}

	/**
	 * Rename a file with a new name
	 * 
	 * @param file
	 * @param newFile
	 * @return
	 */
	private boolean rename(File file, File newFile) {

		LOGGER.info("Renaming " + file + " into " + newFile);

		boolean success = file.renameTo(newFile);

		if (!success) {

			String error = "Cannot rename " + file + " in " + newFile + ". Aborting operation...";

			LOGGER.error(error);
			abort(CBMessages.getString("XmlChangesCreator.RenameAbort"));
		}

		return success;
	}

	/**
	 * Create the end file, which is the file that allows the sas operation to
	 * start.
	 * 
	 * @param endFile
	 * @return
	 */
	private boolean createEndFile(File endFile) {

		// create the .end file to start the
		// sas application
		LOGGER.info("Creating lock file " + endFile);

		try {
			endFile.createNewFile();
		} catch (IOException e) {
			LOGGER.error("Cannot create " + endFile, e);
			abort(e.getMessage());
			return false;
		}

		return true;
	}

	/**
	 * Save the just created xml filename in the db, in order to be able to use it
	 * subsequently in the application.
	 * 
	 * @param catalogue
	 * @param xmlFilename
	 */
	private void saveXmlFilename(Catalogue catalogue, String xmlFilename) {

		XmlUpdateFile xml = new XmlUpdateFile(catalogue, xmlFilename);
		XmlChangesService service = new XmlChangesService();
		service.update(dao, xml);
	}

	/**
	 * Abort the process
	 */
	private void abort(String message) {

		if (progressBar != null)
			progressBar.close();

		if (abortListener != null) {
			Event event = new Event();
			event.data = message;
			abortListener.handleEvent(event);
		}
	}

	/**
	 * Set the progress bar for the export process
	 * 
	 * @param progressBar
	 */
	public void setProgressBar(FormProgressBar progressBar) {
		this.progressBar = progressBar;
	}

	/**
	 * Called if something went wrong during the process
	 * 
	 * @param abortListener
	 */
	public void setAbortListener(Listener abortListener) {
		this.abortListener = abortListener;
	}

	/**
	 * Called if the process terminated correctly
	 * 
	 * @param doneListener
	 */
	public void setDoneListener(Listener doneListener) {
		this.doneListener = doneListener;
	}

	/*
	 * public static void main ( String args[] ) {
	 * 
	 * CatalogueDAO catDao = new CatalogueDAO(); Catalogue cat =
	 * catDao.getCatalogue( "ABUNDANCE", "4.5", DcfType.TEST ); cat.loadData();
	 * XmlUpdatesFactory creator = new XmlUpdatesFactory (); creator.createXml( cat
	 * );
	 * 
	 * try { Thread.sleep( 5000 ); } catch (InterruptedException e) {
	 * e.printStackTrace(); }
	 * 
	 * XmlUpdateFileDAO dao = new XmlUpdateFileDAO();
	 * 
	 * try { dao.getById( cat.getId() ).downloadXml( 5000 ); } catch (IOException e)
	 * { e.printStackTrace(); } }
	 */
}
