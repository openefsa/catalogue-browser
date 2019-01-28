package ict_add_on;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue_browser_dao.DatabaseManager;
import utilities.GlobalUtil;
import zip_manager.ZipManager;

/**
 * Class used for copying the official database for the ICT
 * 
 * @author shahaal
 */

public class ICT {

	private static final Logger LOGGER = LogManager.getLogger(ICT.class);
	private String URL = "https://github.com/openefsa/Interpreting-and-Checking-Tool/releases/download/1.2.6/utils.zip";
	private File ict;
	

	/**
	 * the method is used in order to call all the methods needed for the
	 * installation process of the ICT
	 * 
	 * @throws IOException
	 * @throws ZipException
	 */
	public void installICT() throws ZipException, IOException {

		// download the tool
		LOGGER.info("Starting download process...");
		downloadICT();

		// extract the zip folder
		LOGGER.info("Starting extraction process...");
		ZipManager.extractFolder(GlobalUtil.UTILS_FILE_PATH, GlobalUtil.MAIN_DIR);
		
		// remove the zip file when the extraction has finished
		LOGGER.info("Removing utils.zip file...");
		removeZipFile();

		// install ict
		LOGGER.info("Installing ICT...");
		install();

		// copy database
		LOGGER.info("Copying database for ICT...");
		copyDatabase();

	}

	/**
	 * download the interpreting and checking tool
	 * 
	 * @author shahaal
	 */
	private void downloadICT() {

		try {
			ict = HttpDownloadUtility.downloadFile(URL, GlobalUtil.MAIN_DIR);
		} catch (IOException e) {
			LOGGER.error("Download of ICT failed." + e);
		}

	}

	/**
	 * the method simply remove the downloaded zip file which contains the
	 * installation files for ICT
	 * 
	 * @author shahaal
	 */
	private void removeZipFile() {
		if (ict.exists())
			ict.delete();
	}

	/**
	 * the method moves the files of the "utils" folder
	 * 
	 * @author shahaal
	 * @throws IOException
	 */
	private void install() {

		// get the utils folder
		File utils = new File(GlobalUtil.UTILS_FOLDER_PATH);

		// check if utils exists and is a folder
		if (utils.exists() && utils.isDirectory()) {

			// move all files into the interpreting folder
			for (File subFile : utils.listFiles())
				GlobalUtil.moveFile(subFile.getAbsolutePath(), GlobalUtil.getIctDir() + subFile.getName());

			// remove folder (must be empty)
			utils.delete();
		}
	}

	/**
	 * the method copies only the following folder into ict db (from main db):
	 * PRODUCTION, CAT_MTX_DB
	 * 
	 * @author shahaal
	 */
	private void copyDatabase() {

		try {
			// clean the database
			GlobalUtil.deleteFileCascade(GlobalUtil.ICT_DATABASE_DIR_PATH);

			// copy main cat folder into the ict database one
			GlobalUtil.copyFile(new File(DatabaseManager.MAIN_CAT_DB_FOLDER),
					new File(GlobalUtil.ICT_MAIN_CAT_DB_PATH));

			File lastMtxVersion = getLastMtxVersion();
			System.out.println(lastMtxVersion.getAbsolutePath() + ", " + GlobalUtil.ICT_DATABASE_DIR_PATH);
			// copy in production just the MTX one
			GlobalUtil.copyFile(lastMtxVersion, new File(GlobalUtil.ICT_MTX_CAT_DB_FOLDER + lastMtxVersion.getName()));

			LOGGER.info("ICT correctly installed");

		} catch (IOException e) {
			LOGGER.error("Cannot copy embedded database in ICT db.", e);
		}
	}

	/**
	 * check the latest version of the MTX in cat mtx db
	 * 
	 * @author shahaal
	 * @return
	 */
	private File getLastMtxVersion() {

		File catMtxDb = new File(DatabaseManager.MTX_CAT_DB_FOLDER);

		File[] folders = catMtxDb.listFiles();

		int lastPos = folders.length - 1;

		// sort ascending order
		Arrays.sort(folders);

		return folders[lastPos];
	}

}
