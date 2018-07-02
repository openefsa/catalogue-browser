package export_catalogue;

import java.io.File;
import java.io.IOException;
import org.apache.commons.io.FileUtils;

//class used to make a copy of the MTX db for the interpreting and checking tool
public class CopyDB {

	private static String mainDir = System.getProperty("user.dir") + "\\Database\\";// main dir
	private static String iectDir = System.getProperty("user.dir") + "\\Interpreting_Tool\\Database\\";// int.tool dir
	private String[] prodCat = new String[] { "PRODUCTION_CATS\\", "CAT_MTX_DB" };
	private String mainCat = "MAIN_CATS_DB";
	private File latestDb = null;

	/*
	 * flag used to know if the utils folder is present or not if yes move all the
	 * files inside into the proper place and remove utils if not then do the rest
	 */
	public CopyDB() {

		try {
			createIectDirs();
			checkLatestDB();
			prepIectDir();
			copyDB();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	// the methods create the folders
	private void createIectDirs() throws IOException {

		////////////////////////////////////// Folder
		// ** check folder
		File file = new File(System.getProperty("user.dir") + "\\Check\\");
		// if not present create it
		if (!file.exists())
			file.mkdir();

		// ** int. tool folder
		file = new File(System.getProperty("user.dir") + "\\Interpreting_Tool\\");
		if (!file.exists()) {
			file.mkdir();
		}
		// create the db folder
		file = new File(System.getProperty("user.dir") + "\\Interpreting_Tooll\\Database\\");
		if (!file.exists())
			file.mkdir();
		else
			// remove everything from the folder
			FileUtils.cleanDirectory(file);

		////////////////////////////////////// File
		// get the main folder
		file = new File(System.getProperty("user.dir")).getParentFile();
		File[] subDirs = file.listFiles(File::isDirectory);
		for (File dir : subDirs) {

			// check if utils exists and is a directory
			if (dir.getName().equals("utils")) {
				// list all the files inside
				File[] subFiles = dir.listFiles(File::isFile);
				// check for every file the extension
				for (File sub : subFiles) {

					// move the jar/xlsx files into the interpreting folder
					if (sub.getName().endsWith(".jar") || sub.getName().endsWith(".xlsx")) {
						FileUtils.copyFileToDirectory(sub,
								new File(System.getProperty("user.dir") + "\\Interpreting_Tool\\"));
						sub.delete();
					} else
						// move the xlsm file into the parent folder for an easy access of it
						if (sub.getName().endsWith(".xlsm")) {
							FileUtils.copyFileToDirectory(sub, file);
							sub.delete();
						}
				}
				// remove the utilis folder
				dir.delete();
			}
		}

		file = null;
		return;
	}

	// first search if there are MTX db which should be copied
	private void checkLatestDB() {

		File f = new File(mainDir + prodCat[0] + prodCat[1]);

		if (f.exists() && f.isDirectory()) {

			File[] subDirectories = f.listFiles(File::isDirectory);
			//initialize the version number
			Double versNo = 0.0;
			for (File sub : subDirectories) {
				//check the version number
				Double tempVersNo = Double.parseDouble(sub.getName().replaceAll("[^0-9.]+",""));
				
				if (tempVersNo > versNo) {
					versNo = tempVersNo;
					this.latestDb = sub;
				}
			}
		} else
			System.out.println("DB production folder does not exists!");

	}

	// prepare the I&CT Db folder
	private void prepIectDir() {

		try {

			// create usefull dirs
			File f=new File(iectDir + mainCat);
			checkFolder(f);
			
			f=new File(iectDir + prodCat[0]);
			checkFolder(f);

			// copy the main dir
			FileUtils.copyDirectory(new File(mainDir + mainCat), new File(iectDir + mainCat));

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private void checkFolder(File f) throws IOException {
		if(f.exists()&&f.isDirectory())
			//remove what is inside it
			FileUtils.cleanDirectory(f); 
		else
			//make it
			f.mkdir();
	}

	// method is used so to save the specific MTX folder into the I&CT
	private void copyDB() {

		try {
			// create version folder
			new File(iectDir + prodCat[0] + prodCat[1] + "\\" + latestDb.getName()).mkdir();
			FileUtils.copyDirectory(latestDb, new File(iectDir + prodCat[0] + prodCat[1] + "\\" + latestDb.getName()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
