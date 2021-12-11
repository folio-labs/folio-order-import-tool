package org.olf.folio.order.dataobjects;

import org.json.JSONArray;
import org.marc4j.marc.DataField;
import org.olf.folio.order.Constants;
import org.olf.folio.order.mapping.BaseMapping;

public class ElectronicAccessUrl extends JsonDataObject {

  public static final String P_LINK_TEXT = "linkText";
  public static final String P_URI = "uri";
  public static final String P_PUBLIC_NOTE = "publicNote";
  public static final String P_RELATIONSHIP_ID = "relationshipId";

  // Mappings 856
  private static final String URI                  = "u";
  private static final String LINK_TEXT            = "y";
  private static final String LICENSE_NOTE         = "z";


  public static JSONArray getElectronicAccessFromMarcRecord (BaseMapping mappedMarc, String defaultLinkText) {
    JSONArray electronicAccess = new JSONArray();
    for (DataField d856 : mappedMarc.getAll856s()) {
      if (d856.getSubfieldsAsString(URI) != null) {
        electronicAccess.put(new ElectronicAccessUrl()
                .putLinkTextIfPresent(
                        present(d856.getSubfieldsAsString(LINK_TEXT))
                        ? d856.getSubfieldsAsString(LINK_TEXT) : defaultLinkText)
                .putUri(d856.getSubfieldsAsString(URI))
                .putPublicNoteIfPresent(d856.getSubfieldsAsString(LICENSE_NOTE))
                .putRelationshipId(Constants.ELECTRONIC_ACCESS_RELATIONSHIP_TYPE_RESOURCE)
                .asJson());
      }
    }
    return electronicAccess;
  }

  public ElectronicAccessUrl putLinkText (String linkText) {
    return (ElectronicAccessUrl) putString(P_LINK_TEXT, linkText);
  }

  public ElectronicAccessUrl putLinkTextIfPresent (String linkText) {
    return present(linkText) ? putLinkText(linkText) : this;
  }

  public ElectronicAccessUrl putUri (String uri) {
    return (ElectronicAccessUrl) putString(P_URI, uri);
  }

  public ElectronicAccessUrl putPublicNote (String publicNote) {
    return (ElectronicAccessUrl) putString(P_PUBLIC_NOTE, publicNote);
  }
  public ElectronicAccessUrl putPublicNoteIfPresent (String publicNote) {
    return present(publicNote) ? putPublicNote(publicNote) : this;
  }

  public ElectronicAccessUrl putRelationshipId (String relationshipId) {
    return (ElectronicAccessUrl) putString(P_RELATIONSHIP_ID, relationshipId);
  }
}
