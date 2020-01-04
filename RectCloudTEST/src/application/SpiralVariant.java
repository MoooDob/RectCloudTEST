package application;

import java.util.LinkedList;


public class SpiralVariant {
	
	public SpiralVariant() {
		super();
	}

	public SpiralPoint getFirstPoint() {
		return spiralpoints.size() > 0 ? spiralpoints.getFirst(): null;
	}

	public int latestIndex = -1;
	public double latestX = 0;
	public double latestY = 0;
	public LinkedList<SpiralPoint> spiralpoints = new LinkedList<SpiralPoint>();
	
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
