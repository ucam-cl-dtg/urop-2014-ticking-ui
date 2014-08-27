package uk.ac.cam.cl.ticking.ui.util;

import java.util.Comparator;

class IgnoreCaseComparator implements Comparator<String> {
	public int compare(String strA, String strB) {
		return strA.compareToIgnoreCase(strB);
	}
}