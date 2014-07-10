package uk.ac.cam.rds46.actors;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public enum Role
{
	SUBMITTER(0),
	MARKER(1),
	AUTHOR(2),
	OVERVIEW(3),
	ADMIN(4);
	
	private final int index;
	
	Role(int index) {
		this.index = index;
	}
	
	public int index() { return index; }
	
	public static final Set<Role> DOS = new HashSet<Role>(Arrays.asList(new Role[]{OVERVIEW}));
	public static final Set<Role> SUPERVISOR = new HashSet<Role>(Arrays.asList(new Role[]{OVERVIEW, AUTHOR, MARKER}));
	public static final Set<Role> TICKER = new HashSet<Role>(Arrays.asList(new Role[]{OVERVIEW, MARKER}));
}
