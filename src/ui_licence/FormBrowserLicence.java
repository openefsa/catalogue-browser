<<<<<<< HEAD
package ui_licence;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import messages.Messages;
import utilities.GlobalUtil;

/**
 * Dialog which shows the licence of the catalogue browser.
 * 
 * @author shahaal
 * @author avonva
 *
 */
public class FormBrowserLicence {

	private static final Logger LOGGER = LogManager.getLogger(FormBrowserLicence.class);

	private Shell startupWindow;

	/**
	 * Display the dialog
	 */
	public void display(Shell shell) {

		startupWindow = new Shell(shell, SWT.SHEET | SWT.APPLICATION_MODAL | SWT.WRAP | SWT.BORDER | SWT.TITLE);
		startupWindow.setLayout(new GridLayout(1, false));

		Label label = new Label(startupWindow, SWT.CENTER);
		Image image = new Image(startupWindow.getDisplay(),
				getClass().getClassLoader().getResourceAsStream("Catalogue-browser.gif"));
		label.setImage(image);

		startupWindow.setSize(image.getBounds().width + 15, 500);

		Label l = new Label(startupWindow, SWT.NONE);
		l.setText(Messages.getString("Startup.AppVersion") + " " + GlobalUtil.APP_VERSION);

		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Label l3 = new Label(startupWindow, SWT.NONE);
		l3.setText(Messages.getString("Startup.EFSACopyright"));
		l3.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Label l4 = new Label(startupWindow, SWT.NONE);
		l4.setText(Messages.getString("Startup.LicenceStmt"));
		l4.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		StyledText t1 = new StyledText(startupWindow, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);

		t1.setEditable(false);

		try {
			t1.setText(readLicenceFile("LICENCE.txt"));
		} catch (IOException e) {
			t1.setText("No licence file was found (LICENCE.txt)");
			e.printStackTrace();
			LOGGER.error("Cannot find file LICENCE.txt", e);
		}
		t1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// startupWindow.pack();
		startupWindow.open();

	}

	/**
	 * Read the licence text file to display it
	 * 
	 * @param filename the licence filename
	 * @return the string contained in the file
	 * @throws IOException
	 */
	private String readLicenceFile(String filename) throws IOException {

		InputStream input = FormBrowserLicence.class.getClassLoader().getResourceAsStream(filename);

		BufferedInputStream bis = new BufferedInputStream(input);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();

		int result = bis.read();

		while (result != -1) {
			buf.write((byte) result);
			result = bis.read();
		}

		// StandardCharsets.UTF_8.name() > JDK 7
		String output = buf.toString("UTF-8");

		buf.close();
		bis.close();

		return output;
	}

}
=======
package ui_licence;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import messages.Messages;
import utilities.GlobalUtil;

/**
 * Dialog which shows the licence of the catalogue browser.
 * 
 * @author shahaal
 * @author avonva
 *
 */
public class FormBrowserLicence {

	private static final Logger LOGGER = LogManager.getLogger(FormBrowserLicence.class);

	private Shell startupWindow;

	/**
	 * Display the dialog
	 */
	public void display(Shell shell) {

		startupWindow = new Shell(shell, SWT.SHEET | SWT.APPLICATION_MODAL | SWT.WRAP | SWT.BORDER | SWT.TITLE);
		startupWindow.setLayout(new GridLayout(1, false));

		Label label = new Label(startupWindow, SWT.CENTER);
		Image image = new Image(startupWindow.getDisplay(),
				getClass().getClassLoader().getResourceAsStream("Catalogue-browser.gif"));
		label.setImage(image);

		startupWindow.setSize(image.getBounds().width + 15, 500);

		Label l = new Label(startupWindow, SWT.NONE);
		l.setText(Messages.getString("Startup.AppVersion") + " " + GlobalUtil.APP_VERSION);

		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Label l3 = new Label(startupWindow, SWT.NONE);
		l3.setText(Messages.getString("Startup.EFSACopyright"));
		l3.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		Label l4 = new Label(startupWindow, SWT.NONE);
		l4.setText(Messages.getString("Startup.LicenceStmt"));
		l4.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		StyledText t1 = new StyledText(startupWindow, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);

		t1.setEditable(false);

		try {
			t1.setText(readLicenceFile("LICENCE.txt"));
		} catch (IOException e) {
			t1.setText("No licence file was found (LICENCE.txt)");
			e.printStackTrace();
			LOGGER.error("Cannot find file LICENCE.txt", e);
		}
		t1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		// startupWindow.pack();
		startupWindow.open();

	}

	/**
	 * Read the licence text file to display it
	 * 
	 * @param filename the licence filename
	 * @return the string contained in the file
	 * @throws IOException
	 */
	private String readLicenceFile(String filename) throws IOException {

		InputStream input = FormBrowserLicence.class.getClassLoader().getResourceAsStream(filename);

		BufferedInputStream bis = new BufferedInputStream(input);
		ByteArrayOutputStream buf = new ByteArrayOutputStream();

		int result = bis.read();

		while (result != -1) {
			buf.write((byte) result);
			result = bis.read();
		}

		// StandardCharsets.UTF_8.name() > JDK 7
		String output = buf.toString("UTF-8");

		buf.close();
		bis.close();

		return output;
	}

}
>>>>>>> 574ffe363e78d250cf6350ff4ea89f2f48352380
