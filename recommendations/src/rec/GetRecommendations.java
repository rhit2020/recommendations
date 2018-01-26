package rec;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import rec.ans.example.line.ExampleLineANS;
import rec.proactive.Optimize4allqs.Optimize4allqs;
import rec.proactive.bng.BNG;
import rec.proactive.bng.GetBNKCSummary;
import rec.proactive.random.Random;
import rec.proactive.KM;
import rec.reactive.ReactiveRecommendation;
import rec.reactive.pgsc.PGSC;
import rec.reactive.randout.RandOut;


/**
 * Servlet implementation class GetRecommendations
 */
@WebServlet("/GetRecommendations")
public class GetRecommendations extends HttpServlet {
	private static final long serialVersionUID = 1L;

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
		
		try {
			//parse the json in the request
			InputStreamReader is = new InputStreamReader(request.getInputStream());
			JSONParser jsonParser = new JSONParser();
			org.json.simple.JSONObject jsonObject = (org.json.simple.JSONObject) jsonParser.parse(is);

			String usr = (String) jsonObject.get("usr"); // a string showing user, e.g. rec.test
			String grp = (String) jsonObject.get("grp"); // the class mnemonic, e.g IS172013Fall
			String sid = (String) jsonObject.get("sid"); // a string showing session id, e.g. HHH1
			String cid = (String) jsonObject.get("cid"); // a number showing course id, e.g. 1
			String domain = (String) jsonObject.get("domain"); // a string showing domain, e.g. java
			String lastContentId = (String) jsonObject.get("lastContentId"); // a string showing content_name, e.g. jDouble1
			String lastContentResult = (String) jsonObject.get("lastContentResult"); // a number (0 or 1) showing the result of the last attempt
			String lastContentProvider = (String) jsonObject.get("lastContentProvider"); // a name of the provider for the content user just attempted, e.g. quizjet [currently reactive recommendation works for only quizjet]
			String[] contentList = null;
			if ((String) jsonObject.get("contents")!=null)
				contentList = ((String) jsonObject.get("contents")).split(","); //contents are separated by ,
			String topicContentTxt = (String) jsonObject.get("topicContents");
			Map<String, List<String>> topicContents = getTopicContentMap(topicContentTxt);
			String userContentProgressTxt = (String) jsonObject.get("userContentProgress");
			Map<String, Double> usrContentProgress = getContentProgress(userContentProgressTxt);
	        String updatesm = (String) jsonObject.get("updatesm");
			//--end		
				
			String seq_id =  ""+System.nanoTime();
			response.setContentType("application/json");
			ArrayList<ArrayList<String>> recList = new ArrayList<ArrayList<String>>();
			ArrayList<ArrayList<String>> sequencingList = new ArrayList<ArrayList<String>>();
						
			HashMap<String, Double> itemKCEstimates = null; // knowledge estimates, filled when proactive method is bng
			
			//Step 1: generate proactive recommendation
			String proactive_rec_on = (String) jsonObject.get("proactiveRecOn");
			if (proactive_rec_on == null)
				proactive_rec_on = "true";
	        if (proactive_rec_on.equals("true"))
	        {
	        	//(a) get parameters of proactive recommendation from the request
	        	int proactive_max = rec_cm.proactive_max;
	    		if ((String) jsonObject.get("proactive_max") != null)
	    		{
	    			try {
	    				proactive_max = Integer.parseInt((String) jsonObject.get("proactive_max"));	 
	    			}catch(NumberFormatException e){
	    				proactive_max = rec_cm.proactive_max;
	    			}
	    		}
	    		String  proactive_method = rec_cm.proactive_method;
	    		if ((String) jsonObject.get("proactive_method") != null)
	    			proactive_method = (String) jsonObject.get("proactive_method");
	    		double proactive_threshold = rec_cm.proactive_threshold; 

	    		//(b) get recommendations from the requested proactive method
	    		if (proactive_method.toLowerCase().equals("bng")) {
	    			itemKCEstimates = GetBNKCSummary.getItemKCEstimates(usr, grp, lastContentId,
	    					lastContentResult, contentList, updatesm);
	    			sequencingList = BNG.calculateSequenceRank(usr, grp, domain,
	    					rec_cm.rec_dbstring, rec_cm.rec_dbuser, rec_cm.rec_dbpass,
	    					um2_cm.dbstring, um2_cm.dbuser, um2_cm.dbpass,
	    					contentList, lastContentId, 
	    					proactive_max,
	    					topicContents, usrContentProgress,itemKCEstimates, updatesm);    	
	    		}
	    		else if (proactive_method.toLowerCase().equals("random")) {
	    			sequencingList = Random.calculateSequenceRank(usr, grp, domain, 
	    					rec_cm.rec_dbstring, rec_cm.rec_dbuser, rec_cm.rec_dbpass,
	    					um2_cm.dbstring, um2_cm.dbuser, um2_cm.dbpass,
	    					topicContents, proactive_max, usrContentProgress, contentList);    	
	    		}
	    		else if (proactive_method.toLowerCase().equals("optimize4allqs")) {
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
	    			sequencingList = KM.calculateSequenceRank(usr, grp, domain,proactive_method,proactive_threshold,proactive_max,contentList,getServletContext().getRealPath(rec_cm.relative_resource_path));    	
	    		}    
	        }
	        
			//Step 2: generate reactive recommendation
	    	//(a) get parameters of reactive recommendation from the request
			String reactive_rec_on = (String) jsonObject.get("reactiveRecOn");
			if (reactive_rec_on == null)
				reactive_rec_on = "true";		
			if (reactive_rec_on.equals("true"))
	        {
				int reactive_max = rec_cm.reactive_max;
				if ((String) jsonObject.get("reactive_max") != null)
				{
					try {
						reactive_max = Integer.parseInt((String) jsonObject.get("reactive_max"));	 	    	
					}catch(NumberFormatException e){
						reactive_max = rec_cm.reactive_max;
					}
				}
				String  reactive_method = rec_cm.reactive_method;
				if ((String) jsonObject.get("reactive_method") != null)
					reactive_method = (String) jsonObject.get("reactive_method");
				double reactive_threshold = rec_cm.reactive_threshold;
				if ((String) jsonObject.get("reactive_threshold") != null)
				{
					try {
						reactive_threshold = Double.parseDouble((String) jsonObject.get("reactive_threshold"));	 	    	
					}catch(NumberFormatException e){
						reactive_threshold = rec_cm.reactive_threshold;
					}
				}
				String[] methods = new String[]{reactive_method}; //TODO array is created for flexibility, maybe in future we need to send recommendations by multiple method
				int method_selected = 0; 
	    		
				
				//(b) get recommendations from the requested reactive method
				if (lastContentProvider.equals("quizjet") || lastContentProvider.equals("sqlknot") ||
					lastContentProvider.equals("pcrs") || lastContentProvider.equals("pcex_ch"))
				{
					try {
						double res = Double.parseDouble(lastContentResult);
					    if (res == 0){
					    	if (reactive_method.toLowerCase().equals("pgsc")) {
					    		if ( itemKCEstimates == null) 
					    			itemKCEstimates = GetBNKCSummary.getItemKCEstimates(usr, grp, lastContentId,
					    					lastContentResult, contentList, updatesm);
					    		recList = PGSC.generateReactiveRecommendations(
					    				seq_id, usr, grp, cid, sid, lastContentId, lastContentResult,
					    				reactive_max, contentList, itemKCEstimates,
					    				reactive_threshold, topicContents,
					    				rec_cm.rec_dbstring, rec_cm.rec_dbuser, rec_cm.rec_dbpass,
					    				um2_cm.dbstring, um2_cm.dbuser, um2_cm.dbpass);
					    		
					    	} else if (reactive_method.toLowerCase().equals("randout")) {
						    		recList = RandOut.generateReactiveRecommendations(
					    				seq_id, usr, grp, cid, sid, lastContentId,
					    				reactive_max, contentList, 
					    				rec_cm.rec_dbstring, rec_cm.rec_dbuser, rec_cm.rec_dbpass,
					    				um2_cm.dbstring, um2_cm.dbuser, um2_cm.dbpass);
					    	}
					    	else {
								recList = ReactiveRecommendation.generateReactiveRecommendations(seq_id, usr, grp, domain, cid, sid,
								lastContentId, lastContentResult, reactive_max, methods, method_selected,
							    contentList, reactive_threshold,
								rec_cm.rec_interpolation_alpha, rec_cm.example_count_personalized_approach,
								rec_cm.rec_dbstring, rec_cm.rec_dbuser, rec_cm.rec_dbpass,
								getServletContext().getRealPath(rec_cm.relative_resource_path));
					    	}
					    }
					}catch(Exception e){
						e.printStackTrace();
					}	
				}   
	        }
			
			//Step 3: generate line ANS data
			String lineRecOn = (String) jsonObject.get("lineRecOn");
			String ansLineJSON = null;
			if (lineRecOn != null)
				if (lineRecOn.equals("true"))
				{
					String example = (String) jsonObject.get("example");
					//(a) get parameters of line recommendation from the request
					double line_max = rec_cm.line_max;
					double line_threshold = rec_cm.line_threshold;		

					//(b) get recommendations for lines
					if (example != null) //generate ANS for only the given example
					{
						ansLineJSON = ExampleLineANS.generateLineANSforExample(example,usr,grp,domain,
								getServletContext().getRealPath(rec_cm.relative_ans_path),false,
								line_max,line_threshold,contentList,getServletContext().getRealPath(rec_cm.relative_resource_path));
					}
					else{ //generate ANS for all examples
						ExampleLineANS.generateLineANSforAllExamples(usr,grp,domain,
							getServletContext().getRealPath(rec_cm.relative_ans_path),true,line_max,line_threshold,
							contentList,getServletContext().getRealPath(rec_cm.relative_resource_path));
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
			if (itemKCEstimates != null ) {itemKCEstimates.clear(); itemKCEstimates = null;}

			//destroy data structures for dynamic data
			contentList = null;
			if (topicContents!= null) { topicContents.clear(); topicContents = null;}
			if (usrContentProgress!= null) { usrContentProgress.clear(); usrContentProgress = null;}
		} catch (ParseException e1) {
			e1.printStackTrace();
		}
		
	}

	/*
	 * Sample format of user Content Progress is:
	 * act1,1;act2,0
	 */
	private Map<String, Double> getContentProgress(String userContentProgressTxt) {
		Map<String, Double> contentProgress = null;
		if (userContentProgressTxt != null && userContentProgressTxt.isEmpty() == false ) {
			contentProgress = new HashMap<String, Double>();
			double progress;
			for (String cp : userContentProgressTxt.split(";")) {
				String[] tmp = cp.split(",");
				try {
					progress = Double.parseDouble(tmp[1]);
				}catch (Exception e){
					progress = 0;
				}
				contentProgress.put(tmp[0], progress);
			}
		}
		return contentProgress;
	}

	/*
	 * Sample format of topic contents is:
	 * T1:a,b,c|T2:d,e,f|T3:h,g,i
	 */
	private Map<String, List<String>> getTopicContentMap(String topicContentTxt) {
		Map<String, List<String>> topicContents = null;
		if (topicContentTxt != null && topicContentTxt.isEmpty() == false ) {
			topicContents = new HashMap<String, List<String>>();
			ArrayList<String> list;
			for (String tc : topicContentTxt.split("\\|")) {
				String[] tmp = tc.split(":");
				if ( tmp.length > 1 ) {
					list = new ArrayList<String>();
					for (String c : tmp[1].split(",")) 
						list.add(c);
					topicContents.put(tmp[0], list);	
				}
			}
		}
		return topicContents;
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
}
