package rec.proactive.random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Random {

	public static ArrayList<ArrayList<String>> calculateSequenceRank(
			Map<String, List<String>> topicContents,
			int proactive_max) {
					
		ArrayList<ArrayList<String>> sequenceList = new ArrayList<ArrayList<String>>();
		for (String topic : topicContents.keySet()) {
			getTopicRecommendation(proactive_max, sequenceList,
					topicContents.get(topic));
		}
		return sequenceList;
	}
		
	
	private static void getTopicRecommendation(int proactive_max, ArrayList<ArrayList<String>> sequenceList,
			List<String> topicContents) {
	
		//shuffle the contents in the topic
		Collections.shuffle(topicContents);

			for (int i = 0; i < proactive_max; i++) {
				if (i == topicContents.size()) 
					break;
				else {
					ArrayList<String> list = new ArrayList<String>();
					list.add(topicContents.get(i));
					list.add(getScore(i));
					list.add("random");
					sequenceList.add(list);
				}
			}
	}

	/*
	 * This method assigns score to recommended items in a topic based on the order
	 * that we want add them to the rec list (this is different that using estimates)
	 * returns 1 for the 1st item, 0.7 for the 2nd items, 0.3 for the 3rd items
	 */
	private static String getScore(int i) {
		if (i == 0)
			return "1";
		else if (i == 1)
			return "0.7";
		else 
			return "0.3";
	}	
}
