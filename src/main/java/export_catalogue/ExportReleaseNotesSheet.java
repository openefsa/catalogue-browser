package export_catalogue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Workbook;

import catalogue.Catalogue;
import catalogue.ReleaseNotesOperation;
import catalogue_object.Mappable;
import naming_convention.Headers;
import sheet_header.SheetHeader;

/**
 * Export the catalogue release note into the catalogue release note sheet.
 * @author avonva
 *
 */
public class ExportReleaseNotesSheet extends SheetWriter {

	private Catalogue catalogue;
	
	public ExportReleaseNotesSheet( Catalogue catalogue, 
			Workbook workbook, String sheetName ) {
		
		super(workbook, sheetName);
		this.catalogue = catalogue;
	}

	@Override
	public HashMap<String, SheetHeader> getHeaders() {
		
		HashMap<String, SheetHeader> headers = new HashMap<>();

		headers.put( "OP_NAME", new SheetHeader(0, Headers.OP_NAME ) );
		headers.put( "OP_DATE", new SheetHeader(1, Headers.OP_DATE ) );
		headers.put( "OP_INFO", new SheetHeader(2, Headers.OP_INFO ) );
		headers.put( "OP_GROUP_ID", new SheetHeader(3, Headers.OP_GROUP ) );
		return headers;
	}

	@Override
	public Collection<? extends Mappable> getData() {
		
		Collection<ReleaseNotesOperation> out = new ArrayList<>();
		
		if ( catalogue.getReleaseNotes() != null )
			out = catalogue.getReleaseNotes().getOperations();

		return out;
	}
}
