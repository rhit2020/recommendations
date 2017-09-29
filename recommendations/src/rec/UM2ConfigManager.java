package rec;
import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
/**
 * 
 * @author sherry
 *
 */
public class UM2ConfigManager {
    
	
	// database connection parameters
    public String dbstring;
    public String dbuser;
    public String dbpass;   
    public String fastdbstring;
    public String webex_appID;
    public String quizjet_appID;
    public String sqlnot_appID;
    public String animated_examples_appID;
	public String relative_resource_path;


	private static String config_string = "./WEB-INF/um2_config.xml";

    public UM2ConfigManager(HttpServlet servlet) {
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
				dbstring = doc.getElementsByTagName("dbstring").item(0)
						.getTextContent().trim().toLowerCase();
				dbuser = doc.getElementsByTagName("dbuser").item(0)
						.getTextContent().trim().toLowerCase();
				dbpass = doc.getElementsByTagName("dbpass").item(0)
						.getTextContent().trim().toLowerCase();
				fastdbstring = doc.getElementsByTagName("fastdbstring").item(0)
				.getTextContent().trim().toLowerCase();
				webex_appID = doc.getElementsByTagName("webex").item(0)
				.getTextContent().trim().toLowerCase();
				sqlnot_appID = doc.getElementsByTagName("sqlnot").item(0)
				.getTextContent().trim().toLowerCase();
				animated_examples_appID = doc.getElementsByTagName("animated_examples").item(0)
				.getTextContent().trim().toLowerCase();
				quizjet_appID = doc.getElementsByTagName("quizjet").item(0)
				.getTextContent().trim().toLowerCase();
				
			}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
