package instance_checker;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import org.eclipse.swt.widgets.Shell;

import i18n_messages.CBMessages;
import utilities.GlobalUtil;

/**
 * Checks if another instance of the browser was started or not.
 * 
 * @author avonva
 *
 */
public class InstanceChecker {

	private static ServerSocket socket;
	private static final int PORT = 9999;

	/**
	 * Close the application if it is already running in another instance
	 */
	public static void closeIfAlreadyRunning() {
		// initialize the socket just if not already running/connected

		try {
			// Bind to localhost adapter with a zero connection queue
			socket = new ServerSocket(PORT, 0, InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));
		} catch (BindException e) {
			
			System.err.println("Another instance of the catalogue browser is already running!");
			GlobalUtil.showErrorDialog(new Shell(), CBMessages.getString("AlreadyRunning.ErrorTitle"),
					CBMessages.getString("AlreadyRunning.ErrorMessage"));
			System.exit(1);
			
		} catch (IOException e) {
			System.err.println("Unexpected error.");
			e.printStackTrace();
			System.exit(2);
		}

	}

	public static void close() throws IOException {
		if (socket != null)
			socket.close();
	}
}
