package org.olf.folio.order.dataobjects;

import org.json.JSONArray;

public class Note extends JsonDataObject {

  public static final String P_LINKS = "links";
  public static final String P_TYPE_ID = "typeId";
  public static final String P_DOMAIN = "domain";
  public static final String V_ORDERS = "orders";
  public static final String P_CONTENT = "content";
  public static final String P_TITLE = "title";

  public Note putLinks(JSONArray links) {
    return (Note) putArray(P_LINKS, links);
  }
  public Note addLink (Link link) {
    if (getArray(P_LINKS) == null) {
      putArray(P_LINKS, new JSONArray());
    }
    getArray(P_LINKS).put(link.asJson());
    return this;
  }
  public Note putTypeId(String typeId) {
    return (Note) putString(P_TYPE_ID, typeId);
  }
  public Note putDomain(String domain) {
    return (Note) putString(P_DOMAIN, domain);
  }
  public Note putContent(String content) {
    return (Note) putString(P_CONTENT, content);
  }
  public Note putTitle (String title) {
    return (Note) putString(P_TITLE, title);
  }

}
