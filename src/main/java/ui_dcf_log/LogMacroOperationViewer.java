package ui_dcf_log;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import dcf_log.DcfLog;
import i18n_messages.CBMessages;

/**
 * Viewer which shows all the Dcf Log generic information,
 * as the catalogue code, version, status, the transmission
 * date...
 * @author avonva
 *
 */
public class LogMacroOperationViewer {

	private Composite parent;
	private DcfLog log;
	
	public LogMacroOperationViewer( Composite parent, DcfLog log ) {
		this.parent = parent;
		this.log = log;
		display();
	}
	
	/**
	 * Display the information of the log
	 */
	public void display() {
		
		// catalogue code + version
		String info = CBMessages.getString("LogMacroOp.Catalogue") + " " +
				log.getCatalogueCode() + " " + log.getCatalogueVersion();
		addLabel ( info );
		
		// catalogue status
		info = CBMessages.getString("LogMacroOp.CatStatus") + " " +
				log.getCatalogueStatus();
		addLabel ( info );
		
		// macro op result
		info = CBMessages.getString("LogMacroOp.Result") + " " +
				log.getMacroOpResult();
		addLabel ( info );
		
		// transmission date
		info = CBMessages.getString("LogMacroOp.TrxDate") + " " +
				log.getTransmissionDate();
		addLabel ( info );
		
		// processing date
		info = CBMessages.getString("LogMacroOp.ProcDate") + " " +
				log.getProcessingDate();
		addLabel ( info );
	}
	
	/**
	 * Create a label into the parent composite
	 * @param text the label text
	 */
	private void addLabel ( String text ) {
		
		// macro operation result
		Label label = new Label ( parent, SWT.NONE );
		label.setText( text );
	}
}
