package uk.ac.cam.cl.ticking.ui.util;

public class Strings {

	public static final String DBNAME = "UROP_UI";

	public static final String TICKSCOLLECTION = "Ticks";
	public static final String FORKSCOLLECTION = "Forks";
	public static final String GROUPSCOLLECTION = "Groups";
	public static final String USERSCOLLECTION = "Users";
	public static final String GROUPINGSCOLLECTION = "Groupings";
	public static final String AUTHCODESCOLLECTION = "authCodes";

	public static final String[] ACADEMICINSTITUTIONS = { "Computer Laboratory" };

	public static final String INVALIDROLE = "You do not have the required role for that action.";

	public static final String INVALIDPERMISSION = "You do not have the required permissions for that action.";

	public static final String ATLEASTONEROLE = "Error: You must choose at least one role to assign.";

	public static final String DEADLINE = "You have missed the deadline.";

	public static final String FORKED = "You have already forked this repository, state has not been lost.";

	public static final String IDEMPOTENTRETRY = "Oops: Something went wrong, please try again";

	public static final String EXISTINGTIMEBOOKING = "Error: You already have a booking at this time.";

	public static final String EXISTINGTICKBOOKING = "Error: you already have a booking for this tick.";

	public static final String NOFREESLOTS = "Error: there are no free slots at the given time.";

	public static final String EXISTS = "Error: You have already created a tick with this name.";

	public static final String REMOVECREATOR = "Error: You may not remove the creator of the group. Any other users have been removed.";

	public static final String MISSING = "Error: The resource you requested appears to be missing.";

	public static final String DELETED = "Deleted.";

	public static final String NOCOMMITS = "Error: Your submission appears to have no commits";

	public static final String TESTRUNNING = "Error: You still have a test running for this tick";
	
	public static final String BADKEY = "The key you have entered is malformed";
	
	public static final String REMOVEDUSERS = "Successfully removed users";
	
	public static final String TICKISINGROUP = "Error: That tick is already associated with the group.";
	
	public static final String PASSED = "PASSED";
	public static final String FAILED = "FAILED";
	public static final String UNITPASSEDCODE = "UP";
	public static final String UNITFAILEDCODE = "UF";
	public static final String INITCODE = "I";
	public static final String UNITPASSED = "Passed automated tests";
	public static final String UNITFAILED = "Failed automated tests";
	public static final String INITIALISED = "Not submitted";

	/**
	 * Private constructor to prevent this class being created
	 */
	private Strings() {
		// not allowed
	}
}
