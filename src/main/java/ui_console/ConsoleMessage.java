package ui_console;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.swt.SWT;

public class ConsoleMessage {

	private String message;
	private String now;
	private int color;
	
	/**
	 * Create a message for the console
	 * @param message text message
	 * @param color SWT colour code (as SWT.COLOR_WHITE)
	 */
	public ConsoleMessage(String message, int color) {
		this.message = message;
		this.color = color;
		this.now = new SimpleDateFormat("HH:mm:ss").format(new Date());
	}
	
	/**
	 * Create a message for the console with the default
	 * colour
	 * @param message text message
	 */
	public ConsoleMessage(String message) {
		this(message, SWT.COLOR_WHITE);
	}
	
	public String getMessage() {
		return now + "> " + message;
	}
	
	public int getColor() {
		return color;
	}
}
