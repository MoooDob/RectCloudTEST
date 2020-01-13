package application;

import java.util.ArrayList;
import java.util.Collections;

public class Utils {

	/**
	 * ArrayList is sorted afterwards 
	 * @return 
	 * 
	 */
	public static double getMedianOf(ArrayList<Double> aList) {

		Collections.sort(aList);
		
		if (aList.size() == 0) return 0;
		
		if (aList.size() % 2 == 0) {
			// even: get arithmetic mean of the both items in the middle
			int middle = aList.size() / 2;
		    return ((double)aList.get(middle) + (double)aList.get(middle - 1)) / 2;
		} else {
			// odd: get the item in the middle
		    return (double) aList.get(aList.size() / 2);
		}

	}

}
