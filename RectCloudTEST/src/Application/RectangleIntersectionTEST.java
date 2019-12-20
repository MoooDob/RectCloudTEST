package Application;
import javafx.application.Application; 
import javafx.scene.Group; 
import javafx.scene.Scene; 
import javafx.stage.Stage; 
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class RectangleIntersectionTEST extends Application { 
	
   @Override 
   public void start(Stage stage) { 
      //Drawing a Rectangle 
      Rectangle rectangle1 = new Rectangle();  
      Rectangle rectangle2 = new Rectangle();
      
      //Setting the properties of the rectangle 
      rectangle1.setX(150.0f); 
      rectangle1.setY(75.0f); 
      rectangle1.setWidth(300.0f); 
      rectangle1.setHeight(150.0f);  
      
      //Setting the properties of the rectangle 
      rectangle2.setX(180.0f); 
      rectangle2.setY(55.0f); 
      rectangle2.setWidth(310.0f); 
      rectangle2.setHeight(350.0f);  
         
      //Creating a Group object  
      Group root = new Group(rectangle1, rectangle2); 
      
      if (rectangle1.intersects(rectangle2.getLayoutBounds())) {
    	  rectangle2.setFill(Color.RED);
      }
      
         
      //Creating a scene object 
      Scene scene = new Scene(root, 600, 300);  
      
      //Setting title to the Stage 
      stage.setTitle("Drawing a Rectangle"); 
         
      //Adding scene to the stage 
      stage.setScene(scene); 
         
      //Displaying the contents of the stage 
      stage.show(); 
   }      
   
 } 