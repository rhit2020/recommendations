package rec;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;


public class ValueComparator_StringDouble implements Comparator<String> {

	Map<String, Double> base;
	
	HashMap<String, ArrayList<String[]>> conceptMap;

	public ValueComparator_StringDouble(Map<String, Double> base,
			HashMap<String, ArrayList<String[]>> conceptMap) {
		this.base = base;
		this.conceptMap = conceptMap;
	}

	// Note: this comparator sorts the values descendingly, so that the best
	// activity is in the first element.
	public int compare(String a, String b) {
		if (base.get(a) > base.get(b))
			return -1;
		else if (base.get(a) == base.get(b)) {
			if (conceptMap.get(a).size() <= conceptMap.get(b).size())
				return -1;
			else
				return 1;
		} else {
			return 1;
		}
	}
}

