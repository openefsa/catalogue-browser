package business_rules;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import business_rules.TermRules.WarningLevel;

/**
 * Class which store the information related to a single warning message.
 * 
 * @author shahaal
 * @author Valentino
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
					"1;if a hierarchy is selected as base term (describe function);Warning: the base term is a hierarchy!;LOW;LOW");
			sb.append("\r\n");

			// second message
			sb.append(
					"2;if a non-hierarchy is selected as base term (describe function);The base term was successfully added!;NONE;NONE");
			sb.append("\r\n");

			// third message
			sb.append(
					"3;if a forbidden process is chosen (the derivative should be used);You are trying to generate an already existing derivative!;HIGH;HIGH");
			sb.append("\r\n");

			// fourth message
			sb.append(
					"4;if a derivative is described with a process facet with an ordCode value less than the implicit ordCode;"
							+ "Process applied in the wrong order!;HIGH;HIGH");
			sb.append("\r\n");

			// fifth message
			sb.append("5;if an ambiguous term is selected (terms reported in the BR_Exceptions); "
					+ "You have selected an ambiguous term, please specify the element better!;LOW;LOW");
			sb.append("\r\n");

			// sixth message
			sb.append(
					"6;if more than one process with the same ordCode is chosen (mutually exclusive property violated);"
							+ "You cannot use these processes together!;HIGH;HIGH");
			sb.append("\r\n");

			// seventh message
			sb.append("7;if a base term which does not belong to reporting or exposure hierarchy is selected;"
					+ "You cannot use a non reportable term!;LOW;LOW");
			sb.append("\r\n");

			// eight message
			sb.append(
					"8;if a non exposure hierarchy is selected as base term (describe);You have selected a non-exposure hierarchy!;LOW;LOW");
			sb.append("\r\n");

			// ninth message
			sb.append("9;if a non-specific term is selected (describe);You have selected a non-specific term. "
					+ "Are you sure that you cannot be more precise?;NONE;LOW");
			sb.append("\r\n");

			// tenth message
			sb.append("10;if the generic facet processed is selected;"
					+ "You have selected a generic process facet. Are you sure that you cannot be more precise?;LOW;LOW");
			sb.append("\r\n");

			// eleventh message
			sb.append("11;if an ingredient is selected for raw commodity or derivative;"
					+ "Is it a minor ingredient? Please check!;LOW;LOW");
			sb.append("\r\n");

			// 12 message
			sb.append(
					"12;if a single source commodity is selected for raw commodity;Only multiple source commodities allowed, "
							+ "for mixed raw commodities!;HIGH;HIGH");
			sb.append("\r\n");

			// 13 message
			sb.append("13;if a source is selected for mixed derivative (more than one F27.);"
					+ "Source facet not allowed in mixed derivatives;HIGH;HIGH");
			sb.append("\r\n");

			// 14 message
			sb.append("14;if a source is selected for derivative with only one F27.;"
					+ "Make sure that Source is used for better specifying the raw source -otherwise forbidden;LOW;LOW");
			sb.append("\r\n");

			// 15 message
			sb.append("15;if a base term not valid in the exposure hierarchy is chosen;"
					+ "Not valid for human exposure calculation;NONE;HIGH");
			sb.append("\r\n");

			// 16 message
			sb.append(
					"16;if a source is selected for composite (c or s);Source is not applicable for composite food;HIGH;HIGH");
			sb.append("\r\n");

			// 17 message
			sb.append(
					"17;if a source commodity is selected for composite (c or s);Source commodity is not applicable for composite food;HIGH;HIGH");
			sb.append("\r\n");

			// 18 message
			sb.append(
					"18;if two processes (implicit or explicit) with decimal ordcode and same integer part are applied (at least one explicit);"
							+ "You are trying to generate an already existing derivative!;HIGH;HIGH");
			sb.append("\r\n");

			// 19 message
			sb.append(
					"19;if one or more source commodities are added to a derivative already having an implicit source commodity (not parent of the added);"
							+ "Use the generic derivative as base term for describing a mixed derivative;HIGH;HIGH");
			sb.append("\r\n");

			sb.append(
					"20;if a source is selected for a generic derivative without F27 (neither explicit nor implicit);Forbidden to use the Source without the (single) source commodity;HIGH;HIGH");
			sb.append("\r\n");

			sb.append(
					"21;if wrong term structure or term not found;Error: wrong term structure or term not found!;ERROR;ERROR");
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