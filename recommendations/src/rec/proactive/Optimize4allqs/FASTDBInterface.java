package rec.proactive.Optimize4allqs;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import rec.DBInterface;

public class FASTDBInterface extends DBInterface {
	public static DecimalFormat formatter = new DecimalFormat("#.####");
	private String mainTable = "fast.ent_user_model";
	public boolean verbose;

	// private String url, user, password;

	public FASTDBInterface(String url, String user, String password) {
		super(url, user, password);
		verbose = false;
		// this.url = url;
		// this.user = user;
		// this.password = password;
	}

	public LinkedHashMap<String, Double> getUserModel(String user, int course, String group) {
		try {
			LinkedHashMap<String, Double> userModel = null;

			if (conn == null) {
				// API.log("Warning: conn == null in getUserModel(String user, int course, String group)!");
				// conn = DriverManager.getConnection(url + "?" + "user=" + user + "&password=" + password);
				throw new RuntimeException("ERROR: conn == null!");
			}
			stmt = conn.createStatement();
			if (stmt == null)
				throw new RuntimeException("ERROR: stmt == null!");
			String query = "SELECT model FROM " + mainTable + " WHERE user_id='" + user + "' and course_id=" + course + " and group_id='" + group + "';";// id in " + "(select max(id) from ent_user_model where
			rs = stmt.executeQuery(query);
			if (verbose)
				System.out.println(query);
			int i = 0;
			String model = "";
			while (rs.next()) {
				model = rs.getString("model");
				i++;
			}
			this.releaseStatement(stmt, rs);

			if (i == 1) {
				if (model == null || model.length() == 0)
					throw new RuntimeException("ERROR: the format of model is wrong in FAST DB " + mainTable + "!");
				String[] skillToKnowledge = model.split("\\|");
				for (String pair : skillToKnowledge) {
					if (pair == null || pair.length() == 0 || !pair.contains(":"))
						throw new RuntimeException("ERROR: the format of model is wrong in FAST DB " + mainTable + "!");
					String[] splitResult = pair.split(":");
					if (splitResult.length != 2)
						throw new RuntimeException("ERROR: the format of model is wrong in FAST DB " + mainTable + "!");
					if (splitResult[0].length() == 0)
						throw new RuntimeException("ERROR: the format of model is wrong in FAST DB " + mainTable + "!");
					if (userModel == null)
						userModel = new LinkedHashMap<String, Double>();
					userModel.put(splitResult[0], Double.parseDouble(splitResult[1]));
				}
			}
			else if (i > 1)
				throw new RuntimeException("ERROR: there are multiple records for the same student in FAST DB " + mainTable + "!");
			else
				userModel = null;
			return userModel;

		}
		catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			this.releaseStatement(stmt, rs);
			return null;
		}
		finally {
			this.releaseStatement(stmt, rs);
		}
	}

	public Double getUserModel(String user, int course, String group, String skill) {
		LinkedHashMap<String, Double> userModel = getUserModel(user, course, group);
		if (userModel == null)
			return null;
		else
			return userModel.get(skill);
	}

	public void saveUserModel(String user, int course, String group, HashMap<String, Double> skillToKnowledge) {
		LinkedHashMap<String, Double> existedModel = getUserModel(user, course, group);
		String newModel = "";
		if (existedModel == null) {
			for (Map.Entry<String, Double> pair : skillToKnowledge.entrySet())
				newModel += pair.getKey() + ":" + formatter.format(pair.getValue()) + "|";
		}
		else {
			Set<String> existedSkills = new HashSet<String>();
			for (Map.Entry<String, Double> pair : existedModel.entrySet()) {
				String skill = pair.getKey();
				newModel += skill + ":";
				if (skillToKnowledge.containsKey(skill))
					newModel += formatter.format(skillToKnowledge.get(skill)) + "|";
				else
					newModel += formatter.format(existedModel.get(skill)) + "|";
				existedSkills.add(skill);
			}
			for (Map.Entry<String, Double> pair : skillToKnowledge.entrySet()) {
				if (!existedSkills.contains(pair.getKey()))
					newModel += pair.getKey() + ":" + formatter.format(pair.getValue()) + "|";
			}
		}
		newModel = newModel.substring(0, newModel.length() - 1);
		if (existUserModel(user, course, group))
			updateUserModel(user, course, group, newModel);
		else
			insertUserModel(user, course, group, newModel);
	}

	public void insertUserModel(String user, int course, String group, String model) {
		try {
			stmt = conn.createStatement();
			String query = "INSERT INTO " + mainTable + " (user_id,course_id,group_id,last_update,model) values ('" + user + "'," + course + ",'" + group + "',now(),'" + model + "');";
			if (verbose)
				System.out.println(query);
			if (stmt.execute(query)) {
			}
			this.releaseStatement(stmt, rs);
		}
		catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			this.releaseStatement(stmt, rs);
		}
		finally {
			this.releaseStatement(stmt, rs);
		}

	}

	public void updateUserModel(String user, int course, String group, String model) {
		try {
			stmt = conn.createStatement();
			String query = "UPDATE " + mainTable + " SET model='" + model + "', last_update=now() WHERE user_id = '" + user + "' and course_id=" + course + " and group_id='" + group + "';";
			if (verbose)
				System.out.println(query);
			if (stmt.execute(query)) {
			}
			this.releaseStatement(stmt, rs);
		}
		catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			this.releaseStatement(stmt, rs);
		}
		finally {
			this.releaseStatement(stmt, rs);
		}

	}

	/* TODO: not sure I should check session id or not */
	public boolean existUserModel(String user, int course, String group) {
		int n = 0;
		try {
			stmt = conn.createStatement();
			String query = "SELECT count(*) as npm FROM " + mainTable + " WHERE user_id='" + user + "' and course_id=" + course + " and group_id='" + group + "';";
			if (verbose)
				System.out.println(query);
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				n = rs.getInt("npm");
			}
			if (n > 1)
				throw new RuntimeException("ERROR: there are multiple records for the same student in FAST DB " + mainTable + "!");
			this.releaseStatement(stmt, rs);
		}
		catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			this.releaseStatement(stmt, rs);
		}
		finally {
			this.releaseStatement(stmt, rs);
		}
		return n > 0;
	}

	
	public boolean existUserProbability(String user, int course, String group) {
		int n = 0;
		try {
			stmt = conn.createStatement();
			String query = "SELECT count(*) as npm FROM ent_last_fast_prob WHERE user_id='" + user + "' and course_id=" + course + " and group_id='" + group + "';";
			if (verbose)
				System.out.println(query);
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				n = rs.getInt("npm");
			}
			this.releaseStatement(stmt, rs);
		}
		catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			this.releaseStatement(stmt, rs);
		}
		finally {
			this.releaseStatement(stmt, rs);
		}
		return n > 0;
	}
	
public HashMap<String, Double> getPrevQueProbForAct(String user,
			String domain, int course, String group, String activity) {		
		HashMap<String, Double> probs = new HashMap<String, Double>();
		try {
			stmt = conn.createStatement();
			String query = "select question_id, prob_question from ent_last_fast_prob" +
			" where user_id = '" + user + "' and group_id = '" + group + "' and course_id = " + course + 
			" and activity_id = '" +activity +"';";
			if (verbose)
				System.out.println(query);
			rs = stmt.executeQuery(query);
			while (rs.next()) {
				probs.put(rs.getString(1), rs.getDouble(2));
			}			
			this.releaseStatement(stmt, rs);
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			this.releaseStatement(stmt, rs);
		} finally {
			this.releaseStatement(stmt, rs);
		}
		return probs;
	}

	public void putNewQuestionProbabilityinDB(String user, String domain,
			int course, String group, String activity, String question,
			Double prob) {
		try {
			stmt = conn.createStatement();
			String query = "REPLACE INTO ent_last_fast_prob (user_id,course_id,group_id, activity_id, question_id, prob_question) "+
			"VALUES ('" + user+"', "+course+", '"+group+"', '"+activity+"', '"+question+"', "+prob+") "+";"; 
			if (verbose)
				System.out.println(query);
			if (stmt.execute(query)) {
			}
			this.releaseStatement(stmt, rs);
		} catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
			this.releaseStatement(stmt, rs);
		} finally {
			this.releaseStatement(stmt, rs);
		}

	}
	// for testing
	// public void connIsNull() {
	// if (conn == null)
	// API.log("ERROR: conn==null");
	// }
}
