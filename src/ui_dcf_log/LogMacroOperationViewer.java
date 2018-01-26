package ui_dcf_log;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import dcf_log.DcfLog;
import messages.Messages;

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
		String info = Messages.getString("LogMacroOp.Catalogue") + " " +
				log.getCatalogueCode() + " " + log.getCatalogueVersion();
		addLabel ( info );
		
		// catalogue status
		info = Messages.getString("LogMacroOp.CatStatus") + " " +
				log.getCatalogueStatus();
		addLabel ( info );
		
		// macro op result
		info = Messages.getString("LogMacroOp.Result") + " " +
				log.getMacroOpResult();
		addLabel ( info );
		
		// transmission date
		info = Messages.getString("LogMacroOp.TrxDate") + " " +
				log.getTransmissionDate();
		addLabel ( info );
		
		// processing date
		info = Messages.getString("LogMacroOp.ProcDate") + " " +
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
