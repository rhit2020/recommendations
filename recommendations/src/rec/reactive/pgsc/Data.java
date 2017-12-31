package rec.reactive.pgsc;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class Data {
	
	private static Map<String,Map<String,Double>> contentConceptMap = null; //keys are contents, values are the map with concept as key and weight as value
	private static Map<String,List<Integer>> startEndLineMap = null; //keys are contents, values: list[0]:start line; list[1]:end line
	private static Map<String,Map<Integer,List<Integer>>> blockEndLineMap = null; //keys are contents, values: a map with key:start line and list of end lines of the concept in that start line
	private static Map<String,Map<Integer,Map<Integer,List<String>>>> adjacentConceptMap = null; //keys are contents, values: a map with key:start line and a map as value(key:end line, value, List of concepts in that start and end line)
	private static DecimalFormat df;	
    //maps for using in the evaluation process
	private static Map<String,HashSet<String>> topicOutcomesMap;
	private static List<String> exampleList;
	private static Map<String, Integer> itemKCSize = new HashMap<String, Integer>();

	private static Map<String, List<List<String>>> contentSubtree;
	

	private static Data data = null;
	
	private Data() {
		// Exists only to defeat instantiation.
		//it can only be accessed in the class
    }
	
	public static Data getInstance(String[] contents, String course_id, 
								  String rec_dbstring, String rec_dbuser, String rec_dbpass,
								  String um2_dbstring, String um2_dbuser, String um2_dbpass) {
		if(data == null) {
			data = new Data();
			setup(contents, course_id, rec_dbstring, rec_dbuser, rec_dbpass,
				  um2_dbstring, um2_dbuser, um2_dbpass);
	    }
		return data;
	}
	
	public static void setup(String[] contents, String course_id, String rec_dbstring,
						     String rec_dbuser, String rec_dbpass,
						     String um2_dbstring, String um2_dbuser, String um2_dbpass) {
		PGSCDB pgscDB;
		df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
				
		// fill content concepts
		pgscDB = new PGSCDB(rec_dbstring, rec_dbuser, rec_dbpass);
		pgscDB.openConnection();
		contentConceptMap = pgscDB.getContentConcepts(contents);
		pgscDB.closeConnection();

		
		// fill start end line data
		pgscDB = new PGSCDB(rec_dbstring, rec_dbuser, rec_dbpass);
		pgscDB.openConnection();
		startEndLineMap = pgscDB.getStartEndLineMap(contents);
		pgscDB.closeConnection();

		
		// fill block end line map
		pgscDB = new PGSCDB(rec_dbstring, rec_dbuser, rec_dbpass);
		pgscDB.openConnection();
		blockEndLineMap = pgscDB.getBlockEndLineMap(contents);
		pgscDB.closeConnection();

		// fill adjacent concept
		pgscDB = new PGSCDB(rec_dbstring, rec_dbuser, rec_dbpass);
		pgscDB.openConnection();
		adjacentConceptMap = pgscDB.getAdjacentConceptMap(contents);
		pgscDB.closeConnection();
		
		//fill content subtrees
		fillContentSubtrees(contents);
		
		//fill examples list
		pgscDB = new PGSCDB(um2_dbstring, um2_dbuser, um2_dbpass);
		pgscDB.openConnection();
		exampleList = pgscDB.getExampleList(contents);
		pgscDB.closeConnection();

		//fill topic outcomes
		pgscDB = new PGSCDB(rec_dbstring, rec_dbuser, rec_dbpass);
		pgscDB.openConnection();
		topicOutcomesMap = pgscDB.getTopicOutcomes(course_id);
		pgscDB.closeConnection();

		//fill item KC size
		pgscDB = new PGSCDB(rec_dbstring, rec_dbuser, rec_dbpass);
		pgscDB.openConnection();
		itemKCSize = pgscDB.getItemKcSize(contents);
		pgscDB.closeConnection();

	}


	private static void fillContentSubtrees(String[] contents) {
		
		contentSubtree = new HashMap<String, List<List<String>>>();
		for (String c : contents) {
			contentSubtree.put(c, getSubtrees(c));
		}
	}
	
	private static List<List<String>> getSubtrees(String content) {
		List<List<String>> subtreeList = new ArrayList<List<String>>();
		List<Integer> lines = getStartEndLine(content);
		if(lines == null)
		{
			System.out.println("Error! no start-end line for "+ content);			
		}
		else
		{
			int start = lines.get(0);
			int end = lines.get(1);
			List<String> subtree = null;
			List<String> adjucentConceptsList = null;
			for (int line = start; line <= end; line++)
			{
				//create subtree for concepts that are in the current line
				subtree = getConceptsInSameLine(content, line);
				if (updateSubtreeList(subtree,subtreeList) == true)
					subtreeList.add(subtree);	
				List<Integer> endLines = getEndLineBlock(content,line); //in case that line has a concept that has an end line different than the start line, endlines would be nonempty			
				for (int e : endLines)
				{
					//create subtree for the block
					subtree = new ArrayList<String>();
					adjucentConceptsList = getAdjacentConcept(content,line,e);
					Collections.sort(adjucentConceptsList, new SortByName());
					for (String adjcon : adjucentConceptsList)
						subtree.add(adjcon);				
					if (updateSubtreeList(subtree,subtreeList) == true)
						subtreeList.add(subtree);	
				}			
			}
		}			
		return subtreeList;
	}	

	private static boolean updateSubtreeList(List<String> subtree, List<List<String>> subtreeList) {
		if (subtree.isEmpty() == false && subtreeList.contains(subtree) == false)
			return true;
		return false;		
	}
	
	private static class SortByName implements Comparator<String> {
	    public int compare(String s1, String s2) {
	        return s1.compareTo(s2);
	    }
	}
	
	public void close() {
		df = null;

		if (startEndLineMap != null)
		{
			for (List<Integer> list : startEndLineMap.values()) //destroy the lists in this map
				destroy(list); 
			destroy(startEndLineMap);//destroy the map
		}
		if (contentConceptMap != null)
		{
			for (Map<String,Double> map : contentConceptMap.values()) //destroy the maps in this map
				destroy(map);
			destroy(contentConceptMap);//destroy the map
		}
		if (blockEndLineMap != null)
		{
			for (Map<Integer, List<Integer>> map : blockEndLineMap.values())
			{
				if (map != null)
				{
					for (List<Integer> list : map.values()) //destroy lists in map
						destroy(list);					
					destroy(map); //destroy the map
				}
			}
			destroy(blockEndLineMap); //destroy the map
		}
		if (adjacentConceptMap != null)
		{
			for (Map<Integer, Map<Integer, List<String>>> map : adjacentConceptMap.values())
			{
				if (map != null)
				{
					for (Map<Integer, List<String>> map2 : map.values())
					{
						if (map2 != null)
						{
							for (List<String> list : map2.values()) //destroy the list in map
								destroy(list);	
							destroy(map2); //destroy the map2
						}
					}
					destroy(map); //destroy map
				}
			}
			destroy(adjacentConceptMap);
		}
	}
	
	private void destroy(Map map) {
		if (map != null)
		{
			for (Object obj : map.keySet())
			{
				obj = null;
			}
			for (Object obj : map.values())
			{
				obj = null;
			}
			map.clear();
			map = null;
		}		
	}

	private void destroy(List list) {
		if (list != null)
		{
			for (Object obj : list)
				obj = null;	
			list.clear();
			list = null;
		}		
	}

	public boolean isReady()
	{
		return true;
	}
	
	//String sqlCommand = "SELECT distinct concept,`tfidf` FROM temp2_ent_jcontent_tfidf where title = '" + content + "';";
	public Map<String,Double> getTFIDF(String content) {
		Map<String,Double> weightMap = contentConceptMap.get(content);	
		if (weightMap == null)
			System.out.println("~~~~~~  weightMap is null.");
		return weightMap;
	}	


	public static List<String> getAdjacentConcept(String content, int sline,int eline) {
		List<String> conceptList = new ArrayList<String>();
		Map<Integer,Map<Integer,List<String>>> map = adjacentConceptMap.get(content);
		if (map == null) {
			System.out.println("~~~~~~  adjacentConceptMap is null");
		} else {
			Map<Integer,List<String>> elineMap;
			List<String> concepts;
			for (Integer s : map.keySet())
			{
				if (s >= sline)
				{
					elineMap = map.get(s);
					for (Integer e : elineMap.keySet())
					{
						if (e <= eline)			
						{
							concepts = elineMap.get(e);
							for (String c : concepts)
								if (conceptList.contains(c) == false)	
									conceptList.add(c);
						}
					}
				}
			}
		}
		return conceptList;	
	}

	public static List<String> getConceptsInSameLine(String content, int sline) {
		List<String> conceptList = new ArrayList<String>();
		Map<Integer,Map<Integer,List<String>>> map = adjacentConceptMap.get(content);	
		if (map == null)
			System.out.println("~~~~~~  adjacentConceptMap is null");

		Map<Integer, List<String>> elineMap;
		for (Integer s : map.keySet())
		{
			if (s == sline)
			{
				elineMap = map.get(s);
				for (List<String> concepts : elineMap.values())
				{
					for (String c : concepts)
						if (conceptList.contains(c) == false)	
							conceptList.add(c);					
				}
			}
		}
		return conceptList;		
	}
	
	public static List<Integer> getStartEndLine(String content) {
		List<Integer> lines = startEndLineMap.get(content);
		return lines;
	}
    
	public static List<Integer> getEndLineBlock(String content, int sline) {
        List<Integer> endLines = new ArrayList<Integer>();
		Map<Integer,List<Integer>> map = blockEndLineMap.get(content);
		
		if (map == null) {
			System.out.println("~~~~~~  blockEndLineMap is null");
		} else {
			List<Integer> tmp = map.get(sline);
	        if (tmp != null)
	        {
	        	for (Integer t : tmp)
	            	if ( t != sline)
	            		endLines.add(t);
	        }		
		}
		return endLines;	
	}	
	   
	public Map<String, List<List<String>>> getContentSubtree() {
		return contentSubtree;
	}
	
	public List<String> getExamples() {
		return exampleList;
	}
	
	public Map<String, Integer> getItemKCSize() {
		return itemKCSize;
	}
	
	public HashSet<String> getTopicOutcomeSet(String concept)
	{
		HashSet<String> topicOutcomesSet = new HashSet<String>();
		for (Entry<String, HashSet<String>> entry : topicOutcomesMap.entrySet())
		{	
			if (entry.getValue().contains(concept))
			{
				if (topicOutcomesSet.contains(entry.getKey()) == false)
					topicOutcomesSet.add(entry.getKey());
			}
		}
		return topicOutcomesSet;
	}
	
}
