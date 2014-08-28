package uk.ac.cam.cl.ticking.ui.api.public_interfaces.beans;

import java.util.List;

import uk.ac.cam.cl.ticking.ui.ticks.Fork;
import uk.ac.cam.cl.ticking.ui.ticks.Tick;

public class ToDoBean {

	private List<Tick> ticks;
	private List<Fork> forks;
	
	public ToDoBean(List<Tick> ticks, List<Fork> forks) {
		this.ticks = ticks;
		this.forks = forks;
	}
	
	public ToDoBean() {
		//Default constructor for Jackson
	}

	public List<Tick> getTicks() {
		return ticks;
	}

	public void setTicks(List<Tick> ticks) {
		this.ticks = ticks;
	}

	public List<Fork> getForks() {
		return forks;
	}

	public void setForks(List<Fork> forks) {
		this.forks = forks;
	}
	
	
}
