package org.olf.folio.order.storage;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.olf.folio.order.Config;
import org.olf.folio.order.dataobjects.JsonDataObject;

import java.nio.charset.Charset;

public class FolioAccess {

  protected static String token;
  protected static Logger logger;

  /**
   * Capture configuration from import.properties, authenticate to FOLIO and retrieve reference data
   * @param logger Logger.
   * @throws Exception If FOLIO back-end request fails.
   */
  public static void initialize(Logger logger) throws Exception {
    //GET THE FOLIO TOKEN
    FolioAccess.logger = logger;
    token = authenticate();
  }

  private static String authenticate()
          throws Exception {
    JSONObject authObject = new JSONObject();
    authObject.put("username", Config.apiUsername);
    authObject.put("password", Config.apiPassword);
    authObject.put("tenant", Config.tenant);
    CloseableHttpClient client = HttpClients.custom().build();
    String authUrl = Config.baseOkapiEndpoint + "authn/login";
    HttpUriRequest request = RequestBuilder.post()
            .setUri(authUrl)
            .setEntity(new StringEntity(authObject.toString()))
            .setHeader("x-okapi-tenant",Config.tenant)
            .setHeader("Accept", "application/json").setVersion(HttpVersion.HTTP_1_1)
            .setHeader("content-type","application/json")
            .build();

    CloseableHttpResponse response = client.execute(request);
    HttpEntity entity = response.getEntity();
    String responseString = EntityUtils.toString(entity);
    int responseCode = response.getStatusLine().getStatusCode();

    logger.info("POST auth:");
    logger.info("User:   " + authObject.get("username"));
    logger.info("Tenant: " + authObject.get("tenant"));
    logger.debug(authUrl);
    logger.info("Response code: " + responseCode);
    try {
      JSONObject json = new JSONObject(responseString);
      json.put("password","*******");
      logger.debug(json);
    } catch (JSONException je) {
      logger.info(responseString);
    }

    if (responseCode > 399) {
      throw new Exception(responseString);
    }

    FolioAccess.token = response.getFirstHeader("x-okapi-token").getValue();
    return token;
  }

  public static JSONObject callApiGetById (String apiPath, String id) throws Exception {
    return callApiGet(apiPath + "/" + id);
  }

  public static JSONObject callApiGet(String apiPath) throws Exception {
    CloseableHttpClient client = HttpClients.custom().build();
    HttpUriRequest request = RequestBuilder.get().setUri(Config.baseOkapiEndpoint + apiPath)
            .setHeader("x-okapi-tenant", Config.tenant)
            .setHeader("x-okapi-token", token)
            .setHeader("Accept", "application/json")
            .setHeader("content-type","application/json")
            .build();

    HttpResponse response = client.execute(request);
    HttpEntity entity = response.getEntity();
    String responseString = EntityUtils.toString(entity, "UTF-8");
    int responseCode = response.getStatusLine().getStatusCode();

    logger.debug("GET:");
    logger.debug(apiPath);
    logger.debug(responseCode);
    logger.debug(responseString);

    if (responseCode > 399) {
      throw new Exception(responseString);
    }
    return new JSONObject(responseString);
  }

  public static JSONArray callApiGetArray (String url, String nameOfArray) throws Exception {
    try {
      return callApiGet(url).getJSONArray(nameOfArray);
    } catch (ClassCastException cce) {
      logger.error("GET result from " + url + " did not return an array by the name " + nameOfArray);
      return null;
    }
  }

  public static JSONObject callApiGetFirstObjectOfArray (String url, String nameOfArray) throws Exception {
    try {
      JSONArray array = callApiGet(url).getJSONArray(nameOfArray);
      if (array != null && !array.isEmpty()) {
        return array.getJSONObject(0);
      } else {
        return null;
      }
    } catch (ClassCastException cce) {
      logger.error("GET result from " + url + " did not return an array by the name " + nameOfArray);
      return null;
    }
  }

  public static String callApiPut(String apiPath, JsonDataObject object) throws Exception {
    return callApiPut(apiPath + "/" + object.getId(), object.asJson());
  }

  public static String callApiPut(String uri, JSONObject body)
          throws Exception {
    CloseableHttpClient client = HttpClients.custom().build();
    HttpUriRequest request = RequestBuilder.put()
            .setUri(Config.baseOkapiEndpoint + uri)
            .setCharset(Charset.defaultCharset())
            .setEntity(new StringEntity(body.toString(),"UTF8"))
            .setHeader("x-okapi-tenant", Config.tenant)
            .setHeader("x-okapi-token", token)
            .setHeader("Accept", "application/json")
            .setHeader("Content-type","application/json")
            .build();

    //THE ORDERS-STORAGE ENDPOINT WANTS 'TEXT/PLAIN'
    //THE OTHER API CALL THAT USES PUT,
    //WANTS 'APPLICATION/JSON'
    if (uri.contains("orders-storage") || uri.contains("holdings-storage")) {
      request.setHeader("Accept","text/plain");
    }
    HttpResponse response = client.execute(request);
    int responseCode = response.getStatusLine().getStatusCode();

    logger.info("PUT:");
    logger.info(body.toString());
    logger.info(uri);
    logger.info(responseCode);
    //logger.info(responseString);

    if (responseCode > 399) {
      throw new Exception("Response: " + responseCode);
    }

    return "ok";
  }

  public static JSONObject callApiPostWithUtf8(String apiPath, JsonDataObject object) throws Exception {
    return callApiPostWithUtf8(apiPath, object.asJson());
  }

  //POST TO PO SEEMS TO WANT UTF8 (FOR SPECIAL CHARS)
  //IF UTF8 IS USED TO POST TO SOURCE RECORD STORAGE
  //SPECIAL CHARS DON'T LOOK CORRECT
  public static JSONObject callApiPostWithUtf8(String apiPath, JSONObject body)
          throws Exception {
    CloseableHttpClient client = HttpClients.custom().build();
    HttpUriRequest request = RequestBuilder.post()
            .setUri(Config.baseOkapiEndpoint + apiPath)
            .setHeader("x-okapi-tenant", Config.tenant)
            .setHeader("x-okapi-token", token)
            .setEntity(new StringEntity(body.toString(),"UTF-8"))
            .setHeader("Accept", "application/json")
            .setHeader("content-type","application/json")
            .build();

    HttpResponse response = client.execute(request);
    HttpEntity entity = response.getEntity();
    String responseString = EntityUtils.toString(entity, "UTF-8");
    int responseCode = response.getStatusLine().getStatusCode();

    logger.info("POST:");
    logger.info(body.toString());
    logger.info(apiPath);
    logger.info(responseCode);
    logger.info(responseString);

    if (responseCode > 399) {
      throw new Exception(responseString);
    }
    return new JSONObject(responseString);
  }

}
