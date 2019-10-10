package ui_main_panel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import config.AppConfig;
import i18n_messages.CBMessages;
import utilities.GlobalUtil;

/**
 * the class is used for showing the new changes, new features and bugs fixed on
 * each official version
 * 
 * @author shahaal
 *
 */
public class BrowserReleaseNotes {

	private Shell dialog;

	public void display(Shell shell) {

		dialog = new Shell(shell, SWT.SHEET | SWT.APPLICATION_MODAL | SWT.WRAP | SWT.BORDER | SWT.TITLE);
		dialog.setSize(720, 500);

		String title = CBMessages.getString("FromReleaseNotes.Title") + GlobalUtil.APP_VERSION;
		dialog.setText(title);

		dialog.setLayout(new FillLayout());

		Link link = new Link(dialog, SWT.NONE);
		link.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// Text textField = new Text(dialog, SWT.READ_ONLY | SWT.V_SCROLL |
		// SWT.H_SCROLL);
		// textField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		try {

			// read all the lines in the changelog file
			String content = new String(Files.readAllBytes(Paths.get(GlobalUtil.CHANGELOG_PATH)));

			// print the content into the text field
			link.setText(content);
			// textField.setText(content);

			// add selection listener to the link
			link.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					Program.launch(AppConfig.getHelpRepositoryURL()+event.text);
				}
			});

		} catch (IOException e) {
			e.printStackTrace();
		}

		dialog.open();

	}
}
