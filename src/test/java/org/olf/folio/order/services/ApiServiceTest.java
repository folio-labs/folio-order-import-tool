package org.olf.folio.order.services;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.olf.folio.order.services.ApiService;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;
import java.util.List;

public class ApiServiceTest {
	private String apiUsername;
	private String apiPassword;
	private String tenant;
	private String baseOkapEndpoint;
	private String token;
	

	public ApiServiceTest() {
		// TODO Auto-generated constructor stub
	} 
	
	@Test
	public void nullTest() {
		//init();
		//System.out.println("apiUsername: "+ this.apiUsername);
		//System.out.println("tenant: "+ this.tenant);
		//System.out.println("baseOkapEndpoint: "+ this.baseOkapEndpoint);
	}
	
	@Ignore
	public void testCallApiAuth() {
		init();
		
		
	}
	
	@Ignore
	public void testLookupVendor() {
		init();
		ApiService service = new ApiService(this.tenant);
		String vendorCode = "ALX";
		String organizationEndpoint = this.baseOkapEndpoint + "organizations-storage/organizations?limit=30&offset=0&query=((code='" + vendorCode + "'))";
		try {
			String orgLookupResponse = service.callApiGet(organizationEndpoint,  this.token);
			JSONObject orgObject = new JSONObject(orgLookupResponse);
			String vendorId = (String) orgObject.getJSONArray("organizations").getJSONObject(0).get("id");
			System.out.println("vendorId: "+ vendorId);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Ignore
	public void testGetPONumber() {
	    init();
	    ApiService service = new ApiService(this.tenant);
	    String poNumber = "";
		try {
			poNumber = service.callApiGet(baseOkapEndpoint + "orders/po-number", token);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    JSONObject poNumberObj = new JSONObject(poNumber);
	    System.out.println("NEXT PO NUMBER: " + poNumberObj.get("poNumber"));
	}
	
	@Test
	public void testGetLocations() {
	    init();
	    ApiService service = new ApiService(this.tenant);
	    String endpoint = this.baseOkapEndpoint + "locations?limit=10000";
	    String result = new String();
		try {
			result = service.callApiGet(endpoint, token);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		JsonObject jsonObj = new JsonObject(result);
		 
		JsonArray locations = jsonObj.getJsonArray("locations");
		System.out.println("size: "+locations.size());
	     
	    for (int i=0; i < locations.size(); i++)	{
	    	JsonObject obj = locations.getJsonObject(i);
	    	boolean isActive = obj.getBoolean("isActive");
	    	if (isActive) {
	    	    System.out.println("name: "+ obj.getString("name"));
	    	    System.out.println("code: "+ obj.getString("code"));
	    	    System.out.println();
	    	}
	    }
	    
	}
	
	
	
	
	
	
	protected void init() {
		PropertiesConfiguration props = new PropertiesConfiguration();
		try {
		    props.load(ClassLoader.getSystemResourceAsStream("application.properties"));
		    this.apiUsername = (String) props.getProperty("okapi_username");
		    this.apiPassword = (String) props.getProperty("okapi_password");
		    this.tenant = (String) props.getProperty("tenant");
		    this.baseOkapEndpoint = (String) props.getProperty("baseOkapEndpoint");
		    
		} catch (ConfigurationException e) {
			System.err.println("exception caught");
		    throw new RuntimeException(e);
		}
        ApiService service = new ApiService(this.tenant);
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("username", this.apiUsername);
		jsonObject.put("password", this.apiPassword);
		jsonObject.put("tenant",tenant);
		try {
			String endpoint = this.baseOkapEndpoint + "authn/login";
			//System.out.println(endpoint);
			this.token = service.callApiAuth( endpoint,  jsonObject);
			//System.out.println("token: "+ this.token);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		
	}
	
	
}
