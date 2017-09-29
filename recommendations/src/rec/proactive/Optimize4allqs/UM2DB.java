package rec.proactive.Optimize4allqs;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rec.DBInterface;

/**
 * 
 * @author sherry
 *
 */
public class UM2DB extends DBInterface{
	public boolean verbose;

	public UM2DB(String connurl, String user, String pass) {
		super(connurl, user, pass);
		verbose = false;
	}




	public String[] getLastNActivities(String user, int actNo, String group,
			String domain, String[] providers) {
		String[] activityList = new String[actNo];
		try {
			long startTime = System.currentTimeMillis();
			stmt = conn.createStatement(); 

			String query = "SELECT AllParameters FROM um2.ent_user_activity "+
			"where userID = (select userid from um2.ent_user where login = '"+user+
			"') and groupid = (select userid from um2.ent_user where login = '"+group+"')"+
			" and appid in (";
			for (int i=0; i<providers.length;i++){
				if (providers[i].equals("quizjet")) {
					query = query + String.valueOf(25);  
				}else if(providers[i].equals("sqlnot")){
					query = query + String.valueOf(23);  
				}else if (providers[i].equals("webex")){
					query = query + String.valueOf(3);  

				}else if (providers[i].equals("animatedexamples")){
					query = query + String.valueOf(35);  

				}else{
					System.err.println("provider not defined");
				}
				if(i<providers.length-1){
					query = query+", ";
				}else{
					query = query+") ";
				}
			}

			query = query+"order by id desc "+
			"limit " + String.valueOf(actNo)+";";
			//System.out.println("query " + query);
			if (verbose){
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			int i = 0;
			while (rs.next()) {				
				String unparsed = rs.getString(1);
				if (unparsed !=null && !unparsed.isEmpty()){
					String activity = parseActivityResults(unparsed);
					if (activity !=null && !activity.isEmpty()){
						activityList[i] =activity;
						i++;
					}
				}
			}

			this.releaseStatement(stmt, rs);
			long endTime   = System.currentTimeMillis();
			//System.out.println("getLastNActivities time: "+(endTime-startTime));
			if (i==0){
				System.out.println("UM2DB No previous activities for user "+user +" in group "+ group);
				return null;
			}
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

	private String parseActivityResults(String unparsed) {
		HashMap<String, String> parameterPairs = new HashMap<String, String>();  
		if (verbose){
			System.out.println(unparsed);
		}
		String[] separated = unparsed.split(";");
		for (String s:separated){
			String[] equalPair = s.split("=");
			if (equalPair.length == 2 && !equalPair[0].equals("") && !equalPair[1].equals("")){
				parameterPairs.put(equalPair[0], equalPair[1]);
				//System.out.println(equalPair[1]);
			}
		}
		if (parameterPairs.containsKey("app")){
			String app = parameterPairs.get("app");
			if (app.equals("25")) {
				if (parameterPairs.containsKey("sub")){
					//System.out.println("returning sub "+parameterPairs.get("sub"));
					return parameterPairs.get("sub");
				}
				return null;
			}else if(app.equals("23")){
				if (parameterPairs.containsKey("act")){
					return parameterPairs.get("act");
				}
				return null;
			}else if (app.equals("3")){
				if (parameterPairs.containsKey("act")){
					return parameterPairs.get("act");
				}
				return null;
			}else if (app.equals("35")){
				if (parameterPairs.containsKey("act")){
					return parameterPairs.get("act");
				}
				return null;
			}else{
				System.err.println("application provider not defined");
				if (parameterPairs.containsKey("act")){
					return parameterPairs.get("act");
				}
				return null;
			}
		}else{
			return null;
		}
	}




	public List<String> getStudentList(String group) {
		List<String> studentList = new ArrayList<String>();
		//TODO: right now it only returns ASUFall2015b group
		group = "6003";
		try {
			stmt = conn.createStatement(); 
			String query = "select Login from ent_user as u , rel_user_user as r" 
				+" where u.userID = r.userID"
				+" and r.groupID= '"+group+"';";

			if (verbose){
				System.out.println(query);
			}
			rs = stmt.executeQuery(query);
			while (rs.next()) {				
				studentList.add(rs.getString(1));
			}
			this.releaseStatement(stmt, rs);
			return studentList;
		} catch (SQLException ex) {
			this.releaseStatement(stmt, rs);
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			return null;
		} finally {
			this.releaseStatement(stmt, rs);
		}	}

}
