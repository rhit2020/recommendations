package rec.proactive.Optimize4allqs;
import java.sql.SQLException;
import java.util.HashMap;

import rec.DBInterface;

/**
 * 
 * @author sherry
 *
 */
public class AggregateDB extends DBInterface{
	public boolean verbose;

	public AggregateDB(String connurl, String user, String pass) {
		super(connurl, user, pass);
		verbose = false;

	}



	public HashMap<String, String> getAllofOneContentTypewithTopics(String group, String contentProvider) {
		HashMap<String, String> allQuestions = new HashMap<String, String>();
		try {

			stmt = conn.createStatement(); 

			String query = "select  C.content_name, T.topic_name " +
			"from ent_content C, ent_topic T, rel_topic_content TC, ent_group G " + 
			"where strcmp(lower(G.group_id),lower('" + group + "'))=0 and strcmp(lower(C.provider_id) , lower('" + contentProvider + "')) = 0 and " +
			"TC.content_id = C.content_id and TC.topic_id = T.topic_id " + 
			"and T.active =  1 and C.visible = 1 and G.course_id = T.course_id " +  
			"order by C.content_name;";					
			if (verbose){
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				allQuestions.put(rs.getString(1), rs.getString(2));
			}	
			this.releaseStatement(stmt, rs);
			return allQuestions;
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

	public String getActivityTopic(String activity, String group) {
		String activityTopic = "";
		try {

			stmt = conn.createStatement(); 

			String query = "select T.topic_name " +
			"from ent_content C, ent_topic T, rel_topic_content TC, ent_group G " + 
			"where strcmp(lower(G.group_id),lower('" + group + "'))=0 and strcmp(lower(C.content_name) , lower('" + activity + "')) = 0 and " +
			"TC.content_id = C.content_id and TC.topic_id = T.topic_id " + 
			"and T.active =  1 and C.visible = 1 and G.course_id = T.course_id ;";
			if (verbose){
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				activityTopic=rs.getString(1);
			}	
			this.releaseStatement(stmt, rs);
			return activityTopic;
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



	public HashMap<String, Double> getActivityProgress(String user, int course) {
		HashMap<String, Double> activityProgress = new HashMap<String, Double>();
		String unparsed ="";
		String query = "";
		try {

			stmt = conn.createStatement(); 

			 query = "select model4content from ent_computed_models "+
			" where user_id = '"+ user +"' "+
			" and course_id = " + course + " ;";
			 
			if (verbose){
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			if (rs.next()) {
				unparsed=rs.getString(1);
			}	
			this.releaseStatement(stmt, rs);
			activityProgress = parseActivityProgress(unparsed);
			
			return activityProgress;
		} catch (SQLException ex) {
			System.out.println("query " + query);
			System.out.println("rs  "+rs);
			this.releaseStatement(stmt, rs);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return null;
		} finally {
			this.releaseStatement(stmt, rs);
		}		
	}



	private HashMap<String, Double> parseActivityProgress(String unparsed) {
		HashMap<String, Double> activityProgress = new HashMap<String, Double>();
		if (unparsed==null || unparsed.isEmpty())
			return null;
		
		String[] activitySeparated = unparsed.split("\\|");
		for (int i = 0; i<activitySeparated.length;i++){			
			String temp = activitySeparated[i];
			String[] actVal = temp.split(":");
			String[] vals = actVal[1].split(",");
			activityProgress.put(actVal[0], Double.valueOf(vals[1]));
		}
		
		return activityProgress;
	}


}
