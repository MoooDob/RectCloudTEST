package application;

public class SpiralPoint {
	
	static void init(int numOrientations) {
		localNumOrientations = numOrientations;
	}
	
	public SpiralPoint(int index, double x, double y) {
		super();
		this.index = index;
		this.x = x;
		this.y = y;
		
		// set each testOrientation to maximum value
		for (int o = 0; o < localNumOrientations; o++) {
			testOrientations[o] = new Dimension(Double.MAX_VALUE, Double.MAX_VALUE);
		}

	}
	
	public int index = -1;
	public double x = 0d;
	public double y = 0d;	
	public Dimension testOrientations[] = new Dimension[localNumOrientations];
	public boolean pendingRemove;
	
	private static int localNumOrientations = -1;
	
}
