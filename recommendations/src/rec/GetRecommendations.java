package rec;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import rec.ans.example.line.ExampleLineANS;
import rec.proactive.Optimize4allqs.Optimize4allqs;
import rec.proactive.KM;
import rec.reactive.ReactiveRecommendation;


/**
 * Servlet implementation class GetRecommendations
 */
@WebServlet("/GetRecommendations")
public class GetRecommendations extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String server = "http://adapt2.sis.pitt.edu";
	private String examplesActivityServiceURL = server
	+ "/aggregateUMServices/GetExamplesActivity";
	private String questionsActivityServiceURL = server
	+ "/aggregateUMServices/GetQuestionsActivity";
	private String contentKCURL = server
	+ "/aggregateUMServices/GetContentConcepts";
	private String conceptLevelsServiceURL = server + "/cbum/ReportManager";

	// knowledge components (concepts) and the level of knowledge of the user in each of them
	HashMap<String, ArrayList<String[]>> kcByContent; // for each content there is an array list of kc (concepts) with id, weight (double) and direction (prerequisite/outcome)

	private RecConfigManager rec_cm;
	private AggregateConfigManager aggregate_cm;
	private UM2ConfigManager um2_cm;

	public GetRecommendations() {
		super();       
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		rec_cm = new RecConfigManager(this);
		aggregate_cm = new AggregateConfigManager(this);
		um2_cm = new UM2ConfigManager(this);
		
		//set parameters for recommendation server
		////--start	
		String usr = request.getParameter("usr"); // a string showing user, e.g. rec.test
		String grp = request.getParameter("grp"); // the class mnemonic, e.g IS172013Fall
		String sid = request.getParameter("sid"); // a string showing session id, e.g. HHH1
		String cid = request.getParameter("cid"); // a number showing course id, e.g. 1
		String domain = request.getParameter("domain"); // a string showing domain, e.g. java
		String lastContentId = request.getParameter("lastContentId"); // a string showing content_name, e.g. jDouble1
		String lastContentResult = request.getParameter("lastContentResult"); // a number (0 or 1) showing the result of the last attempt
		String lastContentProvider = request.getParameter("lastContentProvider"); // a name of the provider for the content user just attempted, e.g. quizjet [currently reactive recommendation works for only quizjet]
		String[] contentList = null;
		if (request.getParameter("contents")!=null)
			contentList = request.getParameter("contents").split(","); //contents are separated by ,
		// String[] contentList = readContents( getServletContext().getRealPath(rec_cm.relative_resource_path+"/topic_content.csv")); //TODO remove
		//--end
		
		//set student data
		//--start
		HashMap<String, String[]> examples_activity = getUserExamplesActivity(usr, domain);
		HashMap<String, String[]> questions_activity = getUserQuestionsActivity(usr, domain);
		HashMap<String, double[]> kcSummary = getConceptLevels(usr, domain, grp);		
		//--end
		
		//set the static content-concept data
		//--start
		StaticData static_data = StaticData.getInstance(domain, grp,contentList,
				getServletContext().getRealPath(rec_cm.relative_resource_path+"/adjusted_direction_automatic_indexing.txt"),
				getServletContext().getRealPath(rec_cm.relative_resource_path+"/adjusted_direction_automatic_indexing_sql.txt"),
				getServletContext().getRealPath(rec_cm.relative_resource_path+"/topic_content_ae.csv"),
				getServletContext().getRealPath(rec_cm.relative_resource_path+"/line_concept_java.csv"),
				getServletContext().getRealPath(rec_cm.relative_resource_path+"/annotated_line_index.csv"),
				getServletContext().getRealPath(rec_cm.relative_resource_path+"/example_type.csv"),
				getServletContext().getRealPath(rec_cm.relative_resource_path+"/title_rdfid.csv"),
				getServletContext().getRealPath(rec_cm.relative_resource_path+"/topic_order.csv"),
				contentKCURL);
		kcByContent = static_data.getKcByContent();
		HashMap<String, HashMap<Integer, ArrayList<String>>> exampleLineKC = static_data.getExampleLineKC();
		Map<String,List<String>> topicContentMap = static_data.getTopicContentMap();
		Map<String,List<Integer>> annotatedLineIndex = static_data.getAnnotatedLines();
		Map<String,String> exampleTypeList = static_data.getExampleType();
		Map<String,String> titleRdfMap = static_data.getTitleRdfMap();
		Map<Integer, String>  topicOrderMap = static_data.getTopicOrder();
		//--end
			
		String seq_id =  ""+System.nanoTime();
		response.setContentType("application/json");
		ArrayList<ArrayList<String>> recList = new ArrayList<ArrayList<String>>();
		ArrayList<ArrayList<String>> sequencingList = new ArrayList<ArrayList<String>>();
			
		//Step 1: generate reactive recommendation
    	//(a) get parameters of reactive recommendation from the request
		String reactive_rec_on = request.getParameter("reactiveRecOn");
		if (reactive_rec_on == null)
			reactive_rec_on = "true";
		if (reactive_rec_on.equals("true"))
        {
			int reactive_max = rec_cm.reactive_max;
			if (request.getParameter("reactive_max") != null)
			{
				try {
					reactive_max = Integer.parseInt(request.getParameter("reactive_max"));	 	    	
				}catch(NumberFormatException e){
					reactive_max = rec_cm.reactive_max;
				}
			}
			String  reactive_method = rec_cm.reactive_method;
			if (request.getParameter("reactive_method") != null)
				reactive_method = request.getParameter("reactive_method");
			double reactive_threshold = rec_cm.reactive_threshold;
			if (request.getParameter("reactive_threshold") != null)
			{
				try {
					reactive_threshold = Double.parseDouble(request.getParameter("reactive_threshold"));	 	    	
				}catch(NumberFormatException e){
					reactive_threshold = rec_cm.reactive_threshold;
				}
			}
			String[] methods = new String[]{reactive_method}; //TODO array is created for flexibility, maybe in future we need to send recommendations by multiple method
			int method_selected = 0; 
    		//(b) get recommendations from the requested reactive method
			if (lastContentProvider.equals("quizjet") | lastContentProvider.equals("sqlknot"))
			{
				try{
					int res = Integer.parseInt(lastContentResult);
					if (res == 0)
					{
						recList = ReactiveRecommendation.generateReactiveRecommendations(seq_id, usr, grp, cid, 
								sid, lastContentId, lastContentResult, reactive_max, methods, method_selected,examples_activity,
								questions_activity,contentList,kcByContent,reactive_threshold,
								rec_cm.rec_interpolation_alpha,rec_cm.example_count_personalized_approach,
								rec_cm.rec_dbstring, rec_cm.rec_dbuser, rec_cm.rec_dbpass);	
					}
				}catch(Exception e){}	
			}   
        }
		
		//Step 2: generate proactive recommendation
		String proactive_rec_on = request.getParameter("proactiveRecOn");
		if (proactive_rec_on == null)
			proactive_rec_on = "true";
        if (proactive_rec_on.equals("true"))
        {
        	//(a) get parameters of proactive recommendation from the request
        	int proactive_max = rec_cm.proactive_max;
    		if (request.getParameter("proactive_max") != null)
    		{
    			try {
    				proactive_max = Integer.parseInt(request.getParameter("proactive_max"));	 
    			}catch(NumberFormatException e){
    				proactive_max = rec_cm.proactive_max;
    			}
    		}
    		String  proactive_method = rec_cm.proactive_method;
    		if (request.getParameter("proactive_method") != null)
    			proactive_method = request.getParameter("proactive_method");
    		double proactive_threshold = rec_cm.proactive_threshold; //TODO should ask server to send 0 to service
    		//	    if (request.getParameter("proactive_threshold") != null)
    		//	    {
    		//	    	 try {
    		//	    		 proactive_threshold = Double.parseDouble(request.getParameter("proactive_threshold"));	 	  
    		//	 	    }catch(NumberFormatException e){
    		//	 	    	proactive_threshold = rec_cm.proactive_threshold;
    		//	 	    }
    		//	    }

    		//(b) get recommendations from the requested proactive method
    		if (proactive_method.toLowerCase().equals("optimize4allqs")) {
    			sequencingList = Optimize4allqs.calculateSequenceRank(usr, grp, cid, domain, lastContentId,
    					lastContentResult, lastContentProvider, contentList,
    					proactive_max, proactive_method, proactive_threshold,
    					sequencingList,aggregate_cm.dbstring, aggregate_cm.dbuser, aggregate_cm.dbpass,
    					um2_cm.dbstring, um2_cm.dbuser, um2_cm.dbpass,
    					um2_cm.fastdbstring,
    			        this.getServletContext().getRealPath(aggregate_cm.relative_resource_path+"/Parameters_FAST_11131.csv"),
    			        this.getServletContext().getRealPath(rec_cm.relative_resource_path+"/ASUFall2015b_group_topic_contents_aggreagte.csv"),
    			        this.getServletContext().getRealPath(rec_cm.relative_resource_path+"/ASUFall2015b_topic_order.csv"), "optimize4allqs");

    		}else if (proactive_method.toLowerCase().equals("optimize4allqswithprobabilities")) {
    			sequencingList = Optimize4allqs.calculateSequenceRank(usr, grp, cid, domain, lastContentId,
    					lastContentResult, lastContentProvider, contentList,
    					proactive_max, proactive_method, proactive_threshold,
    					sequencingList,aggregate_cm.dbstring, aggregate_cm.dbuser, aggregate_cm.dbpass,
    					um2_cm.dbstring, um2_cm.dbuser, um2_cm.dbpass,
    					um2_cm.fastdbstring,
    			        this.getServletContext().getRealPath(aggregate_cm.relative_resource_path+"/Parameters_FAST_11131.csv"),
    			        this.getServletContext().getRealPath(rec_cm.relative_resource_path+"/ASUFall2015b_group_topic_contents_aggreagte.csv"),
    			        this.getServletContext().getRealPath(rec_cm.relative_resource_path+"/ASUFall2015b_topic_order.csv"), "optimize4allqswithprobabilities");

    		}
    		else{
    			sequencingList = KM.calculateSequenceRank(
    					kcByContent,kcSummary,examples_activity,questions_activity,proactive_method,proactive_threshold,proactive_max,contentList);    	
    		}    
        }
		
		//Step 3: generate line ANS data
		String lineRecOn = request.getParameter("lineRecOn");
		String ansLineJSON = null;
		if (lineRecOn != null)
			if (lineRecOn.equals("true"))
			{
				//(a) get parameters of line recommendation from the request
				double line_max = rec_cm.line_max;
				double line_threshold = rec_cm.line_threshold;
				String example = request.getParameter("example");
				//(b) get recommendations for lines
				if (example != null) //generate ANS for only the given example
				{
					ansLineJSON = ExampleLineANS.generateLineANSforExample(example,kcByContent,exampleLineKC,
							examples_activity,questions_activity,kcSummary,
							topicContentMap,line_max,line_threshold,annotatedLineIndex,usr,grp,
							getServletContext().getRealPath(rec_cm.relative_ans_path),
							exampleTypeList,titleRdfMap,topicOrderMap,false);
				}
				else{ //generate ANS for all examples
					ExampleLineANS.generateLineANSforAllExamples(kcByContent,exampleLineKC,
						examples_activity,questions_activity,kcSummary,
						topicContentMap,line_max,line_threshold,annotatedLineIndex,usr,grp,
						getServletContext().getRealPath(rec_cm.relative_ans_path),
						exampleTypeList,titleRdfMap,topicOrderMap,true);
				}						
			}
		
		//Step 4: generate output for the request
		String output = null;
		PrintWriter out = response.getWriter();
		if (proactive_rec_on.equals("true") | reactive_rec_on.equals("true"))
		{
			//create the JSON for the reactive/passive Recommendation
			output = "{\n";
			output += getReactiveJSON(recList,seq_id) + ",\n\n";
			output += getProactiveJSON(sequencingList,seq_id);
			output += "\n}";
		}
		else if (lineRecOn.equals("true"))
		{
			output = ansLineJSON;
		}
		out.print(output);
		//System.out.println(output);
		
		//destroy objects
		rec_cm = null; 	aggregate_cm = null; um2_cm = null;
		examples_activity.clear(); examples_activity = null;
		questions_activity.clear(); questions_activity = null;
		kcSummary.clear();kcSummary = null;
	}

	private String getProactiveJSON(ArrayList<ArrayList<String>> recList, String seq_id) {
		String json = "  proactive: {";
		if (recList.isEmpty() == false)
		{
			int count = 0;
			json += "\n    id: \""+seq_id+"\", contentScores: [";
			for (ArrayList<String> rec : recList)
			{
				json += "\n                                                { rec_item_id: \""+(++count)+"\", approach:\""+rec.get(2)+"\", content: \""+rec.get(0)+"\", score: \""+rec.get(1)+"\" }, ";
			}
			json = json.substring(0, json.length()-2);// this is for ignoring the last comma
			json += "\n                                              ]";
		}
		json += "\n  }";
		return json;
	}

	private String getReactiveJSON(ArrayList<ArrayList<String>> recList, String seq_id) {
		String json = "  reactive: {";
		if (recList.isEmpty() == false)
		{
			json += "\n    id: \""+seq_id+"\", contentScores: [";
			for (ArrayList<String> rec : recList)
			{
				json += "\n                                               { rec_item_id: \""+rec.get(0)+"\", approach:\""+rec.get(3)+"\", content: \""+rec.get(1)+"\", score: \""+rec.get(2)+"\" }, ";
			}
			json = json.substring(0, json.length()-2);// this is for ignoring the last comma
			json += "\n                                              ]";
		}
		json += "\n  }";
		return json;
	}

	// CALLING A UM SERVICE: //GET THE LEVELS OF KNOWLEDGE OF THE USER IN CONCEPTS
	// FROM USER MODEL USING THE USER MODEL INTERFACE
	public HashMap<String, double[]> getConceptLevels(String usr, String domain,
			String grp) {
		HashMap<String, double[]> user_concept_knowledge_levels = new HashMap<String, double[]>();
		try {
			URL url = null;
			if (domain.equalsIgnoreCase("java")) {
				url = new URL(conceptLevelsServiceURL
						+ "?typ=con&dir=out&frm=xml&app=25&dom=java_ontology"
						+ "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
						+ URLEncoder.encode(grp, "UTF-8"));

			}

			if (domain.equalsIgnoreCase("sql")) {
				url = new URL(conceptLevelsServiceURL
						+ "?typ=con&dir=out&frm=xml&app=23&dom=sql_ontology"
						+ "&usr=" + URLEncoder.encode(usr, "UTF-8") + "&grp="
						+ URLEncoder.encode(grp, "UTF-8"));

			}
			if (url != null)
				user_concept_knowledge_levels = processUserKnowledgeReport(url);
			// System.out.println(url.toString());
		} catch (Exception e) {
			user_concept_knowledge_levels = null;
			e.printStackTrace();
		}
		return user_concept_knowledge_levels;

	}
	
	private static HashMap<String, double[]> processUserKnowledgeReport(URL url) {

		HashMap<String, double[]> userKnowledgeMap = new HashMap<String, double[]>();
		try {
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory
			.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(url.openStream());
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("concept");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;
					NodeList cogLevels = eElement
					.getElementsByTagName("cog_level");
					for (int i = 0; i < cogLevels.getLength(); i++) {
						Node cogLevelNode = cogLevels.item(i);
						if (cogLevelNode.getNodeType() == Node.ELEMENT_NODE) {
							Element cogLevel = (Element) cogLevelNode;
							if (getTagValue("name", cogLevel).trim().equals(
							"application")) {
								// System.out.println(getTagValue("name",
								// eElement));
								double[] levels = new double[1];
								levels[0] = Double.parseDouble(getTagValue("value",cogLevel).trim());
								userKnowledgeMap.put(
										getTagValue("name", eElement),
										levels);
							}
						}
					}
				}
			}

		} catch (Exception e) {

			e.printStackTrace();
			return null;
		}
		return userKnowledgeMap;
	}

	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0)
		.getChildNodes();
		Node nValue = (Node) nlList.item(0);
		return nValue.getNodeValue();
	}
	
	// CALLING A UM SERVICE
	private HashMap<String, String[]> getUserExamplesActivity(String usr,
			String domain) {
		HashMap<String, String[]> eActivity = null;
		try {
			String url = examplesActivityServiceURL + "?usr=" + usr;
			JSONObject json = readJsonFromUrl(url);

			if (json.has("error")) {
				System.out.println("Error:[" + json.getString("errorMsg") + "]");
			} else {
				eActivity = new HashMap<String, String[]>();
				JSONArray activity = json.getJSONArray("activity");
				for (int i = 0; i < activity.length(); i++) {
					JSONObject jsonobj = activity.getJSONObject(i);
					String[] act = new String[5];
					act[0] = jsonobj.getString("content_name");
					act[1] = jsonobj.getDouble("nactions") + "";
					act[2] = jsonobj.getDouble("distinctactions") + "";
					act[3] = jsonobj.getDouble("totallines") + "";
					act[4] = jsonobj.getJSONArray("clicked") + "";
     				act[4] = act[4].replaceAll("\\[", "");
					act[4] = act[4].replaceAll("\\]", "");
					eActivity.put(act[0], act);
					// System.out.println(jsonobj.getString("name"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return eActivity;
	}

	private HashMap<String, String[]> getUserQuestionsActivity(String usr,
			String domain) {
		HashMap<String, String[]> qActivity = null;
		try {
			String url = questionsActivityServiceURL + "?usr=" + usr;
			JSONObject json = readJsonFromUrl(url);

			if (json.has("error")) {
				System.out
				.println("Error:[" + json.getString("errorMsg") + "]");
			} else {
				qActivity = new HashMap<String, String[]>();
				JSONArray activity = json.getJSONArray("activity");

				for (int i = 0; i < activity.length(); i++) {
					JSONObject jsonobj = activity.getJSONObject(i);
					String[] act = new String[3];
					act[0] = jsonobj.getString("content_name");
					act[1] = jsonobj.getDouble("nattempts") + "";
					act[2] = jsonobj.getDouble("nsuccesses") + "";
					qActivity.put(act[0], act);
					// System.out.println(jsonobj.getString("name"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return qActivity;
	}

	private static JSONObject readJsonFromUrl(String url) throws IOException,
	JSONException {
		InputStream is = new URL(url).openStream();
		JSONObject json = null;
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is,
					Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			json = new JSONObject(jsonText);
		} finally {
			is.close();
		}
		return json;
	}
	
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	//	public static void main(String[] args) {
	//		GetRecommendations gr = new GetRecommendations();
	//		int course = 13;
	//		FASTDBInterface fastDB = new FASTDBInterface("jdbc:mysql://localhost/fast", "student", "student");
	//		fastDB.openConnection();
	//		boolean verbose = true;
	//		gr.numberForFASTHistoryUpdate = 1;
	//		API fastAPI = new API(true, false, false, true, 2, 2, 1, false, true, verbose, fastDB);
	//		fastAPI.getParametersFromFile(getServletContext().getRealPath(aggregate_cm.relative_resource_path+"/Parameters_FAST_11131.csv"), 0, 1, 7, 4, 8, 6, 10, 12);
	//
	//			sequencingList = optimize4allqs( "dguerra",  "java", course, "ASUFall2015b", "decisions1_v2", contentList, proactive_method,  proactive_threshold, proactive_max, fastAPI);
	//		
	//		fastDB.closeConnection();
	//
	//	}
	
	//	private String[] readContents(String realPath) {
	//		ArrayList<String> contentList = new ArrayList<String>();
	//		BufferedReader br = null;
	//		String line = "";
	//		String cvsSplitBy = ",";
	//		boolean isHeader = false; //currently the file has no header
	//		try {
	//			br = new BufferedReader(new FileReader(realPath));
	//			String[] clmn;
	//			String content;			
	//			while ((line = br.readLine()) != null) {
	//				if (isHeader)
	//				{
	//					isHeader = false;
	//					continue;
	//				}
	//				clmn = line.split(cvsSplitBy);
	//				content = clmn[1];				
	//				if (contentList.contains(content) == false)
	//				{
	//					contentList.add(content);
	//				}
	//			}
	//		} catch (FileNotFoundException e) {
	//			e.printStackTrace();
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		} finally {
	//			if (br != null) {
	//				try {
	//					br.close();
	//				} catch (IOException e) {
	//					e.printStackTrace();
	//				}
	//			}
	//		}
	//		int count = contentList.size();		
	//		System.out.println("contents:"+count);
	//		return contentList.toArray(new String[contentList.size()]);
	//	
	//	}

}
