package catalogue_object;

import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Class to handle strings which host several values in a dollar separated way
 * @author avonva
 *
 */
public class RepeatableParser {
	
	public static ArrayList<String> getRepeatableValues ( String repeatableValues ) {
		
		ArrayList<String> values = new ArrayList<>();
		
		StringTokenizer st = new StringTokenizer( repeatableValues, "$" );
		
		while ( st.hasMoreTokens() ) {
			values.add( st.nextToken() );
		}
		
		return values;
	}
}
