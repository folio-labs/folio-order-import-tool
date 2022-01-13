package org.olf.folio.order.importhistory;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;

import org.olf.folio.order.Config;

@CanIgnoreReturnValue
public class Results {

  public static final String P_SCHEMA = "schema";
  public static final String P_SCHEMA_NAME = "name";
  public static final String V_SCHEMA_NAME = "ImportResults";
  public static final String P_SCHEMA_VERSION = "version";
  public static final String V_SCHEMA_VERSION = "1.0";

  public static final String P_FATAL_ERROR = "fatalError";
  public static final String P_HAS_FATAL_ERROR = "hasFatalError";
  public static final String P_SUMMARY = "summary";
  public static final String P_TYPE = "type";
  public static final String P_FILES_IDENTIFIER = "filesIdentifier";
  public static final String P_INPUT_BASE_NAME = "inputBaseName";
  public static final String P_RESULTS_BASE_NAME = "resultsBaseName";
  public static final String P_START_INSTANT_UTC = "startInstantUtc";
  public static final String P_END_INSTANT_UTC = "endInstantUtc";
  public static final String P_START_TIME = "startTime";
  public static final String P_END_TIME = "endTime";
  public static final String P_STATUS = "status";
  public static final String V_STATUS_PARTIAL = "partial";
  public static final String V_STATUS_STARTED = "started";
  public static final String V_STATUS_DONE = "done";
  public static final String V_STATUS_ERROR = "error";
  public static final String P_IS_NOT_DONE = "isNotDone";
  public static final String P_IS_ERROR = "isError";
  public static final String P_MARC_RECORDS = "marcRecords";
  public static final String P_USER = "user";
  public static final String P_VALIDATION = "validation";
  public static final String P_IMPORT = "import";
  public static final String P_HAS_ERRORS = "hasErrors";
  public static final String P_RECORDS_PROCESSED = "recordsProcessed";
  public static final String P_SUCCEEDED_COUNT = "succeeded";
  public static final String P_FAILED_COUNT = "failed";
  public static final String P_HAS_FLAGS = "hasFlags";
  public static final String P_FLAGGED = "flagged";
  public static final String P_RECORDS = "records";

  // To be configurable
  DateTimeFormatter formatTime = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT )
                  .withLocale(Config.locale).withZone(Config.zoneId);
  DateTimeFormatter formatDateTime = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT )
                  .withLocale(Config.locale).withZone(Config.zoneId);

  JSONObject resultsJson = new JSONObject();

  List<RecordResult> recordResults = new ArrayList<>();
  int recNo = 0;
  boolean importing;

  public Results (String errorMessage) {
    resultsJson.put(P_SCHEMA, new JSONObject());
    schema().put(P_SCHEMA_NAME,V_SCHEMA_NAME);
    schema().put(P_SCHEMA_VERSION,V_SCHEMA_VERSION);

    resultsJson.put(P_SUMMARY, new JSONObject());
    setFatalError(errorMessage);
    summary().put(P_START_INSTANT_UTC, Instant.now().toString());
    summary().put(P_START_TIME, getStartTime());
    summary().put(P_END_INSTANT_UTC,"");
    summary().put(P_END_TIME, "");
    summary().put(P_STATUS,V_STATUS_PARTIAL);
    summary().put(P_IS_NOT_DONE, true);
    summary().put(P_VALIDATION, new JSONObject());
    summary().put(P_USER, Config.apiUsername);
    summary().put(P_MARC_RECORDS, 0);
  }

  public Results(boolean importing, FileStorageHelper fileStore) {
    resultsJson.put(P_SCHEMA, new JSONObject());
    schema().put(P_SCHEMA_NAME,V_SCHEMA_NAME);
    schema().put(P_SCHEMA_VERSION,V_SCHEMA_VERSION);

    resultsJson.put(P_SUMMARY, new JSONObject());
    summary().put(P_TYPE, fileStore.typeOfRequest());
    summary().put(P_FILES_IDENTIFIER, fileStore.getFilesIdentifier());
    summary().put(P_INPUT_BASE_NAME, fileStore.baseNameOfMarcFile());
    summary().put(P_RESULTS_BASE_NAME, fileStore.resultsBaseName());
    summary().put(P_START_INSTANT_UTC, Instant.now().toString());
    summary().put(P_START_TIME, getStartTime());
    summary().put(P_END_INSTANT_UTC,"");
    summary().put(P_END_TIME, "");
    summary().put(P_STATUS,V_STATUS_STARTED);
    summary().put(P_IS_NOT_DONE, true);
    summary().put(P_VALIDATION, new JSONObject());
    summary().put(P_USER, Config.apiUsername);
    summary().put(P_MARC_RECORDS, 0);
    this.importing = importing;
    if (importing) {
      summary().put(P_IMPORT, new JSONObject());
    }
  }

  public Results setFatalError (String message) {
    summary().put(P_FATAL_ERROR, message);
    summary().put(P_HAS_FATAL_ERROR, true);
    return this;
  }

  public Results(JSONObject resultsJson) {
    this.resultsJson = resultsJson;
    if (resultsJson.has(P_IMPORT)) {
      importing = true;
    }
    for (Object o : resultsJson.getJSONArray(P_RECORDS)) {
      JSONObject recordResultJson = (JSONObject) o;
      recordResults.add(new RecordResult(recordResultJson));
    }
  }

  public Instant getStartInstantUtc () {
    if (summary().getString(P_START_INSTANT_UTC).isEmpty()) {
      return null;
    } else {
      return Instant.parse(summary().getString(P_START_INSTANT_UTC));
    }
  }

  public String getStartTime () {
    if (getStartInstantUtc() == null) {
      return "";
    } else {
      return formatDateTime.format(getStartInstantUtc());
    }
  }

  public Instant getEndInstantUtc () {
    if (!summary().has(P_END_INSTANT_UTC) || summary().getString(P_END_INSTANT_UTC).isEmpty()) {
      return null;
    } else {
      return Instant.parse(summary().getString(P_END_INSTANT_UTC));
    }
  }

  public String getEndTime () {
    if (getEndInstantUtc() == null) {
      return "";
    } else {
      return formatTime.format(getEndInstantUtc());
    }
  }

  public Results setMarcRecordCount (int count) {
    summary().put(P_MARC_RECORDS, count);
    return this;
  }

  public int getMarcRecordCount() {
    return summary().getInt(P_MARC_RECORDS);
  }

  public String getInputBaseName () {
    return summary().getString(P_INPUT_BASE_NAME);
  }

  public String getResultsBaseName () {
    return summary().getString(P_RESULTS_BASE_NAME);
  }

  public String getRequestType () {
    return summary().getString(P_TYPE);
  }

  public int getRecordsProcessed () {
    return summary().optInt(P_RECORDS_PROCESSED);
  }

  private JSONObject schema() {
    return resultsJson.getJSONObject(P_SCHEMA);
  }
  private JSONObject summary () {
    return resultsJson.getJSONObject(P_SUMMARY);
  }

  private JSONObject validation() {
    return summary().getJSONObject(P_VALIDATION);
  }

  private JSONObject _import() {
    return summary().getJSONObject(P_IMPORT);
  }

  /**
   * Initiate next results element
   * @return the new results element
   */
  public RecordResult nextResult () {
    RecordResult result = new RecordResult(++recNo, importing);
    recordResults.add(result);
    return result;
  }

  public boolean hasFatalError () {
    return summary().has(P_FATAL_ERROR);
  }
  /**
   * Find out if any one record failed validation
   * @return true if one or more records failed validation
   */
  public boolean hasValidationErrors () {
    for (RecordResult result : recordResults) {
      if (result.failedValidation()) return true;
    }
    return false;
  }

  /**
   * Find out if any one record had an error during import
   * @return true if one or more errors occurred during import of records
   */
  public boolean hasImportErrors () {
    for (RecordResult result : recordResults) {
      if (result.hasImportError()) return true;
    }
    return false;
  }

  /**
   * Find out if any one record has a note attached
   * @return true if one or more records had notes attached
   */
  public boolean hasFlags () {
    for (RecordResult outcome : recordResults) {
      if (outcome.hasFlags()) return true;
    }
    return false;
  }

  /**
   * Count records that had notes attached
   * @return record count
   */
  public int getFlagsCount () {
    int flags = 0;
    for (RecordResult outcome : recordResults) {
      if (outcome.hasFlags()) flags++;
    }
    return flags;
  }

  private int getRecordCount () {
    return recordResults.size();
  }

  /**
   * Count records that passed validation
   * @return record count
   */
  private int getPassedValidationCount() {
    int passedCount = 0;
    for (RecordResult result : recordResults) {
      if (!result.failedValidation()) passedCount++;
    }
    return passedCount;
  }

  /**
   * Count records that failed validation
   * @return record count
   */
  private int getFailedValidationCount() {
    int failedCount = 0;
    for (RecordResult result : recordResults) {
      if (result.failedValidation()) failedCount++;
    }
    return failedCount;
  }

  /**
   * Count records that triggered exceptions during import
   * @return record count
   */
  private int getImportExceptionsCount () {
    int exceptions = 0;
    for (RecordResult result : recordResults) {
      if (result.hasImportError()) exceptions++;
    }
    return exceptions;
  }

  /**
   * Count records that were successfully imported
   * @return record count
   */
  private int getSuccessfulImportsCount () {
    if (hasFatalError()) {
      return 0;
    } else {
      int passed = 0;
      for (RecordResult result : recordResults) {
        if (!result.hasImportError() && !result.isSkipped()) {
          passed++;
        }
      }
      return passed;
    }
  }

  /**
   * Mark import initialized
   * @return this
   */
  public Results markStarted() {
    summary().put(P_STATUS, V_STATUS_STARTED);
    summary().put(P_IS_NOT_DONE, true);
    return this;
  }

  /**
   * Mark import complete, without fatal error.
   * @return this
   */
  public Results markDone() {
    summary().put(P_STATUS, V_STATUS_DONE);
    summary().put(P_IS_NOT_DONE, false);
    summary().put(P_END_INSTANT_UTC, Instant.now().toString());
    return this;
  }

  /**
   * Flag that import has ended but with an error
   * @return this
   */
  public Results markEndedWithError () {
    summary().put(P_STATUS, V_STATUS_ERROR);
    summary().put(P_IS_NOT_DONE, false);
    summary().put(P_END_INSTANT_UTC, Instant.now().toString());
    return this;
  }

  /**
   * Mark import partially done.
   * @return this
   */
  public Results markPartial() {
    summary().put(P_STATUS, V_STATUS_PARTIAL);
    summary().put(P_IS_NOT_DONE, true);
    return this;
  }

  /**
   * Return complete results as JSON object
   * @return results JSON
   */
  public JSONObject toJson() {
    // set derived/calculated values
    validation().put(P_HAS_ERRORS, hasValidationErrors());
    validation().put(P_SUCCEEDED_COUNT, getPassedValidationCount());
    validation().put(P_FAILED_COUNT, getFailedValidationCount());
    if (importing) {
      _import().put(P_HAS_ERRORS, hasImportErrors());
      _import().put(P_SUCCEEDED_COUNT, getSuccessfulImportsCount());
      _import().put(P_FAILED_COUNT, getImportExceptionsCount());
    }
    summary().put(P_HAS_FLAGS, hasFlags());
    summary().put(P_FLAGGED, getFlagsCount());
    JSONArray records = new JSONArray();
    for (RecordResult result : recordResults) {
      records.put(result.asJson());
    }
    resultsJson.put(P_RECORDS, records);
    summary().put(P_RECORDS_PROCESSED, records.length());
    return resultsJson;
  }

  /**
   * Return complete results as pretty printed JSON string
   * @return results as indented JSON string
   */
  public String toJsonString() {
    return toJson().toString(2);
  }
}

