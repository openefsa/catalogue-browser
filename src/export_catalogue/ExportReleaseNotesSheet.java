package export_catalogue;

import java.util.Collection;
import java.util.HashMap;

import org.apache.poi.ss.usermodel.Workbook;

import catalogue.Catalogue;
import catalogue_object.Mappable;
import sheet_converter.NotesSheetConverter;
import sheet_converter.SheetHeader;

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

		headers.put( "OP_NAME", new SheetHeader(0, NotesSheetConverter.OP_NAME_NODE ) );
		headers.put( "OP_DATE", new SheetHeader(1, NotesSheetConverter.OP_DATE_NODE ) );
		headers.put( "OP_INFO", new SheetHeader(2, NotesSheetConverter.OP_INFO_NODE ) );
		headers.put( "OP_GROUP_ID", new SheetHeader(3, NotesSheetConverter.OP_GROUP_NODE ) );
		return headers;
	}

	@Override
	public Collection<? extends Mappable> getData() {
		return catalogue.getReleaseNotes().getOperations();
	}
}
