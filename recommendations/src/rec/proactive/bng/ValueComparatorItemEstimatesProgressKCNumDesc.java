package rec.proactive.bng;

import java.util.Comparator;
import java.util.Map;

public class ValueComparatorItemEstimatesProgressKCNumDesc implements Comparator<String> {

	Map<String, double[]> estPro; // estPro[0]: item estimate, estPro[1]: item progress
								  // estPro[2]: number of item concepts

	public ValueComparatorItemEstimatesProgressKCNumDesc(Map<String, double[]> estPro) {
		this.estPro = estPro;
	}

	// Note: this comparator sorts the estimates descendingly, in case two estimates are
	// equal, it first compares the progress [lower progress comes first], if progress are equal, it compares no. kcs
	public int compare(String a, String b) {
		if (estPro.get(a)[0] > estPro.get(b)[0])  
			return -1;
		else if (estPro.get(a)[0] < estPro.get(b)[0])
			return 1;
		else  { //if estimates are equal
			if (estPro.get(a)[1] == estPro.get(b)[1] ) { //if progress is equal
				if (estPro.get(a)[2] <= estPro.get(b)[2]) // comparing no. kcs, lower kc goes first
					return -1;
				else
					return 1;
			} else if (estPro.get(a)[1] < estPro.get(b)[1]) {//if progress is lower
				return -1;
			} else {
				return 1;				
			}
		} 
	}
}
