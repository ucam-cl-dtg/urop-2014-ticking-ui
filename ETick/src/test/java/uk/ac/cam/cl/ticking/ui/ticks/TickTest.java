package uk.ac.cam.cl.ticking.ui.ticks;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TickTest {

	@Test
	public void testReplaceDelimeter() {
		assertEquals(", should be replaces by /", "crsid/tick_name",Tick.replaceDelimeter("crsid,tick_name"));
	}

}
