package ict_add_on;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import catalogue_browser_dao.DatabaseManager;
import utilities.GlobalUtil;

/**
 * Class used for copying the official database for the ICT
 * 
 * @author shahaal
 */

public class ICT {

	private static final Logger LOGGER = LogManager.getLogger(ICT.class);

	public ICT() {

		// extract the content of the utils folder
		extractUtilsFiles();

		// copy database
		copyDatabase();

	}

	/**
	 * the method moves the files of the "utils" folder
	 * 
	 * @author shahaal
	 * @throws IOException
	 */
	private void extractUtilsFiles() {

		// get the utils folder
		File utils = new File(GlobalUtil.UTILS_FOLDER_PATH);
		
		// check if utils exists and is a folder
		if (utils.exists() && utils.isDirectory()) {

			// check for every file the extension
			for (File subFile : utils.listFiles()) {
				
				// get the file name
				String fileName = subFile.getName();
				// get the file extension
				String fileExt = FilenameUtils.getExtension(fileName);
				
				// move the jar/xlsx files into the interpreting folder
				if (fileExt.contains("jar") || fileExt.contains("xlsx")) {
					
					GlobalUtil.moveFile(subFile.getAbsolutePath(), GlobalUtil.getIctDir()+fileName);
				}

				// move the xlsm file into the parent folder for an easy access of it
				if (fileExt.contains("xlsm")) {
					GlobalUtil.moveFile(subFile.getAbsolutePath(), GlobalUtil.ICT_FILE_PATH);
				}

			}

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
	private boolean copyDatabase() {

		try {

			// clean the database
			GlobalUtil.deleteFileCascade(GlobalUtil.ICT_DATABASE_DIR_PATH);
			
			// copy main cat folder into the ict database one
			GlobalUtil.copyFile(new File(DatabaseManager.MAIN_CAT_DB_FOLDER),
					new File(GlobalUtil.ICT_MAIN_CAT_DB_PATH));
			
			File lastMtxVersion = getLastMtxVersion();
			System.out.println(lastMtxVersion.getAbsolutePath()+", "+GlobalUtil.ICT_DATABASE_DIR_PATH);
			// copy in production just the MTX one
			GlobalUtil.copyFile(lastMtxVersion,
					new File(GlobalUtil.ICT_MTX_CAT_DB_FOLDER + lastMtxVersion.getName()));

			LOGGER.info("ICT correctly installed");
			return true;

		} catch (IOException e) {
			LOGGER.error("Cannot copy embedded database in ICT db.", e);
			return false;
		}
	}
	
	/**
	 * check the latest version of the MTX in cat mtx db
	 * @author shahaal
	 * @return
	 */
	private File getLastMtxVersion() {
		
		File catMtxDb = new File(DatabaseManager.MTX_CAT_DB_FOLDER);
		
		File[] folders = catMtxDb.listFiles();
		
		int lastPos = folders.length -1;
		
		// sort ascending order
		Arrays.sort(folders);
		
		return folders[lastPos];
	}
	
	
	public static void main(String[] args) {
		new ICT();
	}
}
