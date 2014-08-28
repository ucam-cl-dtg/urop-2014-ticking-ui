package uk.ac.cam.cl.ticking.ui.util;

import java.util.Comparator;

import uk.ac.cam.cl.ticking.ui.ticks.Tick;

public class DeadlineFirstComparator implements Comparator<Tick> {
	
	public int compare(Tick a, Tick b) {
		if (a.getDeadline() == null) {
			if (b.getDeadline() == null) {
				return a.getName().compareToIgnoreCase(b.getName());
			} else {
				return 1;
			}
		} else {
			if (b.getDeadline() == null) {
				return -1;
			} else {
				return a.getDeadline().compareTo(b.getDeadline());
			}
		} 
		
	}
}