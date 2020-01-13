package application;

import java.util.ArrayList;

import javafx.geometry.Point2D;

public class FolderOrigin {

	private Point2D originMedian = new Point2D(0, 0);
	private Point2D originMean = new Point2D(0, 0);
	
	private ArrayList<Double> fileLocationsX = new ArrayList<Double>();
	private ArrayList<Double> fileLocationsY = new ArrayList<Double>();
	
	private Point2D getMedianOrigin() {
		updateMedianOrigin();
		return originMedian;
	}
	
	private Point2D getMeanOrigin() {
		return originMean;
	}

	public Point2D getOrigin(OriginMovementMode mode) {
		if (mode == OriginMovementMode.MEAN) return getMeanOrigin();
		else return getMedianOrigin();
	}

	
	
	public void addFileLocation(Point2D location) {
		if (location == null) return;
		// MEDIAN: store value for later calculation
		fileLocationsX.add(location.getX());
		fileLocationsY.add(location.getY());
		
		//MEAN: calculate directly
		originMean = originMean.midpoint(location);

	}
	
	public FolderOrigin(Point2D origin){
		if (origin == null) return;
		// MEDIAN: store value for later calculation
		fileLocationsX.add(origin.getX());
		fileLocationsY.add(origin.getY());
		
		//MEAN: simply set
		originMean = origin;
	}
	
	private void updateMedianOrigin() {
		
		double medianX = Utils.getMedianOf(fileLocationsX);
		double medianY = Utils.getMedianOf(fileLocationsY);
		originMedian = new Point2D(medianX, medianY);  
		
	}
}
