package rec.proactive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import rec.ValueComparator_StringDouble;

public class KM {
	/*
	 * @return: a list of lists. Each element in the list is a list which contains:
	 *          - the content_name as id of the content
	 *          - the rank calculated for the content
	 *          - the approach used for calculation of ranks
	 *          Notes: the elements of the main list are sorted descendingly according to the rank
	 */
	public static ArrayList<ArrayList<String>> calculateSequenceRank(
			HashMap<String, ArrayList<String[]>> content_concepts,
			Map<String, double[]> user_concept_knowledge_levels,
			HashMap<String, String[]> examples_activity,
			HashMap<String, String[]> questions_activity, String approach, double proactive_threshold, 
			int proactive_max, String[] contentList) {

		HashMap<String,Double> contentPrerequisiteKnowledgeMap = new HashMap<String,Double>();
		HashMap<String,Double> contentImpactMap = new HashMap<String,Double>();
		HashMap<String,Double> contentProgressMap = new HashMap<String,Double>();
		ArrayList<ArrayList<String>> sequenceList = new ArrayList<ArrayList<String>>();
		HashMap<String,double[]> contentDirectionCountMap = new HashMap<String,double[]>(); // (content, pre,outcome)
		Map<String, Double> rankMap = new HashMap<String,Double>();
		ValueComparator_StringDouble vc =  new ValueComparator_StringDouble(rankMap,content_concepts);
		TreeMap<String,Double> sortedRankMap = new TreeMap<String,Double>(vc);
		/* step1: calculate the prerequisite knowledge of the student in the
		 * contents; The results will be stored in the map
		 * contentPrerequisiteKnowledgeMap. 
		 * Also calculate the impact of the content, here by the impact we focus on the concepts that
		   are outcome of the content. We calculate how much the learner still need to know in each of the outcomes
		   and this will form the content impact. The results will be stored in the map contentImpactMap 
		 *  @see document of optimizer */		
		double prerequisiteKnowledgeRatio = 0.0;
		double dividendPrerequisite = 0.0;
		double denominatorPrerequisite = 0.0;
		double impactRatio = 0.0;
		double dividendImpact = 0.0;
		double denominatorImpact = 0.0;
		double weight = 0.0;		
		ArrayList<String[]> conceptList;		
		double prerequisiteNo = 0, outcomeNo = 0;		
		for (String content_name : contentList)
		{
			conceptList = content_concepts.get(content_name); //[0] concept, [1] weight, [2] direction
			dividendPrerequisite = 0.0;
			denominatorPrerequisite = 0.0;
			dividendImpact = 0.0;
			denominatorImpact = 0.0;
			prerequisiteKnowledgeRatio = 0.0;
			impactRatio = 0.0;			
			prerequisiteNo = 0;
			outcomeNo = 0;
			if (conceptList != null)
			{
				for (String[] concept : conceptList)
				{
					weight = 0.0;
					double klevel = 0.0;
					if(user_concept_knowledge_levels != null && user_concept_knowledge_levels.get(concept[0]) != null) 
						klevel = user_concept_knowledge_levels.get(concept[0])[0]; //TODO currently concept level has only 1 value
					try
					{
						weight = Double.parseDouble(concept[1]);
					}catch(Exception e){ weight = 0.0;}


					//					if (weight == 0)					
					//						System.out.println("zero weight occured");

					if (concept[2].equals("prerequisite"))
					{	
						prerequisiteNo++;
						dividendPrerequisite += (klevel * Math.log10(1+weight)); //all weights are added by one to avoid log1 = 0. In this case, if weight is 1, log would not be 0.
						denominatorPrerequisite += Math.log10(1+weight);	
					}
					else if (concept[2].equals("outcome"))
					{
						outcomeNo++;
						//TODO 0.8 is achieved with 5 successful attempts
						if (klevel > 0.8)
						{ 
							dividendImpact += 0 ;
						}else{
							dividendImpact += ((1-klevel) * Math.log10(1+weight));
						}
						denominatorImpact += Math.log10(1+weight);	
					}												
				}
			}

			if (denominatorPrerequisite != 0)				
				prerequisiteKnowledgeRatio = dividendPrerequisite / denominatorPrerequisite;
			//			else 
			//				System.out.println("denominatorPrerequisite is zero");
			contentPrerequisiteKnowledgeMap.put(content_name,prerequisiteKnowledgeRatio);	

			if (denominatorImpact != 0)
				impactRatio = dividendImpact / denominatorImpact;
			//			else
			//				System.out.println("denominatorImpact is zero");
			contentImpactMap.put(content_name,impactRatio);	

			contentDirectionCountMap.put(content_name, new double[]{prerequisiteNo,outcomeNo});
		}		

		/* step2: calculate the user progress in each content.
		   The results will be stored in the contentUnlearnedRatioMap.
		   Progress for questions is 1 when at least 1 attempt is correct. 
		   For examples is number of different lines displayed / total number of commented lines of the example.
		 */


		double progress = 0.0;

		//for questions
		double attempt = 0.0;
		double success = 0.0;
		// for examples
		double distinctLines = 0.0;
		double totalLines = 0.0;
		for (String content_name : contentList)
		{
			if (questions_activity.containsKey(content_name))
			{
				String[] questionInfo = questions_activity.get(content_name);
				attempt = 0.0;
				success = 0.0;
				try
				{
					attempt = Double.parseDouble(questionInfo[1]); //[1] nattempts
					success = Double.parseDouble(questionInfo[2]);//[2] nsuccess
				}catch(Exception e){ attempt=0;success = 0.0; }
				if (success > 0)
					progress = 1;
				else
					progress = 0;
			}
			else if (examples_activity.containsKey(content_name))
			{
				String[] exampleInfo = examples_activity.get(content_name);
				distinctLines = 0.0;
				totalLines = 0.0;
				try {
					distinctLines = Double.parseDouble(exampleInfo[2]); //[2] distinctactions
					totalLines = Double.parseDouble(exampleInfo[3]);//[3] totallines
				} catch (Exception e) {
					distinctLines = 0.0;
					totalLines = 0.0;
				}
				progress = distinctLines/totalLines;
			}
			else
			{
				progress = 0; //there was no record in the maps of question and example activity for the content, thus it has never been attempted.
			}
			contentProgressMap.put(content_name, progress);
		}

		/* Step3: all contents can be ranked by weighted average of their corresponding values in the following two maps:
		 * 1) contentPrerequisiteKnowledgeMap 
		 * 2)contentImpactMap 
		 * The result is the final rank of content in the contentRankMap. Rank is between 0 and 1. 
		 * Note: contents with progress of 1 are ignored
		 */
		double rank = 0.0;
		double[] count;
		double pre=0.0,outcome=0.0;
		boolean isExample;
		for (String content : contentList)
		{		
			isExample = examples_activity.containsKey(content);
			prerequisiteKnowledgeRatio = 0.0;
			impactRatio = 0.0;
			progress = 0.0;
			pre=0.0;
			outcome=0.0;
			if (contentPrerequisiteKnowledgeMap.get(content) != null)
				prerequisiteKnowledgeRatio = contentPrerequisiteKnowledgeMap.get(content);
			if(contentImpactMap.get(content) != null)
				impactRatio = contentImpactMap.get(content);
			if(contentProgressMap.get(content) != null)
				progress = contentProgressMap.get(content);
			if(contentDirectionCountMap.get(content) != null)
			{
				count = contentDirectionCountMap.get(content);
				pre = count[0];
				outcome = count[1];
			}
			rank = (pre*prerequisiteKnowledgeRatio + outcome*impactRatio)/(pre+outcome); // rank is between 0 and 1 for each content

			if (rank > proactive_threshold) 
			{
				if (!(isExample && (progress == 1.0))) {   //ignore example contents with progress 1
					if (progress == 1) {
						rankMap.put(content, 0.8 * rank);	
				    }
					else{
						rankMap.put(content, rank);		
					}
				}
			}
		}
		sortedRankMap.putAll(rankMap); //sorted map
		int count2 = proactive_max;  
		//TODO
		for (Entry<String, Double> e: sortedRankMap.entrySet())        
		{
			if (count2 == 0)
				break;
			ArrayList<String> list = new ArrayList<String>();
			list.add(e.getKey());
			list.add(""+e.getValue());
			list.add(approach);
			sequenceList.add(list);
			count2--;
		}
		return sequenceList;
	}

}
