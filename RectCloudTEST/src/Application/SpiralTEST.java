package Application;

import java.util.ArrayList;
import java.util.Random;

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
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

public class SpiralTEST extends Application {

	double scene_width = 1000;
	double scene_height = 800;
	int numOfRectangles = 10;
	double maxRectHeight = scene_width / 8;
	double maxRectWidth = scene_height / 9;
	double fadeOffset = 200; 


	// **************************

	private Random randomizer = new Random(34);

	private Rectangle[] rectangles = new Rectangle[numOfRectangles];

	private int rectCounter = 0;
	
	ArrayList<Rectangle> testedRectangles = new ArrayList<Rectangle>();
    
	Timeline spiralCollisionCheckAnimationTimeline = null;
	
	
	
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
		
		private Canvas spiralCanvas;
		private Canvas testRectCanvas;
		
		private Rectangle[] testRects;

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

//			// Debug draw
//			testRectCanvas.getGraphicsContext2D().setStroke(Color.LIGHTGRAY);
//			testRectCanvas.getGraphicsContext2D().strokeRect(currentTestRect.getX(), currentTestRect.getY(), currentTestRect.getWidth(), currentTestRect.getHeight());

	        boolean collision = checkCollisonAABB(x0 + x, y0 + y, rectWidth, rectHeight, step, currentTestRect, testRectCanvas.getGraphicsContext2D());
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

//	            textRectGC.strokeRect(x, y, rectWidth, rectHeight);
//	            textRectGC.strokeRect(x-rectWidth, y, rectWidth, rectHeight);
//	            textRectGC.strokeRect(x, y-rectHeight, rectWidth, rectHeight);
//	            textRectGC.strokeRect(x-rectWidth, y-rectHeight, rectWidth, rectHeight);
//		            
//	            textRectGC.strokeRect(x, y, rectHeight, rectWidth);
//	            textRectGC.strokeRect(x-rectHeight, y, rectHeight, rectWidth);
//	            textRectGC.strokeRect(x, y-rectHeight, rectHeight, rectWidth);
//	            textRectGC.strokeRect(x-rectHeight, y-rectWidth, rectHeight, rectWidth);

//				System.out.println(String.format("radius0 %.2f x= %.0f y= %.0f",
//						radius,
//						x,
//						y
//				));

				step++;
				
	        } else {
	        	// Found collision free location OR more than 1000 steps needed
		        System.out.println("Drawing Animation stopped.");
	        	spiralCollisionCheckAnimationTimeline.stop();
    			
//    			// draw resulting TestRectangle
//    			testRectCanvas.getGraphicsContext2D().setStroke(Color.GREEN);
//    			testRectCanvas.getGraphicsContext2D().setFill(Color.GREEN);
//    			testRectCanvas.getGraphicsContext2D().setLineWidth(4);
//    			testRectCanvas.getGraphicsContext2D().fillOval(currentTestRect.getX() + currentTestRect.getWidth() / 2 - 10/2, currentTestRect.getY() + currentTestRect.getHeight() / 2 - 10/2, 10, 10);
//    			testRectCanvas.getGraphicsContext2D().strokeRect(currentTestRect.getX(), currentTestRect.getY(), currentTestRect.getWidth(), currentTestRect.getHeight());
    			
    			rectangles[rectCounter] = currentTestRect;
    			rectCounter++;
    			System.out.println(String.format("rect @ x= %.0f y= %.0f width= %.0f height= %.0f added.",
    					currentTestRect.getX(),
    					currentTestRect.getY(),
    					currentTestRect.getWidth(),
    					currentTestRect.getHeight()
				));
    			
    			// Clone Rectangle and add to RectCloud
    			Rectangle newRect = new Rectangle(currentTestRect.getX(), currentTestRect.getY(), currentTestRect.getWidth(), currentTestRect.getHeight()); 
    			newRect.setStroke(Color.BLACK);
    			newRect.setFill(Color.TRANSPARENT);
				rectCloud.getChildren().add(newRect);

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
		
		public TimelineEvent init(double x0, double y0, double rectWidth, double rectHeight, double spinRate, Pane rectCloud2, Pane testRectCloud2, Canvas spiralCanvas, Canvas testRectCanvas) {
			this.x0 = x0;
			this.y0 = y0;
			this.rectWidth = rectWidth;
			this.rectHeight = rectHeight;
			this.spinRate = spinRate;
			this.rectCloud = rectCloud2;
			this.spiralCanvas = spiralCanvas;
			this.testRectCanvas = testRectCanvas;
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
				
        Label label = new Label("Press N to create new rects.");
        
    	// Creating a Group object
		StackPane stackpane = new StackPane();
		Pane rectCloud = new Pane();
		stackpane.getChildren().add(rectCloud);
				
		Pane testRectCloud = new Pane();
		stackpane.getChildren().add(testRectCloud);

        BorderPane root = new BorderPane(stackpane);
        root.setBottom(label);
       

		// for debugging
		Canvas canvas = new Canvas(scene_width, scene_height);
		GraphicsContext testRectGC = canvas.getGraphicsContext2D();
		testRectGC.setStroke(Color.GREY);
		testRectGC.setFill(Color.GREY);
		testRectGC.setLineWidth(1);
//		textRectGC.setLineCap(StrokeLineCap.BUTT);
//		textRectGC.setLineJoin(StrokeLineJoin.BEVEL);
//		textRectGC.setLineDashes(new double[] { 7, 7 });
		stackpane.getChildren().add(canvas);
		
		// marker in the corner 
		testRectGC.fillOval(scene_width-10,scene_height-10,10,10);

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
		    if ((spiralCollisionCheckAnimationTimeline == null || spiralCollisionCheckAnimationTimeline.getStatus()==Status.STOPPED) && (e.getCode() == KeyCode.N)) {		    	
		    	spiralGC.clearRect(0, 0, scene_width, scene_height);
//		    	testRectGC.clearRect(0, 0, scene_width, scene_height);
//				testRectGC.fillOval(scene_width-10,scene_height-10,10,10);
		    	createAndSetNewRect(testRectGC, spiralGC, rectCloud, testRectCloud, spiralCanvas, canvas);
	    	}
		});

		// Adding Title to the stage
        stage.setTitle("Spiral Rectangle Cloud");

		// Adding scene to the stage
		stage.setScene(scene);		

		// Displaying the contents of the stage
		stage.show();
		

	
	}

	
	
	
	private void createAndSetNewRect(GraphicsContext textRectGC, GraphicsContext spiralGC, Pane rectCloud, Pane testRectCloud, Canvas spiralCanvas, Canvas testRectCanvas) {
		
		System.out.println("");
		
		if (rectCounter < numOfRectangles) {
		
			// create random width and height in the defined limits 
			double rectWidth = Math.floor(randomizer.nextDouble() * maxRectWidth);
			double rectHeight = Math.floor(randomizer.nextDouble() * maxRectHeight);
	
			checkRectCollisionSpiralOfTheodorus(
					scene_width / 2, scene_height / 2, /*center location of test spiral*/ 
					20.0, // 2.0, /* spinRate */
					rectWidth, /* rectWidth */
					rectHeight, /* rectHeight */
					rectCloud, 
					testRectCloud,
					spiralCanvas, 
					testRectCanvas
			);
			
		} else {
			System.out.println("max num of rects, canceled.");
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
	private boolean checkCollisonAABB(double x, double y, double rectWidth, double rectHeight, int step, Rectangle currentTestRect, GraphicsContext testRectGC) {

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
		for (Rectangle r : rectangles) {
			
			if (r == null) break;
			
			System.out.print(String.format("  against rect @ x= %.0f y= %.0f width= %.0f height= %.0f   ",
					r.getX(),
					r.getY(),
					r.getWidth(),
					r.getHeight()
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
//			x = x0 + radius * Math.cos((2 * Math.PI * step / numSegments) * spainRate);
//			y = y0 + radius * Math.sin((2 * Math.PI * step / numSegments) * spaiRate);
			x = x0 + radius * Math.cos((2 * Math.PI * step / (radius * 10)) * spinRate);
			y = y0 + radius * Math.sin((2 * Math.PI * step / (radius * 10)) * spinRate);
			spiralGC.strokeLine(x, y, x1, y1);

			spiralGC.fillOval(x-5/2, y-5/2, 5, 5);

//			  textRectGC.setStroke(Color.GREY);
//            textRectGC.strokeRect(x - rectHeight /2, y - rectWidth / 2, rectHeight, rectWidth);
//            textRectGC.strokeRect(x - rectWidth /2, y - rectHeight / 2, rectWidth, rectHeight);

//            textRectGC.strokeRect(x, y, rectWidth, rectHeight);
//            textRectGC.strokeRect(x-rectWidth, y, rectWidth, rectHeight);
//            textRectGC.strokeRect(x, y-rectHeight, rectWidth, rectHeight);
//            textRectGC.strokeRect(x-rectWidth, y-rectHeight, rectWidth, rectHeight);
//	            
//            textRectGC.strokeRect(x, y, rectHeight, rectWidth);
//            textRectGC.strokeRect(x-rectHeight, y, rectHeight, rectWidth);
//            textRectGC.strokeRect(x, y-rectHeight, rectHeight, rectWidth);
//            textRectGC.strokeRect(x-rectHeight, y-rectWidth, rectHeight, rectWidth);

//			System.out.println(String.format("radius0 %.2f x= %.0f y= %.0f",
//					radius,
//					x,
//					y
//			));
			
			step++;

		}


		if (newRect != null) {
			textRectGC.setStroke(Color.AQUAMARINE);
			textRectGC.setFill(Color.AQUAMARINE);
			textRectGC.fillOval(newRect.getX() + newRect.getWidth() / 2 - 10/2, newRect.getY() + newRect.getHeight() / 2  - 10/2, 10, 10);
			textRectGC.strokeRect(newRect.getX(), newRect.getY(), newRect.getWidth(), newRect.getHeight());
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
	 * @param testRectCanvas 
	 * @param testRectCloud 
	 */
	public void checkRectCollisionSpiralOfTheodorus (double x0, double y0, double spinRate, 
			double rectWidth, double rectHeight, Pane rectCloud, Pane testRectCloud, Canvas spiralCanvas, Canvas testRectCanvas) {
	
		// Center of spiral
		spiralCanvas.getGraphicsContext2D().fillOval(x0 -5/2, y0 -5/2, 5, 5);

		
		testRectCanvas.setOpacity(1);
		spiralCanvas.setOpacity(1);
		// remove all previous testRects
		testRectCloud.getChildren().clear();

		spiralCollisionCheckAnimationTimeline = new Timeline(new KeyFrame(Duration.millis(200), 
				new TimelineEvent().init(x0, y0, rectWidth, rectHeight, spinRate, rectCloud, testRectCloud, spiralCanvas, testRectCanvas)));
		
		spiralCollisionCheckAnimationTimeline.setCycleCount(Timeline.INDEFINITE);
		spiralCollisionCheckAnimationTimeline.play();
	}


}