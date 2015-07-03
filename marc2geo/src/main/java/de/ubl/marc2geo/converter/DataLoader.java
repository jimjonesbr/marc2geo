package de.ubl.marc2geo.converter;

import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import de.ulb.marc2geo.core.GlobalSettings;
import de.ulb.marc2geo.core.MapRecord;
import de.ulb.marc2geo.infrastructure.MySQLConnector;

public class DataLoader {

	private ResultSet loadData(){

		Statement statement = null;
		ResultSet result = null;
		MySQLConnector cnn = new MySQLConnector();

		try {

			statement =  cnn.getConnection().createStatement();
			result = statement.executeQuery("select * from transfer.KATALOG");

		} catch (SQLException e) {

			e.printStackTrace();
		}

		return result;

	}


	public ArrayList<MapRecord> getMaps(){

		ArrayList<MapRecord> result = new ArrayList<MapRecord>();
		ResultSet data = this.loadData();


		try {

			while (data.next()) {

				MapRecord map = new MapRecord();

				map = parseMAR21(data.getString("rawxml"));

				//map.setTitle(data.getString("title"));
				//map.setGeometry(data.getString("rawxml"));			

				result.add(map);

			}

		} catch (SQLException e) {
			e.printStackTrace();
		}


		return result;
	}


	private MapRecord parseMAR21(String marc21) {

		MapRecord result = new MapRecord();

		/**
		Required fields:

		- Map URI
		- Map title
		- Map scale
		- Map geometry 
		- Map image
		- Map year
		- Map description

		 */


		try {

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();

			InputSource source = new InputSource(new StringReader(marc21));

			Document doc = builder.parse(source);
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();


			/**
			 * Map Title
			 */
			XPathExpression expr = xpath.compile("//record/datafield[@tag='245']/subfield[@code='a']");
			NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

			if(nl.getLength()!=0){
				Node currentItem = nl.item(0);
				//String key = currentItem.getAttributes().getNamedItem("code").getNodeValue();			    
				result.setTitle(currentItem.getTextContent());
			} else {
				result.setTitle("No Title");
			}

			/**
			 * Map URI
			 */
			expr = xpath.compile("//record/datafield[@tag='776']/subfield[@code='w']");
			nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

			if(nl.getLength()!=0){
				Node currentItem = nl.item(0);		    
				result.setUri(GlobalSettings.getBaseURI() + currentItem.getTextContent().replace("(", "").replace(")",""));
			} else {
				result.setUri("No URI");
			}

			/**
			 * Map ID
			 */
			expr = xpath.compile("//record/controlfield[@tag='001']");
			nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

			if(nl.getLength()!=0){
				Node currentItem = nl.item(0);		    
				result.setId(currentItem.getTextContent());
			} else {
				result.setId("No ID");
			}

			/**
			 * Map Description
			 */
			expr = xpath.compile("//record/datafield[@tag='245']/subfield[@code='c']");
			nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

			if(nl.getLength()!=0){
				Node currentItem = nl.item(0);
				result.setDescription(currentItem.getTextContent());
			} else {
				result.setDescription("No Description");
			}

			/**
			 * Map Geometry
			 */
			expr = xpath.compile("//record/datafield[@tag='245']/subfield[@code='n']");
			nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

			if(nl.getLength()!=0){

				Node currentItem = nl.item(0);

				if(currentItem.getTextContent().contains("Rechts")){
					
					String rechts = currentItem.getTextContent().substring(0,currentItem.getTextContent().indexOf("R")).trim();
					String latitudeSouthEast = rechts.split(" ")[1];
					String longitudeSouthEast = rechts.split(" ")[0];

					String hoch = currentItem.getTextContent().substring(currentItem.getTextContent().indexOf("s")+1,currentItem.getTextContent().indexOf("H")).trim();
					String latitudeNorthWest = hoch.split(" ")[1];
					String longitudeNorthWest = hoch.split(" ")[0];

					String wkt = "POLYGON((" + hoch + "," + longitudeNorthWest + " " + latitudeSouthEast + "," + rechts + "," + longitudeSouthEast + " " + latitudeNorthWest + "," + hoch + "))"; 

					result.setGeometry(wkt);
					
				} else {
					
					result.setGeometry("Invalid Coordinates");
				}

				

			} else {
				result.setGeometry("No Geometry");
			}

			/**
			 * Map Size
			 */
			expr = xpath.compile("//record/datafield[@tag='300']/subfield[@code='c']");
			nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

			if(nl.getLength()!=0){
				Node currentItem = nl.item(0);
				result.setMapSize(currentItem.getTextContent());				
			}else {
				result.setMapSize("No Scale");
			}

			/**
			 * Map Year
			 */
			expr = xpath.compile("//record/datafield[@tag='260']/subfield[@code='c']");
			nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

			if(nl.getLength()!=0){
				Node currentItem = nl.item(0);
				result.setYear(currentItem.getTextContent());				
			}else {
				result.setYear("No Year");
			}


			/**
			 * Map Image
			 */
			expr = xpath.compile("//record/datafield[@tag='????']/subfield[@code='????']");
			nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

			if(nl.getLength()!=0){
				Node currentItem = nl.item(0);
				result.setImage(currentItem.getTextContent());				
			}else {
				result.setImage(GlobalSettings.getNoImageURL());
			}

			
			/**
			 * Map Scale
			 */
			expr = xpath.compile("//record/datafield[@tag='????']/subfield[@code='????']");
			nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

			if(nl.getLength()!=0){
				Node currentItem = nl.item(0);
				result.setScale(currentItem.getTextContent());				
			}else {
				result.setScale("0:0000");
			}			
			

		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
		}

		return result;
	}


	public String getSPARQLInsert(MapRecord map){
				
		String SPARQLinsert = "\nINSERT DATA {\n " ;
			   SPARQLinsert = SPARQLinsert + "    GRAPH <" + GlobalSettings.getGraphBaseURI() + map.getId() + "> {\n" ;
			   SPARQLinsert = SPARQLinsert + "           <" + map.getUri() + "> a <http://www.geographicknowledge.de/vocab/maps#Map> . \n" ;
			   SPARQLinsert = SPARQLinsert + "           <" + map.getUri() + "> <http://www.geographicknowledge.de/vocab/maps#medium> <http://www.geographicknowledge.de/vocab/maps#Paper> . \n" ;
			   SPARQLinsert = SPARQLinsert + "           <" + map.getUri() + "> <http://www.geographicknowledge.de/vocab/maps#digitalImageVersion> <" + map.getImage() + ">. \n" ;
			   SPARQLinsert = SPARQLinsert + "           <" + map.getUri() + "> <http://www.geographicknowledge.de/vocab/maps#mapSize> '" + map.getMapSize() + "'^^<http://www.w3.org/2001/XMLSchema#string> . \n" ;
			   SPARQLinsert = SPARQLinsert + "           <" + map.getUri() + "> <http://www.geographicknowledge.de/vocab/maps#title> '" + map.getTitle() + "'^^<http://www.w3.org/2001/XMLSchema#string> . \n" ;
			   SPARQLinsert = SPARQLinsert + "           <" + map.getUri() + "> <http://www.geographicknowledge.de/vocab/maps#presentation> <" + map.getUri() + "> . \n" ;
			   SPARQLinsert = SPARQLinsert + "           <" + map.getUri() + "> <http://www.geographicknowledge.de/vocab/maps#mapsTime> _:TIME_" + map.getId() + " . \n" ;
			   SPARQLinsert = SPARQLinsert + "           _:TIME_" + map.getId() + " a <http://www.w3.org/2006/time#Instant> . \n" ;
			   SPARQLinsert = SPARQLinsert + "           _:TIME_" + map.getId() + " <http://www.w3.org/2001/XMLSchema#gYear> '" + map.getYear() + "'^^<http://www.w3.org/2001/XMLSchema#gYear> .\n" ;
			   SPARQLinsert = SPARQLinsert + "           <" + map.getUri() + "> <http://www.geographicknowledge.de/vocab/maps#hasScale> '" + map.getScale() + "'^^<http://www.w3.org/2001/XMLSchema#string>. \n" ;
			   SPARQLinsert = SPARQLinsert + "           <" + map.getUri() + "> <http://www.geographicknowledge.de/vocab/maps#mapsArea> _:GEOMETRY_" + map.getId() + " .\n";
			   SPARQLinsert = SPARQLinsert + "           _:GEOMETRY_" + map.getId() + " a <http://www.opengis.net/ont/geosparql/1.0#Geometry> . \n" ;			   
			   SPARQLinsert = SPARQLinsert + "           _:GEOMETRY_" + map.getId() + " <http://www.opengis.net/ont/geosparql/1.0#asWKT> '"+ map.getGeometry() +"'^^<http://www.opengis.net/ont/sf#wktLiteral> . \n" ;
			   SPARQLinsert = SPARQLinsert + "           <" + map.getUri() + "> <http://purl.org/dc/terms/description> '" + map.getDescription() + "'^^<http://www.w3.org/2001/XMLSchema#string> . \n" ;
			   SPARQLinsert = SPARQLinsert + "    } \n" ;
			   SPARQLinsert = SPARQLinsert + "} \n" ;

		return SPARQLinsert;

	}

	private void storeTriples(String sparql){

		//TODO: Create function to store triples into Parliament.

	}
}
