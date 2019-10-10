package catalogue_object;

/**
 * Enumerator used to identify the field of the parent terms in the exported
 * excel file. In particular, we have for each hierarchy 4 fields: flag, parent code,
 * order and reportable. We identify of what field we are talking about with
 * this enumerator.
 * @author avonva
 *
 */
public enum ParentField {
	FLAG,
	PARENT_CODE,
	ORDER,
	REPORTABLE,
	HIERARCHY_CODE
}
