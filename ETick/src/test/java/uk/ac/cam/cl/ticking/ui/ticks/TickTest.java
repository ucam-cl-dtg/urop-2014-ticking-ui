package uk.ac.cam.cl.ticking.ui.ticks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TickTest {

	@Test
	public void testReplaceDelimeter() {
		assertEquals(",s should be replaces by /s", "crsid/tick_name",Tick.replaceDelimeter("crsid,tick_name"));
	}

}
