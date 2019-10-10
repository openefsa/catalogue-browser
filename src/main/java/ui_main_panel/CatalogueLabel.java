package ui_main_panel;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolTip;

import catalogue.Catalogue;
import dcf_user.User;
import dcf_user.UserAccessLevel;
import dcf_user.UserListener;
import global_manager.GlobalManager;
import i18n_messages.CBMessages;
import ui_main_menu.FileActions;

/**
 * Class used to display the catalogue label and the tool tip for downloading
 * new versions of it (if any available online)
 * 
 * @author shahaal
 *
 */
public class CatalogueLabel implements Observer {

	private Composite leftGroup;
	private Button btnUpdate;
	private Label label;
	private Catalogue catalogue;
	private ToolTip toolTip;
	private Label lblNewVersion;

	/**
	 * Initialise and display the label in the parent composite
	 * 
	 * @param parent
	 */
	public CatalogueLabel(final Composite parent) {

		// composite which contains cat name + update panel
		leftGroup = new Group(parent, SWT.NONE);
		leftGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		leftGroup.setLayout(new GridLayout(3, false));

		// label which shows the current opened catalogue
		label = new Label(leftGroup, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		label.setFont(getLabelFont(5, SWT.ITALIC | SWT.BOLD));

		// add the update panel
		addUpdatePanel();

		// refresh composite
		refresh();

		// check user level
		registerForUserLevel();
	}

	/**
	 * this panel shows the update catalogue label and button which allow to
	 * download the new version available
	 * 
	 */
	private void addUpdatePanel() {

		// tool tip to show when an update of the catalogue is available
		toolTip = new ToolTip(leftGroup.getShell(), SWT.ICON_INFORMATION | SWT.BALLOON);
		toolTip.setData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		// make it invisible
		makeUpdateVisible(false);

		Image image = new Image(leftGroup.getDisplay(), ClassLoader.getSystemResourceAsStream("icons/help.png"));
		
		// label for notifying new catalogue version
		lblNewVersion = new Label(leftGroup, SWT.NONE);
		lblNewVersion.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		// set the help icon
		lblNewVersion.setImage(image);
		
		// on mouse hover show the tool tip
		lblNewVersion.addListener(SWT.MouseHover, new Listener() {
			public void handleEvent(Event e) {
				open();
			}
		});

		// button which allow to download the new version
		btnUpdate = new Button(leftGroup, SWT.PUSH);
		btnUpdate.setText(CBMessages.getString("CatalogueLabel.UpdateButton"));
		btnUpdate.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		btnUpdate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// download the last catalogue release and open it
				FileActions.downloadLastVersion(leftGroup.getShell(), catalogue, null);

			}
		});

	}

	/**
	 * Refresh composite each time the user level changes
	 * 
	 */
	private void registerForUserLevel() {

		User.getInstance().addUserListener(new UserListener() {

			@Override
			public void connectionChanged(boolean connected) {
			}

			@Override
			public void userLevelChanged(UserAccessLevel newLevel) {

				Display.getDefault().asyncExec(new Runnable() {

					@Override
					public void run() {
						// refresh the label and the update button
						if (!label.isDisposed())
							refresh();
					}
				});

			}
		});
	}

	/**
	 * Set the label text using the catalogue information
	 * 
	 * @param catalogue
	 */
	public void setText(Catalogue catalogue) {

		// update the catalogue
		this.catalogue = catalogue;

		if (catalogue == null)
			label.setText(CBMessages.getString("CatalogueLabel.EmptyLabel"));
		else if (catalogue.isLocal())
			label.setText(catalogue.getLabel());
		else
			label.setText(catalogue.getVersion() + " " + catalogue.getLabel());

	}

	/**
	 * make the update button and the tool tip visible
	 * 
	 */
	private void open() {

		// make the update composite visible
		makeUpdateVisible(true);

		// get the catalogue code
		final String current = this.catalogue.getCode();

		// if the catalogue was changed or not set and tool tip not ready
		if (catalogue == null || !current.equals(catalogue.getCode()) || toolTip.isDisposed())
			return;

		toolTip.setText(CBMessages.getString("CatalogueLabel.ToolTipTitle"));
		toolTip.setMessage(CBMessages.getString("CatalogueLabel.ToolTipMessage"));
		toolTip.setLocation(lblNewVersion.toDisplay(16, 16));
		toolTip.setVisible(true);
		toolTip.setAutoHide(true);

	}

	/**
	 * check if to add or not the update button
	 * 
	 * @return
	 */
	private boolean canUpdateCatalogue() {
		return catalogue != null && catalogue.hasUpdate() && !catalogue.isLastReleaseAlreadyDownloaded()
				&& User.getInstance().isUserLevelDefined();
	}

	/**
	 * Refresh the label
	 * 
	 */
	public void refresh() {

		// set the catalogue label
		setText(catalogue);

		// show update button and tool tip if there is and can update otherwise hide
		if (canUpdateCatalogue())
			open();
		else
			makeUpdateVisible(false);
	}

	/**
	 * method used to set the font style and the font size
	 * 
	 * @author shahaal
	 * @param size
	 */
	private Font getLabelFont(int size, int style) {

		// set the font style
		FontData fontData = Display.getCurrent().getSystemFont().getFontData()[0];

		// set the font size
		Font font = new Font(Display.getCurrent(),
				new FontData(fontData.getName(), fontData.getHeight() + size, style));

		return font;
	}

	/**
	 * set the update label and button visible
	 * 
	 * @param visible
	 */
	private void makeUpdateVisible(boolean visible) {
		if (lblNewVersion != null && btnUpdate != null) {
			lblNewVersion.setVisible(visible);
			btnUpdate.setVisible(visible);
		}
	}

	@Override
	public void update(Observable o, Object arg) {

		// update current catalogue
		if (o instanceof GlobalManager) {

			if (arg instanceof Catalogue)
				this.catalogue = (Catalogue) arg;
			else
				this.catalogue = null;

			this.toolTip.setVisible(false);

			// update the catalogue label
			refresh();
		}
	}
}
