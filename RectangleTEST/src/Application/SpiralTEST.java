package Application;

import java.util.Random;

import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;

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
		
        Label label = new Label("Press N to create new rects.");
        
    	// Creating a Group object
		Group rectGroup = new Group();

        BorderPane root = new BorderPane(rectGroup);
        root.setBottom(label);
       

		// for debugging
		Canvas canvas = new Canvas(scene_width, scene_height);
		GraphicsContext textRectGC = canvas.getGraphicsContext2D();
		textRectGC.setStroke(Color.GREY);
		textRectGC.setLineWidth(1);
//		textRectGC.setLineCap(StrokeLineCap.BUTT);
//		textRectGC.setLineJoin(StrokeLineJoin.BEVEL);
//		textRectGC.setLineDashes(new double[] { 7, 7 });
		rectGroup.getChildren().add(canvas); 

		// for debugging too
		Canvas spiralCanvas = new Canvas(scene_width, scene_height);
		GraphicsContext spiralGC = spiralCanvas.getGraphicsContext2D();
		spiralGC.setStroke(Color.RED);
		spiralGC.setFill(Color.RED);
		rectGroup.getChildren().add(spiralCanvas);

		// Creating a scene object
		Scene scene = new Scene(root, scene_width, scene_height);
		scene.setOnKeyPressed(e -> {
		    if (e.getCode() == KeyCode.N) {
		    	spiralGC.clearRect(0, 0, scene_width, scene_height);
		    	textRectGC.clearRect(0, 0, scene_width, scene_height);
		    	Rectangle r = createAndSetNewRect(textRectGC, spiralGC);
				if (r != null){
					rectGroup.getChildren().add(r);
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

	
	
	
	private Rectangle createAndSetNewRect(GraphicsContext textRectGC, GraphicsContext spiralGC) {
		
		System.out.println("");
		Rectangle newRect = null;
		
		if (rectCounter < numOfRectangles) {
		
			// create random width and height in the defined limits 
			double rectWidth = Math.floor(randomizer.nextDouble() * maxRectWidth);
			double rectHeight = Math.floor(randomizer.nextDouble() * maxRectHeight);
	
			newRect = checkRectCollision(
					scene_width / 2, scene_height / 2, /*new location*/ 
					500, /* max Radius */
					0, // 40.0, /* Segments or -> Segments==0 && deltaRadius > 0 */
					10000.0, // 2.0, /* Spinrate */
					1, /* deltaRadius or -> Segments */
					rectWidth, /* rectWidth */
					rectHeight, /* rectHeight */
					textRectGC, 
					spiralGC
			);
			
			if (newRect != null) {
				newRect.setStroke(Color.BLACK);
				newRect.setFill(Color.TRANSPARENT);
				rectangles[rectCounter] = newRect;
				rectCounter++;
				System.out.println(String.format("rect @ x= %.0f y= %.0f width= %.0f height= %.0f added.",
						newRect.getX(),
						newRect.getY(),
						newRect.getWidth(),
						newRect.getHeight()
				));
			}
		} else {
			System.out.println("max num of rects.");
		}
		
		return newRect;
	}

	
	
	
	
	private Rectangle massCheckCollisonAABB(double x, double y, double rectWidth, double rectHeight) {

		Rectangle[] testRects = { 
				new Rectangle(x - rectHeight / 2, y - rectWidth / 2, rectWidth, rectHeight), // Upright rect
				new Rectangle(x - rectWidth / 2, y - rectHeight / 2, rectHeight, rectWidth)  // 90 degree rotated rect
				};
		
		System.out.println("check new spiral coordinates x= " + x + ", y= " + y + " with width= " + rectWidth + ", height= " + rectHeight);

		System.out.println("check collision of new rect against all " + rectCounter + " already positioned rects. ");
		Rectangle theRect = testRects[0];
		for (Rectangle r : rectangles) {
			
			if (r == null) return theRect;
			
			System.out.println(String.format("check rect @ x= %.0f y= %.0f width= %.0f height= %.0f",
					r.getX(),
					r.getY(),
					r.getWidth(),
					r.getHeight()
			));
			
			for (Rectangle testRect : testRects) {
				
				System.out.println(String.format("  against rect @ x= %.0f y= %.0f width= %.0f height= %.0f",
						testRect.getX(),
						testRect.getY(),
						testRect.getWidth(),
						testRect.getHeight()
				));


				// getBoundsInLocal is only correct if and only if both objects are in the same coordinate system. 
				// if one object is for instance part of another group, the coordinate system have to be transformed
				// furthermore this function tests only the AABB (axis aligned bounding box) of both objects, 
				// so rotated will be approximated with its AABB
//				theRect = 
//						r.getBoundsInParent().intersects(testRect.getBoundsInLocal()) ? null : testRect;
//				if (theRect != null) {
//					System.out.println("no collision...");
//					return theRect;
//				} else {
//					System.out.println("collision!");
//				}
			}
		}

		return theRect;
	}

	
	
	
	// according to https://stackoverflow.com/q/48492980
	/**
	 * @param x0
	 * @param y0
	 * @param maxRadius
	 * @param numSegments
	 * @param spinRate
	 * @param deltaR
	 * @param rectWidth
	 * @param rectHeight
	 * @param textRectGC
	 * @param spiralGC
	 * @return
	 */
	public Rectangle checkRectCollision(double x0, double y0, double maxRadius, double numSegments, double spinRate, double deltaR,
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
//			radius += deltaR;
			radius += deltaR * step / (radius * 10);
//			x = x0 + radius * Math.cos((2 * Math.PI * step / numSegments) * spinRate);
//			y = y0 + radius * Math.sin((2 * Math.PI * step / numSegments) * spinRate);
			x = x0 + radius * Math.cos((2 * Math.PI * step / (radius * 10)));
			y = y0 + radius * Math.sin((2 * Math.PI * step / (radius * 10)));
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

		textRectGC.setStroke(Color.AQUAMARINE);
		if (newRect != null) {
			textRectGC.strokeRect(newRect.getX(), newRect.getY(), newRect.getWidth(), newRect.getHeight());
		}
		
		return newRect;
	}

}