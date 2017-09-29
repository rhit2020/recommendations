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
public class AggregateConfigManager {
    
	
	// database connection parameters
    public String dbstring;
    public String dbuser;
    public String dbpass;   
	public String relative_resource_path;


	private static String config_string = "./WEB-INF/aggregate_config.xml";

    public AggregateConfigManager(HttpServlet servlet) {
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
				
				
			}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
