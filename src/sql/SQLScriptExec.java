package sql;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.sql.SQLException;

import catalogue_browser_dao.DatabaseManager;

public class SQLScriptExec {

	private InputStream	_inputStream;
	private String dbURL;

	SQLScriptExec( String dbURL, String filename ) throws FileNotFoundException {
		_inputStream = new FileInputStream( filename );
		this.dbURL = dbURL;
	}

	public SQLScriptExec( String dbURL, InputStream inputStream ) throws FileNotFoundException {
		_inputStream = inputStream;
		this.dbURL = dbURL;
	}

	/*
	 * After you detect the start of a comment this procedure will continue
	 * reading the comment until the character couple star and slash are found
	 * return true if there are still data available else false
	 */

	private boolean readAndDiscardComment ( Reader reader ) throws IOException {

		int data = reader.read();
		boolean found = false;
		while ( ( data != -1 ) && ( ( (char) data ) != '*' ) && ( !found ) ) {
			if ( data != -1 ) {
				/* I have to check if there is the character '/' */
				data = reader.read();
				if ( data == -1 ) {
					return false;
				} else if ( data == '/' ) {
					found = true;
					continue;
				}
			} else {
				return false;
			}
		}
		return found;
	}

	/* return true if there are still data available else false */
	private boolean readAndDiscardLine ( Reader reader ) throws IOException {

		int data = reader.read();
		boolean found = false;
		// I scan the line until the end of line
		while ( ( data != -1 ) && ( data != 10 ) && ( !found ) ) {
			if ( data != -1 ) {
				/* I have to check if there is the character '/' */
				data = reader.read();
				if ( data == -1 ) {
					return false;
				}
			} else {
				return false;
			}
		}
		return found;
	}

	public void exec ( ) throws IOException {
		
		
		System.out.println("Reading");
		
		Reader reader = new InputStreamReader( _inputStream );
		/* SQL statement can be maximum 2000 characters */

		String bufSQL = "";
		int data = reader.read();

		boolean dataAvailable = true;

		while ( dataAvailable ) {

			while ( ( data != -1 ) && ( data != ';' ) && ( data != '/' ) ) {

				bufSQL = bufSQL + (char) data;

				data = reader.read();

			}

			/*
			 * If I stopped on a ; it means that I have to execute a command
			 * with what I have in the buffer
			 */

			if ( data == ';' ) {
				
				try {
					DatabaseManager.executeSQLStatement( dbURL, bufSQL );
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				dataAvailable = true;
				bufSQL = "";
				data = reader.read();
				continue;

			}

			/*
			 * I need to read another element to understand if it is a comment
			 * or not
			 */

			int nextData = reader.read();

			if ( ( nextData == -1 ) ) {
				dataAvailable = false;
				continue;
			}
			if ( ( nextData == '*' ) ) {
				dataAvailable = readAndDiscardComment( reader );
				continue;
			}

			if ( ( nextData == '/' ) ) {
				dataAvailable = readAndDiscardLine( reader );
				continue;
			}

			bufSQL = bufSQL + nextData;
		}

		reader.close();

	}

}
