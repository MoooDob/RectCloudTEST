package Application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;
import java.util.Comparator;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.animation.Animation.Status;

public class SpiralTEST extends Application {

	boolean showFiles = true;
	double scene_width = 1000;
	double scene_height = 800;
	int numOfRectangles = 10;
	double maxRectHeight = scene_width / 8;
	double maxRectWidth = scene_height / 9;
	
	double defaultWidth = 12;
	double defaultHeight = 12;
	
	double drawingSpeed = 0.1f;
	double fadeOffset = drawingSpeed; 
	double rectFadingSpeed = drawingSpeed * 10;



	
	// Files with this extension will be shown, null or empty array => all files 
	final String[] fileExtensionFilter = {}; //{"java"}; // {"java", "cpp", "h"} // null /*=> all*/

	// files with this extension will shown using their dimension (max line length x lines),
	// other files will be shown using an equal sized rounded rectangle
	// null or empty array => show all files with dimensions
	final String[] dimensionDisplayExtensionFilter = {}; // {"java"}

	// files with this file name will be explicitly shown using their dimension 
	// (max line length x lines)
	final String[] dimensionDisplayFilenameFilter = {}; // {"readme.md"}


	// **************************

	
	ArrayList<Rectangle> testedRectangles = new ArrayList<Rectangle>();
    
	Timeline spiralCollisionCheckAnimationTimeline = null;

	// HSB color space, limits for the hue value
	final float HUE_MIN = .1f;
	final float HUE_MAX = .9f;
		
	private ArrayList<FileInfo> files = new ArrayList<FileInfo>();
	private TimelineEvent collisionCheckEvent;
	
	private Label label;
	private FadeTransition spiralFader;
	
	private File selectedDirectory;

	
	
	private class FileInfo {
		private float hue;
		public FileInfo(String filename, int level, float hue) {
			this.filename = filename;
			this.level = level;
			this.hue = hue;
		}
		
		public FileInfo(String filename, int level, double height, double width, float hue) {
			this(filename, level, hue);
			this.width = width;
			this.height = height;
		}
		
		String filename = "";
		double width = 0;
		double height = 0;
		int level = -1;
	}
	

	
	private class TimelineEvent implements EventHandler<ActionEvent> {

		private double x0;
		private double y0;
		private double rectWidth;
		private double rectHeight;
		private int step;
		private double x;
		private double y;
		private double spinRate;

		private Pane rectCloud;
		private Pane testRectCloud;
		private Rectangle[] testRects;
		
		private Canvas spiralCanvas;
		private int fileindex;
		private int totalNumOfLevels;
		private String filename;
		
		

		

		@Override
		public void handle(ActionEvent event) {
	        
	        if (spiralFader != null) spiralFader.stop();
	        spiralCanvas.setOpacity(1);
	        			
			if (testRects == null || step % 2 == 0) {
			// use x and y as center point for the testRects
				testRects = new Rectangle[2];
				testRects[0] =	new Rectangle(x0 + x - rectWidth / 2, y0 + y - rectHeight / 2, rectWidth, rectHeight); // Upright rectangle
				testRects[1] =	new Rectangle(x0 + x - rectHeight / 2, y0 + y - rectWidth / 2, rectHeight, rectWidth);  // 90 degree rotated rectangle
				
				// Shuffle testRects to get a random orientation/alignment
				shuffleArrayDurstenfeld(testRects);
			}
	        
			Rectangle currentTestRect = testRects[step % 2];
			
			// for visual debugging
			testedRectangles.add(currentTestRect);
			currentTestRect.setStroke(Color.GRAY);
			currentTestRect.setFill(Color.TRANSPARENT);
			// currentTestRect.getStrokeDashArray().addAll(2d, 21d); //dotted line
			testRectCloud.getChildren().add(currentTestRect);
			
			// Rect Fading					
			FadeTransition rectFader = new FadeTransition(Duration.millis(rectFadingSpeed), currentTestRect);
			rectFader.setFromValue(1.0);
			rectFader.setToValue(0);
			rectFader.setCycleCount(1);
			rectFader.setDelay(new Duration(fadeOffset));
			rectFader.setOnFinished(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					// remove faded rect from testRectCloud
					testRectCloud.getChildren().remove(((FadeTransition)event.getSource()).getNode());
				}
			});
			rectFader.play();

	        boolean collision = checkCollisonAABB(x0 + x, y0 + y, rectWidth, rectHeight, step, currentTestRect, rectCloud);
			if (collision 
					&& step < 1000 // emergency break
					) {
				
				if (step % 2 == 1) {
					
					// calc next step in the spiral of Theodorus calculation (Wurzelspirale)
					double x1 = x;
					double y1 = y;
					
					double h = Math.sqrt((Math.pow(x,2) + Math.pow(y,2))) / spinRate;
			        x = x1 - y1/h;
			        y = y1 + x1/h;	 
			        
			        // System.out.println("Step: " + step);
					
					// Draw spiral
					spiralCanvas.getGraphicsContext2D().strokeLine(x0 + x, y0 + y, x0 + x1, y0 + y1);
					spiralCanvas.getGraphicsContext2D().fillOval(x0 + x -5/2, y0 + y -5/2, 5, 5);

				}
				
				step++;
				
	        } else {
	        	// Found collision free location OR more than 1000 steps needed

	        	if (fileindex >= files.size()) {
		        	spiralCollisionCheckAnimationTimeline.stop();
	        	} else {
	        		spiralCollisionCheckAnimationTimeline.pause();
	        	}
    			   			    		
    			// Clone Rectangle and add to RectCloud
    			Rectangle newRect = new Rectangle(currentTestRect.getX(), currentTestRect.getY(), currentTestRect.getWidth(), currentTestRect.getHeight());
    			
    			// Calc color
				float brightness = HUE_MIN + (HUE_MAX - HUE_MIN) * files.get(fileindex).level / totalNumOfLevels;
				Color parentDirectoryColor = ConvertAwtColorToJavaFX(java.awt.Color.getHSBColor(files.get(fileindex).hue, 1.0f, brightness));
    			newRect.setStroke(Color.TRANSPARENT);
    			newRect.setFill(parentDirectoryColor);
				rectCloud.getChildren().add(newRect);
//    			System.out.println(String.format("rect '%s' @ x= %.2f y= %.2f width= %.2f height= %.2f color=%s added.",
//    					files.get(fileindex).filename,
//    					newRect.getX(),
//    					newRect.getY(),
//    					newRect.getWidth(),
//    					newRect.getHeight(),
//    					parentDirectoryColor.toString()
//				));
				
				// csv style
    			System.out.println(String.format(Locale.US, "%s, %d, %.2f, %.2f, %.2f, %.2f, %s, %.2f, %.2f, %.2f",
    					files.get(fileindex).filename,
    					files.get(fileindex).level,
    					newRect.getX(),
    					newRect.getY(),
    					newRect.getWidth(),
    					newRect.getHeight(),
    					parentDirectoryColor.toString(),
    					files.get(fileindex).hue, 
    					1.0f, 
    					brightness
				));


				// Fading spiral canvas
				spiralFader = new FadeTransition(Duration.millis(3000), spiralCanvas);
				spiralFader.setFromValue(1.0);
				spiralFader.setToValue(0.0);
				spiralFader.setCycleCount(1);
				spiralFader.play();
				
				// remove all previous testRects
				testRectCloud.getChildren().clear();
				
				// prepare next file info
				if (fileindex < files.size() - 1) {
					// get next fileInfo
			    	spiralCanvas.getGraphicsContext2D().clearRect(0, 0, scene_width, scene_height);
					fileindex++;
					collisionCheckEvent.init(x0, y0, spinRate, totalNumOfLevels, rectCloud, testRectCloud, spiralCanvas);
	        		spiralCollisionCheckAnimationTimeline.play();
				} else label.setText(selectedDirectory.getName() + " finished.");
				

	        }
	    }
		
		public TimelineEvent init(double x0, double y0, double spinRate, int totalNumOfLevels, Pane rectCloud2, Pane testRectCloud2, Canvas spiralCanvas) {
			this.x0 = x0;
			this.y0 = y0;
			this.filename = files.get(fileindex).filename;
			this.rectWidth = files.get(fileindex).width / 5;
			this.rectHeight = files.get(fileindex).height / 5;
			this.spinRate = spinRate;
			this.totalNumOfLevels = totalNumOfLevels;
			this.rectCloud = rectCloud2;
			this.spiralCanvas = spiralCanvas;
			this.testRectCloud = testRectCloud2;
			this.testRects = null;
			x = 0;
			y = 1;			
			step = 0;
			
			// mark center of spiral
			spiralCanvas.getGraphicsContext2D().fillOval(x0 -5/2, y0 -5/2, 5, 5);

			String info = "find location for file " + filename +
					" width: " + rectWidth +
					" height: " + rectHeight;
			// System.out.println(info);
			label.setText(info);

			return this;
		}
	}
		


	
	/**
	 * @param testRectCloud 
	*
	*/
	@Override
	public void start(Stage stage) {
		
		label = new Label("Select directory to analyse...");
		
		StackPane stackpane = new StackPane();
		
		ScrollPane scroller = new ScrollPane(stackpane);
		scroller.setPannable(true);

		BorderPane root = new BorderPane(scroller);
        root.setBottom(new Pane(label));
        ((Pane)root.getBottom()).setBackground(new Background(new BackgroundFill(Color.WHEAT, CornerRadii.EMPTY, Insets.EMPTY)));

		
		// Creating a scene object
		Scene scene = new Scene(root, scene_width, scene_height + 17 /*height of label*/ );


		// Adding scene to the stage
		stage.setScene(scene);		

		// Displaying the contents of the stage
		stage.show();	

		
		// ask for directory
		DirectoryChooser dc = new DirectoryChooser();
		selectedDirectory = dc.showDialog(stage);

		if (selectedDirectory == null) {
			label.setText("No directory selected. Terminated.");
			System.out.println(label.getText());
		} else {
					
			// Adding Title to the stage
	        stage.setTitle("Spiral Rectangle Cloud of " + selectedDirectory.getName());

			label.setText("Analysing directory structure...");

			// collect all files of the folder structure and sort them by absolute file path
			int totalNumOfLevels = visitDirectory(selectedDirectory, 0, HUE_MIN, HUE_MAX);
			
			// Sort by filename
			Collections.sort(files, new Comparator<FileInfo>(){
				@Override
			    public int compare(FileInfo a, FileInfo b) {
			        return a.filename.compareToIgnoreCase(b.filename);
			    }
			});
	        
			
			
			// creating Panes
			Pane rectCloud = new Pane();
			stackpane.getChildren().add(rectCloud);
					
			Pane testRectCloud = new Pane();
			stackpane.getChildren().add(testRectCloud);
			
			
			// for debugging
			Canvas spiralCanvas = new Canvas(scene_width, scene_height);
			GraphicsContext spiralGC = spiralCanvas.getGraphicsContext2D();
			spiralGC.setStroke(Color.RED);
			spiralGC.setFill(Color.RED);
			stackpane.getChildren().add(spiralCanvas);
			spiralCanvas.getGraphicsContext2D().setLineWidth(20);
			spiralCanvas.getGraphicsContext2D().strokeRect(0, 0, 1000, 800);
			spiralCanvas.getGraphicsContext2D().setLineWidth(1);
			
			// and for debugging too
			Canvas guidesCanvas = new Canvas(scene_width, scene_height);
			GraphicsContext guidesGC = guidesCanvas .getGraphicsContext2D();
			guidesGC.setStroke(Color.LIGHTGRAY);
			guidesGC.setFill(Color.LIGHTGRAY);
			stackpane.getChildren().add(guidesCanvas);
			guidesGC.strokeLine(scene_width / 2, 0, scene_width / 2, scene_height);
			guidesGC.strokeLine(0, scene_height / 2, scene_width, scene_height / 2);
			// draw border around guides canvas
			guidesGC.setLineWidth(10);
			guidesGC.strokeRect(0, 0, 1000, 800);
			guidesGC.setLineWidth(1);
	
			
			scene.setOnKeyPressed(e -> {
			    if (e.getCode() == KeyCode.ESCAPE) {
			    	spiralCollisionCheckAnimationTimeline.stop();
			    	}
			    if (e.getCode() == KeyCode.SPACE) {
			    	if (spiralCollisionCheckAnimationTimeline.getStatus() == Status.PAUSED) spiralCollisionCheckAnimationTimeline.play(); 
			    			else spiralCollisionCheckAnimationTimeline.pause();
			    	}
			    if (spiralCollisionCheckAnimationTimeline.getStatus() == Status.PAUSED) {
				// Fading
					spiralFader = new FadeTransition(Duration.millis(3000), spiralCanvas);
					spiralFader.setFromValue(1.0);
					spiralFader.setToValue(0.0);
					spiralFader.setCycleCount(1);
					spiralFader.play();
			    }
			});
			
			// Create operator
			AnimatedZoomOperator zoomOperator = new AnimatedZoomOperator();
			
			// bind scroll event to zoom operator
			stackpane.setOnScroll(new EventHandler<ScrollEvent>() {
			    @Override
			    public void handle(ScrollEvent event) {
			        double zoomFactor = 1.5;
			        if (event.getDeltaY() <= 0) {
			            // zoom out
			            zoomFactor = 1 / zoomFactor;
			        }
			        zoomOperator.zoom(stackpane, zoomFactor, event.getSceneX(), event.getSceneY());
			    }
			});

			
			
			// init TimelineEvent and create Timeline
			collisionCheckEvent = new TimelineEvent().init(
					scene_width / 2, scene_height / 2, /*center location of test spiral*/  
					20.0, // 2.0, /* spinRate */ 
					totalNumOfLevels,
					rectCloud, testRectCloud, spiralCanvas
			);
			spiralCollisionCheckAnimationTimeline = new Timeline(new KeyFrame(Duration.millis(drawingSpeed), collisionCheckEvent));
			
			spiralCollisionCheckAnimationTimeline.setCycleCount(Animation.INDEFINITE);
			spiralCollisionCheckAnimationTimeline.play();
		
		}
	
	}

	
	
	
	/**
	 * performs a AABB (Axis Aligned Bounding Box) collision check for the rectangle defined by x, y, rectWidth and rectHeight 
	 * and the already plotted rectangle in the array 'rectangles'   
	 * @param x x value of the new rectangle
	 * @param y y value of the new rectangle
	 * @param rectWidth width value of the new rectangle
	 * @param rectHeight height value of the new rectangle
	 * @param step 
	 * @param testRects 
	 * @param testRectGC GraphicsContext for drawing debug rectangles
	 * @return
	 */
	private boolean checkCollisonAABB(double x, double y, double rectWidth, double rectHeight, int step, Rectangle currentTestRect, Pane RectCloud) {

		// System.out.println("center of spiral x= " + x + ", y= " + y );
		// System.out.println("dimensions of new rect: width= " + rectWidth + ", height= " + rectHeight);

		// System.out.println("check collision of new rect against all " + rectCounter + " already positioned rects. ");	
			
//		System.out.println(String.format("check new rect @ x= %.2f y= %.2f width= %.2f height= %.2f",
//				currentTestRect.getX(),
//				currentTestRect.getY(),
//				currentTestRect.getWidth(),
//				currentTestRect.getHeight()
//		));
				
		// ... and check each for collision with all previously created rectangles
		boolean anyCollisions = false;
		for (Node r : RectCloud.getChildren()) {
			
			if (r == null) break;
			
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
			boolean collision = r.getBoundsInParent().intersects(currentTestRect.getBoundsInLocal());
			// System.out.println(collision ? "collision" : "no collision");
			
			anyCollisions |= collision;
			
		}
					
		return anyCollisions;
	}




	/**	Shuffle array (Durstenfeld shuffle, implementation for Fishes-Yates shuffle 
	 * based on https://stackoverflow.com/a/1520212
	 * seems (according to ) the an Fisher-Yates shuffle, exactly the Durstenfeld inplace shuffle implementation (Fisher–Yates shuffle)
	 * @param the array to be shuffled
	 */
	private static <T> void shuffleArrayDurstenfeld(T[] testRects) {
		Random rand = new Random(25);
	    for (int i = testRects.length - 1; i > 0; i--)
	    {
	      int index = rand.nextInt(i + 1);
	      // Simple swap two entries
	      T a = testRects[index];
	      testRects[index] = testRects[i];
	      testRects[i] = a;
	    }
	}

	
	
	
	// according to https://stackoverflow.com/q/48492980
	public Rectangle checkRectCollisionSpiralOfArchimedes(double x0, double y0, double maxRadius, double numSegments, double spinRate, double deltaR,
			double rectWidth, double rectHeight, GraphicsContext textRectGC, GraphicsContext spiralGC) {
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
				radius < maxRadius
				) {
			double x1 = x;
			double y1 = y;
			radius += deltaR;
			x = x0 + radius * Math.cos((2 * Math.PI * step / (radius * 10)) * spinRate);
			y = y0 + radius * Math.sin((2 * Math.PI * step / (radius * 10)) * spinRate);
			spiralGC.strokeLine(x, y, x1, y1);

			spiralGC.fillOval(x-5/2, y-5/2, 5, 5);
			
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
					if (fileExtensionFilter == null || fileExtensionFilter.length == 0 ) {
						// No Filter defined
						return true;
					} else {
						// check if file extension is in the list of allowed extensions
						return file.isDirectory() 
								|| Arrays.stream(fileExtensionFilter).anyMatch(FilenameUtils.getExtension(name.toLowerCase())::equals);
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
	
					numOfLevels = Math.max(numOfLevels, visitDirectory(fileOrDirectory, level + 1, currentMinHue, currentMaxHue));
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
						if (dimensionDisplayFilenameFilter == null 
								|| dimensionDisplayFilenameFilter.length == 0 
								|| Arrays.stream(dimensionDisplayFilenameFilter).anyMatch(fileOrDirectory.getName().toLowerCase()::equals)
								|| dimensionDisplayExtensionFilter == null 
								|| dimensionDisplayExtensionFilter.length == 0 
								|| Arrays.stream(dimensionDisplayExtensionFilter).anyMatch(FilenameUtils.getExtension(fileOrDirectory.getName().toLowerCase())::equals)
								) {
							// extension is in dimensionDisplayExtensionFilter
							height = lineCtr;
							width = maxLineLength;        	
						}
						
						files.add(new FileInfo(fileOrDirectory.getAbsolutePath(), level, width, height, hueOfDirectory));
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

}