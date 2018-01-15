package rec.reactive.pgsc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import rec.DBInterface;

public class PGSCDB extends DBInterface {

	public boolean verbose;

	public PGSCDB(String connurl, String user, String pass) {
		super(connurl, user, pass);
		verbose = false;
	}

	public Map<String,Map<String,Double>> getContentConcepts (String[] contentList) {

		try {
			String availableContentText = "";
			for (String s : contentList) {
				availableContentText += "'" + s + "',";
			}
			availableContentText = availableContentText.substring(0, availableContentText.length() - 1); // ignore
																											// the
																											// last
																											// comma

			stmt = conn.createStatement(); // create a statement

			Map<String,Map<String,Double>> map = new HashMap<String, Map<String,Double>>();
			
			String query = " SELECT title, concept, `tfidf` FROM rec.ent_content_concept_all " +
					" where title in (" + availableContentText + ");" ; 
					
			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);

			String content, concept;
			double weight;
			Map<String,Double> kcMap;
			while (rs.next()) {
				content = rs.getString("title");
				concept = rs.getString("concept");
				weight = rs.getDouble("tfidf");
				kcMap = map.get(content);
				if (kcMap != null) {
					kcMap.put(concept, weight);
				} else {
					kcMap = new HashMap<String,Double>();
					kcMap.put(concept, weight);
					map.put(content, kcMap);
				}
			}
			this.releaseStatement(stmt, rs);
			return map;
		} catch (SQLException ex) {
			this.releaseStatement(stmt, rs);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return null;
		} finally {
			this.releaseStatement(stmt, rs);
		}
	}
	
	public Map<String,List<Integer>> getStartEndLineMap(String[] contentList) {
		
		try {
			String availableContentText = "";
			for (String s : contentList) {
				availableContentText += "'" + s + "',";
			}
			availableContentText = availableContentText.substring(0, availableContentText.length() - 1); // ignore
																											// the
																											// last
																											// comma

			stmt = conn.createStatement(); // create a statement

			Map<String,List<Integer>> map = new HashMap<String,List<Integer>>();

			String query = "select title, min(sline),max(eline) from rec.ent_content_concept_all " +
					       " where title in (" + availableContentText + ")" +
					 	   " group by title";

			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			String content;
			int sline, eline;
			List<Integer> lst;
			while (rs.next()) {
				content = rs.getString("title");
				sline = rs.getInt("min(sline)");
				eline = rs.getInt("max(eline)");
				lst = new ArrayList<Integer>();
				lst.add(sline);
				lst.add(eline);
				map.put(content, lst);
			}
			this.releaseStatement(stmt, rs);
			return map;
		} catch (SQLException ex) {
			this.releaseStatement(stmt, rs);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return null;
		} finally {
			this.releaseStatement(stmt, rs);
		}
	
	}
	
	public Map<String, Map<Integer, List<Integer>>> getBlockEndLineMap(String[] contents) {

		
		try {
			String availableContentText = "";
			for (String s : contents) {
				availableContentText += "'" + s + "',";
			}
			availableContentText = availableContentText.substring(0, availableContentText.length() - 1); // ignore
																											// the
																											// last
																											// comma

			stmt = conn.createStatement(); // create a statement

			Map<String, Map<Integer, List<Integer>>> map = new HashMap<String, Map<Integer, List<Integer>>>();

			String query = "select title, sline, group_concat(distinct eline separator ';') as elines " +
						   " from ent_content_concept_all " +
					       " where title in (" + availableContentText + ")" +
					 	   " group by title, sline";

			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			String content;
			int sline;
			List<Integer> eLst;
			String[] eArr;
			Map<Integer, List<Integer>> lmap;
			while (rs.next()) {
				content = rs.getString("title");
				sline = rs.getInt("sline");
				eLst = new ArrayList<Integer>();
				eArr = rs.getString("elines").split(";");
				for (String e : eArr)
					eLst.add(Integer.parseInt(e));
				lmap = map.get(content);
				if (lmap != null) {
					lmap.put(sline, eLst);
				} else {
					lmap = new HashMap<Integer,List<Integer>>();
					lmap.put(sline,eLst);
					map.put(content, lmap);
				}
			}
			this.releaseStatement(stmt, rs);
			return map;
		} catch (SQLException ex) {
			this.releaseStatement(stmt, rs);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return null;
		} finally {
			this.releaseStatement(stmt, rs);
		}
	}

	public Map<String, Map<Integer, Map<Integer, List<String>>>> getAdjacentConceptMap(String[] contents) {


		
		try {
			String availableContentText = "";
			for (String s : contents) {
				availableContentText += "'" + s + "',";
			}
			availableContentText = availableContentText.substring(0, availableContentText.length() - 1); // ignore
																											// the
																											// last
																											// comma

			stmt = conn.createStatement(); // create a statement

			Map<String,Map<Integer,Map<Integer,List<String>>>> map = new HashMap<String,Map<Integer,Map<Integer,List<String>>>>();;
//TODO query
			String query = " select title, sline, eline, group_concat(distinct concept separator ';') as concepts " +
						   " from ent_content_concept_all " +
					       " where title in (" + availableContentText + ")" +
					 	   " group by title, sline, eline";

			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);

			String content;
			int sline, eline;
			List<String> cLst;
			Map<Integer,List<String>> elineMap;
			Map<Integer,Map<Integer,List<String>>> lcmap;
			while (rs.next()) {
				content = rs.getString("title");
				sline = rs.getInt("sline");
				eline = rs.getInt("eline");
				cLst = new ArrayList<String>(Arrays.asList(rs.getString("concepts").split(";")));
				lcmap = map.get(content);
				if (lcmap != null) {
					elineMap = lcmap.get(sline);
					if (elineMap != null) {
						elineMap.put(eline, cLst);
					}
					else {
						elineMap = new HashMap<Integer,List<String>>();
						elineMap.put(eline,cLst);
						lcmap.put(sline, elineMap);
					}
				}				
				else {
					lcmap = new HashMap<Integer,Map<Integer,List<String>>>();
					elineMap = new HashMap<Integer,List<String>>();
					elineMap.put(eline,cLst);
					lcmap.put(sline,elineMap);
					map.put(content, lcmap);
				}			
			}
			this.releaseStatement(stmt, rs);
			return map;
		} catch (SQLException ex) {
			this.releaseStatement(stmt, rs);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return null;
		} finally {
			this.releaseStatement(stmt, rs);
		}
	
	}
	
	//Note: The pcex examples are referred by their set names in Mastery Grids
	//		Currently this methods returns only pcex examples.
	public List<String> getExampleList(String[] contentList) {

		try {
			String availableContentText = "";
			for (String s : contentList) {
				availableContentText += "'" + s + "',";
			}
			availableContentText = availableContentText.substring(0, availableContentText.length() - 1); // ignore
																											// the
																											// last
																											// comma

			stmt = conn.createStatement(); // create a statement

			List<String> activityList = new ArrayList<String>();

			String query = "select A1.activity from ent_activity A1, ent_activity A2, rel_pcex_set_component PC"
					+ " where A1.activityid = PC.parentactivityid and A2.activityid = PC.childactivityid"
					+ " and A1.appid = 45 and A2.appid = 46  " + " and A1.activity in (" + availableContentText + ");";

			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				activityList.add(rs.getString("activity"));
			}
			this.releaseStatement(stmt, rs);
			return activityList;
		} catch (SQLException ex) {
			this.releaseStatement(stmt, rs);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return null;
		} finally {
			this.releaseStatement(stmt, rs);
		}
	}


	public Map<String, HashSet<String>> getTopicOutcomes(String course_id) {

		
		try {
			stmt = conn.createStatement(); // create a statement

			Map<String,HashSet<String>> map = new HashMap<String,HashSet<String>>();

			String query = "select distinct topic_name, concept" +
						   " FROM rec.ent_topic_outcomes" +
                           " where course_id = " + course_id + ";";

			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			String topic, concept;
			HashSet<String> outcomes;
			while (rs.next()) {
				topic = rs.getString("topic_name");
				concept = rs.getString("concept");
				outcomes = map.get(topic);
				if (outcomes != null)
					outcomes.add(concept);
				else {
					outcomes = new HashSet<String>();
					outcomes.add(concept);
					map.put(topic, outcomes);
				}
			}
			this.releaseStatement(stmt, rs);
			return map;
		} catch (SQLException ex) {
			this.releaseStatement(stmt, rs);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return null;
		} finally {
			this.releaseStatement(stmt, rs);
		}
	}
	
	public Map<String, Integer> getItemKcSize(String[] contentList) {

		try {
			String availableContentText = "";
			for (String s : contentList) {
				availableContentText += "'" + s + "',";
			}
			availableContentText = availableContentText.substring(0, availableContentText.length() - 1); // ignore
																											// the
																											// last
																											// comma

			stmt = conn.createStatement(); // create a statement

			Map<String, Integer> map = new HashMap<String, Integer>();
 
			
			String query = " SELECT title, count(concept) as concept_no FROM rec.ent_content_concept_all " +
					" where title in (" + availableContentText + ") " +
					" group by title;";
					
			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);

			String content;
			int concept_no;
			while (rs.next()) {
				content = rs.getString("title");
				concept_no = rs.getInt("concept_no");
				map.put(content, concept_no);
			}
			this.releaseStatement(stmt, rs);
			return map;
		} catch (SQLException ex) {
			this.releaseStatement(stmt, rs);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return null;
		} finally {
			this.releaseStatement(stmt, rs);
		}
	}

	public Map<String, Integer> getTopicOrder(String course_id ) {	
		try {																									
			stmt = conn.createStatement(); // create a statement

			Map<String,Integer> map = new HashMap<String,Integer>();

			String query = "SELECT distinct topic_name, topic_order FROM rec.ent_topic_outcomes " +
                           " where course_id = " + course_id + ";";

			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			String topic;
			int order;
			while (rs.next()) {
				topic = rs.getString("topic_name");
				order = Integer.parseInt(rs.getString("topic_order"));
				map.put(topic, order);
			}
			this.releaseStatement(stmt, rs);
			return map;
		} catch (SQLException ex) {
			this.releaseStatement(stmt, rs);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return null;
		} finally {
			this.releaseStatement(stmt, rs);
		}
	}
	
}
