package rec.reactive.randout;
import java.util.List;
import java.util.Map;


public class RandOutData {

	private Map<String, Map<String,String>> contentConceptMap;  //keys are contents, value is a map of concept/direction
	private Map<String, List<String>> outcomeExamples;
	private List<String> codingChallengeList;
	private List<String> exampleList;
	

	private static RandOutData data = null;
	
	private RandOutData(String[] contents, String course_id, 
			  String rec_dbstring, String rec_dbuser, String rec_dbpass,
			  String um2_dbstring, String um2_dbuser, String um2_dbpass) {
		setup(contents, course_id, rec_dbstring, rec_dbuser, rec_dbpass,
			  um2_dbstring, um2_dbuser, um2_dbpass);
    }
	
	public static RandOutData getInstance(String[] contents, String course_id, 
								  String rec_dbstring, String rec_dbuser, String rec_dbpass,
								  String um2_dbstring, String um2_dbuser, String um2_dbpass) {
		if(data == null) {
			data = new RandOutData(contents, course_id, rec_dbstring, rec_dbuser, rec_dbpass,
					        um2_dbstring, um2_dbuser, um2_dbpass);
	    }
		return data;
	}
	
	public void setup(String[] contents, String course_id, String rec_dbstring,
					  String rec_dbuser, String rec_dbpass,
					  String um2_dbstring, String um2_dbuser, String um2_dbpass) {
		RandOutDB randoutDB;
				
		// fill content concepts
		randoutDB = new RandOutDB(rec_dbstring, rec_dbuser, rec_dbpass);
		randoutDB.openConnection();
		contentConceptMap = randoutDB.getContentConcepts(contents);
		randoutDB.closeConnection();
		
		//fill outcome contents map
		randoutDB = new RandOutDB(rec_dbstring, rec_dbuser, rec_dbpass);
		randoutDB.openConnection();
		outcomeExamples = randoutDB.getOutcomeExamples(contents);
		randoutDB.closeConnection();
			
		//fill examples list
		randoutDB = new RandOutDB(um2_dbstring, um2_dbuser, um2_dbpass);
		randoutDB.openConnection();
		exampleList = randoutDB.getExampleList(contents);
		randoutDB.closeConnection();
		
		//fill coding and challenge list
		randoutDB = new RandOutDB(um2_dbstring, um2_dbuser, um2_dbpass);
		randoutDB.openConnection();
		codingChallengeList = randoutDB.getChallengeCodingList(contents);
		randoutDB.closeConnection();

	}

	public Map<String, List<String>> getOutcomeExamples() {
		return outcomeExamples;
	}

	public List<String> getCodingChallengeList() {
		return codingChallengeList;
	}

	public Map<String, Map<String, String>> getContentConceptMap() {
		return contentConceptMap;
	}
	
	public List<String> getExamples() {
		return exampleList;
	}
	
	
}
