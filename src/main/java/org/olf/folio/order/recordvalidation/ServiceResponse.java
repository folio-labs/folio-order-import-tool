package org.olf.folio.order.recordvalidation;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


public class ServiceResponse {

  private static final String P_SUMMARY = "summary";
  private static final String P_VALIDATION = "validation";
  private static final String P_IMPORT = "import";
  private static final String P_HAS_ERRORS = "hasErrors";
  private static final String P_RECORDS_PROCESSED = "recordsProcessed";
  private static final String P_SUCCEEDED_COUNT = "succeeded";
  private static final String P_FAILED_COUNT = "failed";
  private static final String P_RECORDS = "records";

  JSONObject response = new JSONObject();
  JSONObject summary = new JSONObject();
  JSONObject validation = new JSONObject();
  JSONObject _import = new JSONObject();
  JSONArray records = new JSONArray();

  List<RecordResult> recordResults = new ArrayList<>();
  int recNo = 0;
  boolean importing;

  public ServiceResponse(boolean importing) {
    this.importing = importing;
    response.put(P_SUMMARY, summary);
    summary.put(P_VALIDATION, validation);
    if (importing) {
      summary.put(P_IMPORT, _import);
    }
    response.put(P_RECORDS, records);
  }

  public RecordResult nextResult () {
    RecordResult result = new RecordResult(++recNo, importing);
    recordResults.add(result);
    return result;
  }

  public boolean hasValidationErrors () {
    for (RecordResult result : recordResults) {
      if (result.failedValidation()) return true;
    }
    return false;
  }

  public boolean hasImportErrors () {
    for (RecordResult result : recordResults) {
      if (result.hasImportError()) return true;
    }
    return false;
  }

  private int getRecordCount () {
    return recordResults.size();
  }

  private int getPassedValidationCount() {
    int passedCount = 0;
    for (RecordResult result : recordResults) {
      if (!result.failedValidation()) passedCount++;
    }
    return passedCount;
  }

  private int getFailedValidationCount() {
    int failedCount = 0;
    for (RecordResult result : recordResults) {
      if (result.failedValidation()) failedCount++;
    }
    return failedCount;
  }

  private int getImportExceptionsCount () {
    int exceptions = 0;
    for (RecordResult result : recordResults) {
      if (result.hasImportError()) exceptions++;
    }
    return exceptions;
  }

  private int getSuccessfulImportsCount () {
    int passed = 0;
    for (RecordResult result : recordResults) {
      if (!result.hasImportError() && !result.isSkipped()) {
        passed++;
      }
    }
    return passed;
  }

  public JSONObject toJson() {
    summary.put(P_RECORDS_PROCESSED, getRecordCount());
    validation.put(P_HAS_ERRORS, hasValidationErrors());
    validation.put(P_SUCCEEDED_COUNT, getPassedValidationCount());
    validation.put(P_FAILED_COUNT, getFailedValidationCount());
    if (importing) {
      _import.put(P_HAS_ERRORS, hasImportErrors());
      _import.put(P_SUCCEEDED_COUNT, getSuccessfulImportsCount());
      _import.put(P_FAILED_COUNT, getImportExceptionsCount());
    }
    for (RecordResult result : recordResults) {
      records.put(result.asJson());
    }
    return response;
  }
}

