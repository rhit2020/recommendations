package rec;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class StaticData {


	private static StaticData instance = null;
	private HashMap<String, ArrayList<String[]>> kcByContent;	// for each content there is an array list of kc (concepts) with id, weight (double) and direction (prerequisite/outcome)
	private HashMap<String, HashMap<Integer, ArrayList<String>>> exampleLineKC; //<example,<line,<list<concept>>>>
	private Map<String,List<String>> topicContentMap; //<topic,list<content>>
	private Map<String,List<Integer>> annotatedLines; //<example,list<annotated line indexs>>
	private Map<String,String> exampleType; //<example,type>>
	private Map<String,String> titleRdfMap; //Map<title,rdf>
	private Map<Integer,String> topicOrderMap;

	private StaticData(String domain, String group_id, String[] contentList, String realpath) {
		setupData(domain,group_id,contentList,realpath);
	}
	
	public static StaticData getInstance(String domain, String group_id, String[] contentList, String realpath) {		
		if (instance == null) {
			instance = new StaticData(domain,group_id,contentList,realpath);
		}
		return instance;
	}
	
	
	private void setupData(String domain, String grp, String[] contentList, String realpath) {
		//read topic contents
		readTopicContent(realpath+"/topic_content_ae.csv");
		//read contentKC
		if (domain.equals("java"))
			kcByContent = readContentKC(realpath+"/adjusted_direction_automatic_indexing.txt");
		else if (domain.equals("sql"))
			kcByContent = readContentKC(realpath+"/adjusted_direction_automatic_indexing_sql.txt");
		else
			kcByContent = getContentKCs(domain,contentList,"http://adapt2.sis.pitt.edu/aggregateUMServices/GetContentConcepts"); //content_name, arraylist of [concept_name, weight, direction]
		//read line concepts
		readLineConcepts(realpath+"/line_concept_java.csv");
		//read annotated lines
		readAnnotatedLines(realpath+"/annotated_line_index.csv");
		//read example type
		readExampleType(realpath+"/example_type.csv");
		//read title rdf
		readTitleRdf(realpath+"/title_rdfid.csv");
		//read topic order
		readTopicOrder(realpath+"/topic_order.csv");
	}

	private void readAnnotatedLines(String path) {
		annotatedLines = new HashMap<String,List<Integer>>(); 
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String example;
			int l;
			List<Integer> list;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				example = clmn[0];				
				l = Integer.parseInt(clmn[1]);
				if (annotatedLines.containsKey(example) == true){
					if (annotatedLines.get(example).contains(l)==false)
						annotatedLines.get(example).add(l);
				}
				else
				{
					list = new ArrayList<Integer>();
					list.add(l);
					annotatedLines.put(example,list);
				}
			}
		}catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}		
	}

	private static HashMap<String, ArrayList<String[]>> readContentKC(String path) {
		HashMap<String, ArrayList<String[]>> kcByContent = new HashMap<String, ArrayList<String[]>>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		ArrayList<String[]> list;
		String[] array;
		boolean isHeader = false; //currently the file has no header
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String title;
			String concept;
			String weight;
			String direction;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				title = clmn[0];
				concept = clmn[2];
				weight = clmn[3];			
				direction = clmn[4];
				if (kcByContent.containsKey(title))
				{
					list = kcByContent.get(title);
					array = new String[]{concept,weight,direction};
					list.add(array);
				}
				else
				{
					array = new String[]{concept,weight,direction};
					list = new ArrayList<String[]>();
					list.add(array);
					kcByContent.put(title,list);
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return kcByContent;
	}
	
	private static HashMap<String, ArrayList<String[]>> getContentKCs(String domain,
			String[] contentList, String contentKCURL) {
		HashMap<String, ArrayList<String[]>> res = null;
		try {
			String url = contentKCURL + "?domain=" + domain;
			//System.out.println(url);
			JSONObject json = readJsonFromUrl(url);
			//System.out.println("\n\n"+json.toString()+"\n\n");
			if (json.has("error")) {
				//System.out.println("HERE ");
				System.out
				.println("Error:[" + json.getString("errorMsg") + "]");
			} else {
				res = new HashMap<String, ArrayList<String[]>>();
				JSONArray contents = json.getJSONArray("content");

				for (int i = 0; i < contents.length(); i++) {
					JSONObject jsonobj = contents.getJSONObject(i);
					String content_name = jsonobj.getString("content_name");
					// if the content exist in the course
					if (Arrays.asList(contentList).contains(content_name)){
						//System.out.println(content_name);
						String conceptListStr = jsonobj.getString("concepts");
						ArrayList<String[]> conceptList;
						if (conceptListStr == null
								|| conceptListStr.equalsIgnoreCase("[null]")
								|| conceptListStr.length() == 0) {
							conceptList = null;
						} else {
							conceptList = new ArrayList<String[]>();
							String[] concepts = conceptListStr.split(";");
							for (int j = 0; j < concepts.length; j++) {
								String[] concept = concepts[j].split(",");
								conceptList.add(concept); // concept_name, weight, direction
								//System.out.println("  " + concept[0] + " " + concept[1] + " " + concept[2]);
							}
						}
						res.put(content_name, conceptList);                        
					}
					// System.out.println(jsonobj.getString("name"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return res;
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

	private void readTopicContent(String path) {
		topicContentMap = new HashMap<String,List<String>>(); 
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String topic;
			String content;
			List<String> list;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				topic = clmn[0];				
				content = clmn[1];
				if (topicContentMap.containsKey(topic) == true)
				{
					list = topicContentMap.get(topic);
					if (list.contains(content) == false)
						list.add(content);									
				}
				else
				{
					list = new ArrayList<String>();
					list.add(content);
					topicContentMap.put(topic,list);
				}
			}
		}catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}		
	}

	private void readExampleType(String path) {
		exampleType = new HashMap<String,String>(); 
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				exampleType.put(clmn[0], clmn[1]);
			}
		}catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}		
	}
	
	private void readLineConcepts(String path) {
		exampleLineKC = new HashMap<String, HashMap<Integer, ArrayList<String>>>(); 
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String example;
			String concept;
			int sline;
			HashMap<Integer, ArrayList<String>> lc_list;
			ArrayList<String> c_list;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				example = clmn[0];				
				concept = clmn[1];
				sline = Integer.parseInt(clmn[2]);
				if (exampleLineKC.containsKey(example) == true)
				{
					lc_list = exampleLineKC.get(example);
					if (lc_list.containsKey(sline) == false)
					{
						c_list = new ArrayList<String>();
						c_list.add(concept);
						lc_list.put(sline,c_list);
					}
					else{
						if (lc_list.get(sline).contains(concept)==false)
							lc_list.get(sline).add(concept);
					}					
				}
				else
				{
					c_list = new ArrayList<String>();
					c_list.add(concept);
					lc_list = new HashMap<Integer, ArrayList<String>>();
					lc_list.put(sline,c_list);
					exampleLineKC.put(example, lc_list);
				}
			}
		}catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}	
//		//TODO for rec.test --remove print
//		int count = 0;
//		for (HashMap<Integer, ArrayList<String>> l : exampleLineKC.values())
//			for (int i : l.keySet())
//				count += l.get(i).size();
//		System.out.println("exampleLineKC: "+count);

	}

	private void readTitleRdf(String path) {
		titleRdfMap = new HashMap<String,String>();
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String title;
			String rdfid;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				title = clmn[0];
				rdfid =clmn[1];	
				titleRdfMap.put(title,rdfid);
			}	 
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
	}
	
	private void readTopicOrder(String path) {
		topicOrderMap = new HashMap<Integer,String>(); 
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = ",";
		boolean isHeader = true;
		try {
			br = new BufferedReader(new FileReader(path));
			String[] clmn;
			String topic;
			int order;
			while ((line = br.readLine()) != null) {
				if (isHeader)
				{
					isHeader = false;
					continue;
				}
				clmn = line.split(cvsSplitBy);
				order = Integer.parseInt(clmn[0]);
				topic = clmn[1];				
				topicOrderMap.put(order, topic);
			}
		}catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}		
	}

	public HashMap<String, ArrayList<String[]>> getKcByContent() {
		return kcByContent;
	}
	

	public HashMap<String, HashMap<Integer, ArrayList<String>>> getExampleLineKC() {
		return exampleLineKC;
	}

	public Map<String, List<String>> getTopicContentMap() {
		return topicContentMap;
	}

	public Map<String, List<Integer>> getAnnotatedLines() {
		return annotatedLines;
	}
	
	public Map<String, String> getExampleType() {
		return exampleType;
	}
	
	
	public Map<Integer, String> getTopicOrder() {
		return topicOrderMap;
	}
	
	public Map<String, String> getTitleRdfMap() {
		return titleRdfMap;
	}
	
}
