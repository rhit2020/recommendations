package rec.proactive.bng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BNGStaticData {

	private static BNGStaticData instance = null;
	private static List<String> challengeList = new ArrayList<String>();
	private static List<String> codingList = new ArrayList<String>();
	private static List<String> exampleList = new ArrayList<String>();
	private static Map<String, List<String>> exampleKCs = new HashMap<String, List<String>>();

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

		// get kcs in examples
		bngDB = new BNGDB(rec_dbstring, rec_dbuser, rec_dbpass);
		bngDB.openConnection();
		exampleKCs = bngDB.getExampleKcs(contentList);
		bngDB.closeConnection();
	}

	public List<String> getCodingList() {
		return codingList;
	}

	public List<String> getChallengeList() {
		return challengeList;
	}

	public List<String> getExampleList() {
		return exampleList;
	}

	public Map<String, List<String>> getExampleKcs() {
		return exampleKCs;
	}

}
