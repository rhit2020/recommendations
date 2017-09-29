package rec.proactive.Optimize4allqs;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;



public class AggregateFileReader {
	//TODO: this is only for one group. If needed for other groups, change the code and fileAddr
	private String fileAddr;
	private String topicFileAddr;
	private String group = "ASUFall2015b";
	private HashMap<String,HashMap<String,String>> providerData;
	private HashMap<String,String> allActivityTopics;
	private HashMap<String, Integer> topicOrder;
//	public AggregateFileReader(String addr) {
//		fileAddr = addr;
//		readIntoMap(group);
//	}

	public AggregateFileReader(String addr,String topicAddr) {
		fileAddr = addr;
		topicFileAddr = topicAddr;
		readIntoMap(group);
		topicOrder = readTopicOrder(topicFileAddr);
	}

	private HashMap<String, Integer> readTopicOrder(String topicFileAddr2) {
		topicOrder = new HashMap<String, Integer>();
		BufferedReader br = null;
		String line = "";
		try {
			br = new BufferedReader(new FileReader(topicFileAddr2));
			String[] row;
			while ((line = br.readLine()) != null) {						
				row = line.split(",");
				String topic = row[0];
				String order = row[1];
				topicOrder.put( topic, Integer.valueOf(order));
			}
			return topicOrder;
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return topicOrder;
	}

	public HashMap<String, String> getAllofOneContentTypewithTopics(
			String group, String contentProvider) {
		//System.out.println("providerData size " + providerData.size());
		return providerData.get(contentProvider);
	}
	public String getActivityTopic(String activity, String group) {		
		if (allActivityTopics.containsKey(activity)){
			return allActivityTopics.get(activity);}
		System.out.println("ERROR: Couldn't find activity "+activity);
		return null;

	}

	private void readIntoMap(String group){
		providerData = new HashMap<String, HashMap<String,String>>();
		allActivityTopics = new HashMap<String, String>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		try {
			br = new BufferedReader(new FileReader(fileAddr));
			String[] row;
			while ((line = br.readLine()) != null) {						
				row = line.split(cvsSplitBy);
				//String grp = row[0];
				//if (!grp.equals(group)){
				//discard this line of code
				//}else{
				String activity = row[1];
				String provider = row[2];
				String topic = row[3];
				allActivityTopics.put(activity, topic);

				HashMap<String, String> temp = new HashMap<String, String>(); 
				if (!providerData.isEmpty() && providerData.containsKey(provider)){
					temp=providerData.get(provider);
				}
				if (temp!=null && !temp.isEmpty()){
					temp.put(activity, topic);					
				}else{
					temp = new HashMap<String, String>();
					temp.put(activity, topic);
				}
				providerData.put(provider, temp);
				//}

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}



	public int getLastRecommendableTopic(
			HashMap<String, Double> activityProgress, String group) {
		// returns last seen topic by user + 1
		int lastTopic = 0;
		if (topicOrder != null && !topicOrder.isEmpty()){
			if (activityProgress!=null && !activityProgress.isEmpty()){
				Iterator<String> i = activityProgress.keySet().iterator();
				while (i.hasNext()){
					String activity = i.next();
					if (activityProgress.get(activity)>0.0){
						int topicNo = topicOrder.get(getActivityTopic(activity, group));
						if (topicNo > lastTopic){
							lastTopic=topicNo;
						}
					}
				}
			}
		}else{
			System.out.println("Topic Order is Empty");
		}
		return lastTopic+1;
	}


	public static void main(String[] args) {
		AggregateFileReader ag = new AggregateFileReader("./WebContent/resources/ASUFall2015b_group_topic_contents_aggreagte.csv", "./WebContent/resources/ASUFall2015b_topic_order.csv");
		HashMap<String, String> a = ag.getAllofOneContentTypewithTopics("ASUFall2015b","quizjet");
		System.out.println(a.size());
		System.out.println(a.keySet());
	}

	public int getTopicOrder(String activityTopic) {
		if (topicOrder.containsKey(activityTopic))
			return topicOrder.get(activityTopic);
		System.out.println("Error: topic of activity cannot be found to retrieve the order. Topic = " + activityTopic);
		return 10000000;
	}

}
