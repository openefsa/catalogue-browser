package detail_level;

import catalogue_object.TermAttribute;

/**
 * This class is used to model the table DETAIL_LEVEL contained in the main database
 * in particular, we are modelling the graphical components of the detail levels flag
 * @author avonva
 *
 */
public class DetailLevelGraphics {

	private String code, name;
	private String imageName;
	
	public DetailLevelGraphics( String code, String name, String imageName ) {
		this.code = code;
		this.name = name;
		this.imageName = imageName;
	}
	
	public String getCode() {
		return code;
	}
	public String getLabel() {
		return name;
	}
	public String getImageName() {
		return imageName;
	}
	
	@Override
	public boolean equals(Object arg0) {
		
		if ( arg0 instanceof DetailLevelGraphics ) {
			DetailLevelGraphics dlg = (DetailLevelGraphics) arg0;
			return dlg.getCode().equals( code );
		}
		
		// for detail levels attributes
		if ( arg0 instanceof TermAttribute ) {
			TermAttribute ta = (TermAttribute) arg0;
			
			if ( !ta.getAttribute().isDetailLevel() )
				return false;
			
			return ta.getValue().equals( code );
		}
		
		return super.equals(arg0);
	}
}
