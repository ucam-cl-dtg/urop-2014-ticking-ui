package uk.ac.cam.cl.ticking.ui.util;

public class Strings {

	public static final String DBNAME = "UROP_UI";

	public static final String TICKSCOLLECTION = "Ticks";
	public static final String FORKSCOLLECTION = "Forks";
	public static final String GROUPSCOLLECTION = "Groups";
	public static final String USERSCOLLECTION = "Users";
	public static final String GROUPINGSCOLLECTION = "Groupings";

	public static final String[] ACADEMICINSTITUTIONS = { "Computer Laboratory" };

	public static final String INVALIDROLE = "You do not have the required role for that action.";

	public static final String ATLEASTONEROLE = "Error: You must choose at least one role to assign.";

	public static final String DEADLINE = "You have missed the deadline.";

	public static final String FORKED = "You have already forked this repository, state has not been lost.";

	public static final String EXISTS = "Error: You have already created a tick with this name.";
	
	public static final String REMOVECREATOR = "Error: You may not remove the creator of the group.";

	/**
	 * Private constructor to prevent this class being created
	 */
	private Strings() {
		// not allowed
	}
}
