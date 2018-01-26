package import_catalogue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;

import javax.xml.stream.XMLStreamException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import catalogue.Catalogue;
import naming_convention.Headers;
import open_xml_reader.ResultDataSet;
import open_xml_reader.WorkbookReader;
import progress_bar.IProgressBar;

/**
 * Import in a parallel way both the parent terms and the term attributes
 * contained in the term sheet.
 * @author avonva
 *
 */
public class QuickParentAttributesImporter extends QuickImporter {
	
	private static final Logger LOGGER = LogManager.getLogger(QuickParentAttributesImporter.class);
	
	private TermAttributeImporter taImp;
	private ParentImporter parentImp;
	private ImportException occurredEx;
	
	/**
	 * Initialize the importer.
	 * @param catalogue the catalogue in which we want to import the data
	 * @param workbookReader the excel reader
	 * @param termSheetName the name of the sheet of terms
	 * @param batchSize the size of the batches which will be used to import
	 * the data. See {@link WorkbookReader#setBatchSize(int)}.
	 * @throws SQLException
	 * @throws XMLStreamException 
	 * @throws IOException 
	 * @throws InvalidFormatException 
	 */
	public QuickParentAttributesImporter( Catalogue catalogue, WorkbookReader workbookReader, 
			String termSheetName, int batchSize ) throws SQLException, 
	InvalidFormatException, IOException, XMLStreamException {
		super(workbookReader, batchSize );
		
		workbookReader.processSheetName( Headers.TERM_SHEET_NAME );
		
		// create the importers
		taImp = new TermAttributeImporter( catalogue );
		parentImp = new ParentImporter( catalogue );
	}
	
	/**
	 * Set a progress bar for the term attribute importer
	 * @param progressBar
	 * @param maxProgress
	 */
	public void setAttributeProgressBar( IProgressBar progressBar, double maxProgress ) {
		taImp.setProgressBar(progressBar, workbookReader.getRowCount(), maxProgress);
	}
	
	/**
	 * Set a progress bar for the parent terms importer
	 * @param progressBar
	 * @param maxProgress
	 */
	public void setParentProgressBar( IProgressBar progressBar, double maxProgress ) {
		parentImp.setProgressBar(progressBar, workbookReader.getRowCount(), maxProgress);
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
	public void importData( final ResultDataSet rs ) throws ImportException {
		
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
					LOGGER.error("Cannot import result data set batch", e);
					return;
				}
				
				// import the dataset
				try {
					taImp.importData( clonedRs );
				} catch (ImportException e) {
					e.printStackTrace();
					LOGGER.error("Cannot import result dataset batch", e);
					occurredEx = e;
				}
				
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
		
		// if an exception occurred in the thread throw it
		if (occurredEx != null) {
			throw occurredEx;
		}
	}
}
