package folder_zipper;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FolderZipper {

	static public void zipFolder ( String srcFolder , String destZipFile, boolean folder ) throws Exception {
		ZipOutputStream zip = null;
		FileOutputStream fileWriter = null;

		fileWriter = new FileOutputStream( destZipFile );
		zip = new ZipOutputStream( fileWriter );

		if ( folder )
			addFolderToZip( "", srcFolder, zip );
		else
			addFileToZip( "", srcFolder, zip, false );
		zip.flush();
		zip.close();
	}

	@Deprecated
	static private void addFileToZip ( String path , String srcFile , ZipOutputStream zip, boolean isFolder ) throws Exception {

		File folder = new File( srcFile );
		if ( folder.isDirectory() ) {
			addFolderToZip( path, srcFile, zip );
		} else {
			byte[] buf = new byte[1024];
			int len;
			FileInputStream in = new FileInputStream( srcFile );
			zip.putNextEntry( new ZipEntry( srcFile ) );
			while ( ( len = in.read( buf ) ) > 0 ) {
				zip.write( buf, 0, len );
			}
			
			in.close();
		}
	}
	
	static private void addFileToZip ( String path , String srcFile , ZipOutputStream zip ) throws Exception {

		File folder = new File( srcFile );
		if ( folder.isDirectory() ) {
			addFolderToZip( path, srcFile, zip );
		} else {
			byte[] buf = new byte[1024];
			int len;
			FileInputStream in = new FileInputStream( srcFile );
			zip.putNextEntry( new ZipEntry( path + "/" + folder.getName() ) );
			while ( ( len = in.read( buf ) ) > 0 ) {
				zip.write( buf, 0, len );
			}
			
			in.close();
		}
	}

	static private void addFolderToZip ( String path , String srcFolder , ZipOutputStream zip )
			throws Exception {
		File folder = new File( srcFolder );

		for ( String fileName : folder.list() ) {
			if ( path.equals( "" ) ) {
				addFileToZip( folder.getName(), srcFolder + "/" + fileName, zip );
			} else {
				addFileToZip( path + "/" + folder.getName(), srcFolder + "/" + fileName, zip );
			}
		}
	}

	static public void extractFolder ( String zipFile , String newPath ) throws ZipException, IOException {
		System.out.println( zipFile );
		int BUFFER = 2048;
		File file = new File( zipFile );

		ZipFile zip = new ZipFile( file );
		// String newPath = zipFile.substring(0, zipFile.length() - 4);

		new File( newPath ).mkdir();
		Enumeration< ? extends ZipEntry > zipFileEntries = zip.entries();

		// Process each entry
		while ( zipFileEntries.hasMoreElements() ) {
			// grab a zip file entry
			ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
			String currentEntry = entry.getName();
			File destFile = new File( newPath , currentEntry );
			// destFile = new File(newPath, destFile.getName());
			File destinationParent = destFile.getParentFile();

			// create the parent directory structure if needed
			destinationParent.mkdirs();

			if ( !entry.isDirectory() ) {
				BufferedInputStream is = new BufferedInputStream( zip.getInputStream( entry ) );
				int currentByte;
				// establish buffer for writing file
				byte data[] = new byte[BUFFER];

				// write the current file to disk
				FileOutputStream fos = new FileOutputStream( destFile );
				BufferedOutputStream dest = new BufferedOutputStream( fos , BUFFER );

				// read and write until last byte is encountered
				while ( ( currentByte = is.read( data, 0, BUFFER ) ) != -1 ) {
					dest.write( data, 0, currentByte );
				}
				dest.flush();
				dest.close();
				is.close();
			}
		}
		
		zip.close();
	}
	
	
	/**
	 * Write a file reading it from the zip stream
	 * @param zipStream
	 * @param filename
	 */
	public static void unzipStream ( InputStream zipStream, String filename ) {
		
		FileOutputStream fos = null;
		try {

			// write the into the file
			fos = new FileOutputStream( filename );
			
			final byte[] buf = new byte[ 2000 ];
			
			int length;
			
			// write until there is something
			while ( ( length = zipStream.read(buf, 0, buf.length) ) >= 0 ) {
				fos.write(buf, 0, length);
			}
			
		} catch (IOException ioex) {
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return;
	}
}
