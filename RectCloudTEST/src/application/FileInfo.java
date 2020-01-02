package application;

public class FileInfo {
	
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
	float hue = -1;
	double width = 0;
	double height = 0;
	int level = -1;
}
