package rec.reactive;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.Map.Entry;

import rec.GetUserActivity;
import rec.StaticData;
import rec.ValueComparator_StringDouble;


public class ReactiveRecommendation {
	
	private static RecDB rec_db;

	public static void openDBConnections(String rec_dbstring, String rec_dbuser, String rec_dbpass) {
		rec_db = new RecDB(rec_dbstring, rec_dbuser, rec_dbpass);
		rec_db.openConnection();
	}

	public static void closeDBConnections() {
		if (rec_db != null)
			rec_db.closeConnection();
		rec_db = null;
	}

	/*
	 * @author : Roya Hosseini 
	 * This method generates the recommendations for the given user at the given time.
	 * Parameters:
	 * - seq_rec_id: is the the id for all recommendations generated by different methods for the given user at given time
	 * - user_id: the id of the user
	 * - group_id: the group id for the user's group
	 * - course_id: is the id of the course
	 * - session_id: is the id of the user's session
	 * - last_content_id: is the rdfid of the the activity (question) that the user has failed
	 * - last_content_res: is the result of the activity (question) with the rdfid equal to last_content_id 
	 * - n: is the number of recommendation generated by each method
	 * - topic_content: is the map containing the keys as topic_names and values as the rdfid of topic activities (questions,examples,readings). @see:guanjieDBInterface.getTopicContent(String)
	 * - examples_activity: is the maps containing the keys as examples and values as the user actions in example. @see um2DBInterface.getUserExamplesActivity(String)
	 * - questions_activity: is the map with the keys as activities and values as the number of success and attempts in the activity. @see: um2DBInterface.getUserQuestionsActivity(String)
	 * - Object[] params :  contains an array of parameters which may be required in future. 
	 * Returns: List of recommended example. Each element is a list with the following items:
	 * 1) item_rec_id from the ent_recommendation table
	 * 2) example name 
	 * 3) similarity value
	 * 4) approach of recommendation
	 */
	
	public static ArrayList<ArrayList<String>> generateReactiveRecommendations(
			String seq_id, String user_id, String group_id, String domain, String course_id, 
			String session_id, String last_content_id, String last_content_res, int n, 
			String[] methods, int method_selected,String[] contentList, 
			double reactive_threshold, double alpha, int example_count_personalized_approach,
			String rec_dbstring, String rec_dbuser, String rec_dbpass, String realpath){

		ArrayList<ArrayList<String>> recommendation_list = new ArrayList<ArrayList<String>>();
		//check if last_content_id and last_content_res are null, return an empty list
		if((last_content_res == null) | (last_content_id == null))
			return recommendation_list;
		
		//Getting static data
		StaticData static_data = StaticData.getInstance(domain, group_id,contentList, realpath);
		HashMap<String, ArrayList<String[]>> kcByContent = static_data.getKcByContent();
		
		HashMap<String, String[]> questions_activity = GetUserActivity.getUserQuestionsActivity(user_id, group_id, domain,contentList);
		
		openDBConnections(rec_dbstring, rec_dbuser, rec_dbpass);
		
		SortedMap<String,Double> exampleMap = null;

		for (String method : methods)
		{
			if (  method.equals("GLOBAL_COS") | method.equals("CONCSIM") | method.equals("CSSIM") | method.equals("NAIVE_LOCAL") | method.equals("EXPERT") )
			{
				exampleMap = rec_db.getSimilarExamples(method,last_content_id,contentList,n,reactive_threshold);
			}
			//personalized approaches
			else if (method.equals("PCSSIM") | method.equals("PCONCSIM"))
			{
				String approach = "CSSIM";
				if (method.equals("PCONCSIM"))
					approach = "CONCSIM";
				exampleMap = rec_db.getSimilarExamples(approach,last_content_id,contentList,example_count_personalized_approach,reactive_threshold);				
				exampleMap = getPersonalizedRecommendation(method,exampleMap, questions_activity, kcByContent,n,alpha);			
			}
			else 
				continue;	
			int shown = 0;
			if (method_selected == -1)
				shown = 0;
			else if (method.equalsIgnoreCase(methods[method_selected]))
				shown = 1;
			recommendation_list.addAll(createRecList(seq_id, user_id, group_id,session_id,
					last_content_id,exampleMap, method, shown, n));
		}
		
		closeDBConnections();
		questions_activity.clear(); questions_activity = null;

		return recommendation_list;
	}
	
	private static ArrayList<ArrayList<String>> createRecList(String seq_rec_id, String user_id,
			String group_id, String session_id, String last_content_id,			
			SortedMap<String, Double> exampleMap, String method, int shown, int n) {
		double sim;
		String ex;
		int count = n;
		ArrayList<ArrayList<String>> recommendation_list = new ArrayList<ArrayList<String>>();
		if (exampleMap !=null &&  exampleMap.isEmpty()== false)
		{
			for (Entry<String, Double> entry : exampleMap.entrySet())
			{
				if (count == 0)
					break;
				ex = entry.getKey();
				sim = entry.getValue();
				int id = rec_db.addRecommendation(seq_rec_id, user_id, group_id, session_id,
						last_content_id, ex, method,sim, shown);
				if (shown == 1 | shown == -1)
				{
					ArrayList<String> rec = new ArrayList<String>();
					rec.add("" + id); // item_rec_id from the ent_recommendation table
					rec.add(ex); // example rdfid 
					rec.add(""+(sim <= 0.001 ? 0.001 : String.format("%.4f", sim))); // similarity value, we set values below 0.001 to 0.001 to avoid scientific notation
					rec.add(method); //the approach which was used for recommendation
					recommendation_list.add(rec);	
					count--;
				}				
			}
		}		
		return recommendation_list;
	}
	
	/* @author: Roya Hosseini
	 * This method re-ranks the top examples selected by the CSS recommendation method using user model
	 * Parameters:
	 * - exampleMap: the map with the key as the top selected examples and the values as their corresponding similarity
	 * - questions_activity: @see guanjieDBInterface.generateRecommendations javaDoc
	 * - limit: @see guanjieDBInterface.generateRecommendations javaDoc
	 * Returns:
	 * - a descendingly sortedmap of examples with their similarity 
	 */
	public static SortedMap<String, Double> getPersonalizedRecommendation(String method,SortedMap<String,Double> exampleMap,
			HashMap<String,String[]> questions_activity, HashMap<String, ArrayList<String[]>> kcByContent, int limit, double alpha)
			{
		Map<String, Double> rankMap = new HashMap<String,Double>();
		ValueComparator_StringDouble vc =  new ValueComparator_StringDouble(rankMap,kcByContent);
		TreeMap<String,Double> sortedRankMap = new TreeMap<String,Double>(vc);
		ArrayList<String[]> conceptList = new ArrayList<String[]>();

		int k = 0;
		int n = 0;
		int s = 0;
		for (String e : exampleMap.keySet())
		{
			k = 0; //number of known concepts in example
			n = 0;//number of new concepts in example
			s = 0;//number of shady concepts in example	
			conceptList = kcByContent.get(e);
			for (String[] c : conceptList) {
				double nsuccess = 0;
				double totalAtt = 0;
				boolean hasAttempt = false;
				List<String> activityList = getActivitiesWithConcept(c[0],kcByContent);//c[0] has the concept name

				for (String a : activityList) {
					if (questions_activity.containsKey(a)) {
						String[] x = questions_activity.get(a);
						totalAtt += Double.parseDouble(x[1]); // x[1] = attempts
						nsuccess += (Double.parseDouble(x[3]) * totalAtt); // x[3] = success-rate
						hasAttempt = true;
					}
				}
				if (hasAttempt == false)
				{
					n++;	
				}
				else {
					if (nsuccess > (totalAtt/2))
						k++;
					else
						s++;
				}
			}
			double rank = 0.0;
			if (conceptList.size() == 0)
				rank = 0.0;
			else if (k==0 & (s+n)> 3.0)
			{
				rank = -(s+n);
			}
			else
				rank = (3 - (s+n)) * Math.pow((double) k / (double) conceptList.size(), (3 - (s+n)));
			double combinedMeasure = (1-alpha)*rank+(alpha*exampleMap.get(e));
			rankMap.put(e, combinedMeasure);
		}
		sortedRankMap.putAll(rankMap);

		return sortedRankMap;	
	}

	private static List<String> getActivitiesWithConcept(String concept, HashMap<String, ArrayList<String[]>> kcByContent ) {
		List<String> activities = new ArrayList<String>();
		for (String content: kcByContent.keySet())
		{
			if (kcByContent.get(content).contains(concept))
			{
				if (activities.contains(content) == false)
					activities.add(content);
			}
		}
		return activities;
	}
	
}
