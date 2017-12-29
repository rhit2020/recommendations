package rec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Servlet implementation class GetUserActivity
 */
@WebServlet("/GetUserActivity")
public class GetUserActivity extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static String server = "http://adapt2.sis.pitt.edu";
	public static String examplesActivityServiceURL = server
	+ "/aggregateUMServices/GetExamplesActivity";
	public static String questionsActivityServiceURL_QJ = server
	+ "/aggregateUMServices/GetQJActivity";
	public static String questionsActivityServiceURL_SK = server
			+ "/aggregateUMServices/GetSKActivity";
	public static String pcrsActivityServiceURL = server
			+ "/aggregateUMServices/GetPCRSActivity";
	public static String pcexChallengeActivityURL = server
			+ "/aggregateUMServices/GetPCEXChallengeActivity";
	public static String pcexExampleActivityURL = server
			+ "/aggregateUMServices/GetPCEXExampleActivity";

    public GetUserActivity() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	// CALLING A UM SERVICE
		public static HashMap<String, String[]> getUserExamplesActivity(String usr,String domain) {
			HashMap<String, String[]> eActivity = new HashMap<String, String[]>();
			try {
				String url = examplesActivityServiceURL;
				
				String serviceParamJSON = "{\n    \"usr\" : \""+usr+"\"\n}";
	    		JSONObject json = callService(url,serviceParamJSON);

				if (json.has("error")) {
					System.out.println("Error:[" + json.getString("errorMsg") + "]");
				} else {
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
		
		public static HashMap<String, String[]> getUserQuestionsActivity(String usr,
				String grp, String domain, String[] contentList) {
			String providerId = "";
			String url = "";
			if (domain.equals("java")) {
				url = questionsActivityServiceURL_QJ;
				providerId = "quizjet";
			} else if (domain.equals("sql")) {
				url = questionsActivityServiceURL_SK;
				providerId = "sqlknot";
			}
			return getUserActivityReport(usr, grp, domain, contentList, providerId, url);
		}

		public static HashMap<String, String[]> getUserActivityReport(String usr,
				String grp, String domain, String[] contentList, 
				String providerId, String url) {
			HashMap<String, String[]> qActivity = new HashMap<String, String[]>();
			
			try {
				
				if (providerId.equals("") == false & url.equals("") == false)
				{
					
					String contentStr = "";
					for (String str : contentList)
					{
						contentStr += "\"" + str + "\",";
					}
					if (contentStr.length() > 0)
						contentStr = contentStr.substring(0, contentStr.length()-1);
					
					String serviceParamJSON = "{\n    \"user-id\" : \""+usr+"\",\n    \"group-id\" : \""+grp+"\",\n    \"domain\" : \""+domain+"\",\n    \"content-list-by-provider\" : [  \n";
		    		serviceParamJSON += "        {\"provider-id\" : \""+ providerId +"\", \"content-list\" : ["+contentStr+"]},\n";
		    	
		    		serviceParamJSON = serviceParamJSON.substring(0,serviceParamJSON.length()-2);
		    		serviceParamJSON += "\n    ]\n}";	
					
					JSONObject json = callService(url,serviceParamJSON);

					if (json.has("error")) {
						System.out
						.println("Error:[" + json.getString("errorMsg") + "]");
					} else {
						JSONArray activity = json.getJSONArray("content-list");

						for (int i = 0; i < activity.length(); i++) {
							JSONObject jsonobj = activity.getJSONObject(i);
							String[] act = new String[4];
							act[0] = jsonobj.getString("content-id");
							act[1] = jsonobj.getDouble("attempts") + "";
							act[2] = jsonobj.getDouble("progress") + "";
							act[3] = jsonobj.getDouble("success-rate") + "";
							System.out.println(act[0]+"   "+ act[2]);
							qActivity.put(act[0], act);
							// System.out.println(jsonobj.getString("name"));
						}
					}
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return qActivity;
		}

		

	    private static JSONObject callService(String url, String json){
	    	InputStream in = null;
	    	JSONObject jsonResponse = null;
			// A JSON object is created to pass the required parameter to the recommendation service implemented by GetRecommendations.java
			try {
				HttpClient client = new HttpClient();
	            PostMethod method = new PostMethod(url);
	            method.setRequestBody(json);
	            method.addRequestHeader("Content-type", "application/json");
	            //System.out.println("Calling service "+url);
	            int statusCode = client.executeMethod(method);

	            if (statusCode != -1) {
	            	
	                in = method.getResponseBodyAsStream();
	                jsonResponse =  readJsonFromStream(in);
	                in.close();
	            }else{
	            	
	            }
			}catch(Exception e){}
			return jsonResponse;
	    }
	    
	    public static JSONObject readJsonFromStream(InputStream is)  throws Exception{
			JSONObject json = null;
			try {
				BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
				String jsonText = readAll(rd);
				json = new JSONObject(jsonText);
			}catch(Exception e){
				e.printStackTrace();
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
		
}
