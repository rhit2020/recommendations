package rec;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class RecConfigManager {
    
	/*
	 *  this is the variable that holds the threshold for rec.proactive recommendation of examples. 
	 *  only examples that have the recommendation scores of greater than the threshold will be sent to the Aggregate UI.
	 */
	// database connection parameters
    public String rec_dbstring;
    public String rec_dbuser;
    public String rec_dbpass;   
	//params in reactive recommendation
	public int reactive_max;
	public String reactive_method;
	public double reactive_threshold;
	//params in rec.proactive recommendation
	public int proactive_max;
	public String proactive_method;
	public double proactive_threshold;	
	//params for personalized reactive recommendation
	public int example_count_personalized_approach;
	public double  rec_interpolation_alpha;
	//params for resources folder
	public String relative_resource_path;
	
	//params in line recommendations
	public double line_threshold;
	public double line_max;
	public String relative_ans_path;


	private static String config_string = "./WEB-INF/rec_config.xml";

    public RecConfigManager(HttpServlet servlet) {
        try {
            ServletContext context = servlet.getServletContext();
            // System.out.println(context.getContextPath());
            InputStream input = context.getResourceAsStream(config_string);
			if (input != null) {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(input);
				doc.getDocumentElement().normalize();
				//resources path
				relative_resource_path = doc.getElementsByTagName("relative-resources-path").item(0)
						.getTextContent().trim().toLowerCase();
				// set database connection parameters
				rec_dbstring = doc.getElementsByTagName("rec_dbstring").item(0)
						.getTextContent().trim().toLowerCase();
				rec_dbuser = doc.getElementsByTagName("rec_dbuser").item(0)
						.getTextContent().trim().toLowerCase();
				rec_dbpass = doc.getElementsByTagName("rec_dbpass").item(0)
						.getTextContent().trim().toLowerCase();
				// set params in reactive recommendation
				try {
					reactive_max = Integer.parseInt(doc
							.getElementsByTagName("reactive_max").item(0)
							.getTextContent().trim());
				} catch (Exception e) {
					reactive_max = 3;
				}
				reactive_method = doc.getElementsByTagName("reactive_method")
						.item(0).getTextContent().trim();
				try {
					reactive_threshold = Double.parseDouble(doc
							.getElementsByTagName("reactive_threshold").item(0)
							.getTextContent().trim());
				} catch (Exception e) {
					reactive_threshold = 0.6;
				}
				// set params in rec.proactive recommendation
				try {
					proactive_max = Integer.parseInt(doc
							.getElementsByTagName("proactive_max").item(0)
							.getTextContent().trim());
				} catch (Exception e) {
					proactive_max = 3;
				}
				proactive_method = doc.getElementsByTagName("proactive_method")
						.item(0).getTextContent().trim();
				try {
					proactive_threshold = Double.parseDouble(doc
							.getElementsByTagName("proactive_threshold")
							.item(0).getTextContent().trim()); 
				} catch (Exception e) {
					proactive_threshold = 0.0;
				}
				// set params for personalized reactive recommendation
				try {
					rec_interpolation_alpha = Double.parseDouble(doc
							.getElementsByTagName("rec_interpolation_alpha")
							.item(0).getTextContent().trim());
				} catch (Exception e) {
					rec_interpolation_alpha = 0.0;
				}
				try {
					example_count_personalized_approach = Integer.parseInt(doc
							.getElementsByTagName(
									"example_count_personalized_approach")
							.item(0).getTextContent().trim());
				} catch (Exception e) {
					example_count_personalized_approach = 10;
				}
				try {
					line_threshold = Double.parseDouble(doc
							.getElementsByTagName(
									"rec_line_threshold")
							.item(0).getTextContent().trim());
				} catch (Exception e) {
					line_threshold = 0.1;
				}
				
				try {
					line_max = Double.parseDouble(doc
							.getElementsByTagName(
									"rec_line_max")
							.item(0).getTextContent().trim());
				} catch (Exception e) {
					line_max = 0.2;
				}
				relative_ans_path = doc.getElementsByTagName("relative-ans-path").item(0)
						.getTextContent().trim().toLowerCase();
			}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
