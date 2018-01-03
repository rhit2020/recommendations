package rec.proactive.random;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import rec.DBInterface;

public class BNGDB extends DBInterface {

	public boolean verbose;

	public BNGDB(String connurl, String user, String pass) {
		super(connurl, user, pass);
		verbose = false;
	}

	public Map<String, List<String>> getItemKcs(String[] contentList) {

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

			Map<String, List<String>> exkcMap = new HashMap<String, List<String>>();
 
			
			String query = " SELECT content_name, concept  FROM rec.ent_content_concepts_reduced " +
					" where content_name in (" + availableContentText + ");" ; 
					
			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);

			String content, concept;
			List<String> kcList;
			while (rs.next()) {
				content = rs.getString("content_name");
				concept = rs.getString("concept");
				kcList = exkcMap.get(content);
				if (kcList != null) {
					kcList.add(concept);
				} else {
					kcList = new ArrayList<String>();
					kcList.add(concept);
					exkcMap.put(content, kcList);
				}
			}
			this.releaseStatement(stmt, rs);
			return exkcMap;
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

	public HashSet<String> getCodingList(String[] contentList) {
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

			HashSet<String> activityList = new HashSet<String>();

			String query = " SELECT distinct activity from um2.ent_activity " + " where appid = 44 "
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

	public HashSet<String> getChallengeList(String[] contentList) {

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

			HashSet<String> activityList = new HashSet<String>();

			String query = " SELECT distinct activity from um2.ent_activity " + " where appid = 47 "
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

	//Note: The examples are referred by their set names in Mastery Grids
	public HashSet<String> getExampleList(String[] contentList) {

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

			HashSet<String> activityList = new HashSet<String>();

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

	public Map<String, HashSet<String>> getSetChallenges(String[] contentList) {
		
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

			Map<String,HashSet<String>> setChList = new HashMap<String, HashSet<String>>();

			String query = "select A1.activity as set_name, A2.activity as ch_name from ent_activity A1, ent_activity A2, rel_pcex_set_component PC"
					+ " where A1.activityid = PC.parentactivityid and A2.activityid = PC.childactivityid"
					+ " and A1.appid = 45 and A2.appid = 47  " + " and A1.activity in (" + availableContentText + ");";

			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			String set_name, ch_name;
			HashSet<String> chList;
			while (rs.next()) {
				set_name = rs.getString("set_name");
				ch_name = rs.getString("ch_name");
				chList = setChList.get(set_name);
				if (chList != null)
					chList.add(ch_name);
				else {
					chList = new HashSet<String>();
					chList.add(ch_name);
					setChList.put(set_name, chList);
				}
			}
			this.releaseStatement(stmt, rs);
			return setChList;
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
