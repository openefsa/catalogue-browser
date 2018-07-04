package ui_main_panel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.eclipse.swt.SWT;
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

		// Message
		MessageBox messageDialog = new MessageBox(shell,
				SWT.ICON_INFORMATION | SWT.WRAP | SWT.BORDER | SWT.V_SCROLL | SWT.TITLE);
		
		try {

			BufferedReader buff = new BufferedReader(new FileReader(GlobalUtil.getChangelogPath()));
			StringBuffer stringBuffer = new StringBuffer();

			// read the header
			messageDialog.setText("\nNew in Catalogue browser vers. " +CatalogueBrowserMain.APP_VERSION + "\n\n");

			// read the rest of the file
			String str = "";
			while ((str = buff.readLine()) != null)
				stringBuffer.append(str + "\n");

			// close the buffer reader
			buff.close();

			// print the string result
			messageDialog.setMessage(stringBuffer.toString());

			// if ok is pressed and coming from new update a new file flag is generated
			if (messageDialog.open() == SWT.OK && flag) {
				new File(GlobalUtil.getFlagPath()).createNewFile();
			}

		} catch (IOException e) {

			e.printStackTrace();
			return;
		}
	}
}
