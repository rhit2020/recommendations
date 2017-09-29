package rec.test;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Map.Entry;


public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		String user = "dguerra";
//		String group = "ADL";
//		int actNo = 1;
//		boolean verbose = true;
//		String[] providers = {"quizjet","webex","animated_examples", "sqlnot"};
//		String query = "SELECT AllParameters FROM um2.ent_user_activity "+
//		"where userID = (select userid from um2.ent_user where login = '"+user+
//		"') and groupid = (select userid from um2.ent_user where login = '"+group+"')"+
//		" and appid in (";
//		for (int i=0; i<providers.length;i++){
//			if (providers[i].equals("quizjet")) {
//				query = query + String.valueOf(25);  
//			}else if(providers[i].equals("sqlnot")){
//				query = query + String.valueOf(23);  
//			}else if (providers[i].equals("webex")){
//				query = query + String.valueOf(3);  
//
//			}else if (providers[i].equals("animated_examples")){
//				query = query + String.valueOf(35);  
//
//			}else{
//				System.err.println("provider not defined");
//			}
//			if(i<providers.length-1){
//				query = query+", ";
//			}else{
//				query = query+") ";
//			}
//		}
//
//		query = query+"order by id desc "+
//		"limit " + String.valueOf(actNo)+";";
//
//		if (verbose){
//			System.out.println(query);
//		}

		Map<String, Double> activityProbability = new HashMap<String, Double>();
		PriorityQueue<Entry<String, Double>> pq = new PriorityQueue<Map.Entry<String,Double>>(3, new Comparator<Entry<String, Double>>() {
			@Override
			public int compare(Entry<String, Double> arg0,
					Entry<String, Double> arg1) {
				return arg1.getValue().compareTo(arg0.getValue());
			}
		});
		activityProbability.put("one", 1.0);
		activityProbability.put("two", 2.0);
		activityProbability.put("three", 3.0);
		
		pq.addAll(activityProbability.entrySet());

		while (!pq.isEmpty()) {
			System.out.println( pq.poll());
		}

	}

}
