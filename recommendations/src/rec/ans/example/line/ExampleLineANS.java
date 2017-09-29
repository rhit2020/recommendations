package rec.ans.example.line;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import rec.ValueComparator_IntegerDouble;


public class ExampleLineANS {

	public static void generateLineANSforAllExamples(
			HashMap<String, ArrayList<String[]>> kcByContent,
			HashMap<String, HashMap<Integer, ArrayList<String>>> exampleLineKC,
			HashMap<String, String[]> examples_activity,
			HashMap<String, String[]> questions_activity,
			HashMap<String, double[]> kcSummary, Map<String, List<String>> topicContentMap,
			double line_max, double line_threshold, Map<String, List<Integer>> annotatedLineIndex,String usr,String grp,
			String ans_path, Map<String, String> exampleTypeList,Map<String, String> titleRdfMap, Map<Integer, String> topicOrderMap, boolean storeANSData) {
		for (String example : annotatedLineIndex.keySet())
		{
			generateLineANSforExample(example,kcByContent,
					exampleLineKC,
					examples_activity,
					questions_activity,
					kcSummary, topicContentMap,
					line_max, line_threshold, annotatedLineIndex, usr, grp,
					 ans_path, exampleTypeList,titleRdfMap,topicOrderMap,storeANSData);
		}
	}
	
	public static String generateLineANSforExample(String example, HashMap<String, ArrayList<String[]>> kcByContent,
			HashMap<String, HashMap<Integer, ArrayList<String>>> exampleLineKC,
			HashMap<String, String[]> examples_activity, HashMap<String, String[]> questions_activity, 
			HashMap<String, double[]> kcSummary, Map<String, List<String>> topicContentMap,
			double line_max, double line_threshold, Map<String, List<Integer>> annotatedLineIndex,
			String usr, String grp, String ans_path, Map<String, String> exampleTypeList, 
			Map<String, String> titleRdfMap, Map<Integer, String> topicOrderMap, boolean storeANSData){
		
		String[] lans=null;
		double avgk = 0.0, gain = 0.0;
		String clicked=null;
		ArrayList<String> lineConcepts=null;
		ArrayList<String> failedQuesInTopics=null;
		HashMap<Integer,String[]> ans=null;
		Map<Integer, Double> rankMap=null;
		ValueComparator_IntegerDouble vc=null;
		TreeMap<Integer,Double> sortedRankMap=null;
		List<Integer> recs =null;

		if (getTopic(example,topicContentMap) != null)
			failedQuesInTopics = getFailedQuestionsInAndAfterTopic(getTopic(example,topicContentMap),questions_activity,topicContentMap,topicOrderMap);
		else
			failedQuesInTopics = new ArrayList<String>();
		ans = new HashMap<Integer,String[]>();
		//calculate ANS for each annotated line	
		if (exampleLineKC.get(example) == null)
			return null;
		for (int line : exampleLineKC.get(example).keySet())
		{
			avgk = 0.0; gain = 0.0;
			clicked =null;
			lineConcepts = null; lans = null;
			if (annotatedLineIndex.get(example).contains(line)){
				lans = new String[3]; //avgk,clicked,gain
				//calculate average knowledge
				lineConcepts = exampleLineKC.get(example).get(line);//note: pass by reference, NEVER delete/clear it
				if (lineConcepts.size() > 0 ){
					for (String c : lineConcepts)
					{
						avgk += kcSummary.get(c)[0]; // currently concept level has only 1 value
					}				
					avgk = avgk/lineConcepts.size();
				}else{
					System.out.println("Error: no concept for example: "+example+" "+" line: "+line);
				}

				//determine clicked or not clicked
				if (examples_activity.get(example) != null)
					clicked = (Arrays.asList(examples_activity.get(example)[4].split(",")).contains(""+line)?"true":"false");
				else
					clicked = "false";
				//determine gain
				gain = calculateLineGain(line,failedQuesInTopics,kcByContent,lineConcepts,kcSummary,titleRdfMap);
				//store ans values for the line
				lans[0] = ""+getFillingLevel(avgk);
				lans[1]= clicked; 
				lans[2]=""+gain;
//				System.out.println("ExampleLineANS,94    example:"+example+"  line#:"+line+"  "+lans[0]+","+lans[1]+","+lans[2]);
				ans.put(line, lans);
			}
		}
					
		//rank lines of the example based on gain desc
		rankMap = new HashMap<Integer,Double>();
		vc =  new ValueComparator_IntegerDouble(rankMap);
		for (Integer l : ans.keySet())
			rankMap.put(l, Double.parseDouble(ans.get(l)[2]));//line,gain
		sortedRankMap = new TreeMap<Integer,Double>(vc);
		sortedRankMap.putAll(rankMap); //sorted map
		
		//select the recommendations using gain values 
		//ignore gain < threshold (.1)  and filling == 100; only select k top lines with highest gain, k=ceil(0.2*annotated.lines)
		int count = (int)(Math.ceil(line_max*(annotatedLineIndex.get(example).size())));
//		System.out.println("---#recomm:"+count+"  "+Math.ceil(line_max*(annotatedLineIndex.get(example).size())));
		recs = new ArrayList<Integer>();
		double prevGain = 0.0;
		for (Entry<Integer, Double> l: sortedRankMap.entrySet())        
		{
			if (count == 0)
			{
			    //if the there are mutiple lines with same gain as the last k, include them in the recommendation
				if (l.getValue() == prevGain)
				{
//					System.out.println("similar to prev ");
					recs.add(l.getKey());
				}
				else
					break;
			}
			if (l.getValue()>=line_threshold & ans.get(l.getKey())[0].equals("100")==false){ 
				recs.add(l.getKey());
				prevGain = l.getValue();
				count--;
			}
		}
//		System.out.println("  rec size: "+recs.size());
		//generate the JSON
		String ansJSON = getLineANSJson(ans, recs);
		
		if (storeANSData == true)
        {
			// write the ansJSON
			if (exampleTypeList.get(example).equals("example"))
				writeLineANSWebex(ans_path, exampleTypeList.get(example), usr,grp, example, ansJSON);
			else if (exampleTypeList.get(example).equals("animated_example")) {
				// TODO
			}

		}
		//destroy objects
		lans = null;
	 	
		if (failedQuesInTopics != null)
		{
			failedQuesInTopics.clear();
	        failedQuesInTopics=null;
       	}
		if (ans != null){
			ans.clear();ans=null;
		}
		if (rankMap!= null){
			rankMap.clear();rankMap=null;
		}
		vc=null;
		if (sortedRankMap!=null)
		{
			sortedRankMap.clear();sortedRankMap=null;
		}
		if (recs != null){
			recs.clear();recs =null;
		}
		return ansJSON;

	}
	
	
	//0=[0,10); 25=[10,45) <-35->; 50=[45,70) <-25->; 75=[70,90) <-20->; 100=[90,100] <-10->
	private static int getFillingLevel(double avgk)
	{
		if (avgk <0.10)
			return 0;
		else if (avgk < 0.45)
			return 25;
		else if (avgk < 0.7)
			return 50;
		else if (avgk < 0.9)
			return 75;
		else 
			return 100;
	}

	private static double calculateLineGain(int line,
			ArrayList<String> failedQuesInTopic,
			HashMap<String, ArrayList<String[]>> kcByContent,
			ArrayList<String> lineConcepts, HashMap<String, double[]> kcSummary, Map<String, String> titleRdfMap) {
		//(a): get concepts in failed questions
		List<String> conceptsInFailedQue = new ArrayList<String>();
		ArrayList<String[]> tmp = null;
		for (String q : failedQuesInTopic)
		{
			tmp = kcByContent.get(q); //note: java pass by reference, NEVER delete this or clear this.
			if (tmp == null)
				tmp = kcByContent.get(titleRdfMap.get(q));
			for (String[] concept_data: tmp)
				conceptsInFailedQue.add(concept_data[0]);// for each content there is an array list of kc (concepts) with id, weight (double) and direction (prerequisite/outcome)
		}
	    //(b): get overalpping concepts between failed questions and concepts in line
		List<String> overlappingConcepts = new ArrayList<String>();
		for (String c : conceptsInFailedQue)
			if (lineConcepts.contains(c))
				if (overlappingConcepts.contains(c) == false)
					overlappingConcepts.add(c);
		//(c): calculate gain
		double gain = 0.0;
		if (overlappingConcepts.size() > 0 )
		{
			for (String c : overlappingConcepts)
				gain += (1 - kcSummary.get(c)[0]);// currently concept level has only 1 value
		gain = gain/overlappingConcepts.size();
		}
		else
		{
//			System.out.println("  **************************  ");
//
//			System.out.println("line :"+line+" has no overlapping concepts with failed questions.");
//			System.out.println("concepts in line");
//			for (String c : lineConcepts)
//				System.out.print(c+"    ");
//			System.out.println("");
//			System.out.println("concepts in failed question");
//			for (String c : conceptsInFailedQue)
//			System.out.print(c+"    ");
//			System.out.print("  **************************  ");

		}
		//destroy objects
		conceptsInFailedQue.clear();conceptsInFailedQue=null;
		overlappingConcepts.clear();overlappingConcepts=null;
		return gain;
	}

	private static ArrayList<String> getFailedQuestionsInAndAfterTopic(String topic,HashMap<String, String[]> questions_activity, Map<String, List<String>> topicContentMap, Map<Integer, String> topicOrderMap) {
		ArrayList<String> failedQues = new ArrayList<String>();
		List<String> topicList = new ArrayList<String>();
		topicList.add(topic);//adding current topic
		topicList.addAll(getTopicsAfter(topic,topicOrderMap));//adding future topic
		for (String t : topicList)
		{
			for (String q : questions_activity.keySet())
				if (topicContentMap.get(t).contains(q))
					// ; // questions_activity.get(q)[3] = success-rate
					if (  (Double.parseDouble(questions_activity.get(q)[3]) * Double.parseDouble(questions_activity.get(q)[1]))
							 <
						   Double.parseDouble(questions_activity.get(q)[1])) //failure i.e., nsuccess < nattempts
						if (failedQues.contains(q)==false)
							failedQues.add(q);
		}		
		return failedQues;
	}

	private static List<String> getTopicsAfter(String topic, Map<Integer, String> topicOrderMap) {
		int topicOrder = 0;
		List<String> list = new ArrayList<String>();
		for (int o : topicOrderMap.keySet())
			if (topicOrderMap.get(o).equals(topic))
			{
				topicOrder = o;
				break;
			}
		for (int i : topicOrderMap.keySet())
			if (i > topicOrder)
				list.add(topicOrderMap.get(i));
		return list;
	}

	private static String getTopic(String example, Map<String, List<String>> topicContentMap) {
		for (String t : topicContentMap.keySet())
			if (topicContentMap.get(t).contains(example))
				return t;
		return null;
	}

	private static String getLineANSJson(HashMap<Integer,String[]> ans, List<Integer> recs) {
		String json = "{";
		if (ans.isEmpty() == false)
		{
			for (int line : ans.keySet())
			{
//				System.out.println("~~~~~~~line:"+line+"  "+recs.contains(line));
				json += "\n    \""+line+"\" : {\n        \"knowledge\" : "+ans.get(line)[0]+"," +
						                      "\n        \"recommended\" : "+recs.contains(line)+","+
						                      "\n        \"checked\" : "+ans.get(line)[1]+
						                      "\n    },";
			}
			json = json.substring(0, json.length()-1);// this is for ignoring the last comma
		}
		json += "\n}";
		return json;
	}
	
	private static void writeLineANSWebex(String path,String exampleType,String usr,String grp,String example,String ansData) {
		File file = new File(path+"/"+grp+"."+exampleType+"."+usr+"."+example+".json");
		FileWriter fw = null;BufferedWriter bw = null;
		try {
			fw = new FileWriter(file.getAbsoluteFile());
		    bw = new BufferedWriter(fw);
			bw.write(ansData);
			bw.close();// be sure to close BufferedWriter
		} catch (IOException e) {
			e.printStackTrace();
		}finally{
			if (bw != null)
				try {
					bw.close(); bw = null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			fw = null;	file = null;
		}

	}


}
