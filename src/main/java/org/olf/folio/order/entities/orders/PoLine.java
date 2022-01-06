package org.olf.folio.order.entities.orders;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.entities.FolioEntity;
import org.olf.folio.order.mapping.MarcToFolio;
import org.olf.folio.order.mapping.MarcMapLambda;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@CanIgnoreReturnValue
public class PoLine extends FolioEntity {
  // Constant values
  public static final String V_USER = "User";
  public static final String V_ELECTRONIC_RESOURCE = "Electronic Resource";
  public static final String V_PHYSICAL_RESOURCE = "Physical Resource";
  public static final String V_RECEIPT_NOT_REQUIRED = "Receipt Not Required";
  // Property names
  public static final String P_ID = "id";
  public static final String P_PURCHASE_ORDER_ID = "purchaseOrderId";
  public static final String P_ORDER_FORMAT = "orderFormat";
  public static final String P_RECEIPT_STATUS = "receiptStatus";
  public static final String P_ERESOURCE = "eresource";
  public static final String P_PHYSICAL = "physical";
  public static final String P_VENDOR_DETAIL = "vendorDetail";
  public static final String P_SOURCE = "source";
  public static final String P_COST = "cost";
  public static final String P_LOCATIONS = "locations";
  public static final String P_TITLE_OR_PACKAGE = "titleOrPackage";
  public static final String P_ACQUISITION_METHOD = "acquisitionMethod";
  public static final String P_RUSH = "rush";
  public static final String P_DESCRIPTION = "description";
  public static final String P_FUND_DISTRIBUTION = "fundDistribution";
  public static final String P_SELECTOR = "selector";
  public static final String P_DONOR = "donor";
  public static final String P_CONTRIBUTORS = "contributors";
  public static final String P_PRODUCT_IDS = "productIds";
  public static final String P_DETAILS = "details";
  public static final String P_EDITION = "edition";
  public static final String P_PUBLISHER = "publisher";
  public static final String P_PUBLICATION_DATE = "publicationDate";
  public static final String P_TAGS = "tags";
  public static final String P_INSTANCE_ID = "instanceId";

  public static PoLine fromMarcRecord(UUID orderId, MarcToFolio mappedMarc)
          throws Exception {

    PoLine poLine = new PoLine()
            .putId(UUID.randomUUID())
            .putPurchaseOrderId(orderId)
            .putSource(V_USER);

    if (mappedMarc.electronic()) {
      poLine.putOrderFormat(V_ELECTRONIC_RESOURCE)
            .putReceiptStatus(V_RECEIPT_NOT_REQUIRED)
            .putEResource(EResource.fromMarcRecord(mappedMarc));
    } else {
      poLine.putPhysical(Physical.fromMarcRecord(mappedMarc))
              .putOrderFormat(PoLine.V_PHYSICAL_RESOURCE);
    }
    if (mappedMarc.hasVendorItemId()) {
      poLine.putVendorDetail(VendorDetail.fromMarcRecord(mappedMarc));
    }
    if (mappedMarc instanceof MarcMapLambda) {
      poLine.putTagsIfPresent(Tags.fromMarcRecord((MarcMapLambda) mappedMarc));
    }
    poLine.putCost(Cost.fromMarcRecord(mappedMarc))
            .putLocations(new JSONArray().put(PoLineLocation.fromMarcRecord(mappedMarc).asJson()))
            .putTitleOrPackage(mappedMarc.title())
            .putAcquisitionMethod(mappedMarc.acquisitionMethodId())
            .putRush(mappedMarc.rush())
            .putDescriptionIfPresent(mappedMarc.description())
            .putFundDistribution(new JSONArray().put(Fund.fromMarcRecord(mappedMarc).asJson()))
            .putPurchaseOrderId(orderId)
            .putSelectorIfPresent(mappedMarc.selector())
            .putDonorIfPresent(mappedMarc.donor())
            .putContributors(mappedMarc.getContributorsForOrderLine())
            .putDetailsIfPresent(OrderLineDetails.fromMarcRecord(mappedMarc).asJson())
            .putEditionIfPresent(mappedMarc.edition());

    if (mappedMarc.has260()) {
      if (mappedMarc.publisher("260") != null)
        poLine.putPublisher(mappedMarc.publisher("260"));
      if (mappedMarc.publisher("260") != null)
        poLine.putPublicationDate(mappedMarc.publicationDate("260"));
    } else if (mappedMarc.has264()) {
      if (mappedMarc.publisher("264") != null)
        poLine.putPublisher(mappedMarc.publisher("264"));
      if (mappedMarc.publisher("264") != null)
        poLine.putPublicationDate(mappedMarc.publicationDate("264"));
    }
    return poLine;
  }

  public static PoLine fromJson(JSONObject poLineJson) {
    PoLine poLine = new PoLine();
    poLine.json = poLineJson;
    return poLine;
  }

  public PoLine putId (UUID id) {
    return (PoLine) putString(P_ID, id.toString());
  }
  public PoLine putPurchaseOrderId (UUID purchaseOrderId) {
    return (PoLine) putString(P_PURCHASE_ORDER_ID, purchaseOrderId.toString());
  }
  public PoLine putOrderFormat (String orderFormat) {
    return (PoLine) putString(P_ORDER_FORMAT, orderFormat);
  }
  public PoLine putReceiptStatus (String receiptStatus) {
    return (PoLine) putString(P_RECEIPT_STATUS, receiptStatus);
  }
  public PoLine putEResource (JSONObject eresource) {
    return (PoLine) putObject(P_ERESOURCE, eresource);
  }
  public PoLine putEResource (EResource resource) {
    return putEResource(resource.asJson());
  }
  public PoLine putPhysical (JSONObject physical) {
    return (PoLine) putObject(P_PHYSICAL, physical);
  }
  public PoLine putPhysical (Physical physical) {
    return putPhysical(physical.asJson());
  }
  public PoLine putVendorDetail (JSONObject vendorDetail) {
    return (PoLine) putObject(P_VENDOR_DETAIL, vendorDetail);
  }
  public PoLine putVendorDetail (VendorDetail vendorDetail) {
    return putVendorDetail(vendorDetail.asJson());
  }
  public PoLine putSource (String source) {
    return (PoLine) putString(P_SOURCE, source);
  }
  public PoLine putCost (JSONObject cost) {
    return (PoLine) putObject(P_COST, cost);
  }
  public PoLine putCost (Cost cost) {
    return putCost(cost.asJson());
  }
  public PoLine putLocations (JSONArray locations) {
    return (PoLine) putArray(P_LOCATIONS, locations);
  }
  public PoLine putTitleOrPackage (String titleOrPackage) {
    return (PoLine) putString(P_TITLE_OR_PACKAGE, titleOrPackage);
  }
  public PoLine putAcquisitionMethod (String acquisitionMethod) {
    return (PoLine) putString(P_ACQUISITION_METHOD, acquisitionMethod);
  }
  public PoLine putRush (boolean rush) {
    return (PoLine) putBoolean(P_RUSH, rush);
  }
  public PoLine putDescription(String description) {
    return (PoLine) putString(P_DESCRIPTION, description);
  }
  public PoLine putDescriptionIfPresent(String description) {
    return present(description) ? putDescription(description) : this;
  }
  public PoLine putFundDistribution (JSONArray fundDistribution) {
    return (PoLine) putArray(P_FUND_DISTRIBUTION, fundDistribution);
  }
  public PoLine putSelector(String selector) {
    return (PoLine) putString(P_SELECTOR, selector);
  }
  public PoLine putSelectorIfPresent(String selector) {
    return present(selector) ? putSelector(selector) : this;
  }
  public PoLine putDonor (String donor) {
    return (PoLine) putString(P_DONOR, donor);
  }
  public PoLine putDonorIfPresent (String donor) {
    return present(donor) ?  putDonor(donor) : this;
  }
  public PoLine putContributors (JSONArray contributors) {
    return (PoLine) putArray(P_CONTRIBUTORS, contributors);
  }
  public PoLine putDetails (JSONObject details) {
    return (PoLine) putObject(P_DETAILS, details);
  }
  public PoLine putDetailsIfPresent(JSONObject details) {
    return present(details) ? putDetails(details) : this;
  }

  public PoLine putEdition (String edition) {
    return (PoLine) putString(P_EDITION, edition);
  }
  public PoLine putEditionIfPresent (String edition) {
    return (present(edition)) ? putEdition(edition) : this;
  }
  public PoLine putPublisher (String publisher) {
    return (PoLine) putString(P_PUBLISHER, publisher);
  }
  public PoLine putPublicationDate (String publicationDate) {
    return (PoLine) putString(P_PUBLICATION_DATE, publicationDate);
  }
  public PoLine putTags (JSONObject tags) {
    return (PoLine) putObject(P_TAGS, tags);
  }
  public PoLine putTags (Tags tags) {
    return putTags(tags.asJson());
  }
  public PoLine putTagsIfPresent (Tags tags) {
    return present(tags.getTagList()) ? putTags(tags) : this;
  }

  public String getInstanceId () {
    return getString(P_INSTANCE_ID);
  }

  public List<PoLineLocation> getLocations() {
    List<PoLineLocation> poLineLocationList = new ArrayList<>();
    JSONArray locations = json.getJSONArray(P_LOCATIONS);
    if (locations != null && !locations.isEmpty()) {
      for (Object o : locations) {
        poLineLocationList.add(PoLineLocation.fromJson((JSONObject) o));
      }
    }
    return poLineLocationList;
  }

  public Physical getPhysical () {
    return Physical.fromJson(getJSONObject(P_PHYSICAL));
  }
}
