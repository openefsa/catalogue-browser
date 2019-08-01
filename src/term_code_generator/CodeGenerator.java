package term_code_generator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import global_manager.GlobalManager;

/**
 * Class used to create a new code for a term, given its code mask.
 * 
 * @author avonva
 * @author shahaal
 *
 */
public class CodeGenerator {

	private final Logger LOGGER = LogManager.getLogger(CodeGenerator.class);

	public final String TEMP_TERM_CODE = "TEMP_";

	/**
	 * Check if the term code is a temporary code or not.
	 * 
	 * @param code
	 * @return
	 */
	public boolean isTempCode(String code) {

		if (code.toUpperCase().contains(TEMP_TERM_CODE.toUpperCase()))
			return true;

		return false;
	}

	/*
	 * Possible increments
	 * 
	 * # numbers -> 0,1,2,3,4,5,6,7,8,9
	 * 
	 * @ letters excluding i, o, u, w -> A,B,C,D,E,F,G,H,J,K,L,M,N,P,Q,R,S,T,V,X,Y,Z
	 * § numbers and letters ->
	 * 0,1,2,3,4,5,6,7,8,9,A,B,C,D,E,F,G,H,J,K,L,M,N,P,Q,R,S,T,V,X,Y,Z
	 */

	private char[] /* # */ numberCode = "0123456789".toCharArray();
	private char[] /* @ */ alphaCode = "ABCDEFGHJKLMNPQRSTVXYZ".toCharArray();
	private char[] /* § */ numberAlphaCode = "0123456789ABCDEFGHJKLMNPQRSTVXYZ".toCharArray();

	private char[] initialiseCode(char[] mask) {

		char[] retVal = new char[mask.length];

		for (int i = 0; i < mask.length; i++) {
			if (mask[i] == '#')
				retVal[i] = numberCode[0];
			if (mask[i] == '@')
				retVal[i] = alphaCode[0];
			if (mask[i] == '§')
				retVal[i] = numberAlphaCode[0];
		}
		return retVal;
	}

	private char[] initializeChar(char code[], char[] mask, int index) {

		// cannot do anything
		if (index >= mask.length)
			return code;

		char[] retVal;
		if (index >= code.length) {
			retVal = new char[index + 1];
		} else {
			retVal = new char[code.length];
		}

		// copy code
		for (int i = 0; i < code.length; ++i)
			retVal[i] = code[i];

		if (mask[index] == '#')
			retVal[index] = numberCode[0];
		if (mask[index] == '@')
			retVal[index] = alphaCode[0];
		if (mask[index] == '§')
			retVal[index] = numberAlphaCode[0];

		return retVal;
	}

	private String initialiseCode(String mask) {
		return (String.valueOf(initialiseCode(mask.toCharArray())));
	}

	private static int findChar(char[] list, char elem) {
		int retpos = -1;
		for (int j = 0; j < list.length; j++) {
			if (list[j] == elem) {
				retpos = j;
				break;
			}
		}
		return retpos;
	}

	private char[] incrementCodeRec(char[] alphaNumCode, char[] mask, int i) throws TermCodeException {

		// if overflow, that is, the maximum code is reached
		if (i == -1) {
			throw new TermCodeException(
					"Maximum term code reached for the current term code mask. Cannot create new code!");
		}

		// fix mask missing elements
		if (i >= alphaNumCode.length) {
			// for each missing term
			for (int j = i; j >= alphaNumCode.length; --j) {
				alphaNumCode = initializeChar(alphaNumCode, mask, j);
			}
		}

		int index;

		if (mask[i] == '#') {

			index = findChar(numberCode, alphaNumCode[i]);

			if (index == (numberCode.length - 1)) {
				alphaNumCode[i] = numberCode[0];
				incrementCodeRec(alphaNumCode, mask, i - 1);
			} else {
				alphaNumCode[i] = numberCode[index + 1];
			}
		} else if (mask[i] == '@') {
			index = findChar(alphaCode, alphaNumCode[i]);
			if (index == (alphaCode.length - 1)) {
				alphaNumCode[i] = alphaCode[0];
				incrementCodeRec(alphaNumCode, mask, i - 1);
			} else {
				alphaNumCode[i] = alphaCode[index + 1];
			}
		} else {
			if (mask[i] == '§') {
				index = findChar(numberAlphaCode, alphaNumCode[i]);
				if (index == (numberAlphaCode.length - 1)) {
					alphaNumCode[i] = numberAlphaCode[0];
					incrementCodeRec(alphaNumCode, mask, i - 1);
				} else {
					alphaNumCode[i] = numberAlphaCode[index + 1];
				}
			}
		}
		return alphaNumCode;
	}

	private String incrementCode(String alphaNumCode, String mask) throws TermCodeException {

		return String.valueOf(incrementCodeRec(alphaNumCode.toCharArray(), mask.toCharArray(), mask.length() - 1));

	}

	private String restructureCode(ArrayList<StringSegment> constantSegments, ArrayList<StringSegment> variableSegments,
			String alphaNumCode, String codeMask) {

		String retVal = "";
		/*
		 * I need to keep for the variable an indication where I am copying the data
		 * since the code was compacted in one string
		 */
		int variableStart = 0;
		int variableEnd = 0;
		int iterations = constantSegments.size() + variableSegments.size();
		for (int i = 0; i < iterations; i++) {
			/*
			 * I need to have a variable segment to use, if there are segments of the two
			 * types, and the variable that starts before
			 */
			if (((variableSegments.size() > 0) && (constantSegments.size() > 0)
					&& (variableSegments.get(0).start < constantSegments.get(0).start))
					|| ((variableSegments.size() > 0) && (constantSegments.size() == 0))) {
				/*
				 * I have to include a variable segment, which will reflect a part of the
				 * generated code
				 */
				variableEnd = variableStart + variableSegments.get(0).length;
				retVal = retVal + alphaNumCode.substring(variableStart, variableEnd);
				variableStart = variableEnd;
				variableSegments.remove(0);
			} else
			/*
			 * I need to have a constant segment to use, if the constant start before
			 */
			if (((variableSegments.size() > 0) && (constantSegments.size() > 0)
					&& (constantSegments.get(0).start < variableSegments.get(0).start))
					|| ((variableSegments.size() == 0) && (constantSegments.size() >= 0))) {
				/*
				 * I have to include a variable segment, which will reflect a part of the code
				 * mask
				 */
				retVal = retVal + codeMask.substring(constantSegments.get(0).start, constantSegments.get(0).getEnd());
				constantSegments.remove(0);
			}
		}
		return retVal;

	}

	/**
	 * Get the code of a new term given the code mask
	 * 
	 * @author shahaal
	 * @author avonva
	 * @param codeMask
	 * @return
	 * @throws TermCodeException
	 */
	public String getTermCode(String codeMask) throws TermCodeException {

		// I am preparing the selection mask

		ArrayList<StringSegment> constantSegments = new ArrayList<StringSegment>();
		ArrayList<StringSegment> variableSegments = new ArrayList<StringSegment>();

		boolean readingConstant = false;
		boolean readingVariable = false;

		for (int i = 0; i < codeMask.length(); i++) {
			if ((codeMask.charAt(i) == '#') || (codeMask.charAt(i) == '@') || (codeMask.charAt(i) == '§')) {
				// I am reading a variable
				if (readingConstant) {
					// I was reading a constant
					StringSegment strSeg = new StringSegment();
					strSeg.start = i;
					strSeg.length = 1;
					variableSegments.add(strSeg);
				} else {
					// if I was not reading a variable it must be the first time
					if (!readingVariable) {
						StringSegment strSeg = new StringSegment();
						strSeg.start = i;
						strSeg.length = 1;
						variableSegments.add(strSeg);
					} else {
						// I was already reading a variable
						StringSegment strSeg = variableSegments.get(variableSegments.size() - 1);
						strSeg.length++;
					}
				}
				readingVariable = true;
				readingConstant = false;
			} else {
				// I am reading a constant
				if (readingVariable) {
					/* I was reading a variable */
					StringSegment strSeg = new StringSegment();
					strSeg.start = i;
					strSeg.length = 1;
					constantSegments.add(strSeg);
				} else {
					/*
					 * I should have been reading a constant, otherwise it is the first time
					 */
					if (!readingConstant) {
						// then I have to create a segment because I was not
						// doing anything
						StringSegment strSeg = new StringSegment();
						strSeg.start = i;
						strSeg.length = 1;
						constantSegments.add(strSeg);
					} else {
						/* I was already reading a constant */
						StringSegment strSeg = constantSegments.get(constantSegments.size() - 1);
						strSeg.length++;
					}
				}
				readingVariable = false;
				readingConstant = true;
			}

		}

		/* prepare SQL variable */

		String sqlVariable = "";

		for (int i = 0; i < variableSegments.size(); i++) {
			sqlVariable = sqlVariable + "SUBSTR(TERM_CODE," + (variableSegments.get(i).start + 1) + ","
					+ variableSegments.get(i).length + ")";
			if (i < variableSegments.size() - 1) {
				sqlVariable = sqlVariable + "||";
			}
		}

		String sqlConstant = "";
		/* prepare SQL constant */
		for (int i = 0; i < constantSegments.size(); i++) {
			sqlConstant = sqlConstant + "SUBSTR(TERM_CODE," + (constantSegments.get(i).start + 1) + ","
					+ constantSegments.get(i).length + ")='"
					+ codeMask.substring(constantSegments.get(i).start, constantSegments.get(i).getEnd()) + "'";
			if (i < constantSegments.size() - 1) {
				sqlConstant = sqlConstant + " AND ";
			}
		}

		/* prepare the mask for the variable part to use in the increment */

		String variableMask = "";

		for (int i = 0; i < variableSegments.size(); i++) {
			variableMask = variableMask
					+ codeMask.substring(variableSegments.get(i).start, variableSegments.get(i).getEnd());
		}

		String alphaNumCode = "";
		String currAlphaNumCode = "";
		GlobalManager manager = GlobalManager.getInstance();

		// solve memory leak
		try (Connection con = manager.getCurrentCatalogue().getConnection()) {

			/* get the maximum code according to the specified mask */
			String sql = "select max(" + sqlVariable + ") as CURR_CODE from APP.TERM";

			if (sqlConstant.length() > 0) {
				sql = sql + " where " + sqlConstant;
			}

			PreparedStatement codeStmt = con.prepareStatement(sql);
			ResultSet codeRs = codeStmt.executeQuery();

			while (codeRs.next()) {
				currAlphaNumCode = codeRs.getString("CURR_CODE");
			}
			if ((currAlphaNumCode == null) || currAlphaNumCode == "") {
				// this is the first instance of this code therefore it has to
				// be initialised
				currAlphaNumCode = initialiseCode(variableMask);
			}

			alphaNumCode = restructureCode(constantSegments, variableSegments,
					incrementCode(currAlphaNumCode, variableMask), codeMask);

			codeRs.close();
			codeStmt.close();
			con.close();

		} catch (SQLException e) {
			e.printStackTrace();
			LOGGER.error("Cannot generate new code", e);
		}
		return alphaNumCode;

	}
}
