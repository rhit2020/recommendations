package rec.proactive.bng;

import java.util.Comparator;
import java.util.Map;

public class ValueComparatorItemEstimatesKCNumDesc implements Comparator<String> {

	Map<String, double[]> estPro; // estPro[0]: item estimate, estPro[1]: item
									// progress
									// estPro[2]: number of item concepts

	public ValueComparatorItemEstimatesKCNumDesc(Map<String, double[]> estPro) {
		this.estPro = estPro;
	}

	// Note: this comparator sorts the estimates descendingly, in case two
	// estimates are
	// equal, they are sorted based on progress values, lower progress comes
	// first
	public int compare(String a, String b) {
		if (estPro.get(a)[0] > estPro.get(b)[0])
			return -1;
		else if (estPro.get(a)[0] == estPro.get(b)[0]) {
			if (estPro.get(a)[2] <= estPro.get(a)[2]) // comparing no. kcs
				return -1;
			else
				return 1;
		} else {
			return 1;
		}
	}
}
