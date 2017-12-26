package rec.proactive.bng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import rec.GetUserActivity;

public class BNG {

	public static ArrayList<ArrayList<String>> calculateSequenceRank(String user_id, String group_id, String domain,
			String rec_dbstring, String rec_dbuser, String rec_dbpass, String um2_dbstring, String um2_dbuser,
			String um2_dbpas, String[] contentList, String lastAct, double lastActRes, int proactive_max,
			Map<String, List<String>> topicContents) {

		// STATIC DATA
		BNGStaticData static_data = BNGStaticData.getInstance(rec_dbstring, rec_dbuser, rec_dbpass, um2_dbstring,
				um2_dbuser, um2_dbpas, contentList);
		List<String> codingList = static_data.getCodingList();
		List<String> challengeList = static_data.getChallengeList();
		List<String> exampleList = static_data.getExampleList();
		Map<String, List<String>> exampleKCs = static_data.getExampleKcs();

		// DYNAMIC DATA
		// Get progress data on coding/challenges/examples (all in one map)
		HashMap<String, String[]> userActivityMap = GetUserActivity.getUserActivityReport(user_id, group_id, domain,
				contentList, "pcrs", GetUserActivity.pcrsActivityServiceURL);
		userActivityMap.putAll(GetUserActivity.getUserActivityReport(user_id, group_id, domain, contentList, "pcex_ch",
				GetUserActivity.pcexChallengeActivityURL));
		userActivityMap.putAll(GetUserActivity.getUserActivityReport(user_id, group_id, domain, contentList, "pcex",
				GetUserActivity.pcexExampleActivityURL));
		// Get student model estimates on items and kcs
		HashMap<String, Double> itemKCEstimates = GetBNKCSummary.getItemKCEstimates(user_id, group_id, lastAct,
				lastActRes, contentList);

		// LOGIC
		double threshold = 0.7;
		double masteryThreshold = 0.95;
		Map<String, double[]> rankMap = new HashMap<String, double[]>();
		ArrayList<ArrayList<String>> sequenceList = new ArrayList<ArrayList<String>>();

		// Get recommendations for each topic
		for (String topic : topicContents.keySet()) {
			getTopicRecommendation(proactive_max, codingList, challengeList, exampleList, exampleKCs, userActivityMap,
					itemKCEstimates, threshold, masteryThreshold, rankMap, sequenceList, topicContents.get(topic));

		}
		return sequenceList;
	}

	private static void getTopicRecommendation(int proactive_max, List<String> codingList, List<String> challengeList,
			List<String> exampleList, Map<String, List<String>> exampleKCs, HashMap<String, String[]> userActivityMap,
			HashMap<String, Double> itemKCEstimates, double threshold, double masteryThreshold,
			Map<String, double[]> rankMap, ArrayList<ArrayList<String>> sequenceList, List<String> topicContents) {
		Map<String, double[]> exRankMapEqAboveT;
		Map<String, double[]> exRankMapBelowT;
		TreeMap<String, double[]> exSortedMapAboveT;
		TreeMap<String, double[]> exSortedMapBelowT;
		double estimate, progress;
		double[] values;

		for (String item : codingList) {
			if (topicContents.contains(item)) {
				estimate = itemKCEstimates.get(item);
				progress = Double.parseDouble(userActivityMap.get(item)[2]);
				if (isEligibleToRecommend(estimate, progress, threshold, masteryThreshold)) {
					values = new double[2];
					values[0] = estimate;
					values[1] = progress;
					rankMap.put(item, values);
				}
			}
		}
		if (rankMap.size() < proactive_max) {
			for (String item : challengeList) {
				if (topicContents.contains(item)) {
					estimate = itemKCEstimates.get(item);
					progress = Double.parseDouble(userActivityMap.get(item)[2]);
					if (isEligibleToRecommend(estimate, progress, threshold, masteryThreshold)) {
						values = new double[2];
						values[0] = estimate;
						values[1] = progress;
						rankMap.put(item, values);
					}
				}
			}
		}
		if (rankMap.size() < proactive_max) {
			exRankMapEqAboveT = new HashMap<String, double[]>();
			exRankMapBelowT = new HashMap<String, double[]>();
			for (String item : exampleList) {
				if (topicContents.contains(item)) {
					estimate = getAveragePLearn(item, exampleKCs.get(item), itemKCEstimates);
					progress = Double.parseDouble(userActivityMap.get(item)[2]);
					values = new double[3];
					values[0] = estimate;
					values[1] = progress;
					if (estimate < masteryThreshold && progress < 1) {
						if (estimate >= threshold) {
							exRankMapEqAboveT.put(item, values);
						} else {
							values[2] = exampleKCs.get(item).size();
							exRankMapBelowT.put(item, values);
						}
					}
				}
			}

			if (exRankMapEqAboveT != null && exRankMapBelowT != null) {
				// Sort the examples above threshold based on probabilities
				// ascendingly
				exSortedMapAboveT = new TreeMap<String, double[]>(
						new ValueComparatorItemEstimatesProgressAsc(exRankMapEqAboveT));
				exSortedMapAboveT.putAll(exRankMapEqAboveT);

				// Sort the examples below threshold based on probabilities
				// descendingly
				exSortedMapBelowT = new TreeMap<String, double[]>(
						new ValueComparatorItemEstimatesKCNumDesc(exRankMapBelowT));
				exSortedMapBelowT.putAll(exRankMapBelowT);

				// add examples to rank map, first check examples above
				// threshold,
				// if map size did not reach to the proactive_max, then move to
				// examples below threshold
				int count = proactive_max - rankMap.size();
				for (Entry<String, double[]> e : exSortedMapAboveT.entrySet()) {
					if (count == 0)
						break;
					rankMap.put(e.getKey(), new double[] { e.getValue()[0], e.getValue()[1] });
					count--;
				}
				count = proactive_max - rankMap.size();
				if (count > 0) {
					for (Entry<String, double[]> e : exSortedMapBelowT.entrySet()) {
						if (count == 0)
							break;
						rankMap.put(e.getKey(), new double[] { e.getValue()[0], e.getValue()[1] });
						count--;
					}
				}
			}
		}

		// Sort the items based on probabilities descendingly, if equal
		// probabilities, lower progress first
		TreeMap<String, double[]> sortedRankMap = new TreeMap<String, double[]>(
				new ValueComparatorItemEstimatesProgressDesc(rankMap));
		sortedRankMap.putAll(rankMap); // sorted map

		// create list of recommended items
		int count = proactive_max;
		for (Entry<String, double[]> e : sortedRankMap.entrySet()) {
			if (count == 0)
				break;
			ArrayList<String> list = new ArrayList<String>();
			list.add(e.getKey());
			list.add("" + e.getValue()[0]);
			list.add("bng");
			sequenceList.add(list);
			count--;
		}
	}

	private static boolean isEligibleToRecommend(double estimate, double progress, double threshold,
			double masteryThreshold) {
		return (estimate >= threshold && estimate < masteryThreshold && progress < 1);
	}

	private static double getAveragePLearn(String item, List<String> kcList, HashMap<String, Double> itemKCEstimates) {
		double pLearnSum = 0;
		for (String kc : kcList) {
			pLearnSum += itemKCEstimates.get(kc);
		}
		return (kcList.size() > 0 ? pLearnSum / kcList.size() : 0);
	}

}
