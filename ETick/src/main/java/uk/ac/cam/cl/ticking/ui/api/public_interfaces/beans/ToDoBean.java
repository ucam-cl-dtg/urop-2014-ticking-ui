package uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans;

import java.util.List;

import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

public class ToDoBean {

	private Tick tick;
	private Fork fork;
	
	public ToDoBean(Tick tick, Fork fork) {
		this.tick = tick;
		this.fork = fork;
	}
	
	public ToDoBean() {
		//Default constructor for Jackson
	}

	public Tick getTick() {
		return tick;
	}

	public void setTick(Tick tick) {
		this.tick = tick;
	}

	public Fork getFork() {
		return fork;
	}

	public void setFork(Fork fork) {
		this.fork = fork;
	}
	
	
}
