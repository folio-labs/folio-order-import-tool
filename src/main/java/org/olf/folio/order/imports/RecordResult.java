package org.olf.folio.order.imports;

import org.json.JSONArray;
import org.json.JSONObject;
import org.olf.folio.order.mapping.BaseMapping;

public class RecordResult {

  private static final String P_REC_NO = "recNo";
  private static final String P_DATA = "data";
  private static final String P_TITLE = "title";
  private static final String P_ISBN = "isbn";
  private static final String P_PO_NUMBER = "poNumber";
  private static final String P_UI_URL_PO = "uiUrlPo";
  private static final String P_UI_URL_INSTANCE = "uiUrlInstance";
  private static final String P_INSTANCE_HRID = "instanceHrid";
  private static final String P_SOURCE = "source";
  private static final String P_HAS_VALIDATION_ERRORS = "hasValidationErrors";
  private static final String P_HAS_IMPORT_ERROR = "hasImportError";
  private static final String P_IMPORT_ERROR = "importError";
  private static final String P_RECORD_IMPORTED = "imported";
  private static final String P_RECORD_SKIPPED = "skipped";
  private static final String P_VALIDATION_ERRORS = "validationErrors";
  private static final String P_FLAGS = "flags";
  private static final String P_HAS_FLAGS = "hasFlags";

  private JSONObject recordResultJson = new JSONObject();

  public RecordResult (int recNo, boolean importing) {
    recordResultJson.put(P_REC_NO, recNo);
    if (importing) {
      recordResultJson.put(P_HAS_IMPORT_ERROR, false);
      recordResultJson.put(P_RECORD_IMPORTED, true);
      recordResultJson.put(P_RECORD_SKIPPED, false);
    }
    recordResultJson.put(P_HAS_VALIDATION_ERRORS, false);
    recordResultJson.put(P_VALIDATION_ERRORS, new JSONArray());
    recordResultJson.put(P_FLAGS, new JSONArray());
    recordResultJson.put(P_HAS_FLAGS, false);
    recordResultJson.put(P_DATA, new JSONObject());
  }

  public RecordResult (JSONObject recordResultJson) {
    this.recordResultJson = recordResultJson;
  }

  private JSONObject data() {
    return recordResultJson.getJSONObject(P_DATA);
  }

  private JSONArray validationErrors () {
    return recordResultJson.getJSONArray(P_VALIDATION_ERRORS);
  }

  private JSONArray flags () {
    return recordResultJson.getJSONArray(P_FLAGS);
  }

  public RecordResult setInputMarcData(BaseMapping mappedMarc) {
    data().put(P_TITLE, mappedMarc.title());
    data().put(P_ISBN, mappedMarc.hasISBN() ? mappedMarc.getISBN() : "No ISBN");
    data().put(P_SOURCE, mappedMarc.getRecord().toString());
    return this;
  }

  public RecordResult setPoNumber (String poNumber) {
    data().put(P_PO_NUMBER, poNumber);
    return this;
  }

  public RecordResult setPoUiUrl (String url) {
    data().put(P_UI_URL_PO, url);
    return this;
  }

  public RecordResult setPoUiUrl (String folioUiUrl, String path, String poId, String poNumber ) {
    setPoUiUrl(String.format("%s%s%s?qindex=poNumber&query=%s", folioUiUrl, path, poId, poNumber));
    return this;
  }

  public RecordResult setInstanceHrid (String hrid) {
    data().put(P_INSTANCE_HRID, hrid);
    return this;
  }

  public RecordResult setInstanceUiUrl (String url) {
    data().put(P_UI_URL_INSTANCE, url);
    return this;
  }

  public RecordResult setInstanceUiUrl(String folioUiUrl, String path, String instanceId, String hrid) {
    if (folioUiUrl != null && path != null) {
      setInstanceUiUrl(
              String.format("%s%s%s?qindex=hrid&query=%s&sort=title", folioUiUrl, path, instanceId, hrid));
    }
    return this;
  }

  public RecordResult addValidationMessageIfAny(String errorMessage) {
    if (errorMessage != null) {
      recordResultJson.put(P_HAS_VALIDATION_ERRORS,true);
      validationErrors().put(errorMessage);
    }
    return this;
  }

  public RecordResult markSkipped (boolean configuredToSkipFailedRecords) {
    if (configuredToSkipFailedRecords) {
      recordResultJson.put(P_RECORD_SKIPPED, true);
    }
    return this;
  }

  public RecordResult setImportError (String errorMessage) {
    recordResultJson.put(P_IMPORT_ERROR, errorMessage);
    recordResultJson.put(P_HAS_IMPORT_ERROR, true);
    recordResultJson.put(P_RECORD_IMPORTED, false);
    return this;
  }

  public RecordResult setFlagIfNotNull (String message) {
    if (message != null) {
      flags().put(message);
      recordResultJson.put(P_HAS_FLAGS,true);
    }
    return this;
  }

  public boolean failedValidation() {
    return recordResultJson.getBoolean(P_HAS_VALIDATION_ERRORS);
  }

  public boolean hasImportError () {
    return recordResultJson.getBoolean(P_HAS_IMPORT_ERROR);
  }

  public boolean hasFlags () {
    return recordResultJson.getJSONArray(P_FLAGS).length()>0;
  }
  public boolean isSkipped() {
    return recordResultJson.getBoolean(P_RECORD_SKIPPED);
  }

  public JSONObject asJson() {
    return recordResultJson;
  }

}
