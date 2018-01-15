package rec.reactive.pgsc;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import rec.reactive.RecDB;


public class PGSC {

	private static Data data;
	private static RecDB RecDB;
	public static DecimalFormat df4 = new DecimalFormat("#.####");

	public static ArrayList<ArrayList<String>> generateReactiveRecommendations(
			String seq_id, String user_id, String group_id, String course_id,
			String session_id, String last_content_id, String last_content_res, int n, 
			String[] contentList, HashMap<String, Double> itemKCEstimates,
			double reactive_threshold, Map<String, List<String>> topicContents,
			String rec_dbstring, String rec_dbuser, String rec_dbpass, 
			String um2_dbstring, String um2_dbuser, String um2_dbpass){
		
		if (data == null)
			data = Data.getInstance(contentList, course_id, rec_dbstring, rec_dbuser, rec_dbpass,
					                um2_dbstring, um2_dbuser, um2_dbpass
					                ); //the singleton instance

		HashMap<String, Double> rankMap = calculateSim(last_content_id, itemKCEstimates,topicContents);
		SortedMap<String,Double> sortedExampleMap = 
				new TreeMap<String,Double>(
				new ValueComparatorItemEstimatesKCNumDesc(rankMap,data.getItemKCSize()));
		sortedExampleMap.putAll(rankMap);
		
		ArrayList<ArrayList<String>> recommendation_list = new ArrayList<ArrayList<String>>();
		recommendation_list.addAll(createRecList(seq_id, user_id, group_id,session_id,
				last_content_id, sortedExampleMap, "pgsc",
				rec_dbstring, rec_dbuser, rec_dbpass, n, topicContents));

		//TODO: clear data structures that are not needed anymore
		rankMap.clear(); rankMap = null;
		sortedExampleMap.clear(); sortedExampleMap = null;
		
		return recommendation_list;
	}
	/*
	 * returns a map, with keys as example and values as the calculated similarity value
	 */
	public static HashMap<String, Double> calculateSim(String q, Map<String, Double> kmap,
			Map<String, List<String>> topicContents) {
		HashMap<String,Double> rankMap = new HashMap<String,Double>();
		Map<String,Double> qConceptWeight = null;
		Map<String,Double> eConceptWeight = null;
		List<List<String>> qtree = null;
		List<List<String>> etree = null;
		List<String> eList = data.getExamples();
		double sim = 0.0;
		 
		
		//TFIDF values used as weight of concepts in question
		qConceptWeight = data.getTFIDF(q);
		//subtrees in question
		qtree = data.getContentSubtree().get(q);
		
		HashSet<String> qTopicSet = getTopicSet(q, topicContents); //topic(s) of the question
		for (String e : eList) {
			sim = 0.0;
			//TFIDF values used as weight of concepts in example
			eConceptWeight = data.getTFIDF(e);
			//subtrees in example
			etree = data.getContentSubtree().get(e);
			//calculate similarity 				
			sim = localSimCos(qtree,etree,qConceptWeight,eConceptWeight,kmap,qTopicSet,q); 
			rankMap.put(e, sim);
		}	
		
		return rankMap;		
	}	
	
	private static ArrayList<ArrayList<String>> createRecList(String seq_rec_id, String user_id,
			String group_id, String session_id, String last_content_id,			
			SortedMap<String, Double> sortedRankMap, String method,
			String rec_dbstring, String rec_dbuser, String rec_dbpass,
			int n, Map<String, List<String>> topicContents) {
		double sim;
		String ex;
		int count = n;
		ArrayList<ArrayList<String>> recommendation_list = new ArrayList<ArrayList<String>>();
		HashSet<String> eTopicSet;
		HashSet<String> qTopicSet = getTopicSet(last_content_id, topicContents);
		if (sortedRankMap !=null)
		{
			for (Entry<String, Double> entry : sortedRankMap.entrySet())
			{
				if (count == 0)
					break;
				ex = entry.getKey();
				sim = entry.getValue();
				if (sim == 0)
					break;
				eTopicSet = getTopicSet(ex, topicContents);
				if (isExAheadQueTopics(eTopicSet, qTopicSet)) continue;
				if (RecDB == null)
					RecDB = new RecDB(rec_dbstring, rec_dbuser, rec_dbpass);
				RecDB.openConnection();
				int id = RecDB.addRecommendation(seq_rec_id, user_id, group_id, session_id,
						last_content_id, ex, method,sim, 1);
				RecDB.closeConnection();

				ArrayList<String> rec = new ArrayList<String>();
				rec.add("" + id); // item_rec_id from the ent_recommendation table
				rec.add(ex); // example rdfid 
				rec.add(df4.format(sim)); // similarity value
				rec.add(method); //the approach which was used for recommendation
				recommendation_list.add(rec);	
				count--;		
			}
		}		
		return recommendation_list;
	}

	private static HashSet<String> getTopicSet(String content, Map<String, List<String>> topicContents) {
		HashSet<String> lst = new HashSet<String>();
		for (String t : topicContents.keySet())
			if (topicContents.get(t).contains(content))
				lst.add(t);
		return lst;
	}
	
	/*
	 * Return value ranges from -1 to 1. 
	 */
	private static double localSimCos(List<List<String>> qtree, List<List<String>> etree, 
			                       Map<String,Double> qConceptWeight,
			                       Map<String,Double> eConceptWeight,
			                       Map<String, Double> kmap,
			                       HashSet<String> qTopicSet, String que)
	{
		double [][] s = new double[qtree.size()][etree.size()]; 
		int [][] alpha = new int[qtree.size()][etree.size()];	
		//initialize all elements of alpha to be 1
		for (int i = 0; i < qtree.size(); i++)
			for(int j = 0; j < etree.size(); j++)
				alpha[i][j] = 1;
				
		//fill s
		for (int i = 0; i < qtree.size(); i++)
			for(int j = 0; j < etree.size(); j++)
			{
				s[i][j] = simCosine(qtree.get(i),etree.get(j),qConceptWeight,eConceptWeight,
						           kmap,qTopicSet,que);
				
			}
		//print(s);//print s[i][j]
		//fill alpha
		for (int i = 0; i < qtree.size(); i++)
			for(int j = 0; j < etree.size(); j++)
			{
				if (alpha[i][j] != 0)
				{
					//set alpha 1 for this element
					alpha[i][j] = 1;
					//if s[i][j] is one, set alpha of other elements in the same row and column to 0. 
					//Math.round is to avoid rounding errors that could occurs in sqrt calculation required for s[i][j]
					if ( (Math.round( s[i][j] * 100.0) / 100.0) == 1)
					{
						//set alpha 0 for other elements in the same row
						for (int e = 0; e < etree.size(); e++)
						{
							if (e!=j)
								alpha[i][e] = 0;
						}
						//set alpha 0 for other elements in the same column
						for (int q = 0; q < qtree.size(); q++)
						{
							if (q!=i)
								alpha[q][j] = 0;
						}
					}								
				}				
			}
		//print(alpha);//print alpha[i][j]
		double sim = 0.0;
		for (int i = 0; i < qtree.size(); i++)
			for(int j = 0; j < etree.size(); j++)
			{
				sim += (alpha[i][j]*s[i][j]);
			}
		
		/*
		 * divide the sim by sum of weights to calculate weighted average
		 * sumOfWeights = qtree.size()*etree.size() when no alpha is 0
		 */
		double sumOfWeights = 0.0;
		for (int i = 0; i < qtree.size(); i++)
			for(int j = 0; j < etree.size(); j++)			
				sumOfWeights += alpha[i][j];
		
		sim = sim / sumOfWeights;		
		//release space
		s = null;
		alpha = null;
		return sim;
	}

	/* 
	 * Return value (cosine similarity) ranges between 0-1 since tfidf values are not negative.
	 */
	private static double simCosine(List<String> qConcepts, List<String> eConcepts, 
			Map<String, Double> qConceptWeight, Map<String, Double> eConceptWeight, 
			Map<String, Double> kmap, HashSet<String> qTopicSet,String que) {
		//create concept space by union of two sets. Set drops repeated elements and contains unique values
		Set<String> qConceptSet = new HashSet<String>(qConcepts);
		Set<String> eConceptSet = new HashSet<String>(eConcepts);
		List<String> conceptSpace = new ArrayList<String>(union(qConceptSet, eConceptSet));
		HashMap<String,Double> evector = new HashMap<String,Double>();// concept vector for example
		HashMap<String,Double> qvector = new HashMap<String,Double>(); // concept vector for question
		HashSet<String> conceptTopicSet;
		boolean isTargetConcept;
		double lackKnowledge;
		for (String c : conceptSpace)
		{
			if (qConcepts == null)
				System.out.println("qconcept is null: "+que);
							
			if (kmap.get(c) == null)
				lackKnowledge = 0;
			else 
				lackKnowledge = 1-kmap.get(c);	
			//if the concept is in the target concepts of the topic weight of concept is non-zero, otherwise it is 0.
			conceptTopicSet = data.getTopicOutcomeSet(c);
			isTargetConcept = false;
			if (intersection(conceptTopicSet,qTopicSet) != null)
				if (intersection(conceptTopicSet,qTopicSet).size()>0)
					isTargetConcept = true;
			evector.put(c, isTargetConcept && eConceptSet.contains(c)?lackKnowledge:0);
			qvector.put(c, isTargetConcept && qConceptSet.contains(c)?lackKnowledge:0);

		}
		
		double numerator = 0.0;
		double eDemoninator = 0.0;
		double qDenominator = 0.0;
		for (String c :  conceptSpace)
		{
			numerator += qvector.get(c) * evector.get(c);
			eDemoninator += (evector.get(c) *  evector.get(c)); //each element in the example vector is raised to the power of 2 
			qDenominator += (qvector.get(c) * qvector.get(c)); //each element in the example vector is raised to the power of 2 
		}

		//this is check for not getting NaN as sim
		//possible NaN cases: when for one vector all weights are 0 for the static method
		//for the goal based method, all concepts are not within target in both vector
		//for personalized method, user already knows all the concepts in either question vector
		//or the example vector, so this example-question pair has no gain for user.
		double sim;
		if (Math.sqrt(qDenominator)*Math.sqrt(eDemoninator) == 0.0)
			sim = 0.0;
		else 
			sim = numerator/(Math.sqrt(qDenominator)*Math.sqrt(eDemoninator)); //square root of the qDenominator/eDenominator
		return sim;	
	}

	private static boolean isExAheadQueTopics(HashSet<String> eTopicSet, HashSet<String> qTopicSet) {
		for (String eTopic : eTopicSet) { //for every topic of the example
			for (String qTopic : qTopicSet) { // for every topic of the question
				//check order example topic and question topic
				if (data.getTopicOrder(eTopic) > data.getTopicOrder(qTopic))
					return true;
			}
		}
		return false;
	}
	private static <T> Set<T> union(Set<T> setA, Set<T> setB) {
		Set<T> tmp = new TreeSet<T>(setA);
		tmp.addAll(setB);
		return tmp;
	}

	private static <T> Set<T> intersection(Set<T> setA, Set<T> setB) {
		Set<T> tmp = new TreeSet<T>();
		for (T x : setA){
			if (setB == null)
				return null;
			if (setB.contains(x))
				tmp.add(x);
		}
		return tmp;
	}
	
//	private static void print(int[][] alpha) {
//		for (int i = 0; i < alpha.length; i++)
//		{
//			for(int j = 0; j < alpha[0].length; j++)
//				System.out.print(String.format("%s ", alpha[i][j]));
//			System.out.println();
//		}			
//	}
//
//	private static void print(double[][] s) {
//		for (int i = 0; i < s.length; i++)
//		{
//			for(int j = 0; j < s[0].length; j++)
//				System.out.print(String.format("%.2f ", s[i][j]));
//			System.out.println();
//		}		
//	}

}
