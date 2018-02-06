package ui_main_menu;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import dcf_user.User.CatalogueStatus;
import messages.Messages;
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
		
		String title = Messages.getString("Reserve.NotAllowedTitle");
		String msg = null;
		
		boolean showMessage = true;
		
		switch (problem) {
		case RESERVED_BY_OTHER:
			msg = Messages.getString("Reserve.ReservedByOther");
			break;
		case NOT_LAST_VERSION:
			msg = Messages.getString("Reserve.NotLast");
			break;
		case DEPRECATED:
			msg = Messages.getString("Reserve.Deprecated");
			break;
		case LOCAL:
			msg = Messages.getString("Reserve.Local");
			break;
		case INV_VERSION:
			msg = Messages.getString("Reserve.Invalid");
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
		
		String title = Messages.getString("Publish.NotAllowedTitle");
		String msg = null;
		
		boolean showMessage = true;
		
		switch (problem) {
		case RESERVED_BY_CURRENT:
		case RESERVED_BY_OTHER:
			msg = Messages.getString("Publish.Reserved");
			break;
		case NOT_LAST_VERSION:
			msg = Messages.getString("Publish.NotLast");
			break;
		case DEPRECATED:
			msg = Messages.getString("Publish.Deprecated");
			break;
		case LOCAL:
			msg = Messages.getString("Publish.Local");
			break;
		case INV_VERSION:
			msg = Messages.getString("Publish.Invalid");
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
