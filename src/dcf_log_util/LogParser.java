package dcf_log_util;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import catalogue_object.Status;

/**
 * Class to parse dcf xml logs. We can check if the operation result is OK or not.
 * @author avonva
 *
 */
public class LogParser {

	// the code of a correct reserve operation which has to
	// be retrieved in the macroOperationResult node of the log
	private static final String CORRECT_OPERATION_CODE = "OK";
	
	// the name of the node which contains the status of the catalogue
	private static final String CATALOGUE_STATUS_NODE_NAME = "catalogueStatus";
	
	// The name of the node which contains the operation result code in the log
	private static final String OPERATION_RESULT_NODE_NAME = "macroOperationResult";
	
	// the log data file
	private Document log;
	
	/**
	 * Initialize the data for the parser
	 * @param log
	 */
	public LogParser( Document log ) {
		this.log = log;
	}
	
	/**
	 * Check if the current operation was successful or not given its log
	 * xml DOM document.
	 * @param log
	 * @return
	 */
	public boolean isOperationCorrect () {
		return getOperationResult().equalsIgnoreCase( CORRECT_OPERATION_CODE );
	}
	
	/**
	 * Get the result of the current soap operation given a log file
	 * We have a code that show that (OK if everything went well)
	 * @param log, the xml DOM document of the log
	 * @return
	 */
	public String getOperationResult() {
		return getFirstLevelNodeValue( OPERATION_RESULT_NODE_NAME );
	}
	

	/**
	 * Get the catalogue status contained in the log.
	 * @return the {@link Status} object containing the catalogue status
	 */
	public Status getCatalogueStatus() {
		return new Status ( getFirstLevelNodeValue ( CATALOGUE_STATUS_NODE_NAME ) );
	}
	
	/**
	 * Get the value of a node in the first xml level
	 * of the log
	 * @param nodeName
	 * @return the node value
	 */
	private String getFirstLevelNodeValue( String nodeName ) {

		// explore the xml nodes contained in the log
		// and search for the macro op result node (it contains
		// OK if everything went well)
		NodeList elements = log.getFirstChild().getChildNodes();

		String value = "";

		for ( int i = 0; i < elements.getLength(); i++ ) {

			Node current = elements.item( i );

			// search for the node name
			if ( !current.getNodeName().equals( nodeName ) )
				continue;

			// get the content of the node
			value = current.getTextContent();
		}
		return value;
	}
}

