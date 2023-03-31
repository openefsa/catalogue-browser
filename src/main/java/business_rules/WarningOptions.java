package business_rules;

import java.io.PrintWriter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * class which contains all the options related to the warnings colors
 * 
 * @author shahaal
 * @author avonva
 *
 */
public class WarningOptions {

	private static final Logger LOGGER = LogManager.getLogger(WarningOptions.class);

	/*
	 * RGB values: the color of the semaphore in the three warning levels, the color
	 * of the log messages in the three warning levels the background color of the
	 * console which prints the warnings the font size of the messages
	 */
	int[] semNoWarnRGB, semLowWarnRGB, semHiWarnRGB, semErrorRGB, txtNoWarnRGB, txtLowWarnRGB, txtHiWarnRGB,
			txtErrorRGB, consoleBG = null;
	int fontSize = 14;

	/*
	 * SETTER METHODS
	 */
	public void setSemNoWarnRGB(int[] semNoWarnRGB) {
		this.semNoWarnRGB = semNoWarnRGB;
	}

	public void setSemLowWarnRGB(int[] semLowWarnRGB) {
		this.semLowWarnRGB = semLowWarnRGB;
	}

	public void setSemHiWarnRGB(int[] semHiWarnRGB) {
		this.semHiWarnRGB = semHiWarnRGB;
	}

	public void setSemErrorRGB(int[] semErrorRGB) {
		this.semErrorRGB = semErrorRGB;
	}

	public void setTxtNoWarnRGB(int[] txtNoWarnRGB) {
		this.txtNoWarnRGB = txtNoWarnRGB;
	}

	public void setTxtLowWarnRGB(int[] txtLowWarnRGB) {
		this.txtLowWarnRGB = txtLowWarnRGB;
	}

	public void setTxtHiWarnRGB(int[] txtHiWarnRGB) {
		this.txtHiWarnRGB = txtHiWarnRGB;
	}

	public void setTxtErrorRGB(int[] txtErrorRGB) {
		this.txtErrorRGB = txtErrorRGB;
	}

	public void setConsoleBG(int[] consoleBG) {
		this.consoleBG = consoleBG;
	}

	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
	}

	/*
	 * GETTER METHODS
	 */
	public int[] getSemNoWarnRGB() {
		return semNoWarnRGB;
	}

	public int[] getSemLowWarnRGB() {
		return semLowWarnRGB;
	}

	public int[] getSemHiWarnRGB() {
		return semHiWarnRGB;
	}
	
	public int[] getSemErrorRGB() {
		return semErrorRGB;
	}

	public int[] getTxtNoWarnRGB() {
		return txtNoWarnRGB;
	}

	public int[] getTxtLowWarnRGB() {
		return txtLowWarnRGB;
	}

	public int[] getTxtHiWarnRGB() {
		return txtHiWarnRGB;
	}
	
	public int[] getTxtErrorRGB() {
		return txtErrorRGB;
	}

	public int[] getConsoleBG() {
		return consoleBG;
	}

	public int getFontSize() {
		return fontSize;
	}

	/**
	 * Function to create the default warning color options
	 * 
	 * @param filename
	 */
	public void createDefaultWarnColorOptionsFile(String filename) {
		try {

			// write the default warning color options
			PrintWriter out = new PrintWriter(filename);

			// string builder to build the string ( or simply a string can be used... )
			StringBuilder sb = new StringBuilder();

			sb.append("SemaphoreNoWarn = 0;255;0\r\n" 
					+ "SemaphoreLowWarn = 255;255;0\r\n"
					+ "SemaphoreHighWarn = 255;165;0\r\n"
					+ "SemaphoreErrorWarn = 255;0;0\r\n"
					+ "TxtNoWarn = 0;255;0\r\n" 
					+ "TxtLowWarn = 255;255;0\r\n"
					+ "TxtHighWarn = 255;165;0\r\n" 
					+ "TxtErrorWarn = 255;0;0\r\n"
					+ "ConsoleBG = 0;90;150\r\n" 
					+ "WarnFontSize = 14\r\n");

			// write the string
			out.write(sb.toString());

			// close the connection
			out.close();
		} catch (Exception e) {
			LOGGER.error("Cannot create the file " + filename, e);
			e.printStackTrace();
		}
	}
}
