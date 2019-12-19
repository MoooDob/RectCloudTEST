package Application;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import org.apache.commons.imaging.color.ColorCieLab;
import org.apache.commons.imaging.color.ColorConversions;
import org.apache.commons.io.FilenameUtils;

import javafx.animation.Animation.Status;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.Pair;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public class SpiralTEST extends Application {

	boolean showFiles = true;
	double scene_width = 1000;
	double scene_height = 800;
	int numOfRectangles = 10;
	double maxRectHeight = scene_width / 8;
	double maxRectWidth = scene_height / 9;
	double fadeOffset = 200; 
	
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

	private Random randomizer = new Random(34);

	//private ArrayList<Rectangle> rectangles = new ArrayList<Rectangle>();

	private int rectCounter = 0;
	
	ArrayList<Rectangle> testedRectangles = new ArrayList<Rectangle>();
    
	Timeline spiralCollisionCheckAnimationTimeline = null;
	
	// Array for all files
	ArrayList<String> allFiles = new ArrayList<String>();
	
	// Array for all files
	HashMap<String, ColorCieLab> fileColors = new HashMap<String,ColorCieLab>(); 
	
	// Color space
	final double L_MIN = 10;
	final double L_MAX = 90;
	
	final int A_MIN = -128;
	final int A_MAX =  127;
	final int B_MIN = -128;
	final int B_MAX =  127;
	
	
	public final class ImmutableTriple<L, M, R> {

	    public final L left;
	    public final M middle;
	    public final R right;

	    public <L, M, R> ImmutableTriple<L, M, R> of(final L left, final M middle, final R right) {
	        return new ImmutableTriple<L, M, R>(left, middle, right);
	    }

	    public ImmutableTriple(final L left, final M middle, final R right) {
	        super();
	        this.left = left;
	        this.middle = middle;
	        this.right = right;
	    }

	    //-----------------------------------------------------------------------
	    public L getLeft() {
	        return left;
	    }

	    public M getMiddle() {
	        return middle;
	    }

	    public R getRight() {
	        return right;
	    }
	}

	
	class TimelineEvent implements EventHandler<ActionEvent> {

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
		
		

		@Override
		public void handle(ActionEvent event) {
	        System.out.println("drawing...");
	        			
			if (testRects == null || step % 2 == 0) {
			// use x and y as center point for the testRects
				testRects = new Rectangle[2];
				testRects[0] =	new Rectangle(x0 + x - rectWidth / 2, y0 + y - rectHeight / 2, rectWidth, rectHeight); // Upright rectangle
				testRects[1] =	new Rectangle(x0 + x - rectHeight / 2, y0 + y - rectWidth / 2, rectHeight, rectWidth);  // 90 degree rotated rectangle
				
				// Shuffle testRects to get a random orientation/alignment
				shuffleArrayDurstenfeld(testRects);
			}
	        
			Rectangle currentTestRect = testRects[step % 2];
			testedRectangles.add(currentTestRect);
			currentTestRect.setStroke(Color.GRAY);
			currentTestRect.setFill(Color.TRANSPARENT);
			// currentTestRect.getStrokeDashArray().addAll(2d, 21d); //dotted line
			testRectCloud.getChildren().add(currentTestRect);
			
			// Fading					
			FadeTransition ft = new FadeTransition(Duration.millis(3000), currentTestRect);
			ft.setFromValue(1.0);
			ft.setToValue(0);
			ft.setCycleCount(1);
			ft.setDelay(new Duration(fadeOffset));
			ft.play();

	        boolean collision = checkCollisonAABB(x0 + x, y0 + y, rectWidth, rectHeight, step, currentTestRect, rectCloud);
			if (collision 
					&& step < 1000 // emergency break
					) {
				double x1 = x;
				double y1 = y;
				
				double h = Math.sqrt((Math.pow(x,2) + Math.pow(y,2))) / spinRate;
		        x = x1 - y1/h;
		        y = y1 + x1/h;	 
		        
		        System.out.println("Step: " + step);
				
				// Draw spiral
				spiralCanvas.getGraphicsContext2D().strokeLine(x0 + x, y0 + y, x0 + x1, y0 + y1);
				spiralCanvas.getGraphicsContext2D().fillOval(x0 + x -5/2, y0 + y -5/2, 5, 5);

				step++;
				
	        } else {
	        	// Found collision free location OR more than 1000 steps needed
		        System.out.println("Drawing Animation stopped.");
	        	spiralCollisionCheckAnimationTimeline.stop();
    			   			    		
    			// Clone Rectangle and add to RectCloud
    			Rectangle newRect = new Rectangle(currentTestRect.getX(), currentTestRect.getY(), currentTestRect.getWidth(), currentTestRect.getHeight()); 
    			newRect.setStroke(Color.BLACK);
    			newRect.setFill(Color.TRANSPARENT);
				rectCloud.getChildren().add(newRect);
    			System.out.println(String.format("rect @ x= %.0f y= %.0f width= %.0f height= %.0f added.",
    					currentTestRect.getX(),
    					currentTestRect.getY(),
    					currentTestRect.getWidth(),
    					currentTestRect.getHeight()
				));


				// Fading
				ft = new FadeTransition(Duration.millis(3000), spiralCanvas);
				ft.setFromValue(1.0);
				ft.setToValue(0.1);
				ft.setCycleCount(1);
				ft.play();
				
//				// Fading					
//				ft = new FadeTransition(Duration.millis(3000), testRectCanvas);
//				ft.setFromValue(1.0);
//				ft.setToValue(0.1);
//				ft.setCycleCount(1);
//				ft.play();
	        }
	    }
		
		public TimelineEvent init(double x0, double y0, double rectWidth, double rectHeight, double spinRate, Pane rectCloud2, Pane testRectCloud2, Canvas spiralCanvas) {
			this.x0 = x0;
			this.y0 = y0;
			this.rectWidth = rectWidth;
			this.rectHeight = rectHeight;
			this.spinRate = spinRate;
			this.rectCloud = rectCloud2;
			this.spiralCanvas = spiralCanvas;
			this.testRectCloud = testRectCloud2;
			this.testRects = null;
			x = 0;
			y = 1;			
			step = 0;

			return this;
		}
	}
		


	
	/**
	 * @param testRectCloud 
	*
	*/
	@Override
	public void start(Stage stage) {
		
		// ask for directory

		DirectoryChooser dc = new DirectoryChooser();
		File selectedDirectory = dc.showDialog(stage);

		if (selectedDirectory == null) {
			System.out.println("No directory selected. Terminated.");
		} else {

			// collect all files of the folder structure and sort
			int totalNumOfLevels = visitDirectory(selectedDirectory, 0, hashAB(A_MIN,B_MIN), hashAB(A_MAX,B_MAX));			
			Collections.sort(allFiles);
			
			// Calculate the RGB colors 
			HashMap<String, Color> RGBColors = new HashMap<String, Color>();
			for (String fullfilename : fileColors.keySet()) {
				ColorCieLab LabColor = fileColors.get(fullfilename);
				double L = L_MIN + (L_MAX - L_MIN)  * LabColor.L /*contains the level*/ / totalNumOfLevels;
				Color parentDirectoryColor = convertLabToFXColor(new ColorCieLab(L, LabColor.a, LabColor.b));
				RGBColors.put(fullfilename, parentDirectoryColor);
			}
				
	        Label label = new Label("Press S to start.");
	        
	    	// Creating a Group object
			StackPane stackpane = new StackPane();
			Pane rectCloud = new Pane();
			stackpane.getChildren().add(rectCloud);
					
			Pane testRectCloud = new Pane();
			stackpane.getChildren().add(testRectCloud);
	
	        BorderPane root = new BorderPane(stackpane);
	        root.setBottom(label);
	       
			
			// for debugging too
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
			guidesGC.setLineWidth(10);
			guidesGC.strokeRect(0, 0, 1000, 800);
			guidesGC.setLineWidth(1);
	
			// Creating a scene object
			Scene scene = new Scene(root, scene_width, scene_height + 17 /*label*/);
			scene.setOnKeyPressed(e -> {
			    if (
			    		(spiralCollisionCheckAnimationTimeline == null
			    		|| spiralCollisionCheckAnimationTimeline.getStatus()==Status.STOPPED
			    		) 
			    		&& (e.getCode() == KeyCode.S)
			    		) {
			    	
					

			    	for (int i = 0; i < allFiles.size(); i++) {
			    		
						File file = new File(allFiles.get(i));				
						Color parentDirectoryColor = RGBColors.get(file.getAbsolutePath());
						
						int lineCtr = 0;
						int maxLineLength = 0;
						try {
							Scanner scanner = new Scanner(file);
							while (scanner.hasNextLine()) {
								String line = scanner.nextLine();
								maxLineLength = Math.max(maxLineLength, line.length());
								lineCtr++;
							}
							scanner.close();
						} catch (FileNotFoundException excep) {
							excep.printStackTrace();
						}

						double height = 0;
						double width = 0;
						if (Arrays.stream(dimensionDisplayFilenameFilter).anyMatch(file.getName().toLowerCase()::equals)
								|| dimensionDisplayExtensionFilter == null 
								|| dimensionDisplayExtensionFilter.length == 0 
								|| Arrays.stream(dimensionDisplayExtensionFilter).anyMatch(FilenameUtils.getExtension(file.getName().toLowerCase())::equals)
								) {
							// extension is in dimensionDisplayExtensionFilter
							height = lineCtr;
							width = maxLineLength;        	
						} else {
							// extension is not in dimensionDisplayExtensionFilter
							height = 12;
							width = 50;        
						}
						
				    	spiralGC.clearRect(0, 0, scene_width, scene_height);
				
						checkRectCollisionSpiralOfTheodorus(
							scene_width / 2, scene_height / 2, /*center location of test spiral*/ 
							20.0, // 2.0, /* spinRate */
							width, /* width of new rectangle */
							height, /* height of new rectangle*/
							rectCloud, 
							testRectCloud,
							spiralCanvas
						);	
			    	}
		    	}
			});
	
			// Adding Title to the stage
	        stage.setTitle("Spiral Rectangle Cloud");
	
			// Adding scene to the stage
			stage.setScene(scene);		
	
			// Displaying the contents of the stage
			stage.show();
			
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

		System.out.println("center x= " + x + ", y= " + y );
		System.out.println("dimensions of new rect: width= " + rectWidth + ", height= " + rectHeight);

		System.out.println("check collision of new rect against all " + rectCounter + " already positioned rects. ");	
			
		System.out.println(String.format("check new rect @ x= %.0f y= %.0f width= %.0f height= %.0f",
				currentTestRect.getX(),
				currentTestRect.getY(),
				currentTestRect.getWidth(),
				currentTestRect.getHeight()
		));
				
		// ... and check each for collision with all previously created rectangles
		boolean anyCollisions = false;
		for (Node r : RectCloud.getChildren()) {
			
			if (r == null) break;
			
			System.out.print(String.format("  against rect @ x= %.0f y= %.0f width= %.0f height= %.0f   ",
					((Rectangle)r).getX(),
					((Rectangle)r).getY(),
					((Rectangle)r).getWidth(),
					((Rectangle)r).getHeight()
			));

			// getBoundsInLocal is only correct if and only if both objects are in the same coordinate system. 
			// if one object is for instance part of another group, the coordinate system have to be transformed
			// furthermore this function tests only the AABB (axis aligned bounding box) of both objects, 
			// so rotated will be approximated with its AABB
			boolean collision = r.getBoundsInParent().intersects(currentTestRect.getBoundsInLocal());
			System.out.println(collision ? "collision" : "no collision");
			
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
	
	
	// Spiral of Theodorus (Wurzelspirale)
	/**
	 * @param <testRectCloud>
	 * @param x0 Center of spiral of theodorus
	 * @param y0 Center of spiral of theodorus
	 * @param maxSteps max radius of the spiral, for debugging only
	 * @param spinRate defines the scaling of the spiral
	 * @param rectWidth width of the new rectangle
	 * @param rectHeight height of the new rectangle
	 * @param testRectGC GraphicsContext for drawing debug information (the checked rectangles)
	 * @param spiralGC GraphicsContext for drawing debug information (the spiral itself)
	 * @param rectCloud 
	 * @param spiralCanvas 
	 * @param testRectCloud 
	 */
	public void checkRectCollisionSpiralOfTheodorus (double x0, double y0, double spinRate, 
			double rectWidth, double rectHeight, Pane rectCloud, Pane testRectCloud, Canvas spiralCanvas) {
	
		// Center of spiral
		spiralCanvas.getGraphicsContext2D().fillOval(x0 -5/2, y0 -5/2, 5, 5);

		spiralCanvas.setOpacity(1);
		// remove all previous testRects
		testRectCloud.getChildren().clear();

		spiralCollisionCheckAnimationTimeline = new Timeline(new KeyFrame(Duration.millis(200), 
				new TimelineEvent().init(x0, y0, rectWidth, rectHeight, spinRate, rectCloud, testRectCloud, spiralCanvas)));
		
		spiralCollisionCheckAnimationTimeline.setCycleCount(Timeline.INDEFINITE);
		spiralCollisionCheckAnimationTimeline.play();
	}

	private int visitDirectory(File directory, int level, double min_ab, double max_ab) {
	
			int numOfLevels = level;
	//		System.out.println(directory.getAbsolutePath() + 
	//				" L: " + level + 
	//				" min_ab: " + min_ab + 
	//				" max_ab: " + max_ab
	//		);		
			// Filter
			String[] childFilesAndDirectories = directory.list(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					File file = new File(dir, name);
					if (fileExtensionFilter == null || fileExtensionFilter.length == 0 ) {
						// No Filter defined
						return true;
					} else {
						// check if file extension is in the list of allowed extensions
						return file.isDirectory() || Arrays.stream(fileExtensionFilter).anyMatch(FilenameUtils.getExtension(name.toLowerCase())::equals);
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
			double delta_ab = max_ab - min_ab;
			double part_ab = delta_ab / numOfChildDirectories;
			double directory_ab = (max_ab + min_ab) / 2; // center of current a,b color range
			Pair<Integer, Integer> AB = unhashAB((int)directory_ab);
			int a = AB.getKey();
			int b = AB.getValue();
			ColorCieLab directoryColor = new ColorCieLab(level, a, b); 
			
	//		System.out.println(" delta: " + delta_ab + 
	//				" num: " + numOfChildDirectories + 
	//				" part_ab: " + part_ab + 
	//				" dirab:" + directory_ab
	//		);
			
			int childDirectoriesCounter = 0;
			for (String FilesAndDirectories : childFilesAndDirectories) {
	
				
				File fileOrDirectory = new File(directory, FilesAndDirectories);
	
				if (fileOrDirectory.isDirectory()) {
					double current_min_ab = min_ab + childDirectoriesCounter * part_ab;
					double current_max_ab = min_ab + childDirectoriesCounter * part_ab + part_ab;
					
	//				System.out.println(" " + fileOrDirectory.getAbsolutePath() + 
	//						" current_min_ab: " + current_min_ab + 
	//						" current_max_ab: " + current_max_ab
	//				);
	
					numOfLevels = Math.max(numOfLevels, visitDirectory(fileOrDirectory, level + 1, current_min_ab, current_max_ab));
					childDirectoriesCounter++;
				} else {
					if (showFiles) {
						String s = fileOrDirectory.getAbsolutePath();
						allFiles.add(fileOrDirectory.getAbsolutePath());
						fileColors.put(s, directoryColor);
					}
				}
			}
			
			return numOfLevels;
			
		}




	private Color convertLabToFXColor(ColorCieLab color) {				
			java.awt.Color colorRGB_awt = new java.awt.Color(ColorConversions.convertXYZtoRGB(ColorConversions.convertCIELabtoXYZ(color)));
			Color fxColor = new Color((double)colorRGB_awt.getRed()/256, (double)colorRGB_awt.getGreen()/256, (double)colorRGB_awt.getBlue()/256, (double)colorRGB_awt.getAlpha()/256);
	//		System.out.println(" Lab: " + color.toString() + 
	//				" RGB: " + fxColor.toString() +
	//				" R " + ((double)colorRGB_awt.getRed()/256) + 
	//		        " G " + ((double)colorRGB_awt.getRed()/256) + 
	//				" B " + ((double)colorRGB_awt.getRed()/256) + 
	//				" A " + ((double)colorRGB_awt.getAlpha()/256)
	//		);
			return fxColor;
		}




	private static Pair<Integer,Integer> unhashAB(int hash) {
		int a = (int) hash / 256;
		int b = hash - a * 256;      
	    return new Pair<Integer,Integer>(a,b);
	}




	private static int hashAB(int a, int b) {
        return a * 256 + b;
	}

}