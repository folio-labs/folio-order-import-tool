package org.olf.folio.order.services;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject; 

public class ApiService {
	
	private static final Logger logger = Logger.getLogger(ApiService.class);
	private String tenant;

	public String getTenant() {
		return tenant;
	}

	public void setTenant(String tenant) {
		this.tenant = tenant;
	}

	public ApiService(String tenant) {
		this.tenant = tenant;
	}
	
	public String callApiGet(String url, String token) throws Exception, IOException, InterruptedException {
		CloseableHttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.get()
				.setUri(url)
				.setHeader("x-okapi-tenant", this.tenant)
				.setHeader("x-okapi-token", token)
				.setHeader("Accept", "application/json")
				.setHeader("content-type", "application/json")
				.build();

		HttpResponse response = client.execute(request);
		HttpEntity entity = response.getEntity();
		String responseString = EntityUtils.toString(entity, "UTF-8");
		int responseCode = response.getStatusLine().getStatusCode();

		logger.debug("GET:");
		logger.debug(url);
		logger.debug(responseCode);
		logger.debug(responseString);

		if (responseCode > 399) {
			logger.error("Failed GET");
			throw new Exception(responseString);
		}
		return responseString;

	}
	
	
	
    //POST TO PO SEEMS TO WANT UTF8 (FOR SPECIAL CHARS)
	//IF UTF8 IS USED TO POST TO SOURCE RECORD STORAGE
	//SPECIAL CHARS DON'T LOOK CORRECT 
	//TODO - combine the two post methods
	public String callApiPostWithUtf8(String url, JSONObject body, String token)
			throws Exception, IOException, InterruptedException {
		CloseableHttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.post()
				.setUri(url)
				.setHeader("x-okapi-tenant", this.tenant)
				.setHeader("x-okapi-token", token)
				.setEntity(new StringEntity(body.toString(),"UTF-8"))
				.setHeader("Accept", "application/json")
				.setHeader("content-type","application/json")
				.build();

		HttpResponse response = client.execute(request);
		HttpEntity entity = response.getEntity();
		String responseString = EntityUtils.toString(entity, "UTF-8");
		int responseCode = response.getStatusLine().getStatusCode();

		logger.debug("POST:");
		logger.debug(body.toString());
		logger.debug(url);
		logger.debug(responseCode);
		logger.debug(responseString);

		if (responseCode > 399) {
			logger.error("Failed POST");
			throw new Exception(responseString);
		}

		return responseString;

	}
	
	public String callApiPut(String url, JSONObject body, String token)
			throws Exception, IOException, InterruptedException {
		CloseableHttpClient client = HttpClients.custom().build();
		HttpUriRequest request = RequestBuilder.put()
				.setUri(url)
				.setCharset(Charset.defaultCharset())
				.setEntity(new StringEntity(body.toString(),"UTF8"))
				.setHeader("x-okapi-tenant", this.tenant)
				.setHeader("x-okapi-token", token)
				.setHeader("Accept", "application/json")
				.setHeader("Content-type","application/json")
				.build();
		
		//TODO
		//UGLY WORK-AROUND
		//THE ORDERS-STORAGE ENDOINT WANTS 'TEXT/PLAIN'
		//THE OTHER API CALL THAT USES PUT,
		//WANTS 'APPLICATION/JSON'
		if (url.contains("orders-storage") || url.contains("holdings-storage")) {
			request.setHeader("Accept","text/plain");
		}
		HttpResponse response = client.execute(request);
		int responseCode = response.getStatusLine().getStatusCode();

		logger.debug("PUT:");
		logger.debug(body.toString());
		logger.debug(url);
		logger.debug(responseCode);

		if (responseCode > 399) {
			logger.error("Failed PUT");
			throw new Exception("Response: " + responseCode);
		}
		return "ok";

	}
	
	public  String callApiAuth(String url,  JSONObject  body)
			throws Exception, IOException, InterruptedException {
		    CloseableHttpClient client = HttpClients.custom().build();
		    
		    HttpUriRequest request = RequestBuilder.post()
		    		.setUri(url)
		    		.setEntity(new StringEntity(body.toString()))
					.setHeader("x-okapi-tenant", this.tenant)
					.setHeader("Accept", "application/json").setVersion(HttpVersion.HTTP_1_1)
					.setHeader("content-type","application/json")
					.build();

		    CloseableHttpResponse response = client.execute(request);
			HttpEntity entity = response.getEntity();
			String responseString = EntityUtils.toString(entity);
			int responseCode = response.getStatusLine().getStatusCode();

			logger.debug("POST:");
			logger.debug(body.toString());
			logger.debug(url);
			logger.debug(responseCode);
			logger.debug(responseString);

			if (responseCode > 399) {
				logger.error("FAILED Authn");
				throw new Exception(responseString);
			}
			
			String token = response.getFirstHeader("x-okapi-token").getValue();
			return token;

	}

}
