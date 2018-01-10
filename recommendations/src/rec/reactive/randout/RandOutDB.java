package rec.reactive.randout;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rec.DBInterface;

public class RandOutDB extends DBInterface {

	public boolean verbose;

	public RandOutDB(String connurl, String user, String pass) {
		super(connurl, user, pass);
		verbose = false;
	}

	public Map<String,Map<String,String>> getContentConcepts (String[] contentList) {

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

			Map<String,Map<String,String>> map = new HashMap<String, Map<String,String>>();
			
			String query = " SELECT distinct content_name, concept, direction FROM rec.rel_content_concept_direction " +
					" where content_name in (" + availableContentText + ");" ; 
					
			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);

			String content, concept, direction;
			Map<String,String> kcMap;
			while (rs.next()) {
				content = rs.getString("content_name");
				concept = rs.getString("concept");
				direction = rs.getString("direction");
				kcMap = map.get(content);
				if (kcMap != null) {
					kcMap.put(concept, direction);
				} else {
					kcMap = new HashMap<String,String>();
					kcMap.put(concept, direction);
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
	
	public List<String> getChallengeCodingList(String[] contentList) {

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

			String query = " SELECT distinct activity from um2.ent_activity " + " where appid in (47,44) "
					+ " and activity in (" + availableContentText + ");";

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
	
	public Map<String, List<String>> getOutcomeExamples(String[] contentList) {

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

			Map<String, List<String>> map = new HashMap<String, List<String>>();

			String query = " SELECT distinct concept, content_name " +
			               " FROM rec.rel_content_concept_direction " +
                           " where direction = 'outcome' and content_type = 'pcex_set' " +
					       " and content_name in (" + availableContentText + ");";

			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			List<String> list;
			String content, outcome;
			while (rs.next()) {
				content = rs.getString("content_name");
				outcome = rs.getString("concept");
				list = map.get(outcome);
				if ( list != null) 
					list.add(content);
				else {
					list = new ArrayList<String>();
					list.add(content);
					map.put(outcome, list);
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

			String query = "select distinct A1.activity from ent_activity A1, ent_activity A2, rel_pcex_set_component PC"
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
}
