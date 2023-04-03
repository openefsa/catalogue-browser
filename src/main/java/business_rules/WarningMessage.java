package business_rules;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import business_rules.TermRules.WarningLevel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class which store the information related to a single warning message.
 * 
 * @author shahaal
 * @author avonva
 *
 */
public class WarningMessage {
	
	private static final Logger LOGGER = LogManager.getLogger(WarningMessage.class);

	int id;
	String message;
	WarningLevel warningLevel;
	WarningLevel textWarningLevel;

	WarningMessage(int id, String message, WarningLevel warningLevel, WarningLevel textWarningLevel) {
		this.id = id;
		this.message = message;
		this.warningLevel = warningLevel;
		this.textWarningLevel = textWarningLevel;
	}

	// the id of the message (used to connect the warning event to the warning
	// message)
	public int getId() {
		return id;
	}

	// the message to be print
	public String getMessage() {
		return message;
	}

	// get the warning level of the message
	public WarningLevel getWarningLevel() {
		return warningLevel;
	}

	public WarningLevel getTextWarningLevel() {
		return textWarningLevel;
	}

	/**
	 * Create the default file which contains the warning messages
	 * 
	 * @param filename
	 * @return
	 */
	public static int createDefaultWarningMessagesFile(String filename) {

		try {

			PrintWriter out = new PrintWriter(filename);

			StringBuilder sb = new StringBuilder();

			sb.append("Message ID;Trigger Event Description;Text;SemaphoreWarningLevel;TextWarningLevel\r\n"
					+ "1;if the added explicit f27 is not children of the already present implicit facet or it is not children of the bt than  raise warning;BR01> For mixed raw primary commodity terms it is only allowed to add under F27 source-commodities children of the already present implicit facet.;HIGH;HIGH\r\n"
					+ "2;empty;BR02> Empty;NONE;NONE\r\n"
					+ "3;if a source is selected for composite terms (c=aggregated or s=simple);BR03> The F01 source facet is not allowed in composite food. Choose instead an F04 ingredient facet.;HIGH;HIGH\r\n"
					+ "4;if a source commodity is selected for composite (c=aggregated or s=simple);BR04> The F27 source-commodities facet is not allowed in composite food. Choose instead an F04 ingredient facet.;HIGH;HIGH\r\n"
					+ "5;for derivative terms it is only allowed to add explicit facets which better define the already resent f27 implicit one;BR05> The F27 source-commodities facet which are not better specifing the alredy present implicit one are not allowed. Start from the generic derivative term instead.;HIGH;HIGH\r\n"
					+ "6;if a source is selected for a generic derivative without F27 (neither explicit nor implicit);BR06> The F01 source facet is only allowed in derivatives with an F27 source-commodities facet implicitly present.;HIGH;HIGH\r\n"
					+ "7;if a source is selected for mixed derivative having more than one F27;BR07> The F01 source facet can only be populated for derivatives having a single F27 source-commodities facet.;HIGH;HIGH\r\n"
					+ "8;the use of not reportable terms is forbidden;BR08> The use of not reportable terms is forbidden.;HIGH;HIGH\r\n"
					+ "9;empty;BR09> Empty;NONE;NONE\r\n"
					+ "10;if non-specific term is selected;BR10> The use of non-specific terms as base term is discouraged.;NONE;LOW\r\n"
					+ "11;if the generic facet Processed (or children) is selected;BR11> The use of generic terms under F28 process facet is discouraged.;LOW;LOW\r\n"
					+ "12;if an ingredient is selected for raw commodity or derivative;BR12> The F04 ingredient facet can only be used as a minor ingredient to derivative or raw primary commodity terms.;LOW;LOW\r\n"
					+ "13;if a physical state facet is added to a food rpc term;BR13> The F03 physical state facet reported creates a new derivative nature and therefore cannot be applied to raw primary commodity.;HIGH;HIGH\r\n"
					+ "14;this br is only applied on ICT and DCF;BR14> This br is only applied on ICT and DCF.;HIGH;HIGH\r\n"
					+ "15;this br is only applied on DCF;BR15> This br is only applied on DCF.;LOW;LOW\r\n"
					+ "16;if a derivative is described with a process facet with an ordCode value less than the implicit ordCode;BR16> Reporting facets less detailed than the implicit facets is discouraged.;HIGH;HIGH\r\n"
					+ "17;if a facet is selected as base term;BR17> Reporting facets as base term is forbidden.;HIGH;HIGH\r\n"
					+ "18;empty;BR18> Empty;NONE;NONE\r\n"
					+ "19;if a forbidden process is chosen (the derivative should be used);BR19> Processes that create a new derivative nature cannot be applied to raw commodity base terms. Start from the exsisting derivative base term instead.;HIGH;HIGH\r\n"
					+ "20;if a deprecated term has been chosen;BR20> The selected term cannot be used since it is deprecated.;HIGH;HIGH\r\n"
					+ "21;if a dismissed term has been chosen;BR21> The selected term cannot be used since it is dismissed.;HIGH;HIGH\r\n"
					+ "22;if a non-hierarchy is selected as base term;BR22> Base term successfully added.;NONE;NONE\r\n"
					+ "23;if a hierarchy is selected as base term;BR23>  The use of hierarchy terms as base term is discouraged.;LOW;LOW\r\n"
					+ "24;if a hierarchy which does not belong to the exposure is selected as base term;BR24> The hierarchy term selected does not belong to the exposure hierarchy.;LOW;LOW\r\n"
					+ "25;it is not allowed to add more than one explicit facet  to a facet category with single cardinality;BR25> Reporting more than one facet is forbidden for this category.;HIGH;HIGH\r\n"
					+ "26;if more than one process with the same ordCode is chosen (mutually exclusive property violated);BR26> The selected processes cannot be used together for derivative base term.;HIGH;HIGH\r\n"
					+ "27;if two processes (implicit or explicit) with decimal ordcode and same integer part are applied (at least one explicit);BR27> Processes that create a new derivative nature cannot be applied to exsisting derivative base terms. Start from a different derivative base term instead.;HIGH;HIGH\r\n"
					+ "28;if reconstitution is added as process to concentrate, powder or other dehydrated terms;BR28> Processes that create a new derivative nature cannot be applied to exsisting derivative base terms. Start from the reconstituted/diluted term instead.;HIGH;HIGH\r\n"
					+ "29;if wrong term structure or term not found;BR29> The code does not follow the required structure or is misspelled.;ERROR;ERROR\r\n"
					+ "30;if the facet group id doesn't exists;BR30> The category does not exist.;ERROR;ERROR\r\n"
					+ "31;if the facet doesn't belong to the group hierarchy;BR31> The facet is not valid for the facet category.;ERROR;ERROR\r\n");

			out.write(sb.toString());
			out.close();

			return 0;

		} catch (FileNotFoundException e1) {
			LOGGER.error("Error", e1);
			e1.printStackTrace();
			return -1;
		}
	}
}