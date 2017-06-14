package dcf_webservice;

import java.io.File;

import catalogue.Catalogue;
import dcf_manager.Dcf.DcfType;
import dcf_pending_action.PendingUploadData;
import dcf_user.User;

/**
 * Web service to upload the local catalogue changes
 * to the dcf, in order to make them official.
 * @author avonva
 *
 */
public class UploadData extends UploadCatalogueFile {

	private Catalogue catalogue;
	private String filename;
	
	/**
	 * Create an upload data request.
	 * @param type
	 * @param filename the xml attachment containing all the
	 * dcf changes which need to be uploaded
	 */
	public UploadData( DcfType type ) {
		super( type );
	}
	
	/**
	 * Start the upload data process
	 * @param catalogue the catalogue we want to upload
	 * @param filename the xml attachment which contains all the
	 * changes which need to be uploaded
	 */
	public PendingUploadData uploadData ( Catalogue catalogue, String filename  ) {
		this.catalogue = catalogue;
		this.filename = filename;
		
		System.out.println( "upload data: filename=" + filename + ";catalogue=" + catalogue );
		
		PendingUploadData pud = (PendingUploadData) upload();
		return pud;
	}

	@Override
	public CatalogueAttachment getAttachment() {

		// get the attachment absolute path
		File file = new File ( filename );
		String path = file.getAbsolutePath();
		
		// create the attachment with path type
		CatalogueAttachment att = new CatalogueAttachment( 
				AttachmentType.FILE_PATH, path );
		
		return att;
	}

	@Override
	public Object processResponse(String logCode) {
		
		// create a pending upload data object
		PendingUploadData pud = PendingUploadData.addPendingUploadData ( logCode, catalogue, 
				User.getInstance().getUsername() );
		
		// return it
		return pud;
	}
}
