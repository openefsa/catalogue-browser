package ui_main_panel;

import java.awt.Event;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;

import utilities.GlobalUtil;

public class BrowserReleaseNotes {

	/*
	 * the static method shwo the window with the latest features added in the
	 * installed version could be possible to see the window form the main menu or
	 * automatically when an update occurs to avoid the creatin of the flag file new
	 * update -> flag=true, main menu-> flag=false
	 */
	public static void display(Shell shell, boolean flag) {

		Shell notes = new Shell(shell, SWT.SHEET | SWT.APPLICATION_MODAL | SWT.WRAP | SWT.BORDER | SWT.TITLE);
		notes.setSize(550, 500);
		notes.setLayout(new FillLayout());

		// read the header
		notes.setText("\nNew in Catalogue browser vers. " + CatalogueBrowserMain.APP_VERSION + "\n\n");

		StyledText t1 = new StyledText(notes, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
		t1.setEditable(false);

		try {

			BufferedReader buff = new BufferedReader(new FileReader(GlobalUtil.getChangelogPath()));
			StringBuffer stringBuffer = new StringBuffer();

			// read the rest of the file
			String str = "";
			while ((str = buff.readLine()) != null)
				stringBuffer.append(str + "\n");

			// close the buffer reader
			buff.close();

			// print the string result
			t1.setText(stringBuffer.toString());

			//if closed and flag then create the flag for not showing the release notes automatically
			notes.addListener(SWT.Close, new Listener() {

				@Override
				public void handleEvent(org.eclipse.swt.widgets.Event arg0) {
					if(flag)
						try {
							new File(GlobalUtil.getFlagPath()).createNewFile();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
				}
			});

		} catch (IOException e) {

			e.printStackTrace();
			return;
		}

		notes.open();

	}
}
