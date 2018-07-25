package ui_main_panel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Listener;
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

			// if closed and flag then create the flag for not showing the release notes
			// automatically
			notes.addListener(SWT.Close, new Listener() {

				@Override
				public void handleEvent(org.eclipse.swt.widgets.Event arg0) {
					BufferedWriter writer = null;
					if (flag)
						try {
							File flagFile = new File(GlobalUtil.getFlagPath());
							flagFile.createNewFile();
							writer = new BufferedWriter(new FileWriter(flagFile));
							writer.write(CatalogueBrowserMain.APP_VERSION);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} finally {
							try {
								// Close the writer regardless of what happens...
								writer.close();
							} catch (Exception e) {
							}
						}
				}
			});

		} catch (IOException e) {

			e.printStackTrace();
			return;
		}

		notes.open();

	}

	public static void checkVersion(Shell shell) {
		//check if the flag contains a number older then the new version
		String[] v1 = readAllBytesJava7(GlobalUtil.getFlagPath()).split("\\.");
		String[] v2 = CatalogueBrowserMain.APP_VERSION.split("\\.");

		if (v1.length != v2.length)
		    return;

		for (int pos = 0; pos < v1.length; pos++) {
		    // compare v1[pos] with v2[pos] as necessary
		    if (Integer.parseInt(v1[pos]) < Integer.parseInt(v2[pos])) {
		    	File flagFile = new File(GlobalUtil.getFlagPath());
				flagFile.delete();
				display(shell, true);
				break;
		    }
		}
		
	}

	// Read file content into string with - Files.readAllBytes(Path path)

	private static String readAllBytesJava7(String filePath) {
		String content = "";
		try {
			content = new String(Files.readAllBytes(Paths.get(filePath)));
			//if the content is empty then assign the minimum version
			if(content.equals(""))
				content="1.0.0";
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}
}
