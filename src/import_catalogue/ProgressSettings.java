package import_catalogue;

import java.util.HashMap;

/**
 * Settings used for the import process in {@link CatalogueWorkbookImporter}
 * class.
 * @author avonva
 *
 */
public class ProgressSettings {
	
	private static final HashMap<Integer, Integer> progress;
	public static final int CAT_SHEET = 0;
	public static final int HIER_SHEET = 1;
	public static final int ATTR_SHEET = 2;
	public static final int TERM_SHEET = 3;
	public static final int TERM_ATTR_SHEET = 4;
	public static final int PARENT_SHEET = 5;
	public static final int NOTES_SHEET = 6;
	public static final int DEFAULT_PREF = 7;
	
	/**
	 * Static initializer for hash map
	 * Here we define the percentage of progress
	 * for each import step
	 */
	static {
		progress = new HashMap<>();
		progress.put( CAT_SHEET, 1 );
		progress.put( HIER_SHEET, 2 );
		progress.put( ATTR_SHEET, 2 );
		progress.put( TERM_SHEET, 25 );
		progress.put( TERM_ATTR_SHEET, 30 );
		progress.put( PARENT_SHEET, 30 );
		progress.put( NOTES_SHEET, 5 );
		progress.put( DEFAULT_PREF, 5 );
	}
	
	/**
	 * Get the progress related to the required import step.
	 * @param step
	 * @param maxProgress
	 * @return
	 */
	public static double getProgress ( int step, double maxProgress ) {
		return progress.get(step) * maxProgress / 100.00;
	}
}
