package ui_main_menu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import dcf_user.User.CatalogueStatus;
import i18n_messages.CBMessages;
import utilities.GlobalUtil;

/**
 * This class is used to display warnings when a
 * button of the {@link MainMenu} is pressed (if needed).
 * @author avonva
 *
 */
public class Warnings {

	Shell shell;
	
	public Warnings(Shell shell) {
		this.shell = shell;
	}
	
	/**
	 * Preliminary warnings for reserve operations
	 * @param problem
	 * @return true if a warning was raised
	 */
	public boolean reserve(CatalogueStatus problem) {
		
		String title = CBMessages.getString("Reserve.NotAllowedTitle");
		String msg = null;
		
		boolean showMessage = true;
		
		switch (problem) {
		case RESERVED_BY_OTHER:
			msg = CBMessages.getString("Reserve.ReservedByOther");
			break;
		case NOT_LAST_VERSION:
			msg = CBMessages.getString("Reserve.NotLast");
			break;
		case DEPRECATED:
			msg = CBMessages.getString("Reserve.Deprecated");
			break;
		case LOCAL:
			msg = CBMessages.getString("Reserve.Local");
			break;
		case INV_VERSION:
			msg = CBMessages.getString("Reserve.Invalid");
			break;
		default:
			showMessage = false;
			break;
		}
		
		if (showMessage)
			GlobalUtil.showDialog(shell, title, msg, SWT.ICON_ERROR);
		
		return showMessage;
	}
	
	/**
	 * Warnings for publish operations
	 * @param problem
	 * @return
	 */
	public boolean publish (CatalogueStatus problem) {
		
		String title = CBMessages.getString("Publish.NotAllowedTitle");
		String msg = null;
		
		boolean showMessage = true;
		
		switch (problem) {
		case RESERVED_BY_CURRENT:
		case RESERVED_BY_OTHER:
			msg = CBMessages.getString("Publish.Reserved");
			break;
		case NOT_LAST_VERSION:
			msg = CBMessages.getString("Publish.NotLast");
			break;
		case DEPRECATED:
			msg = CBMessages.getString("Publish.Deprecated");
			break;
		case LOCAL:
			msg = CBMessages.getString("Publish.Local");
			break;
		case INV_VERSION:
			msg = CBMessages.getString("Publish.Invalid");
			break;
		default:
			showMessage = false;
			break;
		}
		
		if (showMessage)
			GlobalUtil.showDialog(shell, title, msg, SWT.ICON_ERROR);
		
		return showMessage;
	}
}
