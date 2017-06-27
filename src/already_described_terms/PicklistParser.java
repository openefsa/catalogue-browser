package already_described_terms;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import catalogue.Catalogue;

/**
 * Parse a pick-list csv file to extract its information
 * The pick-list csv file is composed by 3 columns:
 * Level Pick-list elements and Picklist code
 * @author avonva
 *
 */
public class PicklistParser {

	private Catalogue catalogue;
	private String delim;
	private String currentLine;
	private BufferedReader reader;
	
	public PicklistParser( Catalogue catalogue, String filename, String delim ) {
		
		this.catalogue = catalogue;
		
		File file = new File( filename );
		
		if ( !file.exists() ) {
			System.err.println ( "The file " + filename + " does not exist" );
			return;
		}
		
		try {
			
			// prepare the reader
			reader = new BufferedReader( new FileReader( filename ) );
			
			// skip headers
			reader.readLine();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		this.delim = delim;
	}
	
	public boolean hasNext() throws IOException {
		
		currentLine = reader.readLine();
		
		return currentLine != null;
	}
	
	/**
	 * Parse a line and get the current picklist term
	 * @param inputFilename
	 * @param delim
	 * @throws IOException 
	 */
	public PicklistTerm nextTerm() {

		try {
			if ( !hasNext() ) {
				reader.close();
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// parse the current line
		StringTokenizer st = new StringTokenizer ( currentLine, delim );

		// if wrong number of tokens return
		if ( st.countTokens() < 3 ) {
			System.err.println ( "Wrong number of columns, expected 3, found : " + st.countTokens() );
			return null;
		}

		// get the picklist variables from the tokens
		int level = Integer.parseInt( st.nextToken() );
		String label = st.nextToken();
		String code = st.nextToken();

		// create a picklist and return it
		PicklistTerm pt = new PicklistTerm( catalogue, level, code, label );

		return pt;
	}
}
