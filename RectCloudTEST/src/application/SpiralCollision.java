package application;

import java.io.File;

import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.Comparator;
import java.util.HashMap;
import java.util.ListIterator;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;

import javafx.animation.FadeTransition;
import javafx.animation.FileWalker;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.animation.Animation.Status;



enum Sorting {
   BY_PATH, 
   BY_PARENTFOLDER, 
   BY_LEVEL_ASC, BY_LEVEL_DESC, 
   BY_AREA_ASC, BY_AREA_DESC, 
   BY_WIDTH_ASC, BY_WIDTH_DESC, 
   BY_HEIGHT_ASC, BY_HEIGHT_DESC; 
}


public class SpiralCollision extends Application {

	final boolean showFiles = true;
	final double scene_width = 1000;
	final double scene_height = 800;
	final int numOfRectangles = 10;
	final double maxRectHeight = scene_width / 8;
	final double maxRectWidth = scene_height / 9;
	
	final Sorting sorting = Sorting.BY_LEVEL_ASC;

	final double defaultWidth = 12;
	final double defaultHeight = 12;

	public static long seed = 324;

	final double drawingSpeed = 10f;
	final double fadeOffset = 1 / drawingSpeed;
	final double rectFadingSpeed = 1 / drawingSpeed * 3;

	final int maxSteps = 1000;
	// density of winding
	final double spinRate = 0.5;
	// number of probes per wind = distance of the probe
	final double probeRate = 0.1;

	// increment while getting closer to the center of the scene
	final double fineTuningIncrementRate = 10;

	// scaling for the width and height of a file
	public double fileScale = 5;

	// max number of check positions on the spiral, -1 = no limit
	public int maxSpiralPositions = 1000;

	// man number of steps for fine tuning
	public int maxFineTuningSteps = 100;

	// false = leave the center of all spirals at the origin
	// true = move the center of the current spiral to the center of the first placed rectangle 
	public boolean moveSpiralOrigin = true;
	
	// if moveSpiralCenter == true
	// false = leave the center of the current spiral at center of the first placed rectangle
	// true = move the center of the current spiral to the center of mass (mean of locations) of all rectangles in this folder
	public boolean moveSpiralCenter = true;
	
	
	// true = always start search at origin
	// false = start search at origin of the (pre)parent folder 
	public boolean alwaysStartSearchAtOrigin = false;

	public OriginMovementMode originMovementMode = OriginMovementMode.MEDIAN;
	
	public double rasterX = 100;
	public double rasterY = 100;
	public double rasterOffsetX = 0; // rasterX / 2;
	public double rasterOffsetY = 0; // rasterY / 2;
	
	
	public final static boolean DEBUG_DRAW = true;
	public final static boolean DEBUG_PRINT = true;

	// Files with this extension will be shown, null or empty array => all files 
	final String[] fileExtensionFilter = {}; //{"java"}; // {"java", "cpp", "h"} // null /*=> all*/

	// files with this extension will shown using their dimension (max line length x #lines),
	// files with other extensions will be shown using an equal sized rectangle
	// null or empty array => show all files with dimensions
	final String[] dimensionDisplayExtensionFilter = {}; // {"java"}

	// files with this file name will be explicitly shown using their dimension 
	// (max line length x lines)
	final String[] dimensionDisplayFilenameFilter = {}; // {"readme.md"}

	// **************************

	//	private final ArrayList<Rectangle> testedRectangles = new ArrayList<Rectangle>();

	//	private final Timeline OLD_spiralCollisionCheckAnimationTimeline = new Timeline();
	// HSB color space, limits for the hue value
	private final float HUE_MIN = .1f;
	private final float HUE_MAX = .9f;

	private final ArrayList<FileInfo> files = new ArrayList<FileInfo>();
	private final HashMap<String, Double> fileSizeInfo = new HashMap<String, Double>();
	
	// Store widths and heights of all files for calculation of median
	private final ArrayList<Double> fileHeights = new ArrayList<Double>();
	private final ArrayList<Double> fileWidths = new ArrayList<Double>();

	
	private final Label label = new Label();
	private FadeTransition spiralFader;

	private File rootDirectory;
	private final int numRectOrientationsAndAlignments = 4;
	private Rectangle[] testRects = new Rectangle[numRectOrientationsAndAlignments + 1];

	private final static Random randomizer = new Random(seed);

	private Timeline fileTimeline = new Timeline();
	private Timeline spiralPositionTimeline = new Timeline();
	private Timeline orientationAndAlignmentTimeline = new Timeline();
	private Timeline finePositioningTimeline = new Timeline();

	private final FileEventHandler fileEventHandler = new FileEventHandler();
	private final SpiralEventHandler spiralPositionEventHandler = new SpiralEventHandler();
	private final OrientationEventHandler orientationEventHandler = new OrientationEventHandler();
	private final FinePositioningEventHandler fineTuningEventHandler = new FinePositioningEventHandler();

	private Canvas spiralCanvas; // set in "start'
	private final Pane testRectCloud = new Pane();
	private final Pane rectCloud = new Pane();
	private final Pane spiralOriginsCloud = new Pane();
	
	private float totalNumOfLevels = -1;
	private Canvas FineTuningCanvas;
	private FadeTransition FineTuningFader;
	
	// origin of the drawing area
	private Point2D origin =  new Point2D(scene_width / 2, scene_height / 2);

	public final int numOfSpiralVariants = 1;
	public final double variantOffset = 1; 

	
	// array of all spiral points of all spiral start variants
	// each SpiralPoint contains informations about the checked test rectangle orientations 
	// and their minimal tested dimensions. 
	// a spiral point will be deleted from the array if the last test rectangle with a minimal size collides 
	// with something (that means, there is already a rectangle exactly at this point)
	// finally the array contains spiral points that had to be checked 
	// the larger the spiral grows, the larger the array grows
	// if points at the end are missing, the lastIndex, lastX and lastY fields contain the highest index, x and y values
	private final HashMap<String, SpiralInfo> spirals = new HashMap<String, SpiralInfo>();
	
	private Canvas guidesCanvas;
		
	private final HashMap<String, FolderOrigin> folderOrigins = new HashMap<String, FolderOrigin>();
	
	// Origin of the canvas
	private Point2D canvasOrigin = new Point2D(scene_width / 2, scene_height / 2);

	


	private class FileEventHandler implements EventHandler<ActionEvent> {

		private int numFiles = 0;
		private String filename = "";
		private double fileWidth = 0;
		private double fileHeight = 0;
		private int fileIndex = 0;
		//private double x0;
		//private double y0;
		private int level;
		private float hue;
		private String parentFolder;
		
		// set spiral origin of this file to origin
		private Point2D spiralOriginOfParentDirectory = null;

		private boolean isFirstFileInParentFolder = false;
		private Color parentFolderColor;

		public void handle(ActionEvent event) {
			
			fileTimeline.pause();
			
			fileIndex++;
			if (fileIndex >= numFiles) {
				OnFinished();
			}


			// skip files with width == 0 or height == 0
			while (fileIndex < numFiles &&
					((fileWidth = files.get(fileIndex).width / fileScale) == 0
					|| (fileHeight = files.get(fileIndex).height / fileScale) == 0) 
					) {
				fileIndex++;
			}

			if (fileIndex < numFiles) {

				filename = files.get(fileIndex).filename;
				parentFolder = new File(filename).getParent();
				level = files.get(fileIndex).level;
				hue = files.get(fileIndex).hue;
				
				// Calculate color
				float brightness = HUE_MIN + (HUE_MAX - HUE_MIN) * fileEventHandler.level / totalNumOfLevels;
				parentFolderColor = ConvertAwtColorToJavaFX(
						java.awt.Color.getHSBColor(fileEventHandler.hue, 1.0f, brightness));
				
				isFirstFileInParentFolder = false;

				// set Info label
				String info = "find location for file #" + fileIndex + "/" + numFiles + " " + filename + " width: " + fileWidth + " height: "
						+ fileHeight;
				if (DEBUG_PRINT) System.out.println(info);
				label.setText(info);

				if (DEBUG_PRINT) System.out.println("fileindex: " + fileIndex + " out of 0.." + (numFiles - 1) + " (" + filename + ", "
						+ fileWidth + " x " + fileHeight + ")");
							
				// Move spiral origin
				
					// set spiral origin of this file to origin
					spiralOriginOfParentDirectory = new Point2D(origin.getX(), origin.getY());
					
					// Move it to the first occurrence / the center of mass of the current folder?
					// (or leave the center of all spirals at the origin?) 
					if (moveSpiralOrigin) {
										
						// search for center of a spiral for the current parent folder
						if (folderOrigins.containsKey(parentFolder)) {
							// origin of the spiral for this file is the spiral orgin of the parent folders
							spiralOriginOfParentDirectory = folderOrigins.get(parentFolder).getOrigin(originMovementMode);
						} else {
							// its the first file in this folder
							isFirstFileInParentFolder = true;
							if (alwaysStartSearchAtOrigin) {
								// start search at the origin 
								spiralOriginOfParentDirectory = origin; 
							}
							else {
								// lookup for a parent folder declaration in the (pre)parent folders 
								String preParentFolder = parentFolder; 
								boolean foundPreParentFolder;
								// Loop until you find a parent folder
								while ( ! (foundPreParentFolder = folderOrigins.containsKey(preParentFolder)) ) {
									preParentFolder = new File(preParentFolder).getParent();
								}
								
								if (foundPreParentFolder) {
									spiralOriginOfParentDirectory = folderOrigins.get(preParentFolder).getOrigin(originMovementMode); 
								}
								else {
									// this should never happen!
								}
							}							
							
							// no spiral center stored till now, so store it
							folderOrigins.put(parentFolder, new FolderOrigin(spiralOriginOfParentDirectory));

						}																
						
					}
				
				if (DEBUG_PRINT) System.out.println("  spiral origin: " + spiralOriginOfParentDirectory + " (parent: " + parentFolder + ")");				
				if (DEBUG_DRAW) {
					Circle circle = new Circle(spiralOriginOfParentDirectory.getX(), spiralOriginOfParentDirectory.getY(), 3);
					circle.setFill(parentFolderColor);
					circle.setStroke(Color.YELLOW);
					spiralOriginsCloud.getChildren().add(circle);
//					SpiralOriginsCanvas.getGraphicsContext2D().setFill(parentFolderColor);
//					SpiralOriginsCanvas.getGraphicsContext2D().fillOval(spiralOriginOfParentDirectory.getX() - 5/2, spiralOriginOfParentDirectory.getY() - 5/2, 5, 5); 
				}

				spiralPositionEventHandler.init(rasterize(spiralOriginOfParentDirectory));
				spiralPositionTimeline.play();
				
			};

		}

		public void init() {
			
			this.fileIndex = -1;
			this.numFiles = files.size();

		}

		private void OnFinished() {
			System.out.println("finished.");
			label.setText("finished.");
			spiralFader.playFromStart();
			FineTuningFader.playFromStart();
			guidesCanvas.setVisible(false);

		}

	}

	private class SpiralEventHandler implements EventHandler<ActionEvent> {

		int spiralIndex = 0;

		private double x = 0;
		private double y = 0;

		private double prev_x = 0;
		private double prev_y = 0;

		private int variant = -1;

		private ListIterator<SpiralPoint> spiralPointIterator;
		private SpiralPoint currentSpiralPoint;

		private SpiralVariant currentSpiral;

		private Point2D spiralOrigin;

		@Override
		public void handle(ActionEvent event) {

			spiralPositionTimeline.pause();
						
			currentSpiralPoint = null;
			
			if (spiralPointIterator != null) {
				
				boolean hasNextPoint;
				// remove all pending remove spiral points
				// (pending remove spiral points are necessary because adding and 
				// removing on an iterator is not so easy because of concurrency)				
				while ((hasNextPoint = spiralPointIterator.hasNext()) 
						&& (currentSpiralPoint = spiralPointIterator.next()).pendingRemove
						) {
					spiralPointIterator.remove();
				}
				
				if (hasNextPoint) {
					// next spiral point set, so get index, x and y
					x = currentSpiralPoint.x;
					y = currentSpiralPoint.y;
					spiralIndex = currentSpiralPoint.index;
					
					if (DEBUG_PRINT) System.out.println(String.format(
							Locale.US, 
							"  got point %d @ %.2f/%.2f",
							spiralIndex, x, y));
				} else currentSpiralPoint = null;
				
			}
			
			// no next spiral point, cause...								
			if (currentSpiralPoint == null 
					&& spiralIndex <= currentSpiral.latestIndex
					) {
				// spiralIndex smaller than highest index, cause there are some points missing at the end
				// start with next index after the last one, calculate the new x and y coordinates
				// and append the new spiral point to the spiral point list  
				spiralIndex = currentSpiral.latestIndex + 1;
				x = currentSpiral.latestX;
				y = currentSpiral.latestY;
				
				// calculate next point in the spiral of Theodorus (german: Wurzelspirale)
				
					// store the old x and y
					prev_x = x;
					prev_y = y;

					double h = Math.sqrt((Math.pow(x * probeRate, 2) + Math.pow(y * probeRate, 2))) * spinRate;
					x = prev_x - prev_y / h;
					y = prev_y + prev_x / h;
					
				// create and append new spiral point, update the 'latest' fields accordingly 
				currentSpiralPoint = new SpiralPoint(spiralIndex, x, y);
				currentSpiral.appendSpiralPoint(currentSpiralPoint);
				spiralPointIterator = null;
				
				if (DEBUG_PRINT) System.out.println("  created and appended new spiral point " + spiralIndex + " @ " + x + "/" + y);

			} else {
				//spiralIndex larger then the stored highest index: this should never happen!
			}


			// Draw spiral, center point
			if (DEBUG_DRAW) {
				spiralCanvas.getGraphicsContext2D().strokeLine(CartesianToCanvasX(spiralOrigin.getX(), x), CartesianToCanvasY(spiralOrigin.getY(), y),
					CartesianToCanvasX(spiralOrigin.getX(), prev_x), CartesianToCanvasY(spiralOrigin.getY(), prev_y));
				spiralCanvas.getGraphicsContext2D().fillOval(CartesianToCanvasX(spiralOrigin.getX(), x) - 5 / 2,
					CartesianToCanvasY(spiralOrigin.getY(), y) - 5 / 2, 5, 5);
			}

			if (maxSpiralPositions > 0 && spiralIndex >= maxSpiralPositions) {
				System.out.println("  file '" + fileEventHandler.filename + "': collision in more than "
						+ maxSpiralPositions + " spiral positions, canceled. Next file.");
				OnFinished();
			} else {
				if (DEBUG_PRINT) System.out.println("  ->test spiral point " + spiralIndex + " (variant: " + variant + ") x=" + x + " y=" + y);
				orientationEventHandler.init(x, y);
				orientationAndAlignmentTimeline.play();
			}

		}
		

		public void init(Point2D spiralOrigin) {
			
			this.spiralOrigin = spiralOrigin;

			prev_x = 0;
			prev_y = 0;

			//variant++;
			if (variant < 0 || variant >= numOfSpiralVariants) {
				variant = 0;
			} 
			if (DEBUG_PRINT) System.out.println("  spiral variant: " + variant + " @ " + spiralOrigin);
			
			String spiralOriginKey = getSpiralOriginAsKey(spiralOrigin);
			if ( ! spirals.containsKey(spiralOriginKey)) {
				SpiralInfo spiralinfo = new SpiralInfo(numOfSpiralVariants);
				spirals.put(spiralOriginKey, spiralinfo);
				appendDefaultSpiralVariants(spiralinfo, spiralOrigin);
			}
			currentSpiral = spirals.get(spiralOriginKey).spiralvariants[variant];
			spiralPointIterator = currentSpiral.spiralpoints.listIterator();
			spiralIndex = 0;

			if (spiralFader != null)
				spiralFader.stop();
			if (FineTuningFader != null)
				FineTuningFader.stop();

			spiralCanvas.setOpacity(1);
			spiralCanvas.getGraphicsContext2D().clearRect(0, 0, spiralCanvas.getWidth(), spiralCanvas.getHeight());
			spiralCanvas.setVisible(true);

			// mark center of spiral
			if (DEBUG_DRAW) spiralCanvas.getGraphicsContext2D().fillOval(spiralOrigin.getX() - 5 / 2, spiralOrigin.getY() - 5 / 2, 5, 5);

		}

		
		private void OnFinished() {
			spiralPositionTimeline.stop();

			// next spiral pos
			fileTimeline.play();
		}

		
	}

	private class OrientationEventHandler implements EventHandler<ActionEvent> {

		private int orientationIndex;
		private double rectHeight;
		private double rectWidth;
	
		// shortcut 
		private Dimension currentSpiralPointOrientation;
		private Point2D spiralOrigin;

		@Override
		public void handle(ActionEvent event) {
			
			orientationAndAlignmentTimeline.pause();
			
			boolean locationOccupiedTest = orientationIndex >= numRectOrientationsAndAlignments;
			
			if (DEBUG_PRINT) System.out.println(
					"    Orientation " + orientationIndex + " out of 0.." + (numRectOrientationsAndAlignments + 1 - 1));

			Rectangle currentTestRect = testRects[orientationIndex];
			if (DEBUG_DRAW) addTestRectangleToTestRectCloud(
					currentTestRect.getX(), 
					currentTestRect.getY(), 
					currentTestRect.getWidth(),
					currentTestRect.getHeight(), 
					null /* use default color */
			);

			if (DEBUG_PRINT) System.out.print(String.format("    test " + (orientationIndex == 4 ? "square" : "rect")+ " x= %.2f y= %.2f w= %.2f h= %.2f : ", 
					currentTestRect.getX(),
					currentTestRect.getY(), 
					currentTestRect.getWidth(), 
					currentTestRect.getHeight()
			));

			
			// fast collision check ...
			boolean collision = false;
			// .. but only if necessary ( no smaller rectangle tested before on this position and do NOT test with minimal test rectangle ) 
			if ( ! locationOccupiedTest ) {
				Dimension currentStoredMinimalTestDimensions = spiralPositionEventHandler.currentSpiralPoint.testOrientations[orientationIndex];				
				collision  = currentTestRect.getWidth() > currentStoredMinimalTestDimensions.width 
						|| currentTestRect.getHeight() > currentStoredMinimalTestDimensions.height;
			}
			if (DEBUG_PRINT) System.out.print(collision ? "fast collision - ":"no fast collision - ");

			// slow check collision ...
			if ( ! collision ) {
				// collision check is necessary because previously checked testRects were always bigger (bigger width and bigger height) 
				collision = checkCollisonAABB(currentTestRect.getX(), currentTestRect.getY(),
						currentTestRect.getWidth(), currentTestRect.getHeight());
			}
			if (DEBUG_PRINT) System.out.println(collision ? "collision":"no collision");
			
			if ( ! collision && ! locationOccupiedTest ) {
				// collision free position found

				// stop orientation
				orientationAndAlignmentTimeline.stop();

				// reset finePos
				fineTuningEventHandler.init(currentTestRect);
				finePositioningTimeline.play();

			} else {
				// replace width and height of test orientation of spiral points, if necessary

				// Orientation 0..3: 4 variants of orientations
				if ( ! locationOccupiedTest ) {
					
					currentSpiralPointOrientation = spiralPositionEventHandler.currentSpiralPoint.testOrientations[orientationIndex];

					// current file dimension smaller than the smallest previously checked? 
					if (currentTestRect.getWidth() < currentSpiralPointOrientation.width 
						&& currentTestRect.getHeight() < currentSpiralPointOrientation.height  ) {
						currentSpiralPointOrientation.width = currentTestRect.getWidth();
						currentSpiralPointOrientation.height = currentTestRect.getHeight();
					}

				} else {
					// orientationIndex == 4 => minimal test square collision
					// remove spiralInfo for this spiral coordinate to mark this point as not to be checked any more in future
					if (DEBUG_PRINT) System.out.println("    -> remove spiral point");
					spiralPositionEventHandler.currentSpiralPoint.pendingRemove = true;
				}
			}	
				
			orientationIndex++;
			if (orientationIndex >= (numRectOrientationsAndAlignments + 1)) {
				OnFinished();
				return;
			} else if (orientationAndAlignmentTimeline.getStatus() == Status.PAUSED) 
				orientationAndAlignmentTimeline.play();
	
		}

		/**
		 * @param templateRect
		 * @return
		 */

		public void init(double x, double y) {
			
			// shortcut
			this.spiralOrigin = spiralPositionEventHandler.spiralOrigin;
			
			orientationIndex = 0;

			this.rectWidth = fileEventHandler.fileWidth;
			this.rectHeight = fileEventHandler.fileHeight;

			// use x and y as center point for the testRects
			
			// portrait rectangle, center point
			testRects[0] = new Rectangle( 
					CartesianToCanvasX(spiralOrigin.getX(), x) - rectWidth / 2, 
					CartesianToCanvasY(spiralOrigin.getY(), y) - rectHeight / 2, 
					rectWidth, 
					rectHeight
			); 
			
			// landscape rectangle, center point
			testRects[1] = new Rectangle(
					CartesianToCanvasX(spiralOrigin.getX(), x) - rectHeight / 2, 
					CartesianToCanvasY(spiralOrigin.getY(), y) - rectWidth / 2, 
					rectHeight, 
					rectWidth
			); 

			testRects[2] = new Rectangle(
					CartesianToCanvasX(spiralOrigin.getX(), x) - (x < 0 ? rectWidth : 0), 
					CartesianToCanvasY(spiralOrigin.getY(), y) - (y > 0 ? rectHeight : 0), 
					rectWidth,
					rectHeight
			);
			testRects[3] = new Rectangle(
					CartesianToCanvasX(spiralOrigin.getX(), x) - (x < 0 ? rectHeight : 0), 
					CartesianToCanvasY(spiralOrigin.getY(), y) - (y > 0 ? rectWidth : 0),
					rectHeight, 
					rectWidth
			);

			// minimal test square with center pivot for testing the general collision on this point
			testRects[4] = new Rectangle(
					CartesianToCanvasX(spiralOrigin.getX(), x) - 0.5, 
					CartesianToCanvasY(spiralOrigin.getY(), y) - 0.5, 
					1, 
					1
			);

			// Shuffle testRects to get a random orientation/alignment
			shuffleArrayDurstenfeld(testRects, 0, 1);
			shuffleArrayDurstenfeld(testRects, 2, testRects.length - 2); // without minimal test square
		}

		private void OnFinished() {
			orientationAndAlignmentTimeline.stop();
			// next spiral pos
			spiralPositionTimeline.play();
		}
	}

	private class FinePositioningEventHandler implements EventHandler<ActionEvent> {

		private static final int DIRECTION_X = 0;
		private static final int DIRECTION_Y = 1;

		private int fineTuningStep = 0;

		private double prev_x = 0;
		private double prev_y = 0;
		private double ny;
		private double nx;
		private double x;
		private double y;
		private double len;
		private double width;
		private double height;

		private Rectangle currentTestRect;

		private double canvas_x;
		private double canvas_y;

		private double offset_x;
		private double offset_y;

		// direction array for decide in which direction to move the rectangle
		private boolean[] direction = new boolean[2];
		// number of movement direction combinations
		private int fineTuningDirectionStep;
		
		//private Point2D spiralOrigin;

		@Override
		public void handle(ActionEvent event) {
			
			finePositioningTimeline.pause();

			boolean collision = false;
			boolean crossedXAxis = false;
			boolean crossedYAxis = false;

			boolean toMuchFineTuningSteps = fineTuningStep >= maxFineTuningSteps;
			if (toMuchFineTuningSteps) {
				// takes longer than expected
				System.out.println("      fine tuning needed more than " + maxFineTuningSteps
						+ " to find a better location, canceled. Use last good.");
			} else {
				// check collision

				//finePositioningTimeline.pause();
				if (DEBUG_PRINT) System.out.println("      Fine tuning step " + fineTuningStep + " out of 0.." + (maxFineTuningSteps - 1));

				// store last good location
				prev_x = x;
				prev_y = y;

				// calculate new location to be tested
				if (direction[DIRECTION_X])
					x = prev_x - offset_x;
				if (direction[DIRECTION_Y])
					y = prev_y - offset_y;
				if (DEBUG_PRINT) System.out.println(String.format("       %s x= %.2f %s y= %.2f ", direction[DIRECTION_X] ? "new":"old",x, direction[DIRECTION_Y] ? "new":"old",y));

				// create new test rectangle
				canvas_x = CartesianToCanvasX(origin.getX(), x);
				canvas_y = CartesianToCanvasY(origin.getY(), y);
				if (DEBUG_DRAW) currentTestRect = addTestRectangleToTestRectCloud(canvas_x, canvas_y, width, height, null);

				if (direction[DIRECTION_X])
					crossedXAxis = Math.signum(prev_x + width / 2) != Math.signum(x + width / 2);
				if (direction[DIRECTION_Y])
					crossedYAxis = Math.signum(prev_y - height / 2) != Math.signum(y - height / 2);

				if (DEBUG_DRAW) {
					// debug draw location on FineTuningCanvas
					double prev_canvas_x = CartesianToCanvasX(origin.getX(), prev_x);
					double prev_canvas_y = CartesianToCanvasY(origin.getY(), prev_y);
					FineTuningCanvas.getGraphicsContext2D().strokeLine(prev_canvas_x, prev_canvas_y, canvas_x, canvas_y);
					FineTuningCanvas.getGraphicsContext2D().fillOval(canvas_x - 5 / 2, canvas_y - 5 / 2, 5, 5);
				}

				// check collision at new location 
				collision = checkCollisonAABB(canvas_x, canvas_y, width, height);

			}

			if (collision || crossedXAxis || crossedYAxis || toMuchFineTuningSteps) {

				// reset to last good

				// debug output
				ArrayList<String> sl = new ArrayList<String>();
				if (collision) {
					sl.add("collision"); 	
					if (DEBUG_DRAW) {
						Paint fill = FineTuningCanvas.getGraphicsContext2D().getFill();
						FineTuningCanvas.getGraphicsContext2D().setFill(Color.RED);
						FineTuningCanvas.getGraphicsContext2D().fillOval(canvas_x - 5 / 2, canvas_y - 5 / 2, 5, 5);
						FineTuningCanvas.getGraphicsContext2D().setFill(fill);
						currentTestRect.setStroke(Color.RED);
					}
				}

				if (crossedXAxis) {
					sl.add("crossed x axis"); 			
					if (DEBUG_DRAW) {
						Paint fill = FineTuningCanvas.getGraphicsContext2D().getFill();
						FineTuningCanvas.getGraphicsContext2D().setFill(Color.ORANGE);
						FineTuningCanvas.getGraphicsContext2D().fillOval(canvas_x - 5 / 2, canvas_y - 5 / 2, 5, 5);
						FineTuningCanvas.getGraphicsContext2D().setFill(fill);
						currentTestRect.setStroke(Color.ORANGE);
					}
				}

				if (crossedYAxis) {
					sl.add("crossed y axis");
					if (DEBUG_DRAW) {
						Paint fill = FineTuningCanvas.getGraphicsContext2D().getFill();
						FineTuningCanvas.getGraphicsContext2D().setFill(Color.ORANGE);
						FineTuningCanvas.getGraphicsContext2D().fillOval(canvas_x - 5 / 2, canvas_y - 5 / 2, 5, 5);
						FineTuningCanvas.getGraphicsContext2D().setFill(fill);
						currentTestRect.setStroke(Color.ORANGE);
					}
				}
				if (toMuchFineTuningSteps) sl.add("to much fine tuning steps");
				sl.stream().collect(Collectors.joining(" and "));
				if (DEBUG_PRINT) System.out.println("      -> " + String.join(" and ", sl) + String.format(Locale.US, "! Reset to last good: %.2f/%.2f", prev_x, prev_y));
				
				if (DEBUG_DRAW) if (collision)
					currentTestRect.setStroke(Color.RED);

				x = prev_x;
				y = prev_y;

				fineTuningDirectionStep++;

				// fineTuningDirectionStep = 0 : both directions
				// fineTuningDirectionStep = 1 : only one direction (x or y, randomly selected)
				// fineTuningDirectionStep = 2 = the other direction
				// fineTuningDirectionStep = 3 = finish

				// decide future strategy
				if (fineTuningDirectionStep == 1) {
					// deactivate randomly one direction calculation
					int rndDirection = randomizer.nextInt(2);
					direction[rndDirection] = false;

					// already crossed axis in selected direction?
					if ((crossedXAxis && direction[DIRECTION_X]) || (crossedYAxis && direction[DIRECTION_Y])) {
						// already crossed axis in selected direction --> next step
						fineTuningDirectionStep++;
					} else {
						if (DEBUG_PRINT) System.out.println("       try again in " + (direction[DIRECTION_X] ? "X direction" : "Y direction"));
					}

				}

				if (fineTuningDirectionStep == 2) {
					// use the other direction
					direction[DIRECTION_X] = !direction[DIRECTION_X];
					direction[DIRECTION_Y] = !direction[DIRECTION_Y];
					if ((crossedXAxis && direction[DIRECTION_X]) || (crossedYAxis && direction[DIRECTION_Y])) {
						// already crossed axis in selected direction --> next step
						fineTuningDirectionStep++;
					} else {
						if (DEBUG_PRINT) System.out.println("       try again in " + (direction[DIRECTION_X] ? "X direction" : "Y direction"));
					}

				}

				if (fineTuningDirectionStep >= 3) {

					// create new rectangle
					addNewRectangleToRectCloud(CartesianToCanvasX(origin.getX(), prev_x), CartesianToCanvasY(origin.getY(), prev_y),
							width, height);
					
					Point2D prevSpiralOrigin = new Point2D(
							fileEventHandler.spiralOriginOfParentDirectory.getX(), 
							fileEventHandler.spiralOriginOfParentDirectory.getY()
					);
					
					// update spiral origin if needed
					if (moveSpiralOrigin) {
						
						Point2D CenterPointOfRect = new Point2D(
								CartesianToCanvasX(origin.getX(), prev_x) + width / 2, 
								CartesianToCanvasY(origin.getY(), prev_y) + height / 2
								);

						if (fileEventHandler.isFirstFileInParentFolder) {
							// finally set the origin of the parent folder to the center location of the rectangle
							folderOrigins.put(fileEventHandler.parentFolder, new FolderOrigin(CenterPointOfRect));			
							if (DEBUG_PRINT) {
								Point2D parentFolderPoint = folderOrigins.get(fileEventHandler.parentFolder).getOrigin(originMovementMode);
								System.out.println(String.format(Locale.US,"        spiral origin set to %.2f/%.2f for folder %s (first Rectangle)",
									parentFolderPoint.getX(), 
									parentFolderPoint.getY(),
									fileEventHandler.parentFolder
								));
							}
						} else {
						
							// move it to the center of mass / the median center of the current folder?
							// (or leave the spiral center to the point of the first occurrence?) 
							if (moveSpiralCenter) {
								
								// set the origin of the parent folder to the mean of the locations 
								// of the previous origin and the center of the new rectangle  
								((FolderOrigin)folderOrigins.get(fileEventHandler.parentFolder)).addFileLocation(CenterPointOfRect);
								
								if (DEBUG_PRINT) { 
									Point2D parentFolderPoint = folderOrigins.get(fileEventHandler.parentFolder).getOrigin(originMovementMode);
									System.out.println(String.format(Locale.US,"        spiral origin updated (" + originMovementMode.name() + ") to %.2f/%.2f for folder %s",															
											parentFolderPoint.getX(), 
											parentFolderPoint.getY(),
											fileEventHandler.parentFolder
									));
								}
	
							}
							
						}
						
					}

					// 
					if (DEBUG_DRAW) {
						Point2D parentFolderPoint = null;
						parentFolderPoint = folderOrigins.get(fileEventHandler.parentFolder).getOrigin(originMovementMode);
						Line line = new Line(parentFolderPoint.getX(), parentFolderPoint.getY(), prevSpiralOrigin.getX(), prevSpiralOrigin.getY());
						line.setFill(fileEventHandler.parentFolderColor);
						line.setStroke(Color.YELLOW);
						spiralOriginsCloud.getChildren().add(line);
						
//						SpiralOriginsCanvas.getGraphicsContext2D().strokeLine(parentFolderPoint.getX(), parentFolderPoint.getY(), prevSpiralOrigin.getX(), prevSpiralOrigin.getY());
						
						Circle circle = new Circle(parentFolderPoint.getX(), parentFolderPoint.getY(), 3);
						circle.setFill(Color.VIOLET);
						circle.setStroke(Color.YELLOW);
						spiralOriginsCloud.getChildren().add(circle);
						
//						SpiralOriginsCanvas.getGraphicsContext2D().setFill(Color.VIOLET);
//						SpiralOriginsCanvas.getGraphicsContext2D().fillOval(parentFolderPoint.getX() - 6/2, parentFolderPoint.getY() - 6/2, 6, 6);
					}


					// if finally no fine tuning was possible
					if (fineTuningStep == 0) {
						if (spiralPositionEventHandler.currentSpiral.spiralpoints.contains(spiralPositionEventHandler.currentSpiralPoint)
								&& spiralPositionEventHandler.currentSpiral.spiralpoints.size() > 1) {
							// remove spiral point to mark this spiral location is already occupied
							if (DEBUG_PRINT) System.out.println("    -> remove spiral point");
							spiralPositionEventHandler.currentSpiralPoint.pendingRemove = true;
						}
					}					
				}

			} 

			fineTuningStep++;
			
			if (fineTuningDirectionStep < 3) {
				finePositioningTimeline.play();
			} else {
				OnFinished();
			}

		}

		
		public void init(Rectangle rect) {
			init(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
		}
		
//		public void init(BoundingBox box) {
//			init(box.get .getX(), box.getY(), box.getWidth(), box.getHeight());
//		}
		
		public void init(double canvas_x, double canvas_y, double w, double h) {

			this.width = w;
			this.height = h;

			fineTuningStep = 0;

			// calculate both directions
			direction[DIRECTION_X] = true;
			direction[DIRECTION_Y] = true;
			fineTuningDirectionStep = 0;

			// convert canvas coordinates to Cartesian coordinates, x0 and y0 is the center point
			x = CanvasToCartesianX(origin.getX(), canvas_x);
			y = CanvasToCartesianY(origin.getY(), canvas_y);
			if (DEBUG_PRINT) System.out.println(String.format("     canvas_x= %.2f canvas_y= %.2f", canvas_x, canvas_y));
			if (DEBUG_PRINT) System.out.println(String.format("     x= %.2f y= %.2f", x, y));

			// calculate center point of the rectangle
			double center_x = x + width / 2;
			double center_y = y - height / 2;
			
			// Calculate length of the vector between center point of the rectangle 
			// and the origin of the Cartesian coordinates (0,0) 
			len = Math.hypot(center_x, center_y); // = sqrt(center_x², center_y²)

			// center vector normalized, x and y components 
			nx = center_x / len;
			ny = center_y / len;

			offset_x = nx * fineTuningIncrementRate;
			offset_y = ny * fineTuningIncrementRate;

			if (DEBUG_PRINT) System.out.println(String.format("     offset_x= %.2f offset_y= %.2f", offset_x, offset_y));

			// clear canvas
			FineTuningCanvas.getGraphicsContext2D().clearRect(0, 0, FineTuningCanvas.getWidth(),
					FineTuningCanvas.getHeight());
			
			// show Canvas
			FineTuningCanvas.setOpacity(1);
			FineTuningCanvas.setVisible(true);

		}

		private void OnFinished() {
			finePositioningTimeline.stop();
			// next file (no init)
			fileTimeline.play();
			FineTuningFader.play();

		}
		
	}

	/**
	 * @param testRectCloud
	 *
	 */
	@Override
	public void start(Stage stage) {

		FileWalker fileWalker = new FileWalker();
		fileWalker.setCycleCount(1);
		// fileWalker.setDelay(Duration.millis(2000));
		fileWalker.setDuration(Duration.millis(2000));
		if (DEBUG_PRINT) System.out.println("Rate: " + fileWalker.getRate());
		fileWalker.setAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				if (DEBUG_PRINT) System.out.println("setAction " + event);
			}

		});
		fileWalker.play();

		label.setText("Select directory to analyze...");

		Pane pane = new Pane();

		ScrollPane scroller = new ScrollPane(pane);
		scroller.setPannable(true);

		BorderPane root = new BorderPane(scroller);
		root.setBottom(new Pane(label));
		((Pane) root.getBottom())
		.setBackground(new Background(new BackgroundFill(Color.WHEAT, CornerRadii.EMPTY, Insets.EMPTY)));

		// Creating a scene object
		Scene scene = new Scene(root, scene_width, scene_height + 17 /* height of label */ );

		// Adding scene to the stage
		stage.setScene(scene);

		// Displaying the contents of the stage
		stage.show();

		// ask for directory
		DirectoryChooser dc = new DirectoryChooser();
		rootDirectory = dc.showDialog(stage);

		if (rootDirectory == null) {
			label.setText("No directory selected. Terminated.");
			if (DEBUG_PRINT) System.out.println(label.getText());
		} else {

			// Adding Title to the stage
			stage.setTitle("Spiral Rectangle Cloud of " + rootDirectory.getName() + " (" + sorting.name() + " / " + originMovementMode.name() + ")");

			label.setText("Analyzing directory structure...");
			
			fileSizeInfo.put("width.min", 0d);
			fileSizeInfo.put("width.max", 0d);
			fileSizeInfo.put("height.min", 0d);
			fileSizeInfo.put("height.max", 0d);
			fileSizeInfo.put("width.mean", 0d);
			fileSizeInfo.put("height.mean", 0d);
						
			try {
				folderOrigins.put(rootDirectory.getCanonicalPath(), new FolderOrigin(origin));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			// collect all files of the folder structure and sort them by absolute file path
			totalNumOfLevels = visitDirectory(rootDirectory, 0, HUE_MIN, HUE_MAX);
			
			if (files.size() == 0 ) {
				label.setText("No files found based on filter.");
				return;
			}
			fileSizeInfo.put("width.mean", fileSizeInfo.get("width.mean") / files.size());
			fileSizeInfo.put("height.mean", fileSizeInfo.get("height.mean") / files.size());


			double median = Utils.getMedianOf(fileWidths);
			fileSizeInfo.put("width.median", median);
			
			median = Utils.getMedianOf(fileWidths);
			fileSizeInfo.put("height.median", median);

			switch (sorting) {		
			case BY_PARENTFOLDER:
				// Sort by parent folder
				Collections.sort(files, new Comparator<FileInfo>() {
					@Override
					public int compare(FileInfo a, FileInfo b) {
						return new File(a.filename).getParent().compareToIgnoreCase(new File(b.filename).getParent());
					}
				});
				break;
				
			case BY_LEVEL_ASC: 
				// Sort by 
				Collections.sort(files, new Comparator<FileInfo>() {
					@Override
					public int compare(FileInfo a, FileInfo b) {
						return a.level - b.level;
					}
				});
				break;
				
			case BY_LEVEL_DESC: 
				// Sort by 
				Collections.sort(files, new Comparator<FileInfo>() {
					@Override
					public int compare(FileInfo a, FileInfo b) {
						return b.level - a.level;
					}
				});
				break;
				
			case BY_AREA_ASC : 
				// Sort by 
				Collections.sort(files, new Comparator<FileInfo>() {
					@Override
					public int compare(FileInfo a, FileInfo b) {
						return (int) ((a.width * a.height) - (b.width * b.height));
					}					
				});
				break;
				
			case BY_AREA_DESC : 
				// Sort by 
				Collections.sort(files, new Comparator<FileInfo>() {
					@Override
					public int compare(FileInfo a, FileInfo b) {
						return (int) ((b.width * b.height) - (a.width * a.height));
					}					
				});
				break;
				
			case BY_WIDTH_ASC: 
				// Sort by 
				Collections.sort(files, new Comparator<FileInfo>() {
					@Override
					public int compare(FileInfo a, FileInfo b) {
						return (int) (a.width - b.width);
					}
				});
				break;
				
			case BY_WIDTH_DESC: 
				// Sort by 
				Collections.sort(files, new Comparator<FileInfo>() {
					@Override
					public int compare(FileInfo a, FileInfo b) {
						return (int) (b.width - a.width);
					}
				});
				break;
				
			case BY_HEIGHT_ASC:			
				// Sort by 
				Collections.sort(files, new Comparator<FileInfo>() {
					@Override
					public int compare(FileInfo a, FileInfo b) {
						return (int) (a.height - b.height);
					}
				});
				break;
			
			case BY_HEIGHT_DESC:			
				// Sort by 
				Collections.sort(files, new Comparator<FileInfo>() {
					@Override
					public int compare(FileInfo a, FileInfo b) {
						return (int) (b.height - a.height);
					}
				});
				break;

			default: // == BY_PATH
				// Sort by path
				Collections.sort(files, new Comparator<FileInfo>() {
					@Override
					public int compare(FileInfo a, FileInfo b) {
						return a.filename.compareToIgnoreCase(b.filename);
					}
				});
				break;
			
			} // switch

			// adding clouds (panes)
			pane.getChildren().add(rectCloud);
			pane.getChildren().add(testRectCloud);
			pane.getChildren().add(spiralOriginsCloud);

			// for debugging
			spiralCanvas = new Canvas(scene_width * 5, scene_height * 5);
			GraphicsContext spiralGC = spiralCanvas.getGraphicsContext2D();
			spiralGC.setStroke(Color.RED);
			spiralGC.setFill(Color.RED);
			pane.getChildren().add(spiralCanvas);
			spiralCanvas.getGraphicsContext2D().setLineWidth(20);
			spiralCanvas.getGraphicsContext2D().strokeRect(0, 0, spiralCanvas.getWidth(), spiralCanvas.getHeight());
			spiralCanvas.getGraphicsContext2D().setLineWidth(1);

			// and for debugging too
			guidesCanvas = new Canvas(scene_width * 5, scene_height * 5);
			GraphicsContext guidesGC = guidesCanvas.getGraphicsContext2D();
			guidesGC.setStroke(Color.LIGHTGRAY);
			guidesGC.setFill(Color.LIGHTGRAY);
			pane.getChildren().add(guidesCanvas);
			guidesGC.strokeLine(guidesCanvas.getWidth() / 2, 0, guidesCanvas.getWidth() / 2, guidesCanvas.getHeight());
			guidesGC.strokeLine(0, guidesCanvas.getHeight() / 2, guidesCanvas.getWidth(), guidesCanvas.getHeight() / 2);
			// draw border around guides canvas
			guidesGC.setLineWidth(10);
			guidesGC.strokeRect(0, 0, 1000, 800);
			guidesGC.setLineWidth(1);

			FineTuningCanvas = new Canvas(scene_width * 5, scene_height * 5);
			FineTuningCanvas.getGraphicsContext2D().setStroke(Color.GREEN);
			FineTuningCanvas.getGraphicsContext2D().setFill(Color.GREEN);
			pane.getChildren().add(FineTuningCanvas);
			
			
			scene.setOnKeyPressed(e -> {
				if (e.getCode() == KeyCode.ESCAPE) {
					fileTimeline.stop();
					spiralPositionTimeline.stop();
					orientationAndAlignmentTimeline.stop();
					finePositioningTimeline.stop();
				}
				if (e.getCode() == KeyCode.SPACE) {
					if (fileTimeline.getStatus() == Status.PAUSED && spiralPositionTimeline.getStatus() == Status.PAUSED
							&& orientationAndAlignmentTimeline.getStatus() == Status.PAUSED
							&& finePositioningTimeline.getStatus() == Status.PAUSED) {
						finePositioningTimeline.play();
					} else {
						fileTimeline.pause();
						spiralPositionTimeline.pause();
						orientationAndAlignmentTimeline.pause();
						finePositioningTimeline.pause();
					}
					if (fileTimeline.getStatus() == Status.PAUSED && spiralPositionTimeline.getStatus() == Status.PAUSED
							&& orientationAndAlignmentTimeline.getStatus() == Status.PAUSED
							&& finePositioningTimeline.getStatus() == Status.PAUSED) {
						spiralFader.playFromStart();

						FineTuningFader.playFromStart();
					}
				}
			});

			// Create operator
			AnimatedZoomOperator zoomOperator = new AnimatedZoomOperator();

			// bind scroll event to zoom operator
			pane.setOnScroll(new EventHandler<ScrollEvent>() {
				@Override
				public void handle(ScrollEvent event) {
					double zoomFactor = 1.5;
					if (event.getDeltaY() <= 0) {
						// zoom out
						zoomFactor = 1 / zoomFactor;
					}
					zoomOperator.zoom(pane, zoomFactor, event.getSceneX(), event.getSceneY());
				}
			});

			
			if (DEBUG_PRINT) System.out.println("Center point: "+ (scene_width / 2) + "/" + (scene_height / 2));
			
			String canvasCenterKey = getSpiralOriginAsKey(rasterize(canvasOrigin));
			fileEventHandler.init();
			KeyFrame fileKeyFrame = new KeyFrame(Duration.millis(1 / drawingSpeed), fileEventHandler);
			KeyFrame spiralPositionKeyFrame = new KeyFrame(Duration.millis(1 / drawingSpeed), spiralPositionEventHandler);
			KeyFrame orientationKeyFrame = new KeyFrame(Duration.millis(1 / drawingSpeed), orientationEventHandler);
			KeyFrame finePositionKeyFrame = new KeyFrame(Duration.millis(1 / drawingSpeed), fineTuningEventHandler);

			fileTimeline.getKeyFrames().add(fileKeyFrame);
			fileTimeline.setCycleCount(Timeline.INDEFINITE);
			fileTimeline.setAutoReverse(false);

			spiralPositionTimeline.getKeyFrames().add(spiralPositionKeyFrame);
			spiralPositionTimeline.setCycleCount(Timeline.INDEFINITE);
			spiralPositionTimeline.setAutoReverse(false);

			orientationAndAlignmentTimeline.getKeyFrames().add(orientationKeyFrame);
			orientationAndAlignmentTimeline.setCycleCount(Timeline.INDEFINITE);
			orientationAndAlignmentTimeline.setAutoReverse(false);

			finePositioningTimeline.getKeyFrames().add(finePositionKeyFrame);
			finePositioningTimeline.setCycleCount(Timeline.INDEFINITE);
			finePositioningTimeline.setAutoReverse(false);
			
			
			
			// Prepare Fader
			
			// Prepare Fading SpiralCanvas
			spiralFader = new FadeTransition(Duration.millis(3000), spiralCanvas);
			spiralFader.setFromValue(1.0);
			spiralFader.setToValue(0.0);
			spiralFader.setCycleCount(1);
			spiralFader.setOnFinished(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					// hide spiralCanvas
					spiralCanvas.setVisible(false);
				}
			});			

			
			// Prepare FineTuningCanvas Fading					
			FineTuningFader = new FadeTransition(Duration.millis(rectFadingSpeed), FineTuningCanvas);
			FineTuningFader.setFromValue(1.0);
			FineTuningFader.setToValue(0);
			FineTuningFader.setCycleCount(1);
			FineTuningFader.setDelay(new Duration(fadeOffset));
			FineTuningFader.setOnFinished(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					// remove faded rectangles from testRectCloud
					testRectCloud.getChildren().remove(((FadeTransition) event.getSource()).getNode());
					FineTuningCanvas.setVisible(false);
				}
			});			
			

			// Set start locations for the spirals in spiral point 0
			SpiralPoint.init(numRectOrientationsAndAlignments);
			
			SpiralInfo spiralinfo = new SpiralInfo(numOfSpiralVariants);
			appendDefaultSpiralVariants(spiralinfo, canvasOrigin);

			spirals.put(canvasCenterKey, spiralinfo);
			

			fileTimeline.play();

		}

	}

	public Point2D rasterize(Point2D point2D) {
		if (point2D == null) return null;
		
		Point2D rasterPoint2D = new Point2D((int)(point2D.getX() / rasterX) * rasterX + rasterOffsetX, (int)(point2D.getY() / rasterY) * rasterY + rasterOffsetY);
		
		return rasterPoint2D;
	}

	/**
	 * @param spiralinfo
	 */
	private void appendDefaultSpiralVariants(SpiralInfo spiralinfo, Point2D spiralOrigin) {		
		try{
			spiralinfo.spiralvariants[0].appendSpiralPoint(new SpiralPoint(0, + 1 * variantOffset, + 1 * variantOffset));
			spiralinfo.spiralvariants[1].appendSpiralPoint(new SpiralPoint(0, + 1 * variantOffset, - 1 * variantOffset));
			spiralinfo.spiralvariants[2].appendSpiralPoint(new SpiralPoint(0, - 1 * variantOffset, + 1 * variantOffset));
			spiralinfo.spiralvariants[3].appendSpiralPoint(new SpiralPoint(0, - 1 * variantOffset, - 1 * variantOffset));
		} catch (IndexOutOfBoundsException e) {}
	}

	/**
	 * @return
	 */
	private static String getSpiralOriginAsKey(Point2D point) {
		return point.getX() + "|" + point.getY();
	}

	public double CanvasToCartesianX(double x0, double canvas_x) {
		return canvas_x - x0;
	}

	public double CanvasToCartesianY(double y0, double canvas_y) {
		return y0 - canvas_y;
	}

	public double CartesianToCanvasX(double x0, double cart_x) {
		return x0 + cart_x;
	}

	public double CartesianToCanvasY(double y0, double cart_y) {
		return y0 - cart_y;
	}

	public void shuffleArrayDurstenfeld(Rectangle[] testRects) {
		shuffleArrayDurstenfeld(testRects, 0, testRects.length);
	}

	//	/**
	//	 * performs a AABB (Axis Aligned Bounding Box) collision check for the given rectangle 
	//	 * and the already plotted rectangle in the array 'rectangles'   
	//     */
	//	/**
	//	 * @param currentTestRect
	//	 * @param RectCloud
	//	 * @return
	//	 */
	//	private boolean checkCollisonAABB(Rectangle currentTestRect) {
	//
	////		System.out.println("center of spiral x= " + x + ", y= " + y );
	////		System.out.println("dimensions of new rect: width= " + rectWidth + ", height= " + rectHeight);
	////		
	////		System.out.println(String.format("check collision of new rect @ x= %.2f y= %.2f width= %.2f height= %.2f against all " + RectCloud.getChildren().size() + " already positioned rects.",
	////				currentTestRect.getX(),
	////				currentTestRect.getY(),
	////				currentTestRect.getWidth(),
	////				currentTestRect.getHeight()
	////		));
	//				
	//		// ... and check each for collision with all previously created rectangles
	//		boolean anyCollisions = false;
	//		for (Node r : rectCloud.getChildren()) {
	//			
	//			if (r == null) break;
	//			
	////			System.out.print(String.format("  against rect @ x= %.0f y= %.0f width= %.0f height= %.0f   ",
	////					((Rectangle)r).getX(),
	////					((Rectangle)r).getY(),
	////					((Rectangle)r).getWidth(),
	////					((Rectangle)r).getHeight()
	////			));
	//
	//			// getBoundsInLocal is only correct if and only if both objects are in the same coordinate system. 
	//			// if one object is for instance part of another group, the coordinate system have to be transformed
	//			// furthermore this function tests only the AABB (axis aligned bounding box) of both objects, 
	//			// so rotated will be approximated with its AABB
	//			boolean collision = r.getBoundsInParent().intersects(currentTestRect.getBoundsInLocal());
	////			System.out.println(collision ? "collision" : "no collision");
	//			
	//			anyCollisions |= collision;
	//			
	//		}
	//					
	//		return anyCollisions;
	//	}

	/**
	 * performs a AABB (Axis Aligned Bounding Box) collision check for the given
	 * rectangle and the already plotted rectangle in the array 'rectangles'
	 */
	/**
	 * @param bounds bounds of the rectangle to be set
	 * @return
	 */
	private boolean checkCollisonAABB(double canvas_x, double canvas_y, double width, double height) {

		BoundingBox bounds = new BoundingBox(canvas_x, canvas_y, 0, /* z, unused */
				width, height, 0 /* depth, unused */
				);

//		if (DEBUG_PRINT) System.out.println("center of spiral x= " + canvas_x + ", y= " + canvas_y );
//		if (DEBUG_PRINT) System.out.println("dimensions of new rect: width= " + width + ", height= " + height);
		
		// ... and check each for collision with all previously created rectangles
		boolean anyCollisions = false;
		for (Node r : rectCloud.getChildren()) {

			if (r == null)
				break;

//			if (DEBUG_PRINT) System.out.print(String.format("  against rect @ x= %.0f y= %.0f width= %.0f height= %.0f   ",
//					((Rectangle)r).getX(),
//					((Rectangle)r).getY(),
//					((Rectangle)r).getWidth(),
//					((Rectangle)r).getHeight()
//			));

			// getBoundsInLocal is only correct if and only if both objects are in the same coordinate system. 
			// if one object is for instance part of another group, the coordinate system have to be transformed
			// furthermore this function tests only the AABB (axis aligned bounding box) of both objects, 
			// so rotated will be approximated with its AABB
			boolean collision = r.intersects(bounds);
			//if (DEBUG_PRINT) System.out.println(collision ? "collision" : "no collision");

			anyCollisions |= collision;

		}

		return anyCollisions;
	}

	/**
	 * Shuffle array (Durstenfeld shuffle, implementation for Fishes-Yates shuffle
	 * based on https://stackoverflow.com/a/1520212 seems (according to https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle#The_modern_algorithm) a
	 * Fisher-Yates shuffle, exactly the 'Durstenfeld inplace' shuffle implementation
	 * (Fisher–Yates shuffle)
	 * 
	 * For small arrays (two items) the original shuffle will shuffle always the same way, so the result is predictable.
	 * Here it is extended by a random factor which decides if a swap has to occur.
	 *  
	 * 
	 * @param the array to be shuffled
	 */
	/**
	 * @param <T>
	 * @param testRects
	 * @param from
	 * @param to
	 */
	private static <T> void shuffleArrayDurstenfeld(T[] testRects, int from, int to) {
		for (int i = to; i > from; i--) {
			int index = from + randomizer.nextInt(i - from);
			// Simply swap two entries if a random number is greater than a threshold
			if (randomizer.nextFloat() > 0.5) {
				T a = testRects[index];
				testRects[index] = testRects[i];
				testRects[i] = a;
			}
		}
	}

	// according to https://stackoverflow.com/q/48492980
	public Rectangle checkRectCollisionSpiralOfArchimedes(double x0, double y0, double maxRadius, double numSegments,
			double spinRate, double deltaR, double rectWidth, double rectHeight, GraphicsContext textRectGC,
			GraphicsContext spiralGC) {
		double radius = 0.00001;
		double x = x0;
		double y = y0;
		if (numSegments > 0) {
			deltaR = maxRadius / numSegments;
		} else {
			numSegments = maxRadius / deltaR;
		}

		int step = 1;
		Rectangle newRect = null;
		while (
				//				(newRect = massCheckCollisonAABB(x, y, rectWidth, rectHeight)) == null && 
				radius < maxRadius) {
			double x1 = x;
			double y1 = y;
			radius += deltaR;
			x = x0 + radius * Math.cos((2 * Math.PI * step / (radius * 10)) * spinRate);
			y = y0 + radius * Math.sin((2 * Math.PI * step / (radius * 10)) * spinRate);
			if (DEBUG_DRAW) {
				spiralGC.strokeLine(x, y, x1, y1);
				spiralGC.fillOval(x - 5 / 2, y - 5 / 2, 5, 5);
			}

			step++;

		}

		return newRect;
	}

	private int visitDirectory(File directory, int level, float minHue, float maxHue) {

		int numOfLevels = level;
//		if (DEBUG_PRINT) System.out.println(directory.getAbsolutePath() + 
//				" L: " + level + 
//				" minHue: " + minHue + 
//				" maxHue: " + maxHue
//		);	

		// Filter
		String[] childFilesAndDirectories = directory.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				File file = new File(dir, name);
				if (fileExtensionFilter == null || fileExtensionFilter.length == 0) {
					// No Filter defined
					return true;
				} else {
					// check if file extension is in the list of allowed extensions
					return file.isDirectory() || Arrays.stream(fileExtensionFilter)
							.anyMatch(FilenameUtils.getExtension(name.toLowerCase())::equals);
				}
			};
		});

		// determine the number of child directories
		String[] childDirectories = directory.list(new FilenameFilter() {
			@Override
			public boolean accept(File current, String name) {
				return new File(current, name).isDirectory();
			}
		});
		int numOfChildDirectories = childDirectories.length;

		// Color calculation
		// splitting the current hue color range given by minHue and maxHue into numOfChildDirectories parts of equal distance
		// the hue of the current parent directory is the center of the hue color range
		float deltaHue = maxHue - minHue;
		float partHue = deltaHue / numOfChildDirectories;
		float hueOfDirectory = (maxHue + minHue) / 2; // center of current hue color range

//		if (DEBUG_PRINT) System.out.println(directory.getAbsolutePath() + " deltaHue: " + deltaHue + 
//				" num: " + numOfChildDirectories + 
//				" partHue: " + partHue + 
//				" dirHue:" + hueOfDirectory
//		);

		int childDirectoriesCounter = 0;

		for (String FilesAndDirectories : childFilesAndDirectories) {

			File fileOrDirectory = new File(directory, FilesAndDirectories);

			if (fileOrDirectory.isDirectory()) {
				float currentMinHue = minHue + childDirectoriesCounter * partHue;
				float currentMaxHue = minHue + childDirectoriesCounter * partHue + partHue;

//				if (DEBUG_PRINT) System.out.println(" " + fileOrDirectory.getAbsolutePath() + 
//						" currentMinHue: " + currentMinHue + 
//						" currentMaxHue: " + currentMaxHue
//				);

				numOfLevels = Math.max(numOfLevels,
						visitDirectory(fileOrDirectory, level + 1, currentMinHue, currentMaxHue));
				childDirectoriesCounter++;
			} else {
				if (showFiles) {

					// extract width and height of file
					int lineCtr = 0;
					int maxLineLength = 0;
					try {
						Scanner scanner = new Scanner(fileOrDirectory);
						while (scanner.hasNextLine()) {
							String line = scanner.nextLine();
							maxLineLength = Math.max(maxLineLength, line.length());
							lineCtr++;
						}
						scanner.close();
					} catch (FileNotFoundException excep) {
						excep.printStackTrace();
					}

					// set the width and height of the file depending on file name and file extension filter parameters
					// set default width and height					
					double width = defaultWidth;
					double height = defaultHeight;
					if (dimensionDisplayFilenameFilter == null || dimensionDisplayFilenameFilter.length == 0
							|| Arrays.stream(dimensionDisplayFilenameFilter)
							.anyMatch(fileOrDirectory.getName().toLowerCase()::equals)
							|| dimensionDisplayExtensionFilter == null || dimensionDisplayExtensionFilter.length == 0
							|| Arrays.stream(dimensionDisplayExtensionFilter).anyMatch(
									FilenameUtils.getExtension(fileOrDirectory.getName().toLowerCase())::equals)) {
						// extension is in dimensionDisplayExtensionFilter
						height = lineCtr;
						width = maxLineLength;
					}

					if (width > 0 && height > 0) {
						try {
							files.add(
									new FileInfo(fileOrDirectory.getCanonicalPath(), level, width, height, hueOfDirectory));
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						fileSizeInfo.put("width.min", Math.min(fileSizeInfo.get("width.min"), width));
						fileSizeInfo.put("width.max", Math.max(fileSizeInfo.get("width.max"), width));
						fileSizeInfo.put("height.min", Math.min(fileSizeInfo.get("height.min"), height));
						fileSizeInfo.put("height.max", Math.max(fileSizeInfo.get("height.max"), height));
						fileSizeInfo.put("width.mean", fileSizeInfo.get("width.mean") + width);
						fileSizeInfo.put("height.mean", fileSizeInfo.get("height.mean") + height);
						fileWidths.add(width);
						fileHeights.add(height);
						
					}
				}
			}
		}

		return numOfLevels;

	}

	//	private Color convertLabToFXColor(ColorCieLab color) {				
	//			java.awt.Color colorRGB_awt = new java.awt.Color(ColorConversions.convertXYZtoRGB(ColorConversions.convertCIELabtoXYZ(color)));
	//			Color fxColor = new Color((double)colorRGB_awt.getRed()/256, (double)colorRGB_awt.getGreen()/256, (double)colorRGB_awt.getBlue()/256, (double)colorRGB_awt.getAlpha()/256);
	//	//		System.out.println(" Lab: " + color.toString() + 
	//	//				" RGB: " + fxColor.toString() +
	//	//				" R " + ((double)colorRGB_awt.getRed()/256) + 
	//	//		        " G " + ((double)colorRGB_awt.getRed()/256) + 
	//	//				" B " + ((double)colorRGB_awt.getRed()/256) + 
	//	//				" A " + ((double)colorRGB_awt.getAlpha()/256)
	//	//		);
	//			return fxColor;
	//		}

	private Color ConvertAwtColorToJavaFX(java.awt.Color c) {
		return Color.rgb(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255.0);
	}

	/**
	 * Adds a new rectangle to the rectangle cloud (rectCloud), based on the given
	 * rectTemplate (location and dimension) and the color data from the 'files'
	 * array The color will be calculated based on the HSB scheme, the level of the
	 * directory the file is located in will be used as hue (H, brightness, the
	 * depth: the higher in the hierarchy the brighter the color will be) the color
	 * was spread over the wideness of the directory tree.
	 * 
	 * @param rectTemplate
	 * @return 
	 * 
	 */
	private Rectangle addNewRectangleToRectCloud(double canvas_x, double canvas_y, double w, double h) {

		// create rectangle and add to RectCloud
		Rectangle newRect = new Rectangle(canvas_x, canvas_y, w, h);
		
		newRect.setStroke(Color.TRANSPARENT);
		newRect.setFill(fileEventHandler.parentFolderColor);
		Tooltip.install(newRect, 
				new Tooltip(
						fileEventHandler.filename 
						+ "\nw=" + fileEventHandler.fileWidth 
						+ " h=" + fileEventHandler.fileHeight
						+ " a=" + (fileEventHandler.fileWidth * fileEventHandler.fileHeight) 
						+ " l=" + fileEventHandler.level
//						+ "\nrw=" + newRect.getWidth()
//						+ " rh=" + newRect.getHeight()
				)
		);
		rectCloud.getChildren().add(newRect);
		if (DEBUG_PRINT) System.out.println(String.format("      new rect @ x= %.2f y= %.2f width= %.2f height= %.2f color=%s for '%s' added.",
				newRect.getX(),
				newRect.getY(),
				newRect.getWidth(),
				newRect.getHeight(),
				fileEventHandler.parentFolderColor.toString(),
				files.get(fileEventHandler.fileIndex).filename
		));

//		// csv style
//		if (DEBUG_PRINT) System.out.println(String.format(Locale.US, "%s, %d, %.2f, %.2f, %.2f, %.2f, %s, %.2f, %.2f, %.2f",
//				files.get(fileEventHandler.fileIndex).filename,
//				files.get(fileEventHandler.fileIndex).level,
//				newRect.getX(),
//				newRect.getY(),
//				newRect.getWidth(),
//				newRect.getHeight(),
//				parentDirectoryColor.toString(),
//				files.get(fileEventHandler.fileIndex).hue, 
//				1.0f, 
//				brightness
//		));
		
		return newRect;
	}

	private Rectangle addTestRectangleToTestRectCloud(double x, double y, double w, double h, Color color) {

		Rectangle testRect = new Rectangle(x, y, w, h);
		if (color == null) {
			testRect.setStroke(Color.GRAY);
			testRect.setFill(Color.TRANSPARENT);
		} else {
			testRect.setStroke(color);
			testRect.setFill(Color.TRANSPARENT);
		}

		// add to test rect cloud
		testRectCloud.getChildren().add(testRect);

		//  add fading transition to currentTestRect
		FadeTransition rectFader = new FadeTransition(Duration.millis(rectFadingSpeed), testRect);
		rectFader.setFromValue(1.0);
		rectFader.setToValue(0);
		rectFader.setCycleCount(1);
		rectFader.setDelay(new Duration(fadeOffset));
		rectFader.setOnFinished(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				// remove faded rect from testRectCloud
				testRectCloud.getChildren().remove(((FadeTransition) event.getSource()).getNode());
			}
		});
		rectFader.play();
		return testRect;
	}

}