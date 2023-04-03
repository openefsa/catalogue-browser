package business_rules;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.StringTokenizer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jface.resource.FontDescriptor;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;

import global_manager.GlobalManager;
import utilities.GlobalUtil;

/**
 * This class is used to manage all the software related to the warnings in the
 * describe function In particular, it is necessary to initialise it with a
 * table viewer of the warnings and a semaphore (canvas) in order to use this
 * class. Example: TableViewer warningTable = ... Canvas semaphore = ...
 * WarningUtils warnUtils = new WarningUtils( warningTable, semaphore );
 * 
 * Calling from WarningUtils the method refreshWarningsTable will check if the
 * full code of a term raise warnings or not ( and possibly it shows the related
 * warnings in the warningTable ).
 * 
 * @author avonva
 * @author shahaal
 *
 */

public class WarningUtil extends TermRules {

	private static final Logger LOGGER = LogManager.getLogger(WarningUtil.class);

	// log console
	private TableViewer warningsTable;

	// semaphore
	private Canvas semaphore;

	// maintain the current warningLevel
	private WarningLevel currentWarningLevel = WarningLevel.NONE;

	/**
	 * constructor
	 * 
	 * @param warningTable
	 * @param semaphore
	 */
	public WarningUtil(TableViewer warningTable, Canvas semaphore) {

		this.warningsTable = warningTable;
		this.semaphore = semaphore;

		// get an instance of the global manager
		GlobalManager manager = GlobalManager.getInstance();

		// get the current catalogue
		currentCat = manager.getCurrentCatalogue();

		// TODO load file data only once
		loadFileData();
	}

	private void loadFileData() {

		forbiddenProcesses = loadForbiddenProcesses(GlobalUtil.getBRData());
		warnOptions = loadWarningOptions(GlobalUtil.getBRColors());
		warningMessages = loadWarningMessages(GlobalUtil.getBRMessages());
	}

	/**
	 * Refresh the warning table, that is, remove all the warnings and recompute
	 * them starting from the fullCode of the term. Examples of full code: A0DPP or
	 * A0DPP#F01.A0FGM or A0DPP#F01.A0FGM$F04.A000J
	 * 
	 * @param fullCode: the full code of a term (IMPORTANT: without the implicit
	 *                  facets code if enabled!)
	 */
	public void refreshWarningsTable(String fullCode) {

		//////////////////// GRAPHICS UPDATE
		// reset the warning messages and level
		resetWarningState();
		// refresh the graphics ( font and colours )
		refreshWarningTableGraphics();

		//////////////////// CHECKS
		// execute all the warning checks
		performWarningChecks(fullCode, false, false);
	}

	/**
	 * Reset the warnings contents and level
	 */
	private void resetWarningState() {
		// remove all the warnings
		warningsTable.getTable().removeAll();
		// reset the current warning level
		currentWarningLevel = WarningLevel.NONE;
	}

	/**
	 * Refresh the graphics of the warning table accordingly to the warning options
	 */
	private void refreshWarningTableGraphics() {

		// set the font size of the warnings table accordingly to the warning options
		FontDescriptor descriptor = FontDescriptor.createFrom(warningsTable.getTable().getFont())
				.setHeight(warnOptions.getFontSize());

		warningsTable.getTable().setFont(descriptor.createFont(Display.getCurrent()));

		// set the background colour of the table accordingly to the warning options
		int[] rgb = warnOptions.getConsoleBG();
		warningsTable.getTable().setBackground(new Color(Display.getCurrent(), rgb[0], rgb[1], rgb[2]));

		// refresh the graphical elements of the table
		warningsTable.refresh();
	}

	/**
	 * Print a warning into the warningsTable It appends the date time to the
	 * message Update the current warning level to the highest retrieved until now
	 * Update the semaphore and text accordingly to the warning level
	 * 
	 * @param event:             the event which causes the printWarning
	 * @param postMessageString: a string which is attached to the end of the
	 *                           message (used for additional info)
	 * @param attachDatetime:    should the datetime be attached at the end of the
	 *                           message?
	 */
	protected void printWarning(WarningEvent event, String postMessageString, boolean attachDatetime, boolean stdOut) {

		// create the warning message to be printed
		String message = createMessage(event, postMessageString, attachDatetime);

		// get the warning levels for making colours
		WarningLevel semaphoreLevel = getSemaphoreLevel(event);
		WarningLevel textWarningLevel = getTextLevel(event);

		// if graphical object are not used
		if (warningsTable == null || semaphore == null)
			return;

		// print the message into the warnings table
		warningsTable.add(message);

		// scroll the table to the new message
		warningsTable.reveal(message);

		// get the index of the last inserted element (in the table)
		int lastElementIndex = warningsTable.getTable().getItemCount() - 1;

		// get the warning color (related to the warning level)
		Device device = Display.getCurrent();
		Color warningColor; // semaphore color
		Color txtColor; // message color in the console
		int[] rgb;

		// semaphore colour based on warning level
		switch (semaphoreLevel) {
		case HIGH:
			rgb = warnOptions.getSemHiWarnRGB();
			warningColor = new Color(device, rgb[0], rgb[1], rgb[2]);
			break;
		case LOW:
			rgb = warnOptions.getSemLowWarnRGB();
			warningColor = new Color(device, rgb[0], rgb[1], rgb[2]);
			break;
		case NONE:
			rgb = warnOptions.getSemNoWarnRGB();
			warningColor = new Color(device, rgb[0], rgb[1], rgb[2]);
			break;
		default:
			rgb = warnOptions.getSemErrorRGB();
			warningColor = new Color(device, rgb[0], rgb[1], rgb[2]);
		}

		// text colour based on warning level
		switch (textWarningLevel) {
		case HIGH:
			rgb = warnOptions.getTxtHiWarnRGB();
			txtColor = new Color(device, rgb[0], rgb[1], rgb[2]);
			break;
		case LOW:
			rgb = warnOptions.getTxtLowWarnRGB();
			txtColor = new Color(device, rgb[0], rgb[1], rgb[2]);
			break;
		case NONE:
			rgb = warnOptions.getTxtNoWarnRGB();
			txtColor = new Color(device, rgb[0], rgb[1], rgb[2]);
			break;
		default:
			rgb = warnOptions.getTxtErrorRGB();
			txtColor = new Color(device, rgb[0], rgb[1], rgb[2]);
		}

		// update the text colour accordingly to the warning colour
		warningsTable.getTable().getItems()[lastElementIndex].setForeground(txtColor);

		// if the warning level of this message is greater than or equal the previous
		if (semaphoreLevel.ordinal() >= currentWarningLevel.ordinal()) {
			// update the currentWarningLevel
			currentWarningLevel = semaphoreLevel;
			// change the colour of the semaphore
			semaphore.setBackground(warningColor);
		}
	}
	
	/**
	 * check if high warnings are present
	 */
	protected boolean highWarningsPresent() {
		return currentWarningLevel.ordinal()>1;
	}

	/**
	 * Parse the file of the warning options and load into memory all the color and
	 * font options required
	 * 
	 * @param filename
	 * @return
	 */
	private WarningOptions loadWarningOptions(String filename) {
		try {

			WarningOptions options = new WarningOptions();

			File file = new File(filename);
			if (!file.exists())
				options.createDefaultWarnColorOptionsFile(filename);

			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(filename);

			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);

			String line;

			// while there is a line to be red
			while ((line = bufferedReader.readLine()) != null) {

				// remove white spaces
				line = line.replace(" ", "");

				// analyze the line tokens
				StringTokenizer st = new StringTokenizer(line, "=");

				// get the current field
				String fieldName = st.nextToken();

				// font size is the only field which is not an RGB value
				if (!fieldName.equals("WarnFontSize")) {

					// get the RGB values
					int[] rgb = parseRGB(st.nextToken(), ";");

					// add them to the options
					switch (fieldName) {
					case "SemaphoreNoWarn":
						options.setSemNoWarnRGB(rgb);
						break;
					case "SemaphoreLowWarn":
						options.setSemLowWarnRGB(rgb);
						break;
					case "SemaphoreHighWarn":
						options.setSemHiWarnRGB(rgb);
						break;
					case "SemaphoreErrorWarn":
						options.setSemErrorRGB(rgb);
						break;
					case "TxtNoWarn":
						options.setTxtNoWarnRGB(rgb);
						break;
					case "TxtLowWarn":
						options.setTxtLowWarnRGB(rgb);
						break;
					case "TxtHighWarn":
						options.setTxtHiWarnRGB(rgb);
						break;
					case "TxtErrorWarn":
						options.setTxtErrorRGB(rgb);
						break;
					case "ConsoleBG":
						options.setConsoleBG(rgb);
						break;
					}
				} else { // font size, parse the integer
					try {
						options.setFontSize(Integer.parseInt(st.nextToken()));
					} catch (Exception e) {
						LOGGER.error("Error parsing font size in warningColors options.", e);
						e.printStackTrace();
					}
				}
			}

			// Close the connection
			bufferedReader.close();

			return (options);

		} catch (Exception e) {
			LOGGER.error(filename + " not found.", e);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Function to parse RGB values separated by a delim character
	 * 
	 * @param line
	 * @param delim
	 * @return
	 */
	static int[] parseRGB(String line, String delim) {

		StringTokenizer st = new StringTokenizer(line, delim);

		// three numbers have to be present for RGB coding
		if (st.countTokens() != 3)
			return null;

		try {
			// get the RGB values

			String token = st.nextToken();
			int red = Integer.parseInt(token);

			token = st.nextToken();
			int green = Integer.parseInt(token);

			token = st.nextToken();
			int blue = Integer.parseInt(token);

			return (new int[] { red, green, blue });
		} catch (Exception e) {
			LOGGER.error("ERROR IN PARSING RGB VALUES", e);
			e.printStackTrace();
			return null;
		}
	}

}
