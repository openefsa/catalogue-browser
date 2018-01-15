package utilities;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import ui_main_panel.CatalogueBrowserMain;
import user_preferences.GlobalPreference;
import user_preferences.GlobalPreferenceDAO;

public class Log {

	private static PrintStream ps;
	
	public void close() {
		
		if (ps == null)
			return;
		
		ps.close();
	}
	
	/**
	 * Refresh logging state if the user set the preference
	 */
	public void refreshLogging() {

		this.close();
		
		GlobalPreferenceDAO prefDao = new GlobalPreferenceDAO();
		boolean active = prefDao.getPreferenceBoolValue( GlobalPreference.LOGGING, 
				false );
		
		// start logging if it was enabled
		if ( active ) {

			// create a logging file
			Calendar dCal = Calendar.getInstance();
			Format formatter;
			formatter = new SimpleDateFormat( "yyyy-MM-dd HH.mm.ss" );
			
			FileOutputStream os = null;
			try {
				os = new FileOutputStream( GlobalUtil.LOG_FILES_DIR_PATH
						+ "session=" + CatalogueBrowserMain.sessionId
						+ " date=" + formatter.format( dCal.getTime() )
						+ ".txt" );
			} catch ( FileNotFoundException e ) {
				e.printStackTrace();
			}

			// set the logging file as output for the standard error messages
			ps = new PrintStream( os );
			System.setErr( ps );
			System.setOut( ps );
		}
		else {
			// reset the standard error output
			System.setOut( new PrintStream(new FileOutputStream( FileDescriptor.out ) ) );
			System.setErr( new PrintStream(new FileOutputStream( FileDescriptor.err ) ) );
		}
	}
}
