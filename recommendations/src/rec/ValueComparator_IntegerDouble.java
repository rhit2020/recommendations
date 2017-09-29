package rec;
import java.util.Comparator;
import java.util.Map;


public class ValueComparator_IntegerDouble implements Comparator<Integer> {

	Map<Integer, Double> base;

	public ValueComparator_IntegerDouble(Map<Integer, Double> base) {
		this.base = base;
	}

	// Note: this comparator sorts the values descendingly, so that the best
	// activity is in the first element.
	@Override
	public int compare(Integer a, Integer b) {
		if (base.get(a) > base.get(b))
			return -1;
		else {
			return 1;
		}
	}
}

