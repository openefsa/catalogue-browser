package dcf_log_util;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

/**
 * Class used to parse a dcf log document and retrieve
 * all the {@link LogNode} contained in it.
 * @author avonva
 *
 */
public class DcfLogParser {

	private SAXParser saxParser;
	private LogParserHandler handler;
	
	/**
	 * Initialize the parser
	 */
	public DcfLogParser() {
		init();
	}

	/**
	 * Initialize the parser
	 */
	private void init() {
		
		// instantiate the SAX parser
		SAXParserFactory factory = SAXParserFactory.newInstance();

		try {
			saxParser = factory.newSAXParser();
		} catch (ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}

		// create the parser handler
		handler = new LogParserHandler();
	}

	/**
	 * Parse the log document and retrieve all the operation log nodes
	 * @param file
	 * @return
	 * @throws SAXException
	 * @throws IOException
	 */
	public DcfLog parse ( File file ) throws SAXException, IOException {
		
		saxParser.parse( file, handler );
		
		DcfLog log = handler.getDcfLog();
		log.setLogFilename( file.getName() );
		
		return log;
	}
}
