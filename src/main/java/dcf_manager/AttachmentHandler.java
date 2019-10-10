package dcf_manager;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.ZipInputStream;

import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPException;

import zip_manager.ZipManager;

/**
 * Class to manage soap attachments
 * @author avonva
 * @author shahaal
 *
 */
public class AttachmentHandler implements Closeable {
	
	private ArrayList<InputStream> inputToClose;
	private AttachmentPart attachment;
	private boolean isZipped;
	
	/**
	 * Initialize the handler giving the attachment to
	 * be analyzed and if the attachment isZipped or not
	 * Be careful in setting isZipped correctly, otherwise
	 * weird results will appear (perhaps errors also)
	 * @param attachment the attachment to be analyzed
	 * @param isZipped true if zipped attachment
	 */
	public AttachmentHandler( AttachmentPart attachment, boolean isZipped ) {
		this.attachment = attachment;
		this.isZipped = isZipped;
		this.inputToClose = new ArrayList<>();
	}

	/**
	 * Read the attachment and get its content as input stream
	 * @param attachment
	 * @param zipped
	 * @return
	 * @throws SOAPException
	 * @throws IOException
	 */
	public InputStream readAttachment () throws SOAPException, IOException {

		InputStream attachmentStream = attachment.getRawContent();
		inputToClose.add( attachmentStream );
		
		// if zipped return the zipped stream
		if ( isZipped ) {

			// unzip the input stream
			ZipInputStream zipStream = new ZipInputStream( attachmentStream );

			inputToClose.add( zipStream );
			
			// get the next entry
			zipStream.getNextEntry();

			return zipStream;
		}
		else {  // else the standard stream
			return attachmentStream;
		}
	}

	/**
	 * Write an attachment to a file. If the attachment is zipped => set zipped to true,
	 * otherwise false.
	 * @param attachment
	 * @param filename
	 * @throws SOAPException 
	 * @throws IOException 
	 */
	public void writeAttachment( String filename ) throws SOAPException, IOException {

		// if zipped unzipped it and write
		if ( isZipped ) {
			writeZippedAttachment ( filename );
		}
		else {  // write without unzipping
			writeNonEncodedAttachment ( filename );
		}
	}

	/**
	 * Write an attachment which is zipped
	 * @param attachment
	 * @param filename
	 * @throws SOAPException
	 * @throws IOException
	 */
	private void writeZippedAttachment ( String filename ) throws SOAPException, IOException {

		// unzip the stream into the file
		ZipManager.unzipStream( readAttachment(), new File(filename) );
		
		// close zip stream and nested stream
		close();
	}

	/**
	 * Write a standard attachment which is not encoded.
	 * @author shahaal
	 * @author avonva
	 * @param attachment
	 * @param filename
	 * @throws SOAPException
	 * @throws IOException
	 */
	private void writeNonEncodedAttachment ( String filename ) throws SOAPException, IOException {

		//for avoiding that the input string is not closed
		try(InputStream input = readAttachment()){

			// write the input stream into the output filename
			byte[] buffer = new byte[ input.available() ];
			input.read( buffer );
	
			File targetFile = new File( filename );
			OutputStream outStream = new FileOutputStream( targetFile );
			outStream.write( buffer );
			
			// close stream
			outStream.close();
	
			// close input stream of read attachment
			close();
		}
	}
	
	/**
	 * Close the streams
	 * @throws IOException
	 */
	public void close() throws IOException {
		
		for ( InputStream input : inputToClose ) {
			input.close();
		}
		
		// remove all objects
		inputToClose.clear();
	}
}
