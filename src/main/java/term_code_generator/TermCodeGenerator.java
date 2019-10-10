package term_code_generator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.eclipse.core.runtime.AssertionFailedException;

import catalogue.Catalogue;

@Deprecated
public class TermCodeGenerator {

	private HashMap<Character, String> map;
	private Catalogue catalogue;
	private String termCodeMask;
	
	public TermCodeGenerator(Catalogue catalogue) {
		this.catalogue = catalogue;
		this.termCodeMask = catalogue.getTermCodeMask();
		
		if (this.termCodeMask == null || this.termCodeMask.isEmpty())
			throw new IllegalArgumentException("No term code mask found for catalogue " + catalogue);
		
		// init map between special characters and values
		this.map = new HashMap<>();
		map.put('#', "0123456789");
		map.put('@', "ABCDEFGHJKLMNPQRSTVXYZ");
		map.put('§', "0123456789ABCDEFGHJKLMNPQRSTVXYZ");
	}
	
	/**
	 * Generate a new term code (the first available one)
	 * following the term code mask defined in the catalogue.
	 * @return
	 * @throws SQLException 
	 */
	@SuppressWarnings("unused")
	public String generate() throws SQLException {
		
		String lastCode = getLastUsedCode();

		// if no code was used, then generate the
		// first code
		if (lastCode == null)
			return getFirstCode();
		
		if (lastCode.length() != termCodeMask.length()) {
			throw new AssertionFailedException("The last code " + lastCode 
					+ " does not follow the term code mask " + termCodeMask + ". Cannot generate a new code.");
		}

		// iterate term code mask
		char[] termCodeMaskElems = termCodeMask.toCharArray();
		char[] newTermCode = termCodeMaskElems.clone();

		//newTermCode = increaseCode(newTermCode);
		
		return null;
	}
	
	@SuppressWarnings("unused")
	private char[] increaseCodeRec(char[] codeChars, int index) {
		
		char[] output = codeChars.clone();
		
		Character maskChar = getMaskAt(index);
		if (maskChar != null) {
			
			// increase the last character and
			// possibly propagate the rest
			
			String values = this.map.get(maskChar);
			
			if (values != null) {
				
				// get the next element and save it
				output[index] = getNext(codeChars[index], values.toCharArray());
				
				// if overflow, then need to carry rest
				if (output[index] == values.charAt(0)) {
					output = increaseCodeRec(output, index - 1);
				}
			}
		}
		
		return output;
	}

	
	private Character getMaskAt(int index) {
		
		if (index >= termCodeMask.length())
			return null;
		
		return termCodeMask.charAt(index);
	}
	
	/**
	 * Get the next character in an array given the current one
	 * @param current
	 * @param values
	 * @return
	 */
	private Character getNext(char current, char[] values) {
		
		Character nextValue = null;
		
		for (int i = 0; i < values.length; i++) {
			if (values[i] == current) {
				if (i < values.length - 1) {
					nextValue = values[i + 1];
				}
				else {
					nextValue = values[0];
				}
			}
		}
		
		return nextValue;
	}

	/**
	 * Create the first code that can be generated with
	 * the catalogue term code mask
	 * @return
	 */
	private String getFirstCode() {
		
		char[] mask = termCodeMask.toCharArray();
		
		char[] code = new char[mask.length];

		for (int i = 0 ; i < mask.length ; i++) {
			
			String replacement = this.map.get(mask[i]);
			
			// if no special character, simply copy the mask value
			if (replacement == null) {
				code[i] = mask[i];
			}
			else {  // otherwise get the first available element for the special character
				code[i] = replacement.charAt(0);
			}
		}
		
		return new String(code);
	}
	
	/**
	 * Get the last code used for the terms of the catalogue
	 * @return
	 * @throws SQLException 
	 */
	private String getLastUsedCode() throws SQLException {
		
		String lastUsedCode = null;
		
		String query = "select max(TERM_CODE) as LAST_USED_CODE from APP.TERM";

		try (Connection con = catalogue.getConnection(); 
			PreparedStatement codeStmt = con.prepareStatement(query);
			ResultSet rs = codeStmt.executeQuery();) {
			
			if (rs.next())
				lastUsedCode = rs.getString("LAST_USED_CODE");
			
			rs.close();
			codeStmt.close();
			con.close();
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw e;
		}
		
		return lastUsedCode;
	}
	
	/*
	public static void main(String[] args) throws SQLException {
		CatalogueDAO dao = new CatalogueDAO();
		Catalogue catalogue = dao.getLastVersionByCode("MDACC", DcfType.PRODUCTION);
		System.out.println(catalogue);
		TermCodeGenerator gen = new TermCodeGenerator(catalogue);
		String last = gen.getLastUsedCode();
		System.out.println(last);
		System.out.println(gen.getFirstCode());
	}*/
}
