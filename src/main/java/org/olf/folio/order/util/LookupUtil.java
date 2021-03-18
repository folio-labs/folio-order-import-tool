package org.olf.folio.order.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject; 
import org.olf.folio.order.services.ApiService;

public class LookupUtil {
	
	private static final Logger logger = Logger.getLogger(LookupUtil.class);
	private String baseOkapEndpoint; 
	private List<String> referenceTables = new ArrayList<String>();
	private ApiService apiService;

	public String getBaseOkapEndpoint() {
		return baseOkapEndpoint;
	}

	public void setBaseOkapEndpoint(String baseOkapEndpoint) {
		this.baseOkapEndpoint = baseOkapEndpoint;
	}

	public List<String> getReferenceTables() {
		return referenceTables;
	}

	public void setReferenceTables(List<String> referenceTables) {
		this.referenceTables = referenceTables;
	}

	public ApiService getApiService() {
		return apiService;
	}

	public void setApiService(ApiService apiService) {
		this.apiService = apiService;
	}

	public LookupUtil() { 
		//
	}
	
	public void load() {
		logger.info("set endpoints");
		setEndPoint("identifier-types", "1000");
		setEndPoint("contributor-types", "1000");
		setEndPoint("classification-types", "1000");
		setEndPoint("contributor-types", "1000");
		setEndPoint("contributor-name-types", "1000");
		setEndPoint("locations", "10000");
		setEndPoint("loan-types", "1000");
		setEndPoint("note-types", "1000");
		setEndPoint("material-types", "1000");
		setEndPoint("instance-types", "1000");
		setEndPoint("holdings-types", "1000"); 
	}
	
	public void setEndPoint(String type, String limit) { 
		this.referenceTables.add(this.baseOkapEndpoint + type + "?limit="+ limit);
	}
	
	public HashMap<String,String> getReferenceValues(String token ) throws IOException, InterruptedException, Exception  {
		
		Map<String, String> table = new HashMap<String,String>();

		Iterator<String> refTablesIterator = this.referenceTables.iterator();
		while (refTablesIterator.hasNext()) {
			
			String endpoint = refTablesIterator.next();
			logger.info("calling "+ endpoint);
			String response = this.apiService.callApiGet(endpoint, token); 
			 
			JSONObject jsonObject = new JSONObject(response);
			//System.out.println(jsonObject.toString(3));
			
			//TODO - IMPROVE THIS
			//(0) IS THE NUMBER OF ITEMS FOUND
			//(1) IS THE ARRAY OF ITEMS
			//NOT A GOOD APPROACH
			String elementName = (String) jsonObject.names().get(1);	
			JSONArray elements = jsonObject.getJSONArray(elementName);
			Iterator elementsIterator = elements.iterator();
			while (elementsIterator.hasNext()) {
				JSONObject element = (JSONObject) elementsIterator.next();
				String id = element.getString("id");
				String name = element.getString("name");
				if (endpoint.contains("locations")) name = name + "-location";
				//SAVING ALL OF THE 'NAMES' SO THE UUIDs CAN BE LOOKED UP
				 
				//logger.info("lookupTable name: "+ name);
				table.put(name,id);		
			}
		}
		logger.debug("finished loading lookup table");

		return (HashMap<String, String>) table;
	}
	
	

}
