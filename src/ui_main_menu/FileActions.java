package ui_main_menu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import catalogue.Catalogue;
import dcf_user.User;
import messages.Messages;
import ui_main_panel.OldCatalogueReleaseDialog;
import utilities.GlobalUtil;

public class FileActions {
	
	public static void openCatalogue( Shell shell, Catalogue catalogue ) {
		
		User user = User.getInstance();
		
		if ( catalogue.isDeprecated() ) {
			
			int val = GlobalUtil.showDialog( shell, 
					catalogue.getLabel(), 
					Messages.getString("BrowserMenu.CatalogueDeprecatedMessage"), 
					SWT.ICON_WARNING | SWT.YES | SWT.NO );
			
			if ( val == SWT.NO )
				return;
		}
		
		// if the user is logged in we can check the updates
		else if ( user.isLogged() ) {
			
			// check if there is a catalogue update
			boolean hasUpdate = catalogue.hasUpdate();
			
			// check if the update was already downloaded
			boolean alreadyDownloaded = catalogue.isLastReleaseAlreadyDownloaded();

			// if there is a new version and we have not downloaded it yet
			// we warn the user that a new version is available
			if ( hasUpdate && !alreadyDownloaded ) {
				
				OldCatalogueReleaseDialog dialog = 
						new OldCatalogueReleaseDialog( shell, catalogue );
				
				dialog.open();
			}
		}
		else {
			
			// only for official catalogues
			if ( !catalogue.isLocal() ) {
				// if we are not logged in, simply warn the user that we cannot
				// be sure that this is the last release
				MessageBox mb = new MessageBox( shell, SWT.ICON_INFORMATION );
				mb.setText( Messages.getString("BrowserMenu.CatalogueReleaseInfoTitle") );
				mb.setMessage( Messages.getString("BrowserMenu.CatalogueReleaseInfoMessage") );
				mb.open();
			}
		}

		// open the catalogue when the dialog is closed
		GlobalUtil.setShellCursor( shell, SWT.CURSOR_WAIT );

		// open the catalogue
		catalogue.open();

		GlobalUtil.setShellCursor( shell, SWT.CURSOR_ARROW );
	}
}
