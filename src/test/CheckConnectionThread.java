package test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Procedure to test if several jdcb connections are still
 * opened or not.
 * @author avonva
 *
 */
public class CheckConnectionThread extends Thread {

	private static CopyOnWriteArrayList<Integer> openConnections = new CopyOnWriteArrayList<>();
	
	private static int ID = 0;
	private int id;
	private Connection con;
	
	public CheckConnectionThread( Connection con ) {
		
		this.con = con;
		this.id = ID;
		
		addValue ( id );

		System.err.println( "Opening connection n° " + id );
		
		ID++;
	}
	
	private synchronized void addValue ( Integer id ) {
		openConnections.add( id );
	}
	
	private synchronized void removeValue ( Integer id ) {
		openConnections.remove( id );
	}
	
	private synchronized void print () {
		System.err.println( "Connections still open " + openConnections);
	}

	@Override
	public void run() {

		try {
			
			while ( !con.isClosed() ) {
				Thread.sleep( 1000 );
			}

			removeValue( id );
			
			print();
			
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
