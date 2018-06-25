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

	public CopyDB() {

		try {
			createIectDirs();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		checkLatestDB();
		prepIectDir();
		copyDB();

	}

	// the methods create the folders
	private void createIectDirs() throws IOException {

		//////////////////////////////////////Folder
		//** check folder
		File file = new File(System.getProperty("user.dir") + "\\Check\\");
		// if not present create it
		if (!file.exists())
			file.mkdir();

		//** int. tool folder
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

		//////////////////////////////////////File
		//** business rules app if are not present
		file = new File(System.getProperty("user.dir") + "\\Interpreting_Tool\\app.jar");
		if (!file.exists())
			FileUtils.copyFileToDirectory(new File(System.getProperty("user.dir") + "\\utils\\app.jar"),
					new File(System.getProperty("user.dir") + "\\Interpreting_Tool\\"));
		//** foodex1
		file = new File(System.getProperty("user.dir") + "\\Interpreting_Tool\\FoodEx1.xlsx");
		if (!file.exists())
			FileUtils.copyFileToDirectory(new File(System.getProperty("user.dir") + "\\utils\\FoodEx1.xlsx"),
					new File(System.getProperty("user.dir") + "\\Interpreting_Tool\\"));

		file=null;
		return;
	}

	// first search if there are MTX db which should be copied
	private void checkLatestDB() {

		File f = new File(mainDir + prodCat[0] + prodCat[1]);

		if (f.exists() && f.isDirectory()) {

			// track just the latest version of the production's sub folders
			long lastMod = Long.MIN_VALUE;

			File[] subDirectories = f.listFiles(File::isDirectory);

			for (File sub : subDirectories)
				if (sub.lastModified() > lastMod) {
					lastMod = sub.lastModified();
					this.latestDb = sub;
				}

		} else
			System.out.println("DB production folder does not exists!");

	}

	// prepare the I&CT Db folder
	private void prepIectDir() {

		try {

			// create usefull dirs
			new File(iectDir + mainCat).mkdir();
			new File(iectDir + prodCat[0]).mkdir();
			new File(iectDir + prodCat[0] + prodCat[1]).mkdir();

			// copy the main dir
			FileUtils.copyDirectory(new File(mainDir + mainCat), new File(iectDir + mainCat));

		} catch (IOException e) {
			e.printStackTrace();
		}

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
