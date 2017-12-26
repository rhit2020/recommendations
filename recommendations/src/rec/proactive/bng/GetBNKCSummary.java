package rec.proactive.bng;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.json.JSONArray;
import org.json.JSONObject;

public class GetBNKCSummary {

	public static String server = "http://adapt2.sis.pitt.edu";
	public static String bnServiceURL = server + "/bn_general/GetStudentModel";

	public static HashMap<String, Double> getItemKCEstimates(String usr, String grp, String lastAct, double lastActRes,
			String[] contentList) {

		HashMap<String, Double> itemKCEstimates = new HashMap<String, Double>();

		try {
			JSONObject json = callService(bnServiceURL, usr, grp, lastAct, lastActRes, contentList);

			if (json.has("error")) {
				System.out.println("Error:[" + json.getString("errorMsg") + "]");
			} else {
				JSONArray activity = json.getJSONArray("item-kc-estimates");

				String name;
				double probability;
				for (int i = 0; i < activity.length(); i++) {
					JSONObject jsonobj = activity.getJSONObject(i);
					name = jsonobj.getString("name");
					probability = jsonobj.getDouble("p");
					itemKCEstimates.put(name, probability);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return itemKCEstimates;
	}

	private static JSONObject callService(String url, String usr, String grp, String lastAct, double lastActRes,
			String[] contentList) {
		InputStream in = null;
		JSONObject jsonResponse = null;
		// A JSON object is created to pass the required parameter to the
		// recommendation service implemented by GetRecommendations.java
		try {
			HttpClient client = new HttpClient();
			PostMethod method = new PostMethod(url);
			method.addParameter("usr", URLEncoder.encode(usr, "UTF-8"));
			method.addParameter("grp", URLEncoder.encode(grp, "UTF-8"));
			method.addParameter("lastContentId", URLEncoder.encode(lastAct, "UTF-8"));
			method.addParameter("lastContentResult", URLEncoder.encode(Double.toString(lastActRes), "UTF-8"));
			method.addParameter("contents", getContents(contentList));

			// System.out.println("Calling service "+url);
			int statusCode = client.executeMethod(method);

			if (statusCode != -1) {

				in = method.getResponseBodyAsStream();
				jsonResponse = readJsonFromStream(in);
				in.close();
			} else {

			}
		} catch (Exception e) {
		}
		return jsonResponse;
	}

	private static String getContents(String[] contentList) {
		String contents = "";
		for (String c : contentList)
			contents += c + ",";
		if (contents.length() > 0)
			contents = contents.substring(0, contents.length() - 1); // this is
																		// for
																		// ignoring
																		// the
																		// last
																		// ,
		return contents;
	}

	public static JSONObject readJsonFromStream(InputStream is) throws Exception {
		JSONObject json = null;
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			json = new JSONObject(jsonText);
		} catch (Exception e) {
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
