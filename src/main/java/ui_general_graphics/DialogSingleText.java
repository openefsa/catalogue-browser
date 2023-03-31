package ui_general_graphics;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import i18n_messages.CBMessages;
import session_manager.BrowserWindowPreferenceDao;
import window_restorer.RestoreableWindow;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DialogSingleText extends Dialog {
	
	private static final Logger LOGGER = LogManager.getLogger(DialogSingleText.class);

	private RestoreableWindow window;
	private String windowCode;
	private Shell shell;
	private String message;
	private int minLength;
	private String okText = CBMessages.getString("DialogSingleText.Ok");
	private String cancelText = CBMessages.getString("DialogSingleText.Cancel");;
	private String input;

	/**
	 * Create a dialog box for ask a text input. Min length refers to the minimum
	 * number of characters which have to be inserted in order to accept the string
	 * 
	 * @param parent
	 * @param minLength
	 * @param style
	 */
	public DialogSingleText(Shell parent, int minLength, int style) {
		super(parent, style);
		this.minLength = minLength;
	}

	/**
	 * Create a dialog box for ask a text input. Min length refers to the minimum
	 * number of characters which have to be inserted in order to accept the string
	 * 
	 * @param parent
	 * @param minLength
	 */
	public DialogSingleText(Shell parent, int minLength) {
		this(parent, minLength, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	/**
	 * Create a dialog box for ask a text input.
	 * 
	 * @param parent
	 */
	public DialogSingleText(Shell parent) {
		this(parent, 0, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
	}

	/**
	 * Set the title of the dialog
	 * 
	 * @param title
	 */
	public void setTitle(String title) {
		setText(title);
	}

	/**
	 * Set the message of the dialog
	 * 
	 * @param message
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	public void setOkText(String okText) {
		this.okText = okText;
	}

	public void setCancelText(String cancelText) {
		this.cancelText = cancelText;
	}

	/**
	 * Set the code for the session manager to restore the previous window
	 * dimensions. Since this class can be used for several instances, we need a
	 * variable window code.
	 * 
	 * @param windowCode
	 */
	public void setWindowCode(String windowCode) {
		this.windowCode = windowCode;
	}

	/**
	 * Open the dialog
	 * 
	 * @return
	 */
	public String open() {
		LOGGER.debug("Open dialog");

		shell = new Shell(getParent(), getStyle());
		window = new RestoreableWindow(shell, windowCode);
		
		shell.setText(getText());

		createContents(shell);
		shell.pack();

		// restore dimensions if we have set a window code
		if (windowCode != null) {
			window.restore(BrowserWindowPreferenceDao.class);
			window.saveOnClosure(BrowserWindowPreferenceDao.class);
		}

		shell.open();

		Display display = getParent().getDisplay();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}

		return input;
	}

	/**
	 * Create the contents of the dialog
	 * 
	 * @param shell
	 */
	private void createContents(final Shell shell) {
		LOGGER.debug("Creating the contents of the dialog");
		
		shell.setLayout(new GridLayout(2, true));

		Label label = new Label(shell, SWT.NONE);
		label.setText(message);

		GridData data = new GridData();
		data.horizontalSpan = 2;
		label.setLayoutData(data);

		final Text text = new Text(shell, SWT.BORDER);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		text.setLayoutData(data);

		final Button ok = new Button(shell, SWT.PUSH);
		ok.setText(okText);
		data = new GridData(GridData.FILL_HORIZONTAL);
		ok.setLayoutData(data);
		ok.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				input = text.getText();
				shell.close();
			}
		});
		ok.setEnabled(false);

		Button cancel = new Button(shell, SWT.PUSH);
		cancel.setText(cancelText);
		data = new GridData(GridData.FILL_HORIZONTAL);
		cancel.setLayoutData(data);
		cancel.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {

				input = null;
				shell.close();
			}
		});

		text.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {

				// enable ok button only if text length >= minLength
				ok.setEnabled(text.getText().length() >= minLength);
			}
		});

		shell.setDefaultButton(ok);
	}
}
