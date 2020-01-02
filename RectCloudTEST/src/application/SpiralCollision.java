package application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.Comparator;
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
import javafx.scene.shape.Rectangle;
import javafx.animation.Animation.Status;

public class SpiralCollision extends Application {

	final boolean showFiles = true;
	final double scene_width = 1000;
	final double scene_height = 800;
	final int numOfRectangles = 10;
	final double maxRectHeight = scene_width / 8;
	final double maxRectWidth = scene_height / 9;

	final double defaultWidth = 12;
	final double defaultHeight = 12;

	public long seed = 324;

	final double drawingSpeed = 0.1f;
	final double fadeOffset = drawingSpeed;
	final double rectFadingSpeed = drawingSpeed * 5;

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
	
	public final static boolean DRAW_DEBUG = false;

	// Files with this extension will be shown, null or empty array => all files 
	final String[] fileExtensionFilter = {}; // {"java"}; // {"java", "cpp", "h"} // null /*=> all*/

	// files with this extension will shown using their dimension (max line length x lines),
	// other files will be shown using an equal sized rounded rectangle
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
	private final Label label = new Label();
	private FadeTransition spiralFader;

	private File selectedDirectory;
	private final int numRectOrientationsAndAlignments = 4;
	private Rectangle[] testRects = new Rectangle[numRectOrientationsAndAlignments + 1];

	private final Random randomizer = new Random(seed);

	private Timeline fileTimeline = new Timeline();
	private Timeline spiralPositionTimeline = new Timeline();
	private Timeline orientationAndAlignmentTimeline = new Timeline();
	private Timeline finePositioningTimeline = new Timeline();

	private final FileEventHandler fileEventHandler = new FileEventHandler();
	private final SpiralEventHandler spiralEventHandler = new SpiralEventHandler();
	private final OrientationEventHandler orientationEventHandler = new OrientationEventHandler();
	private final FineTuningEventHandler fineTuningEventHandler = new FineTuningEventHandler();

	private Canvas spiralCanvas; // set in "start'
	private final Pane testRectCloud = new Pane();
	private final Pane rectCloud = new Pane();
	private float totalNumOfLevels = -1;
	private Canvas FineTuningCanvas;
	private FadeTransition FineTuningFader;

	public int numOfSpiralVariants = 4;
	// array of all spiral points of all spiral start variants
	// each SpiralPoint contains informations about the checked test rectangle orientations 
	// and their minimal tested dimensions. 
	// a spiral point will be deleted from the array if the last test rectangle with a minimal size collides 
	// with something (that means, there is already a rectangle exactly at this point)
	// finally the array contains spiral points that had to be checked 
	// the larger the spiral grows, the larger the array grows
	// if points at the end are missing, the lastIndex, lastX and lastY fields contain the highest index, x and y values
	private final SpiralInfo spirals[] = new SpiralInfo[numOfSpiralVariants];

	private class FileEventHandler implements EventHandler<ActionEvent> {

		private int numFiles = 0;
		private String filename = "";
		private double fileWidth = 0;
		private double fileHeight = 0;
		private int fileIndex = 0;
		private double x0;
		private double y0;
		private int level;
		private float hue;

		public void handle(ActionEvent event) {
			
			fileTimeline.pause();

			// skip files with width == 0 or height == 0
			while (fileIndex < numFiles &&
					((fileWidth = files.get(fileIndex).width / fileScale) == 0
					|| (fileHeight = files.get(fileIndex).height / fileScale) == 0) 
					) {
				fileIndex++;
			}

			if (fileIndex < numFiles) {

				filename = files.get(fileIndex).filename;
				level = files.get(fileIndex).level;
				hue = files.get(fileIndex).hue;

				// set Info label
				String info = "find location for file " + filename + " width: " + fileWidth + " height: "
						+ fileHeight;
				// System.out.println(info);
				label.setText(info);

//				System.out.println("fileindex: " + fileIndex + " out of 0.." + (numFiles - 1) + " (" + filename + ", "
//						+ fileWidth + " x " + fileHeight + ")");
				spiralEventHandler.init(x0, y0);
				fileTimeline.pause();
				spiralPositionTimeline.play();
				
			};

			fileIndex++;
			if (fileIndex >= numFiles) {
				OnFinished();
			}

		}

		public void init(double x0, double y0) {
			this.x0 = x0;
			this.y0 = y0;
			this.fileIndex = 0;
			this.numFiles = files.size();

		}

		private void OnFinished() {
			System.out.println("finished.");
		}

	}

	private class SpiralEventHandler implements EventHandler<ActionEvent> {

		int spiralIndex = 0;

		private double x = 0;
		private double y = 0;

		private double x0;
		private double y0;

		private double prev_x = 0;
		private double prev_y = 0;

		private int variant = -1;

		private ListIterator<SpiralPoint> spiralPointIterator;
		private SpiralPoint currentSpiralPoint;

		private SpiralInfo currentSpiral;

		@Override
		public void handle(ActionEvent event) {

			spiralPositionTimeline.pause();
			
			currentSpiralPoint = null;
			
			if (spiralPointIterator != null) {
				
				boolean hasNextPoint;
				// remove all pending remove spiral points
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
					
//					System.out.println(String.format(
//							Locale.US, 
//							"  got point %d @ %.2f/%.2f",
//							spiralIndex, x, y));
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
				
				// calculate next point in the spiral of Theodorus (Wurzelspirale)
				
					// store the old x and y
					prev_x = x;
					prev_y = y;

					double h = Math.sqrt((Math.pow(x * probeRate, 2) + Math.pow(y * probeRate, 2))) * spinRate;
					x = prev_x - prev_y / h;
					y = prev_y + prev_x / h;
					
				currentSpiralPoint = new SpiralPoint(spiralIndex, x, y);
				currentSpiral.appendSpiralPoint(currentSpiralPoint);
				spiralPointIterator = null;
				
				// set latest values
				currentSpiral.latestIndex = spiralIndex;
				currentSpiral.latestX = x;
				currentSpiral.latestY = y;

				
				// System.out.println("  created and appended new spiral point " + spiralIndex + " @ " + x + "/" + y);

			} else {
				//spiralIndex larger then the stored highest index: this should never happen!
			}


			// Draw spiral, center point
			if (DRAW_DEBUG) {
				spiralCanvas.getGraphicsContext2D().strokeLine(CartesianToCanvasX(x0, x), CartesianToCanvasY(y0, y),
					CartesianToCanvasX(x0, prev_x), CartesianToCanvasY(y0, prev_y));
				spiralCanvas.getGraphicsContext2D().fillOval(CartesianToCanvasX(x0, x) - 5 / 2,
					CartesianToCanvasY(y0, y) - 5 / 2, 5, 5);
			}

			if (maxSpiralPositions > 0 && spiralIndex >= maxSpiralPositions) {
//				System.out.println("  file '" + fileEventHandler.filename + "': collision in more than "
//						+ maxSpiralPositions + " spiral positions, canceled. Next file.");
				OnFinished();
			} else {
				// System.out.println("  spiral point " + spiralIndex + " x=" + x + " y=" + y);
				orientationEventHandler.init(x0, y0, x, y);
				orientationAndAlignmentTimeline.play();
			}

		}
		

		public void init(double x0, double y0) {
			this.x0 = x0;
			this.y0 = y0;

			prev_x = 0;
			prev_y = 0;

			//variant++;
			if (variant < 0 || variant >= numOfSpiralVariants) {
				variant = 0;
			} 

			// there should always be a first key in the LinkedList!
			//			spiralStep = spirals[startOrientation].getFirstPoint().nr;

			currentSpiral = spirals[variant];
			spiralPointIterator = currentSpiral.spiralpoints.listIterator();

			if (spiralFader != null)
				spiralFader.stop();
			if (FineTuningFader != null)
				FineTuningFader.stop();

			spiralCanvas.setOpacity(1);
			spiralCanvas.getGraphicsContext2D().clearRect(0, 0, spiralCanvas.getWidth(), spiralCanvas.getHeight());

			// mark center of spiral
			spiralCanvas.getGraphicsContext2D().fillOval(x0 - 5 / 2, y0 - 5 / 2, 5, 5);

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

		private double y0;
		private double x0;
		
		// shortcut 
		private Dimension currentSpiralPointOrientation;

		@Override
		public void handle(ActionEvent event) {
			
			orientationAndAlignmentTimeline.pause();
			
//			System.out.println(
//					"    Orientation " + orientationIndex + " out of 0.." + (numRectOrientationsAndAlignments + 1 - 1));

			Rectangle currentTestRect = testRects[orientationIndex];
			if (DRAW_DEBUG) addTestRectangleToTestRectCloud(currentTestRect.getX(), currentTestRect.getY(), currentTestRect.getWidth(),
					currentTestRect.getHeight(), null /* use default color */
					);

//			System.out.print(String.format("    test " + (orientationIndex == 4 ? "square" : "rect")+ " x= %.2f y= %.2f w= %.2f h= %.2f : ", currentTestRect.getX(),
//					currentTestRect.getY(), currentTestRect.getWidth(), currentTestRect.getHeight()));

			// check collision

			boolean collision = true;
			if (orientationIndex < numRectOrientationsAndAlignments) {
				Dimension currentStoredMinimalTestDimensions = spiralEventHandler.currentSpiralPoint.testOrientations[orientationIndex];
				collision  = currentStoredMinimalTestDimensions.width < currentTestRect.getWidth()
						|| currentStoredMinimalTestDimensions.height < currentTestRect.getHeight();	
			}
			
			if ( ! collision ) {
				// collision check is necessary because previously checked testRects were always bigger (bigger width and bigger height) 
				collision = checkCollisonAABB(currentTestRect.getX(), currentTestRect.getY(),
						currentTestRect.getWidth(), currentTestRect.getHeight());
			}
	

			// System.out.println(collision ? "collision":"no collision");
			if ( ! collision ) {
				// collision free position found

				// stop orientation
				orientationAndAlignmentTimeline.stop();

				// reset finePos
				fineTuningEventHandler.init(x0, y0, currentTestRect);
				finePositioningTimeline.play();

			} else {
				// replace width and height of test orientation of spiral points, if necessary

				// Orientation 0..3: 4 variants of orientations
				if (orientationIndex < numRectOrientationsAndAlignments) {
					
					currentSpiralPointOrientation = spiralEventHandler.currentSpiralPoint.testOrientations[orientationIndex];

					// current file dimension smaller than the smallest previously checked? 
					if (currentTestRect.getWidth() < currentSpiralPointOrientation.width 
						&& currentTestRect.getHeight() < currentSpiralPointOrientation.height  ) {
						currentSpiralPointOrientation.width = currentTestRect.getWidth();
						currentSpiralPointOrientation.height = currentTestRect.getHeight();
					}

				} else {
					// orientationIndex == 4 => minimal test square collision
					// remove spiralInfo for this spiral coordinate to mark this point as not to be checked any more in future
					// System.out.println("    -> remove spiral point");
					spiralEventHandler.currentSpiralPoint.pendingRemove = true;
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

		public void init(double x0, double y0, double x, double y) {

			this.x0 = x0;
			this.y0 = y0;

			orientationIndex = 0;

			this.rectWidth = fileEventHandler.fileWidth;
			this.rectHeight = fileEventHandler.fileHeight;

			// use x and y as center point for the testRects
			
			// portrait rectangle, center point
			testRects[0] = new Rectangle( 
					x0 + x - rectWidth / 2, 
					y0 + y - rectHeight / 2, 
					rectWidth, 
					rectHeight
			); 
			
			// landscape rectangle, center point
			testRects[1] = new Rectangle(
					x0 + x - rectHeight / 2, 
					y0 + y - rectWidth / 2, 
					rectHeight, 
					rectWidth
			); 

			testRects[2] = new Rectangle(
					x0 + x - (x < 0 ? rectWidth : 0), 
					y0 + y - (y < 0 ? rectHeight : 0), 
					rectWidth,
					rectHeight
			);
			testRects[3] = new Rectangle(
					x0 + x - (x < 0 ? rectHeight : 0), 
					y0 + y - (y < 0 ? rectWidth : 0),
					rectHeight, 
					rectWidth
			);

			// minimal test square with center pivot for testing the general collision on this point
			testRects[4] = new Rectangle(x0 + x - 0.5, y0 + y - 0.5, 1, 1);

			// Shuffle testRects to get a random orientation/alignment
			shuffleArrayDurstenfeld(testRects, 0, 1);
			shuffleArrayDurstenfeld(testRects, 2, testRects.length - 1); // without minimal test square
		}

		private void OnFinished() {
			orientationAndAlignmentTimeline.stop();
			// next spiral pos
			spiralPositionTimeline.play();
		}
	}

	private class FineTuningEventHandler implements EventHandler<ActionEvent> {

		private static final int DIRECTION_X = 0;
		private static final int DIRECTION_Y = 1;

		private int fineTuningStep = 0;

		private double prev_x = 0;
		private double prev_y = 0;
		private double x0 = 0;
		private double y0 = 0;
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

		@Override
		public void handle(ActionEvent event) {
			
			finePositioningTimeline.pause();

			boolean collision = false;
			boolean crossedXAxis = false;
			boolean crossedYAxis = false;

			boolean toMuchFineTuningSteps = fineTuningStep >= maxFineTuningSteps;
			if (toMuchFineTuningSteps) {
				// takes longer than expected
//				System.out.println("      fine tuning needed more than " + maxFineTuningSteps
//						+ " to find a better location, canceled. Use last good.");
			} else {
				// check collision

				//finePositioningTimeline.pause();
				// System.out.println("      Fine tuning step " + fineTuningStep + " out of 0.." + (maxFineTuningSteps - 1));

				// store last good location
				prev_x = x;
				prev_y = y;

				// calculate new location to be tested
				if (direction[DIRECTION_X])
					x = prev_x - offset_x;
				if (direction[DIRECTION_Y])
					y = prev_y - offset_y;
				// System.out.println(String.format("       %s x= %.2f %s y= %.2f ", direction[DIRECTION_X] ? "new":"old",x, direction[DIRECTION_Y] ? "new":"old",y));

				// create new test rectangle
				canvas_x = CartesianToCanvasX(x0, x);
				canvas_y = CartesianToCanvasY(y0, y);
				if (DRAW_DEBUG) currentTestRect = addTestRectangleToTestRectCloud(canvas_x, canvas_y, width, height, null);

				if (direction[DIRECTION_X])
					crossedXAxis = Math.signum(prev_x) != Math.signum(x);
				if (direction[DIRECTION_Y])
					crossedYAxis = Math.signum(prev_y) != Math.signum(y);

				// debug draw location on FineTuningCanvas
				double prev_canvas_x = CartesianToCanvasX(x0, x);
				double prev_canvas_y = CartesianToCanvasY(y0, y);
				FineTuningCanvas.getGraphicsContext2D().strokeLine(prev_canvas_x, prev_canvas_y, canvas_x, canvas_y);
				FineTuningCanvas.getGraphicsContext2D().fillOval(canvas_x - 5 / 2, canvas_y - 5 / 2, 5, 5);

				// check collision at new location 
				collision = checkCollisonAABB(canvas_x, canvas_y, width, height);

			}

			if (collision || crossedXAxis || crossedYAxis || toMuchFineTuningSteps) {

				// reset to last good

				// debug output
				ArrayList<String> sl = new ArrayList<String>();
				if (collision) sl.add("collision");
				if (crossedXAxis) sl.add("crossed x axis");
				if (crossedYAxis) sl.add("crossed y axis");
				if (toMuchFineTuningSteps) sl.add("to much fine tuning steps");
				sl.stream().collect(Collectors.joining(" and "));
				// System.out.println("      -> " + String.join(" and ", sl) + String.format(Locale.US, "! Reset to last good: %.2f/%.2f", prev_x, prev_y));
				
				if (DRAW_DEBUG) if (collision)
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
						// System.out.println("       try again in " + (direction[DIRECTION_X] ? "X direction" : "Y direction"));
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
						// System.out.println("       try again in " + (direction[DIRECTION_X] ? "X direction" : "Y direction"));
					}

				}

				if (fineTuningDirectionStep >= 3) {

					addNewRectangleToRectCloud(CartesianToCanvasX(x0, prev_x), CartesianToCanvasY(y0, prev_y),
							width, height);

					// if finally no fine tuning was possible
					if (fineTuningStep == 0) {
						if (spirals[spiralEventHandler.variant].spiralpoints.contains(spiralEventHandler.currentSpiralPoint)
								&& spirals[spiralEventHandler.variant].spiralpoints.size() > 1) {
							// remove spiral point to mark this spiral location is already occupied
							// System.out.println("    -> remove spiral point");
							spiralEventHandler.currentSpiralPoint.pendingRemove = true;
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

		
		public void init(double x0, double y0, Rectangle rect) {
			init(x0, y0, rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight());
		}

		
		public void init(double x0, double y0, double canvas_x, double canvas_y, double w, double h) {

			this.x0 = x0;
			this.y0 = y0;
			this.width = w;
			this.height = h;

			fineTuningStep = 0;

			// calculate both directions
			direction[DIRECTION_X] = true;
			direction[DIRECTION_Y] = true;
			fineTuningDirectionStep = 0;

			// convert canvas to Cartesian coordinates, x0 and y0 is the center point
			x = CanvasToCartesianX(x0, canvas_x);
			y = CanvasToCartesianY(y0, canvas_y);

			// Calc pitch 
			len = Math.hypot(x, y);

			// pitch normalized, x and y components 
			nx = x / len;
			ny = y / len;

			offset_x = nx * fineTuningIncrementRate;
			offset_y = ny * fineTuningIncrementRate;

			// System.out.println(String.format("     dx= %.2f dy= %.2f", offset_x, offset_y));

			// clear canvas
			FineTuningCanvas.getGraphicsContext2D().clearRect(0, 0, FineTuningCanvas.getWidth(),
					FineTuningCanvas.getHeight());

		}

		private void OnFinished() {
			finePositioningTimeline.stop();
			// next file (no init)
			fileTimeline.play();

			// FineTuningCanvas Fading					
			FineTuningFader = new FadeTransition(Duration.millis(rectFadingSpeed), FineTuningCanvas);
			FineTuningFader.setFromValue(1.0);
			FineTuningFader.setToValue(0);
			FineTuningFader.setCycleCount(1);
			FineTuningFader.setDelay(new Duration(fadeOffset));
			FineTuningFader.setOnFinished(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					// remove faded rect from testRectCloud
					testRectCloud.getChildren().remove(((FadeTransition) event.getSource()).getNode());
				}
			});
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
		// System.out.println("Rate: " + fileWalker.getRate());
		fileWalker.setAction(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent event) {
				// System.out.println("setAction " + event);
			}

		});
		fileWalker.play();

		label.setText("Select directory to analyse...");

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
		selectedDirectory = dc.showDialog(stage);

		if (selectedDirectory == null) {
			label.setText("No directory selected. Terminated.");
			// System.out.println(label.getText());
		} else {

			// Adding Title to the stage
			stage.setTitle("Spiral Rectangle Cloud of " + selectedDirectory.getName());

			label.setText("Analysing directory structure...");

			// collect all files of the folder structure and sort them by absolute file path
			totalNumOfLevels = visitDirectory(selectedDirectory, 0, HUE_MIN, HUE_MAX);

			// Sort by filename
			Collections.sort(files, new Comparator<FileInfo>() {
				@Override
				public int compare(FileInfo a, FileInfo b) {
					return a.filename.compareToIgnoreCase(b.filename);
				}
			});

			// adding clouds (panes)
			pane.getChildren().add(rectCloud);
			pane.getChildren().add(testRectCloud);

			// for debugging
			spiralCanvas = new Canvas(scene_width, scene_height);
			GraphicsContext spiralGC = spiralCanvas.getGraphicsContext2D();
			spiralGC.setStroke(Color.RED);
			spiralGC.setFill(Color.RED);
			pane.getChildren().add(spiralCanvas);
			spiralCanvas.getGraphicsContext2D().setLineWidth(20);
			spiralCanvas.getGraphicsContext2D().strokeRect(0, 0, 1000, 800);
			spiralCanvas.getGraphicsContext2D().setLineWidth(1);

			// and for debugging too
			Canvas guidesCanvas = new Canvas(scene_width, scene_height);
			GraphicsContext guidesGC = guidesCanvas.getGraphicsContext2D();
			guidesGC.setStroke(Color.LIGHTGRAY);
			guidesGC.setFill(Color.LIGHTGRAY);
			pane.getChildren().add(guidesCanvas);
			guidesGC.strokeLine(scene_width / 2, 0, scene_width / 2, scene_height);
			guidesGC.strokeLine(0, scene_height / 2, scene_width, scene_height / 2);
			// draw border around guides canvas
			guidesGC.setLineWidth(10);
			guidesGC.strokeRect(0, 0, 1000, 800);
			guidesGC.setLineWidth(1);

			FineTuningCanvas = new Canvas(scene_width, scene_height);
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
						// Fading SpiralCanvas
						spiralFader = new FadeTransition(Duration.millis(3000), spiralCanvas);
						spiralFader.setFromValue(1.0);
						spiralFader.setToValue(0.0);
						spiralFader.setCycleCount(1);
						spiralFader.play();

						// Fading FineTuningCanvas
						FineTuningFader = new FadeTransition(Duration.millis(3000), FineTuningCanvas);
						FineTuningFader.setFromValue(1.0);
						FineTuningFader.setToValue(0.0);
						FineTuningFader.setCycleCount(1);
						FineTuningFader.play();
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

			fileEventHandler.init(scene_width / 2,
					scene_height / 2 /* center location of first item of the collision spiral */
					);
			KeyFrame fileKeyFrame = new KeyFrame(Duration.millis(drawingSpeed), fileEventHandler);

			KeyFrame spiralPositionKeyFrame = new KeyFrame(Duration.millis(drawingSpeed), spiralEventHandler);

			KeyFrame orientationKeyFrame = new KeyFrame(Duration.millis(drawingSpeed), orientationEventHandler);

			KeyFrame finePositionKeyFrame = new KeyFrame(Duration.millis(drawingSpeed), fineTuningEventHandler);

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

			// Set start locations for the spirals in spiral point 0
			SpiralPoint.init(numRectOrientationsAndAlignments);

			spirals[0] = new SpiralInfo();
			spirals[0].appendSpiralPoint(new SpiralPoint(0,  1,  1));
			spirals[1] = new SpiralInfo();
			spirals[1].appendSpiralPoint(new SpiralPoint(0,  1, -1));
			spirals[2] = new SpiralInfo();
			spirals[2].appendSpiralPoint(new SpiralPoint(0, -1,  1));
			spirals[3] = new SpiralInfo();
			spirals[3].appendSpiralPoint(new SpiralPoint(0, -1, -1));

			fileTimeline.play();

		}

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

		//		System.out.println("center of spiral x= " + x + ", y= " + y );
		//		System.out.println("dimensions of new rect: width= " + rectWidth + ", height= " + rectHeight);
		//		
		//		System.out.println(String.format("check collision of new rect @ x= %.2f y= %.2f width= %.2f height= %.2f against all " + RectCloud.getChildren().size() + " already positioned rects.",
		//				currentTestRect.getX(),
		//				currentTestRect.getY(),
		//				currentTestRect.getWidth(),
		//				currentTestRect.getHeight()
		//		));

		// ... and check each for collision with all previously created rectangles
		boolean anyCollisions = false;
		for (Node r : rectCloud.getChildren()) {

			if (r == null)
				break;

			//			System.out.print(String.format("  against rect @ x= %.0f y= %.0f width= %.0f height= %.0f   ",
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
			//			System.out.println(collision ? "collision" : "no collision");

			anyCollisions |= collision;

		}

		return anyCollisions;
	}

	/**
	 * Shuffle array (Durstenfeld shuffle, implementation for Fishes-Yates shuffle
	 * based on https://stackoverflow.com/a/1520212 seems (according to ) the an
	 * Fisher-Yates shuffle, exactly the Durstenfeld inplace shuffle implementation
	 * (Fisher�Yates shuffle)
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
		Random rand = new Random(25);
		for (int i = testRects.length - 1 - to; i > from; i--) {
			int index = rand.nextInt(i + 1);
			// Simple swap two entries
			T a = testRects[index];
			testRects[index] = testRects[i];
			testRects[i] = a;
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
			if (DRAW_DEBUG) {
				spiralGC.strokeLine(x, y, x1, y1);
				spiralGC.fillOval(x - 5 / 2, y - 5 / 2, 5, 5);
			}

			step++;

		}

		return newRect;
	}

	private int visitDirectory(File directory, int level, float minHue, float maxHue) {

		int numOfLevels = level;
		//			System.out.println(directory.getAbsolutePath() + 
		//					" L: " + level + 
		//					" min_ab: " + min_ab + 
		//					" max_ab: " + max_ab
		//			);	

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

		//			System.out.println(directory.getAbsolutePath() + " deltaHue: " + deltaHue + 
		//					" num: " + numOfChildDirectories + 
		//					" partHue: " + partHue + 
		//					" dirHue:" + hueOfDirectory
		//			);

		int childDirectoriesCounter = 0;

		for (String FilesAndDirectories : childFilesAndDirectories) {

			File fileOrDirectory = new File(directory, FilesAndDirectories);

			if (fileOrDirectory.isDirectory()) {
				float currentMinHue = minHue + childDirectoriesCounter * partHue;
				float currentMaxHue = minHue + childDirectoriesCounter * partHue + partHue;

				//				System.out.println(" " + fileOrDirectory.getAbsolutePath() + 
				//						" current_min_ab: " + current_min_ab + 
				//						" current_max_ab: " + current_max_ab
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
						files.add(
								new FileInfo(fileOrDirectory.getAbsolutePath(), level, width, height, hueOfDirectory));
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
	 * 
	 */
	private void addNewRectangleToRectCloud(double canvas_x, double canvas_y, double w, double h) {

		// create rectangle and add to RectCloud
		Rectangle newRect = new Rectangle(canvas_x, canvas_y, w, h);
		// Calc color
		float brightness = HUE_MIN + (HUE_MAX - HUE_MIN) * fileEventHandler.level / totalNumOfLevels;
		Color parentDirectoryColor = ConvertAwtColorToJavaFX(
				java.awt.Color.getHSBColor(fileEventHandler.hue, 1.0f, brightness));
		newRect.setStroke(Color.TRANSPARENT);
		newRect.setFill(parentDirectoryColor);
		Tooltip.install(newRect, new Tooltip(fileEventHandler.filename));
		rectCloud.getChildren().add(newRect);
		//	    			System.out.println(String.format("rect '%s' @ x= %.2f y= %.2f width= %.2f height= %.2f color=%s added.",
		//	    					files.get(fileEventHandler.fileindex).filename,
		//	    					newRect.getX(),
		//	    					newRect.getY(),
		//	    					newRect.getWidth(),
		//	    					newRect.getHeight(),
		//	    					parentDirectoryColor.toString()
		//					));

		//					// csv style
		//	    			System.out.println(String.format(Locale.US, "%s, %d, %.2f, %.2f, %.2f, %.2f, %s, %.2f, %.2f, %.2f",
		//	    					files.get(fileEventHandler.fileindex).filename,
		//	    					files.get(fileEventHandler.fileindex).level,
		//	    					newRect.getX(),
		//	    					newRect.getY(),
		//	    					newRect.getWidth(),
		//	    					newRect.getHeight(),
		//	    					parentDirectoryColor.toString(),
		//	    					files.get(fileEventHandler.fileindex).hue, 
		//	    					1.0f, 
		//	    					brightness
		//					));
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