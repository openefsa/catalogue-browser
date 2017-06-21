package import_catalogue;

import java.sql.SQLException;
import java.util.HashMap;

import catalogue.Catalogue;
import open_xml_reader.ResultDataSet;
import open_xml_reader.WorkbookReader;

/**
 * Import in a parallel way both the parent terms and the term attributes
 * contained in the term sheet.
 * @author avonva
 *
 */
public class QuickParentAttributesImporter extends QuickImporter {
	
	private TermAttributeImporter taImp;
	private ParentImporter parentImp;
	
	/**
	 * Initialize the importer.
	 * @param catalogue the catalogue in which we want to import the data
	 * @param workbookReader the excel reader
	 * @param termSheetName the name of the sheet of terms
	 * @param batchSize the size of the batches which will be used to import
	 * the data. See {@link WorkbookReader#setBatchSize(int)}.
	 * @throws SQLException
	 */
	public QuickParentAttributesImporter( Catalogue catalogue, WorkbookReader workbookReader, 
			String termSheetName, int batchSize ) throws SQLException {
		super(workbookReader, termSheetName, batchSize );
		
		// create the importers
		taImp = new TermAttributeImporter( catalogue );
		parentImp = new ParentImporter( catalogue );
	}
	
	/**
	 * Manage new terms for the append function,
	 * if needed.
	 * @param newCodes
	 */
	public void manageNewTerms( HashMap<String, String> newCodes ) {
		this.taImp.manageNewTerms( newCodes );
		this.parentImp.manageNewTerms( newCodes );
	}
	
	@Override
	public void importData( final ResultDataSet rs ) {

		// import the term attribute sheet in parallel
		Thread thread = new Thread( new Runnable() {
			
			@Override
			public void run() {
				
				// copy result data set to perform
				// parallel actions
				ResultDataSet clonedRs;
				try {
					clonedRs = (ResultDataSet) rs.clone();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
					return;
				}
				
				// import the dataset
				taImp.importData( clonedRs );
				
				// close the dataset
				clonedRs.close();
			}
		});
		
		// start the thread
		thread.start();
		
		// import also the parent terms
		// in parallel to the term attributes
		parentImp.importData( rs );
		
		// wait for the term attribute process
		// to finish to guarantee that we can
		// proceed with the next batch
		try {
			thread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
