package Application;

import java.util.Random;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;

public class SpiralTEST extends Application {

	double scene_width = 1000;
	double scene_height = 800;
	int numOfRectangles = 10;
	double maxRectHeight = 200;
	double maxRectWidth = 200;

	// **************************

	private Random randomizer = new Random(34);

	private Rectangle[] rectangles = new Rectangle[numOfRectangles];

	int rectCounter = 0;
	
	
	
	/**
	*
	*/
	@Override
	public void start(Stage stage) {			

		// Creating a Group object
		Group root = new Group();
		
//		for (Rectangle r : rectangles) {
//			root.getChildren().add(r);
//		}

		// for debugging
		Canvas canvas = new Canvas(scene_width, scene_height);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		
		// for visual Debugging
		root.getChildren().add(canvas);

		// Creating a scene object
		Scene scene = new Scene(root, scene_width, scene_height);
		scene.setOnKeyPressed(e -> {
		    if (e.getCode() == KeyCode.N) {
		    	Rectangle r = createAndSetNewRect(gc);
				if (r != null){
					root.getChildren().add(r);
				}
		    }
		});

		// Adding scene to the stage
		stage.setScene(scene);

		// Displaying the contents of the stage
		stage.show();
	}

	
	
	
	private Rectangle createAndSetNewRect(GraphicsContext gc) {
		double rectWidth = randomizer.nextDouble() * maxRectWidth;
		double rectHeight = randomizer.nextDouble() * maxRectHeight;



		Rectangle newRect = checkRectCollision(
				scene_width / 2, scene_height / 2, /*new location*/ 
				100, /* max Radius */
				40, /* Segments */
				2.0, /* Spinrate */
				rectWidth, /* rectWidth */
				rectHeight, /* rectHeight */
				gc
		);
		
		if (newRect != null) {
			newRect.setStroke(Color.AQUAMARINE);
			newRect.setFill(Color.TRANSPARENT);
			rectangles[rectCounter] = newRect;
			rectCounter++;
		}
		
		return newRect;
	}

	
	
	
	
	private Rectangle massCheckCollisonAABB(double x, double y, double rectWidth, double rectHeight) {

		Rectangle[] testRects = { 
				new Rectangle(x - rectHeight / 2, y - rectWidth / 2, rectWidth, rectHeight), // Upright rect
				new Rectangle(x - rectWidth / 2, y - rectHeight / 2, rectHeight, rectWidth)  // 90 degree rotated rect
				};

		Rectangle theRect = testRects[0];
		for (Rectangle r : rectangles) {
			
			if (r == null) return theRect;
			
			for (Rectangle testRect : testRects) {
				theRect = 
						r.intersects(testRect.getBoundsInLocal()) ? null : testRect;
				if (theRect != null) {
					return theRect;
				}				
			}
		}

		return theRect;
	}

	
	
	
	// according to https://stackoverflow.com/q/48492980
	public Rectangle checkRectCollision(double x0, double y0, double maxRadius, int numSegments, double spinRate,
			double rectWidth, double rectHeight, GraphicsContext gc) {
		double radius = 0;
		double x = x0;
		double y = y0;
		double deltaR = maxRadius / numSegments;

		int step = 0;
		Rectangle newRect; 
		while ((newRect = massCheckCollisonAABB(x, y, rectWidth, rectHeight)) == null) {
			double x1 = x;
			double y1 = y;
			radius += deltaR;
			x = x0 + radius * Math.cos((2 * Math.PI * step / numSegments) * spinRate);
			y = y0 + radius * Math.sin((2 * Math.PI * step / numSegments) * spinRate);
			gc.setStroke(Color.LIME);
			gc.strokeLine(x, y, x1, y1);

			gc.setStroke(Color.BLUE);
            gc.strokeRect(x - rectHeight /2, y - rectWidth / 2, rectHeight, rectWidth);
            gc.strokeRect(x - rectWidth /2, y - rectHeight / 2, rectWidth, rectHeight);

//		            gc.strokeRect(x, y, rectWidth, rectHeight);
//		            gc.strokeRect(x-rectWidth, y, rectWidth, rectHeight);
//		            gc.strokeRect(x, y-rectHeight, rectWidth, rectHeight);
//		            gc.strokeRect(x-rectWidth, y-rectHeight, rectWidth, rectHeight);
//		            
//		            gc.strokeRect(x, y, rectHeight, rectWidth);
//		            gc.strokeRect(x-rectHeight, y, rectHeight, rectWidth);
//		            gc.strokeRect(x, y-rectHeight, rectHeight, rectWidth);
//		            gc.strokeRect(x-rectHeight, y-rectWidth, rectHeight, rectWidth);

			System.out.println("x=" + x + " y=" + y);

		}

		gc.setStroke(Color.AQUAMARINE);
		if (newRect != null) {
			gc.strokeRect(newRect.getX(), newRect.getY(), newRect.getWidth(), newRect.getHeight());
		}
		
		return newRect;
	}

}