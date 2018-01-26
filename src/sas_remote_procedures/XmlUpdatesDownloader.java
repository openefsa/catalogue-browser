package sas_remote_procedures;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * Thread to download in background the .xml updates
 * file related to a {@link XmlUpdateFile} object.
 * @author avonva
 *
 */
public class XmlUpdatesDownloader extends Thread {
	
	private static final Logger LOGGER = LogManager.getLogger(XmlUpdatesDownloader.class);
	
	private Listener doneListener;
	
	private XmlUpdateFile xmlFilename;
	private File updatesFile;
	
	public XmlUpdatesDownloader( XmlUpdateFile xmlFilename ) {
		this.xmlFilename = xmlFilename;
	}
	
	@Override
	public void run() {
		
		// download the file
		try {
			updatesFile = xmlFilename.downloadXml( 5000 );
		} catch (IOException e) {
			e.printStackTrace();
			LOGGER.error("Cannot download xml", e);
		}
		
		// call the doneListener
		if ( doneListener != null ) {
			Event event = new Event();
			event.data = updatesFile;
			doneListener.handleEvent( event );
		}
	}
	
	public XmlUpdateFile getXmlFile() {
		return xmlFilename;
	}
	
	/**
	 * Get the File which points to the
	 * updates xml file
	 * @return
	 */
	public File getUpdatesFile() {
		return updatesFile;
	}
	
	/**
	 * Listener called when the thread finishes. The event.data
	 * contains the File which points to the downloaded .xml file.
	 * @param doneListener
	 */
	public void setDoneListener(Listener doneListener) {
		this.doneListener = doneListener;
	}
}
