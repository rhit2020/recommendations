package rec.proactive.random;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class BNGStaticData {

	private static BNGStaticData instance = null;
	private static HashSet<String> challengeList = new HashSet<String>();
	private static HashSet<String> codingList = new HashSet<String>();
	private static HashSet<String> exampleList = new HashSet<String>();
	private static Map<String, List<String>> itemKCs = new HashMap<String, List<String>>();
	private static Map<String, HashSet<String>> setChallenges = new HashMap<String, HashSet<String>>();

	private BNGStaticData() {
		// Exists only to defeat instantiation.
	}

	public static BNGStaticData getInstance(String rec_dbstring, String rec_dbuser, String rec_dbpass,
			String um2_dbstring, String um2_dbuser, String um2_dbpas, String[] contentList) {
		if (instance == null) {
			instance = new BNGStaticData();
			setupData(rec_dbstring, rec_dbuser, rec_dbpass, um2_dbstring, um2_dbuser, um2_dbpas, contentList);
		}
		return instance;
	}

	private static void setupData(String rec_dbstring, String rec_dbuser, String rec_dbpass, String um2_dbstring,
			String um2_dbuser, String um2_dbpass, String[] contentList) {
		BNGDB bngDB;
		// get list of coding exercises
		bngDB = new BNGDB(um2_dbstring, um2_dbuser, um2_dbpass);
		bngDB.openConnection();
		codingList = bngDB.getCodingList(contentList);
		bngDB.closeConnection();

		// get list of challenges
		bngDB = new BNGDB(um2_dbstring, um2_dbuser, um2_dbpass);
		bngDB.openConnection();
		challengeList = bngDB.getChallengeList(contentList);
		bngDB.closeConnection();

		// get list of examples
		bngDB = new BNGDB(um2_dbstring, um2_dbuser, um2_dbpass);
		bngDB.openConnection();
		exampleList = bngDB.getExampleList(contentList);
		bngDB.closeConnection();

		// get kcs in items
		bngDB = new BNGDB(rec_dbstring, rec_dbuser, rec_dbpass);
		bngDB.openConnection();
		itemKCs = bngDB.getItemKcs(contentList);
		bngDB.closeConnection();
		
		// get set challenges
		bngDB = new BNGDB(um2_dbstring, um2_dbuser, um2_dbpass);
		bngDB.openConnection();
		setChallenges = bngDB.getSetChallenges(contentList);
		bngDB.closeConnection();
		
	}

	public HashSet<String> getCodingList() {
		return codingList;
	}

	public HashSet<String> getChallengeList() {
		return challengeList;
	}

	public HashSet<String> getExampleList() {
		return exampleList;
	}

	public Map<String, List<String>> getItemKcs() {
		return itemKCs;
	}

	public Map<String, HashSet<String>> getSetChallenges() {
		return setChallenges;
	}

}
