package dcf_webservice;

import dcf_webservice.Publish.PublishLevel;

/**
 * This class contains all the xml messages which need to be
 * attached to the uploadCatalogueFile message to make the
 * related operations, as the reserve, unreserve...
 * @author avonva
 *
 */
public class UploadMessages {

	/**
	 * Get the xml message to reserve a catalogue. We need the catalogue code and
	 * the descriptions, which is the reason why we are reserving the catalogue
	 * @param code
	 * @param description
	 * @param opType, can be reserveMinor or reserveMajor
	 * @return
	 */
	public static String getReserveMessage ( String code, ReserveLevel level, String description ) {

		String message = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<message xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
				+ "xsi:noNamespaceSchemaLocation=\"file:///D:/cat_xsd/UploadCatalogue.xsd\">"
				+ "<updateCatalogue catalogueCode=\"" + code + "\">"
				+ "<" + level.getOp() + ">"
				+ "<reservationNote>" + description + "</reservationNote>"
				+ "</" + level.getOp() + ">"
				+ "</updateCatalogue>"
				+ "</message>";

		return message;
	}
	
	public static String getPublishMessage ( String code, PublishLevel level ) {

		String message = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<message xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
				+ "xsi:noNamespaceSchemaLocation=\"file:///D:/cat_xsd/UploadCatalogue.xsd\">"
				+ "<" + level.getOp()
				+ " catalogueCode=\"" + code + "\">"
				+ "</" + level.getOp() + ">"
				+ "</message>";

		return message;
	}
}
