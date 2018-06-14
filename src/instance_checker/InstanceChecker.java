package instance_checker;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.eclipse.swt.widgets.Shell;

import messages.Messages;
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
			
			Thread ServerThread1 = new Thread(new Runnable() {

				@Override
				public void run() {
					ServerSocket ServerSocketObject = null;
					while (true) {
						try {
							ServerSocketObject = new ServerSocket(8080, 0, InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }));

							Socket SocketObject = ServerSocketObject.accept();
							// Your Code Here
							SocketObject.close();

						} catch (IOException e) {
							try {
								ServerSocketObject.close();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
							e.printStackTrace();
						}
					}
				}
			});/*
			System.err.println("Another instance of the catalogue browser is already running!");

			GlobalUtil.showErrorDialog(new Shell(), Messages.getString("AlreadyRunning.ErrorTitle"),
					Messages.getString("AlreadyRunning.ErrorMessage"));

			System.exit(1);*/
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
