package business_rules;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import business_rules.TermRules.WarningLevel;

/**
 * Class which store the information related to a single warning message.
 * 
 * @author shahaal
 * @author avonva
 *
 */
public class WarningMessage {

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

			sb.append("Message ID;Trigger Event Description;Text;SemaphoreWarningLevel;TextWarningLevel");
			sb.append("\r\n");

			// first message
			sb.append(
					"1;if a single Source Commodity is selected for raw commodity;BR01> For mixed raw commodities only multiple explicit source commodities are allowed;HIGH;HIGH");
			sb.append("\r\n");

			// second message
			sb.append(
					"2;if a source is selected for mixed derivative (having more than one F27.);BR02> The source facet is not allowed in mixed derivatives;HIGH;HIGH");
			sb.append("\r\n");

			// third message
			sb.append(
					"3;if a source is selected for composite (c or s);BR03> The source facet is not allowed in composite food;HIGH;HIGH");
			sb.append("\r\n");

			// fourth message
			sb.append(
					"4;if a source commodity is selected for composite (c or s);BR04> The source commodity facet is not allowed in composite food;HIGH;HIGH");
			sb.append("\r\n");

			// fifth message
			sb.append("5;if one or more source commodities are added to a derivative already having an implicit source commodity (not parent of the added);BR05> Source commodities which are not children of the implicit one are not allowed (use the generic derivative for describing a mixed derivative);HIGH;HIGH");
			sb.append("\r\n");

			// sixth message
			sb.append(
					"6;if a source is selected for a generic derivative without F27 (neither explicit nor implicit);BR06> The source facet is not allowed for derivatives without the (single) source commodity;HIGH;HIGH");
			sb.append("\r\n");

			// seventh message
			sb.append("7;(ONLY DCF)if more than one explicit facet is added to a group with single cardinality;BR07> Reporting more than one facet is forbidden for this category;HIGH;HIGH");
			sb.append("\r\n");

			// eight message
			sb.append("8;this rule is applied on the ui of the CB;BR08> under development;HIGH;HIGH");
			sb.append("\r\n");

			// ninth message
			sb.append("9;if a hierarchy is selected as base term (describe function);BR09> The use of hierarchies as base term is discouraged;LOW;LOW");
			sb.append("\r\n");

			// tenth message
			sb.append("10;if non-specific term is selected (describe);BR10> The use of non-specific terms is discouraged;NONE;LOW");
			sb.append("\r\n");

			// eleventh message
			sb.append("11;if the generic facet Processed (or children) is selected;BR11> The use of generic terms is discouraged;LOW;LOW");
			sb.append("\r\n");

			// 12 message
			sb.append("12;if an ingredient is selected for raw commodity or derivative;BR12> Ingredient facet can only be used as minor ingredient for derivatives;LOW;LOW");
			sb.append("\r\n");

			// 13 message
			sb.append("13;if a source is selected for derivative with only one F27.;BR13> The source facet is allowed for derivatives with only one source commodity just for better specifying the raw source;LOW;LOW");
			sb.append("\r\n");

			// 14 message
			sb.append("14;under development;BR14> under development;HIGH;HIGH");
			sb.append("\r\n");

			// 15 message
			sb.append("15;if the user add an already existing implicit to the baseterm;BR15> The facet is already implicitly present in the baseterm. Please remove it;HIGH;HIGH");
			sb.append("\r\n");

			// 16 message
			sb.append("16;if a derivative is described with a process facet with an ordCode value less than the implicit ordCode;BR16> Reporting facets less detailed than the implicit facets is discouraged;HIGH;HIGH");
			sb.append("\r\n");

			// 17 message
			sb.append("17;if a facet is selected as baseterm is not valid;BR17> Reporting a facet as base term is forbidden;HIGH;HIGH");
			sb.append("\r\n");

			// 18 message
			sb.append(
					"18;if an ambiguous term is selected (terms reported in the BR_Exceptions);BR18> The use of ambiguous terms is discouraged;LOW;LOW");
			sb.append("\r\n");

			// 19 message
			sb.append(
					"19;if a forbidden process is chosen (the derivative should be used);BR19> The reported processes cannot be applied to the raw commodity (use the existing derivative);HIGH;HIGH");
			sb.append("\r\n");

			sb.append(
					"20;if a deprecated term has been chosen;BR20> The selected term cannot be used since it is deprecated;HIGH;HIGH");
			sb.append("\r\n");

			sb.append(
					"21;(DEPRECATED)if a dismissed term has been chosen;BR21> The selected term cannot be used since has been dismissed;HIGH;HIGH");
			sb.append("\r\n");
			
			sb.append(
					"22;(ONLY CB) if a non-hierarchy is selected as base term (describe function);BR22> Base term successfully added;NONE;NONE");
			sb.append("\r\n");
			
			sb.append(
					"23;(ONLY CB) if more than one process with the same ordCode is chosen (mutually exclusive property violated);BR23> The selected processes cannot be used together;HIGH;HIGH");
			sb.append("\r\n");
			
			sb.append(
					"24;(ONLY CB) if a base term which does not belong to reporting or exposure hierarchy is selected;BR24> The term selected cannot be reported;LOW;LOW");
			sb.append("\r\n");
			
			sb.append(
					"25;(ONLY CB) if a non exposure hierarchy-term (blue pyramid) is selected as base term (describe);BR25> A non-exposure hierarchy term has been selected;LOW;LOW");
			sb.append("\r\n");
			
			sb.append(
					"26;(ONLY CB) if a base term not valid in the exposure hierarchy is chosen;BR26> The term selected is not valid for human exposure calculation;NONE;HIGH");
			sb.append("\r\n");
			
			sb.append(
					"27;(ONLY CB) if two processes (implicit or explicit) with decimal ordcode and same integer part are applied (at least one explicit);BR27> Use the existing derivative instead of adding the facet;HIGH;HIGH");
			sb.append("\r\n");
			
			sb.append(
					"28;(ONLY CB) if reconstitution is added as process to concentrate, powder or other dehydrated terms;BR28> Select the reconstituted version of the product instead;HIGH;HIGH");
			sb.append("\r\n");
			
			sb.append(
					"29;(ONLY ICT) if wrong term structure or term not found;BR29> The code does not follow the required structure or is misspelled;ERROR;ERROR");
			sb.append("\r\n");
			
			sb.append(
					"30;(ONLY ICT)if the facet group id doesn't exists;BR30> The category does not exist;ERROR;ERROR");
			sb.append("\r\n");
			
			sb.append(
					"31;(ONLY ICT)if the facet doesn't belong to the group hierarchy;BR31> The facet has not been found in the category;ERROR;ERROR");
			sb.append("\r\n");

			out.write(sb.toString());
			out.close();

			return 0;

		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
			return -1;
		}
	}
}