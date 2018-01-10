package rec.reactive.randout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import rec.reactive.RecDB;

public class RandOut {
	
	private static RandOutData data;
	private static RecDB RecDB;

	public static ArrayList<ArrayList<String>> generateReactiveRecommendations(
			String seq_id, String user_id, String group_id, String course_id,
			String session_id, String last_content_id, int n, 
			String[] contentList,
			String rec_dbstring, String rec_dbuser, String rec_dbpass, 
			String um2_dbstring, String um2_dbuser, String um2_dbpass){
		
		if (data == null)
			data = RandOutData.getInstance(contentList, course_id, rec_dbstring, rec_dbuser, rec_dbpass,
					                um2_dbstring, um2_dbuser, um2_dbpass
					                ); //the singleton instance

		List<String> exRecList = generateRecommendedExamples(last_content_id, data);
		ArrayList<ArrayList<String>> recommendation_list = new ArrayList<ArrayList<String>>();
		recommendation_list.addAll(createRecList(seq_id, user_id, group_id,session_id,
				last_content_id, exRecList, n,
				"randout", 1,
				rec_dbstring, rec_dbuser, rec_dbpass));

		//clear data structures that are not needed anymore
		exRecList.clear(); exRecList = null;
		return recommendation_list;
	}
	
	private static List<String> generateRecommendedExamples(String last_content_id, 
			RandOutData data) {

		List<String> exRecList = new ArrayList<String>();

		Map<String, List<String>> outcomeExamples = data.getOutcomeExamples();

		// get a map of content concepts <String, Map<Srting,String>> 1st val is
		// concept, second is direction
		Map<String, Map<String, String>> contentConcepts = data.getContentConceptMap();
		
		for (Entry<String, String> e : contentConcepts.get(last_content_id).entrySet()) {
			if (outcomeExamples.get(e.getKey()) != null) {
				exRecList.addAll(outcomeExamples.get(e.getKey()));
			}
		}
		
		//shuffle the contents in the topic
		Collections.shuffle(exRecList);
		
		return exRecList;

	}
	

	private static ArrayList<ArrayList<String>> createRecList(String seq_rec_id, String user_id,
			String group_id, String session_id, String last_content_id,			
			List<String> exRecList, int n, String method, int shown,
			String rec_dbstring, String rec_dbuser, String rec_dbpass) {

		ArrayList<ArrayList<String>> recommendation_list = new ArrayList<ArrayList<String>>();
		int count = 0;
		if (exRecList !=null &&  exRecList.isEmpty()== false)
		{
			for (String ex : exRecList)
			{
				if (count == n) 
					break;
				if (RecDB == null)
					RecDB = new RecDB(rec_dbstring, rec_dbuser, rec_dbpass);
				RecDB.openConnection();
				int id = RecDB.addRecommendation(seq_rec_id, user_id, group_id, session_id,
						last_content_id, ex, method, 0.5, shown);
				RecDB.closeConnection();

				if (shown == 1 | shown == -1)
				{
					ArrayList<String> rec = new ArrayList<String>();
					rec.add("" + id); // item_rec_id from the ent_recommendation table
					rec.add(ex); // example rdfid 
					rec.add("0.5"); // similarity value
					rec.add(method); //the approach which was used for recommendation
					recommendation_list.add(rec);	
					count++;
				}				
			}
		}		
		return recommendation_list;
	}

}
