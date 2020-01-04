package application;

public class SpiralInfo {
	
	SpiralVariant spiralvariants[];
	
	public SpiralInfo(int numOfSpiralVariants) {
		super();
		if (numOfSpiralVariants > 0) {
			spiralvariants = new SpiralVariant[numOfSpiralVariants];
			for (int i = 0; i < numOfSpiralVariants; i++) 
				spiralvariants[i] = new SpiralVariant();
		}
	}

}
