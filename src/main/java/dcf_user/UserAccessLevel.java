package dcf_user;

/**
 * The permission level of the user. Can be 
 * data provider (i.e. read only) or catalogue
 * manager (i.e. edit mode on).
 * @author avonva
 * @author shahaal
 *
 */
public enum UserAccessLevel {
	UNKNOWN,
	DATA_PROVIDER,
	CATALOGUE_MANAGER
}
