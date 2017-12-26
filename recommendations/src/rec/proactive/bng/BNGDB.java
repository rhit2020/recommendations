package rec.proactive.bng;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rec.DBInterface;

public class BNGDB extends DBInterface {

	public boolean verbose;

	public BNGDB(String connurl, String user, String pass) {
		super(connurl, user, pass);
		verbose = false;
	}

	public Map<String, List<String>> getExampleKcs(String[] contentList) {

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

			String query = " SELECT distinct content_name, concept " + " FROM ent_content_concepts "
					+ " and content_name in (" + availableContentText + ");";

			if (verbose) {
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);

			String example, concept;
			List<String> kcList;
			while (rs.next()) {
				example = rs.getString("content_name");
				concept = rs.getString("concept");
				kcList = exkcMap.get(example);
				if (kcList != null) {
					kcList.add(concept);
				} else {
					kcList = new ArrayList<String>();
					kcList.add(concept);
					exkcMap.put(example, kcList);
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

	public List<String> getCodingList(String[] contentList) {
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

			String query = " SELECT distinct activity from um2.ent_activity " + " where appid = 44 "
					+ " and content_name in (" + availableContentText + ");";

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

	public List<String> getChallengeList(String[] contentList) {

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

			String query = " SELECT distinct activity from um2.ent_activity " + " where appid = 47 "
					+ " and content_name in (" + availableContentText + ");";

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

			String query = " SELECT distinct activity from um2.ent_activity "
					+ " where appid = 46 and description = 'PCEX Example' " + " and content_name in ("
					+ availableContentText + ");";

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
