package dcf_user;

public interface UserLevelChangedListener {

	/**
	 * Called when the user level changed (i.e. data provider/catalogue manager)
	 */
	public void userLevelChanged(UserAccessLevel newLevel);
}
