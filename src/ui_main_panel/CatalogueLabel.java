<<<<<<< HEAD
package ui_main_panel;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolTip;

import catalogue.Catalogue;
import dcf_user.User;
import dcf_user.UserAccessLevel;
import dcf_user.UserListener;
import global_manager.GlobalManager;
import messages.Messages;
import ui_main_menu.FileActions;

/**
 * Class used to display the catalogue label and the tool tip for downloading
 * new versions of it (if any available online)
 * 
 * @author shahaal
 *
 */
public class CatalogueLabel implements Observer {

	private Composite mainComposite;
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
		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		mainComposite.setLayout(new GridLayout(3, false));

		// label which shows the label of the current opened catalogue
		label = new Label(mainComposite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// set the label font
		label.setFont(getLabelFont(4, SWT.ITALIC | SWT.BOLD));

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
		toolTip = new ToolTip(mainComposite.getShell(), SWT.ICON_INFORMATION | SWT.BALLOON);

		// make it invisible
		makeUpdateVisible(false);

		// label for notifying new catalogue version
		lblNewVersion = new Label(mainComposite, SWT.NONE);
		lblNewVersion.setText(Messages.getString("CatalogueLabel.UpdateLabel"));
		lblNewVersion.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED));
		lblNewVersion.setFont(getLabelFont(2, SWT.BOLD));

		// button which allow to download the new version
		btnUpdate = new Button(mainComposite, SWT.PUSH);
		btnUpdate.setText(Messages.getString("CatalogueLabel.UpdateButton"));

		btnUpdate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// download the last catalogue release and open it
				FileActions.downloadLastVersion(mainComposite.getShell(), catalogue, null);

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
			label.setText(Messages.getString("CatalogueLabel.EmptyLabel"));
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

		toolTip.setText(Messages.getString("CatalogueLabel.ToolTipTitle"));
		toolTip.setMessage(Messages.getString("CatalogueLabel.ToolTipMessage"));
		toolTip.setLocation(btnUpdate.toDisplay(16, 16));
		toolTip.setVisible(true);
		toolTip.setAutoHide(false);

		// start auto hide after other 5 seconds
		Display.getDefault().timerExec(10000, new Runnable() {

			@Override
			public void run() {

				// if the catalogue was changed or not set or tool tip disposed
				if (catalogue == null || !current.equals(catalogue.getCode()) || toolTip.isDisposed())
					return;

				toolTip.setAutoHide(true);
			}
		});
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
		else {
			makeUpdateVisible(false);
		}
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
=======
package ui_main_panel;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolTip;

import catalogue.Catalogue;
import dcf_user.User;
import dcf_user.UserAccessLevel;
import dcf_user.UserListener;
import global_manager.GlobalManager;
import messages.Messages;
import ui_main_menu.FileActions;

public class CatalogueLabel implements Observer {

	private Group composite;
	private Composite buttonComp;
	private Button updateBtn;
	private Label label;
	private Catalogue catalogue;
	private ToolTip toolTip;
	private boolean toolTipShown;

	/**
	 * Initialize and display the label in the parent composite
	 * 
	 * @param parent
	 */
	public CatalogueLabel(final Composite parent) {

		composite = new Group(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));

		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.exclude = true;
		composite.setVisible(false);
		composite.setLayoutData(gd);

		// label which shows the label of the current opened catalogue
		label = new Label(composite, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		// set the label font to italic and bold
		FontData fontData = Display.getCurrent().getSystemFont().getFontData()[0];

		Font font = new Font(Display.getCurrent(),
				new FontData(fontData.getName(), fontData.getHeight() + 5, SWT.ITALIC | SWT.BOLD));

		label.setFont(font);

		addUpdatePanel();

		refresh();

		registerForUserLevel();
	}

	private void addUpdatePanel() {

		toolTip = new ToolTip(composite.getShell(), SWT.ICON_INFORMATION | SWT.BALLOON);
		toolTipShown = false;

		buttonComp = new Composite(composite, SWT.NONE);
		buttonComp.setLayout(new GridLayout(2, false));

		GridData gd = new GridData();
		gd.exclude = true;
		buttonComp.setVisible(false);
		buttonComp.setLayoutData(gd);

		Label newVersionAvailable = new Label(buttonComp, SWT.NONE);
		newVersionAvailable.setText(Messages.getString("CatalogueLabel.UpdateLabel"));

		// set the label font to italic and bold
		FontData fontData = Display.getCurrent().getSystemFont().getFontData()[0];

		Font font = new Font(Display.getCurrent(),
				new FontData(fontData.getName(), fontData.getHeight() + 3, SWT.BOLD));

		newVersionAvailable.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_RED));

		newVersionAvailable.setFont(font);

		updateBtn = new Button(buttonComp, SWT.PUSH);
		updateBtn.setText(Messages.getString("CatalogueLabel.UpdateButton"));
		
		updateBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				// download the last catalogue release and open it
				FileActions.downloadLastVersion(composite.getShell(), catalogue, null);
				
			}
		});

	}

	/**
	 * Refresh composite each time the user level changes
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
	 * Set the default label
	 */
	public void setDefaultLabel() {
		label.setText(Messages.getString("CatalogueLabel.EmptyLabel"));
	}

	/**
	 * Set the label text using the catalogue information
	 * 
	 * @param catalogue
	 */
	public void setText(Catalogue catalogue) {

		this.catalogue = catalogue;

		String text = "";

		// display only the name if the catalogue is local
		if (catalogue.isLocal())
			text = catalogue.getLabel();
		else
			text = catalogue.getVersion() + " " + catalogue.getLabel();

		label.setText(text);
	}

	private void open() {
		this.composite.setVisible(true);
		((GridData) this.composite.getLayoutData()).exclude = false;

		// show update button and tool tip if there is an update
		if (addUpdateButton()) {

			this.buttonComp.setVisible(true);
			((GridData) this.buttonComp.getLayoutData()).exclude = false;
			final String current = this.catalogue.getCode();

			Display.getDefault().timerExec(5000, new Runnable() {

				@Override
				public void run() {

					// if the catalogue was changed
					if (!current.equals(catalogue.getCode()))
						return;

					if (toolTip.isDisposed() || toolTipShown)
						return;

					toolTipShown = true;

					toolTip.setText(Messages.getString("CatalogueLabel.ToolTipTitle"));
					toolTip.setMessage(Messages.getString("CatalogueLabel.ToolTipMessage"));
					toolTip.setLocation(updateBtn.toDisplay(16, 16));
					toolTip.setVisible(true);
					toolTip.setAutoHide(false);

					// start auto hide after other 5 seconds
					Display.getDefault().timerExec(10000, new Runnable() {

						@Override
						public void run() {

							// if the catalogue was changed
							if (!current.equals(catalogue.getCode()))
								return;

							if (toolTip.isDisposed())
								return;

							toolTip.setAutoHide(true);
						}
					});
				}
			});
		} else {
			this.buttonComp.setVisible(false);
			((GridData) this.buttonComp.getLayoutData()).exclude = true;
		}

		this.composite.getParent().layout();
	}

	private boolean addUpdateButton() {
		return catalogue != null && catalogue.hasUpdate() && !catalogue.isLastReleaseAlreadyDownloaded();
	}

	/**
	 * Refresh the label
	 */
	public void refresh() {
		if (catalogue != null)
			setText(catalogue);
		else
			setDefaultLabel();

		open();
	}

	/**
	 * Redraw the label
	 */
	public void redraw() {
		label.redraw();
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

			toolTipShown = false;

			// update the catalogue label
			refresh();
		}
	}
}
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
