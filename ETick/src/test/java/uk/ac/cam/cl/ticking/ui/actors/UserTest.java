package uk.ac.cam.cl.ticking.ui.actors;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UserTest {

	@Test
	public void testCompareTo() {
		User first;
		User second;
		
		first = new User("aa123");
		first.setSurname("aardvark");
		second = new User("zz123");
		second.setSurname("zebra");
		assertTrue("Testing with both having both fields", (first.compareTo(second)<0));
		
		first = new User("aa123");
		first.setSurname("aardvark");
		second = new User("zz123");
		assertTrue("Testing with second lacking surname", (first.compareTo(second)<0));
		
		first = new User("aa123");
		second = new User("zz123");
		second.setSurname("zebra");
		assertTrue("testing with first lacking surname", (first.compareTo(second)<0));
		
		first = new User("aa123");
		second = new User("zz123");
		assertTrue("tesing with no surnames", (first.compareTo(second)<0));
	}

}
