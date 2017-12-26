package rec;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@WebServlet("/GetKCSummary")
public class GetKCSummary extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static String server = "http://adapt2.sis.pitt.edu";
	private static String conceptLevelsServiceURL = server + "/cbum/ReportManager";

       
    public GetKCSummary() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	// CALLING A UM SERVICE: //GET THE LEVELS OF KNOWLEDGE OF THE USER IN CONCEPTS
	// FROM USER MODEL USING THE USER MODEL INTERFACE
	public static HashMap<String, double[]> getConceptLevels(String usr, String domain,
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
}
