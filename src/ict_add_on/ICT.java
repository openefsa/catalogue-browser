package ict_add_on;

import java.io.File;
import java.io.IOException;
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

	/**
	 * call all the methods needed for the
	 * installation of the ICT
	 * @return
	 * @throws IOException 
	 * @throws ZipException 
	 */
	public void installICT() throws ZipException, IOException {

		String utilsZipFile = GlobalUtil.UTILS_FILE_PATH;

		// extract the zip folder
		LOGGER.info("Starting extraction process...");
		ZipManager.extractFolder(utilsZipFile, GlobalUtil.MAIN_DIR);
		
		// remove the zip file when the extraction has finished
		LOGGER.info("Removing utils.zip file...");
		removeZipFile(utilsZipFile);

		// install ict
		LOGGER.info("Setting up ICT...");
		setupICT();

		// copy database
		LOGGER.info("Creating ICT database...");
		createDatabase();
			
	}

	/**
	 * the method simply remove the downloaded zip file which contains the
	 * installation files for ICT
	 * 
	 * @author shahaal
	 */
	private void removeZipFile(String file) {

		File f = new File(file);

		if (f.exists())
			f.delete();
	}

	/**
	 * the method moves the files of the "utils" folder
	 * 
	 * @author shahaal
	 * @throws IOException
	 */
	private void setupICT() {

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
	 * @throws IOException 
	 */
	public void createDatabase() throws IOException {

		// clean the database
		GlobalUtil.deleteFileCascade(GlobalUtil.ICT_DATABASE_DIR_PATH);

		// copy main cat folder into the ict database one
		GlobalUtil.copyFile(new File(DatabaseManager.MAIN_CAT_DB_FOLDER),
				new File(GlobalUtil.ICT_MAIN_CAT_DB_PATH));

		File lastMtxVersion = getLastMtxVersion();

		if (lastMtxVersion == null)
			return;

		// copy in production just the MTX one
		GlobalUtil.copyFile(lastMtxVersion, new File(GlobalUtil.ICT_MTX_CAT_DB_FOLDER + lastMtxVersion.getName()));

		LOGGER.info("ICT correctly installed");
			
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

		double latestVersion = 0.0;
		File lastMtxFolder = null;

		for (File f : folders) {

			// extract the version number
			double tempVersion = Double.parseDouble(f.getName().replaceAll("[^0-9\\.]+", ""));
			// take the highest value and save the folder file
			if (tempVersion > latestVersion) {
				latestVersion = tempVersion;
				lastMtxFolder = f;
			}
		}

		return lastMtxFolder;
	}
}
