package business_rules;

/**
 * This class is used to store all the information related to the forbidden
 * processes for each hierarchy group which contains derivatives. In fact, a
 * warning should be raised if the user applies processes to raw commodities in
 * order to create a derivative and that derivative is already present in the
 * list (with its own code). Moreover, the processes have to be applied with a
 * particular order, therefore we check that order with the ordCode field.
 * 
 * @author avonva
 * @author shahaal
 */
public class ForbiddenProcess {

	// create the variables of interest
	// baseTermGroupCode: the code of hierarchies which could be subjected to
	// warnings
	// forbiddenProcessCode: the code of a forbidden process related to the
	// baseTermGroup selected
	// ordCode: code used to check the order of the process applicability
	String baseTermGroupCode, forbiddenProcessCode;
	double ordCode;

	public ForbiddenProcess(String baseTermGroupCode, String forbiddenProcessCode, double ordCode) {
		this.baseTermGroupCode = baseTermGroupCode;
		this.forbiddenProcessCode = forbiddenProcessCode;
		this.ordCode = ordCode;
	}

	// getter methods

	public String getBaseTermGroupCode() {
		return baseTermGroupCode;
	}

	public String getForbiddenProcessCode() {
		return forbiddenProcessCode;
	}

	public double getOrdCode() {
		return ordCode;
	}
}
