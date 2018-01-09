package dcf_webservice;

import java.io.File;
import java.io.IOException;

import javax.xml.soap.SOAPException;

import catalogue.Catalogue;
import dcf_manager.Dcf.DcfType;
import dcf_pending_action.PendingUploadData;
import soap.UploadCatalogueFile;
import user.IDcfUser;

/**
 * Web service to upload the local catalogue changes
 * to the dcf, in order to make them official.
 * @author avonva
 *
 */
public class UploadData extends UploadCatalogueFile {

	private IDcfUser user;
	private DcfType type;
	
	/**
	 * Create an upload data request.
	 * @param type
	 * @param filename the xml attachment containing all the
	 * dcf changes which need to be uploaded
	 */
	public UploadData(IDcfUser user, DcfType type) {
		super(user);
		this.user = user;
		this.type = type;
	}
	
	/**
	 * Start the upload data process
	 * @param catalogue the catalogue we want to upload
	 * @param filename the xml attachment which contains all the
	 * changes which need to be uploaded
	 * @throws SOAPException 
	 * @throws IOException 
	 */
	public PendingUploadData uploadData(Catalogue catalogue, File file) 
			throws SOAPException, IOException {
		
		System.out.println("upload data: file=" + file.getAbsolutePath() 
			+ ";catalogue=" + catalogue);
		
		String logCode = this.send(file);
		
		// create a pending upload data object
		PendingUploadData pud = PendingUploadData.addPendingUploadData(logCode, catalogue, 
				user.getUsername(), type);

		return pud;
	}
}
