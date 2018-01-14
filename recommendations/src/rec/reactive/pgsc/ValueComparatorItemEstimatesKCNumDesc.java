package rec.reactive.pgsc;

import java.util.Comparator;
import java.util.Map;

public class ValueComparatorItemEstimatesKCNumDesc implements Comparator<String> {

	Map<String, Double> est;
	Map<String, Integer> kcs;

	public ValueComparatorItemEstimatesKCNumDesc(Map<String, Double> est, Map<String, Integer> map) {
		this.est = est;
		this.kcs = map;
	}

	/* Note: this comparator sorts the estimates descendingly
	 * For ranking examples, we don't care about progress, 
	 * because even viewing 100% progress examples migth be helpful.
	 * if estimates are equal, lower KC goes first
	 *	 */
	public int compare(String a, String b) {
		if (est.get(a) > est.get(b))  
			return -1;
		else if (est.get(a) < est.get(b))
			return 1;
		else  { //if estimates are equal
			if (kcs.get(a) <= kcs.get(b)) // comparing no. kcs, lower kc goes first
				return -1;
			else
				return 1;
		} 
	}
}
