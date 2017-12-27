package test;

import java.util.ArrayList;

import javax.xml.soap.SOAPException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import catalogue_browser_dao.DatabaseManager;
import dcf_manager.Dcf;
import dcf_pending_action.PendingActionListener;
import dcf_user.User;
import dcf_webservice.ReserveLevel;
import ui_main_panel.MainPanel;
import ui_main_panel.UpdateableUI;
import ui_pending_action_listener.DefaultListeners;
import ui_progress_bar.FormProgressBar;

public class ForcedReserve {

	public static boolean GO_ON = false;
	
	public static void main( String[] args ) throws InterruptedException, SOAPException {
		
		Display display = new Display();
		final Shell shell = new Shell ( display );
		
		DatabaseManager.startMainDB();
		
		final MainPanel panel = new MainPanel( shell );
		panel.initGraphics();
		
		shell.open();
		
		User.getInstance().login( "gibinda", "Ab123456", false);

		Dcf dcf = new Dcf();
		
		ArrayList<Catalogue> cats = dcf.getCataloguesList();
		
		// get basic effect catalogue
		Catalogue basicEffect = null;
		for ( Catalogue cat : cats ) {
			if ( cat.getCode().equals( "MTX" ) )
				basicEffect = cat;
		}
		
		// create a progress bar for the possible import process
		/*final FormProgressBar progressBar = new FormProgressBar( shell, 
				"Download basic effect", false, SWT.TITLE );
		
		// download the catalogue
		CatalogueDownloader down = new CatalogueDownloader( basicEffect );
		down.setProgressBar( progressBar );
		down.start();
		
		while ( !down.isFinished() ) {
			Thread.sleep( 1000 );
		}

		basicEffect.open();*/
		
		PendingActionListener listener = 
				DefaultListeners.getDefaultPendingListener( 
						new UpdateableUI() {
			
			@Override
			public void updateUI(Object data) {
				panel.refresh();
			}
			
			@Override
			public Shell getShell() {
				return shell;
			}
		});
		
		
		final FormProgressBar progressBar = new FormProgressBar( shell, 
				"internal version", false, SWT.TITLE );
		
		dcf.setProgressBar( progressBar );
		
		dcf.reserveBG( basicEffect, ReserveLevel.MINOR, 
				"sfihaifhsiahfiahfia", 
				listener );
		
		
		
		MessageBox mb = new MessageBox ( shell, SWT.OK );
		int val = mb.open();
		
		GO_ON = true;
		
		while ( !shell.isDisposed() )
			if ( !display.readAndDispatch() )
				display.sleep();
	}
}
