package dcf_manager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipInputStream;

import javax.xml.soap.AttachmentPart;
import javax.xml.soap.SOAPException;

import folder_zipper.FolderZipper;

/**
 * Class to manage soap attachments
 * @author avonva
 *
 */
public class AttachmentHandler {
	
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

		// if zipped return the zipped stream
		if ( isZipped ) {

			// unzip the input stream
			ZipInputStream zipStream = new ZipInputStream( attachment.getRawContent() );

			// get the next entry
			zipStream.getNextEntry();

			return zipStream;
		}
		else {  // else the standard stream
			return attachment.getRawContent();
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

		// unzip the input stream
		InputStream zipStream = readAttachment();

		// unzip the stream into the file
		FolderZipper.unzipStream( zipStream, filename );

		zipStream.close();
	}

	/**
	 * Write a standard attachment which is not encoded.
	 * @param attachment
	 * @param filename
	 * @throws SOAPException
	 * @throws IOException
	 */
	private void writeNonEncodedAttachment ( String filename ) throws SOAPException, IOException {

		InputStream input = readAttachment();

		// write the input stream into the output filename
		byte[] buffer = new byte[ input.available() ];
		input.read( buffer );

		File targetFile = new File( filename );
		OutputStream outStream = new FileOutputStream( targetFile );
		outStream.write( buffer );
		outStream.close();

		input.close();
	}
}
