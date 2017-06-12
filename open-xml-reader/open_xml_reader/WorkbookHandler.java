package excel_file_management;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class WorkbookHandler extends DefaultHandler {

	private String ATTRIBUTE = null;
	private String HIERARCHY = null;
	private String TERM = null;
	private String CATALOGUE = null;
	private String RELEASENOTES = null;

	@Override
	public void startElement ( String uri , String localName , String name , Attributes attributes )
			throws SAXException {
		if ( name.equals( "sheet" ) ) {
			String nameSheet = attributes.getValue( "name" );
			nameSheet = nameSheet.toUpperCase();
			if ( nameSheet.equals( "ATTRIBUTE" ) ) {
				ATTRIBUTE = attributes.getValue( "r:id" );
				return;
			}
			if ( nameSheet.equals( "HIERARCHY" ) ) {
				HIERARCHY = attributes.getValue( "r:id" );
				return;
			}
			
			if ( nameSheet.equals( "TERM" ) ) {
				TERM = attributes.getValue( "r:id" );
				return;
			}
			
			if ( nameSheet.equals( "CATALOGUE" ) ) {
				CATALOGUE = attributes.getValue( "r:id" );
				return;
			}
			if ( nameSheet.equals( "RELEASENOTES" ) ) {
				RELEASENOTES = attributes.getValue( "r:id" );
				return;
			}
		}
	}

	public String getRELEASENOTES() {
		return RELEASENOTES;
	}
	
	public String getATTRIBUTE ( ) {
		return ATTRIBUTE;
	}

	public String getHIERARCHY ( ) {
		return HIERARCHY;
	}
	
	public String getTERM() {
		return TERM;
	}
	
	public String getCATALOGUE() {
		return CATALOGUE;
	}
}