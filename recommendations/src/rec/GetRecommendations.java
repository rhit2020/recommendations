package rec;
import java.io.IOException;
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

import rec.ans.example.line.ExampleLineANS;
import rec.proactive.Optimize4allqs.Optimize4allqs;
import rec.proactive.bng.BNG;
import rec.proactive.KM;
import rec.reactive.ReactiveRecommendation;


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
		String topicContentTxt = request.getParameter("topicContents");
		Map<String, List<String>> topicContents = getTopicContentMap(topicContentTxt);
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
				try {
					double res = Double.parseDouble(lastContentResult);
				    if (res == 0){
							recList = ReactiveRecommendation.generateReactiveRecommendations(seq_id, usr, grp, domain, cid, sid,
							lastContentId, lastContentResult, reactive_max, methods, method_selected,
						    contentList, reactive_threshold,
							rec_cm.rec_interpolation_alpha, rec_cm.example_count_personalized_approach,
							rec_cm.rec_dbstring, rec_cm.rec_dbuser, rec_cm.rec_dbpass,getServletContext().getRealPath(rec_cm.relative_resource_path));
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
    		double proactive_threshold = rec_cm.proactive_threshold; 

    		//(b) get recommendations from the requested proactive method
    		if (proactive_method.toLowerCase().equals("bng")) {
    			sequencingList = BNG.calculateSequenceRank(usr, grp, domain,
    					rec_cm.rec_dbstring, rec_cm.rec_dbuser, rec_cm.rec_dbpass,
    					um2_cm.dbstring, um2_cm.dbuser, um2_cm.dbpass,
    					contentList, lastContentId, 
    					Double.parseDouble(lastContentResult),proactive_max,topicContents);    	
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
		
		//Step 3: generate line ANS data
		String lineRecOn = request.getParameter("lineRecOn");
		String ansLineJSON = null;
		if (lineRecOn != null)
			if (lineRecOn.equals("true"))
			{
				String example = request.getParameter("example");
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
			for (String tc : topicContentTxt.split("|")) {
				String[] tmp = tc.split(":");
				list = new ArrayList<String>();
				for (String c : tmp[1].split(",")) 
					list.add(c);
				topicContents.put(tmp[0], list);
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
