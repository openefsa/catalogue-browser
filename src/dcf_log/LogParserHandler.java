package dcf_log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import dcf_webservice.DcfResponse;

/**
 * Handler used to parse a dcf log document.
 * @author avonva
 *
 */
public class LogParserHandler extends DefaultHandler {

	// builders to create objects step by step
	// while reading data from the xml file
	private DcfLogBuilder logBuilder;
	private LogNodeBuilder nodeBuilder;
	private LogNodeBuilder validationBuilder;
	
	private StringBuilder lastContent;
	
	// booleans to know in which xml block we are
	private boolean operationsBlock;
	private boolean macroLogsBlock;
	private boolean validationErrorsBlock;

	/**
	 * Initialize the log parser handler memory
	 */
	public LogParserHandler() {
		lastContent = new StringBuilder();
		logBuilder = new DcfLogBuilder();
		operationsBlock = false;
		macroLogsBlock = false;
		validationErrorsBlock = false;
	}

	// when a node is encountered
	public void startElement(String uri, String localName, String qName, 
			Attributes attributes) throws SAXException {

		// remove content of the node from the string
		// before opening a new node
		lastContent.setLength(0);

		switch ( qName ) {

		// entering operations block
		case LogXmlNodes.OPERATIONS_BLOCK:
			operationsBlock = true;
			break;

			// new operation, create a new log node
		case LogXmlNodes.OPERATION_BLOCK:
			nodeBuilder = new LogNodeBuilder();
			break;

			// entering the macro op logs block
		case LogXmlNodes.MACRO_OP_LOGS_BLOCK:
			macroLogsBlock = true;
			break;
			
		case LogXmlNodes.VALIDATION_ERROR:
			validationErrorsBlock = true;
			validationBuilder = new LogNodeBuilder();
			break;
		
		default:
			break;
		}
	}

	@Override
	// when the end of a node is encountered
	public void endElement(String uri, String localName, String qName) throws SAXException {

		analyzeMacroOperationsBlock( qName );
		analyzeOperationsBlock( qName );
		analyzeValidationErrorsBlock ( qName );
		
		switch ( qName ) {
		
		// end operations block
		case LogXmlNodes.OPERATIONS_BLOCK:
			operationsBlock = false;
			break;
			
		// end operation
		case LogXmlNodes.OPERATION_BLOCK:
			// add the single log node to the entire Log document
			logBuilder.addLogNode( nodeBuilder.build() );
			nodeBuilder = null;
			break;
			
		// end macro operations log block
		case LogXmlNodes.MACRO_OP_LOGS_BLOCK:
			macroLogsBlock = false;
			break;

		case LogXmlNodes.VALIDATION_ERROR:
			validationErrorsBlock = false;
			logBuilder.addLogNode( validationBuilder.build() );
			validationBuilder = null;
			break;
			
		default:
			break;
		}

		// remove content of the node from the string
		lastContent.setLength(0);
	}

	/**
	 * Analyze the macro operation block of the log document
	 * which is, the first part before the log nodes.
	 * @param qName
	 */
	private void analyzeMacroOperationsBlock ( String qName ) {

		switch ( qName ) {
		case LogXmlNodes.ACTION:
			logBuilder.setAction( getValue() );
			break;
		case LogXmlNodes.TRANSMISSION_DATE:
			logBuilder.setTransmissionDate( getValue() );
			break;
		case LogXmlNodes.PROCESSING_DATE:
			logBuilder.setProcessingDate( getValue() );
			break;
		case LogXmlNodes.UPLOADED_FILENAME:
			logBuilder.setUploadedFilename( getValue() );
			break;
		case LogXmlNodes.CATALOGUE_CODE:
			logBuilder.setCatalogueCode( getValue() );
			break;
		case LogXmlNodes.CATALOGUE_VERSION:
			logBuilder.setCatalogueVersion( getValue() );
			break;
		case LogXmlNodes.CATALOGUE_STATUS:
			logBuilder.setCatalogueStatus( getValue() );
			break;
		case LogXmlNodes.MACRO_OP_NAME:
			logBuilder.setMacroOpName( getValue() );
			break;
		case LogXmlNodes.MACRO_OP_RESULT:
			logBuilder.setMacroOpResult( getValue() );
			break;
		case LogXmlNodes.MACRO_OP_LOG:
			
			// stop if we are not in the macro logs block
			if ( !macroLogsBlock )
				break;
			
			// otherwise add the macro operation log
			logBuilder.addMacroOpLog( getValue() );
			break;
		
		default:
			break;
		}
	}

	/**
	 * Analyze the validation errors blocks
	 * @param qName
	 */
	private void analyzeValidationErrorsBlock ( String qName ) {
		
		if ( !validationErrorsBlock )
			return;
		
		validationBuilder.setResult( DcfResponse.ERR );
		validationBuilder.setName( LogXmlNodes.VALIDATION_ERROR );
		validationBuilder.addOpLog( getValue() );
	}
	
	/**
	 * Analyze the log nodes inside the operations block
	 * @param qName
	 */
	private void analyzeOperationsBlock( String qName ) {

		// if we are not in the operations block
		// we do nothing
		if ( !operationsBlock )
			return;

		switch ( qName ) {

			// set the name
		case LogXmlNodes.OP_NAME:
			nodeBuilder.setName( getValue() );
			break;

			// set the result
		case LogXmlNodes.OP_RESULT:
			nodeBuilder.setResult( getValue() );
			break;

			// add the operation log
		case LogXmlNodes.OP_LOG:
			nodeBuilder.addOpLog( getValue() );
			break;
			
		default:
			break;
		}
	}

	/**
	 * Get the current value
	 * @return
	 */
	private String getValue() {
		String value = lastContent.toString();
		lastContent.setLength(0);
		return value;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {

		// ADD CONTENT TO LAST CONTENT! because the parser sometimes gives only
		// a small piece of the value and not the entire value in a single call!!!

		String newPiece = new String(ch, start, length);

		// add the new piece if it is not the new line
		// get the data only if we are in a root node
		if ( !newPiece.equals( "\n" ) )
			lastContent.append( newPiece );
	}

	/**
	 * Get the parsed log nodes
	 * @return
	 */
	public DcfLog getDcfLog() {
		return logBuilder.build();
	}
}
