package dcf_user;

public interface UserListener {

	/**
	 * Called when the user level changed (i.e. data provider/catalogue manager)
	 */
	public void userLevelChanged(UserAccessLevel newLevel);
	
	/**
	 * Called when the user logs in or logs out.
	 * @param connected
	 */
	public void connectionChanged(boolean connected);
}
