package rec.reactive;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import rec.DBInterface;


public class RecDB extends DBInterface{

    public boolean verbose;

	public RecDB(String connurl, String user, String pass) {
		super(connurl, user, pass);
		verbose = false;
	}

	public SortedMap<String,Double> getSimilarExamples(String method, String last_content_id, String[] contentList, int limit, double rec_score_threshold) {

		try {
			String availableContentText = "";
			for (String s : contentList)
			{
				availableContentText += "'"+ s + "',";
			}
			availableContentText = availableContentText.substring(0,availableContentText.length()-1); //ignore the last comma

			stmt = conn.createStatement(); //create a statement

			Map<String,Double> exampleMap = new HashMap<String,Double>();			
			ValueComparator vc =  new ValueComparator(exampleMap);
			TreeMap<String,Double> sortedRankMap = new TreeMap<String,Double>(vc);			

			String query = " SELECT rs.example_content_name,rs.sim " +
			" FROM rel_con_con_sim rs" +
			" WHERE rs.method = '"+method+"' " +
			" and rs.question_content_name = '"+ last_content_id + "' " +
			" and rs.sim >= '"+rec_score_threshold+"' " +
			" and rs.example_content_name in ("+availableContentText +")" +
			" order by rs.sim Desc limit "+limit+";";	
			
			if (verbose){
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				exampleMap.put(rs.getString(1), rs.getDouble(2));
			}	
			this.releaseStatement(stmt, rs);
			sortedRankMap.putAll(exampleMap);
			return sortedRankMap;
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

	public int addRecommendation(String seq_rec_id, String user_id,
			String group_id, String session_id, String last_content_id,
			String example_content_name, String method, double sim, int shown) {
		int id = -1;
		try {

			stmt = conn.createStatement(); //create a statement
			String query = "INSERT INTO ent_recommendation (seq_rec_id,user_id,group_id,session_id,src_content_name,rec_content_name,rec_approach,rec_score,datentime,shown) values ('"
				+ seq_rec_id + "','" + user_id + "','" + group_id + "','" + session_id 
				+ "','" + last_content_id + "','" + example_content_name + "','" + method + "'," + sim + ","
				+"now(), "+ shown +");";
			if (verbose){
				System.out.println(query);
			}
			stmt.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
			//TODO
			ResultSet rskeys = stmt.getGeneratedKeys();
			if (rskeys.next()){
				id=rskeys.getInt(1);
			}
			this.releaseStatement(stmt, rs);
			return id;
		} catch (SQLException ex) {
			this.releaseStatement(stmt, rs);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return id;
		} finally {
			this.releaseStatement(stmt, rs);
		}
	}

	static class ValueComparator implements Comparator<String> {

		Map<String, Double> base;
		public ValueComparator(Map<String, Double> base) {
			this.base = base;
		}

		// Note: this comparator sorts the values descendingly, so that the best activity is in the first element.
		public int compare(String a, String b) {
			if (base.get(a) >= base.get(b)) {
				return -1;
			} else {
				return 1;
			} // 
		} // returning 0 would merge keys	   
	}


	
}
