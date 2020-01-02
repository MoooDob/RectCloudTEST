package application;

import java.util.LinkedList;

import org.apache.commons.collections4.list.CursorableLinkedList;

public class SpiralInfo {
	
	public SpiralInfo() {
		super();
	}

	public SpiralPoint getFirstPoint() {
		return spiralpoints.size() > 0 ? spiralpoints.getFirst(): null;
	}

	public int latestIndex = -1;
	public double latestX = 0;
	public double latestY = 0;
	public CursorableLinkedList<SpiralPoint> spiralpoints = new CursorableLinkedList<SpiralPoint>();
	
	public void appendSpiralPoint(SpiralPoint spiralpoint) {
		spiralpoints.add(spiralpoint);
		// Update 'last' informations if necessary
		if (spiralpoint.index > latestIndex) {
			latestIndex = spiralpoint.index;
			latestX = spiralpoint.x;
			latestY = spiralpoint.y;
		}
	}	
}
