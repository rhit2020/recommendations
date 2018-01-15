package rec.proactive.random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import rec.GetUserActivity;
import rec.proactive.bng.BNGStaticData;

public class Random {

	public static ArrayList<ArrayList<String>> calculateSequenceRank(
			String user_id, String group_id, String domain,
			String rec_dbstring, String rec_dbuser, String rec_dbpass, 
			String um2_dbstring, String um2_dbuser, String um2_dbpass,
			Map<String, List<String>> topicContents,
			int proactive_max, Map<String, Double> usrContentProgress,
			String[] contentList) {
			
		BNGStaticData static_data = BNGStaticData.getInstance(rec_dbstring, rec_dbuser, rec_dbpass, 
				um2_dbstring,um2_dbuser, um2_dbpass, contentList);

		HashSet<String> codingList = static_data.getCodingList();
		HashSet<String> challengeList = static_data.getChallengeList();
		HashSet<String> exampleList = static_data.getExampleList();
		Map<String, HashSet<String>> setChallenges = static_data.getSetChallenges();


		// DYNAMIC DATA
		if (usrContentProgress == null || usrContentProgress.size() == 0) {
			// Get progress data on coding/challenges/examples (all in one map)
			HashMap<String, String[]> userActivityMap = GetUserActivity.getUserActivityReport(user_id, group_id, domain,
					codingList.toArray(new String[codingList.size()]), "pcrs", GetUserActivity.pcrsActivityServiceURL);
			userActivityMap.putAll(GetUserActivity.getUserActivityReport(user_id, group_id, domain, 
					challengeList.toArray(new String[challengeList.size()]), "pcex_ch",
					GetUserActivity.pcexChallengeActivityURL));
			userActivityMap.putAll(GetUserActivity.getUserActivityReport(user_id, group_id, domain, 
					exampleList.toArray(new String[exampleList.size()]), "pcex",
					GetUserActivity.pcexExampleActivityURL));
			
			usrContentProgress = new HashMap<String,Double>();
			for (Entry<String, String[]> entry : userActivityMap.entrySet())
			{
				//progress is the 3rd value in String[]
				usrContentProgress.put(entry.getKey(), Double.parseDouble(entry.getValue()[2]));
			}
		}
		
		ArrayList<ArrayList<String>> sequenceList = new ArrayList<ArrayList<String>>();
		for (String topic : topicContents.keySet()) {
			getTopicRecommendation(proactive_max, sequenceList,
					topicContents.get(topic), usrContentProgress,
					exampleList, setChallenges);
		}
		return sequenceList;
	}
		
	
	private static void getTopicRecommendation(int proactive_max, ArrayList<ArrayList<String>> sequenceList,
			List<String> topicContents, Map<String, Double> usrContentProgress,
			HashSet<String> exampleList, Map<String, HashSet<String>> setChallenges) {
	
		//shuffle the contents in the topic
		Collections.shuffle(topicContents);
		double progress;
		int count = 0;
		for (int i = 0; i < topicContents.size(); i++) {
			if (count == proactive_max)
				break;
			else {
				progress = usrContentProgress.get(topicContents.get(i)) == null ? 0 : 
					       usrContentProgress.get(topicContents.get(i));
				if (progress < 1) {
					if (exampleList.contains(topicContents.get(i))) {
						if (allExChallengedSolved(usrContentProgress,setChallenges,
								topicContents.get(i)) == false) {
							ArrayList<String> list = new ArrayList<String>();
							list.add(topicContents.get(i));
							list.add(getScore(count));
							list.add("random");
							sequenceList.add(list);
							count++;
						}
					} else {
						ArrayList<String> list = new ArrayList<String>();
						list.add(topicContents.get(i));
						list.add(getScore(count));
						list.add("random");
						sequenceList.add(list);
						count++;
					}
				}
			}
		}
	}
	
	private static boolean allExChallengedSolved( Map<String, Double> usrContentProgress,
			Map<String, HashSet<String>> setChallenges, String example) {
		int countChNotSolved = 0;
		double progress;
		HashSet<String> chList = setChallenges.get(example);
		for (String ch : chList) {
			progress = usrContentProgress.get(ch);
			if (progress < 1)
				countChNotSolved++;
		}
		return (countChNotSolved == 0)  ;
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
