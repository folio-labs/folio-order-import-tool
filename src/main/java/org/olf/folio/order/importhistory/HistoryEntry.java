package org.olf.folio.order.importhistory;

import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryEntry {

  public static final String P_RESULTS_BASE_NAME = "resultsBaseName";
  public static final String P_MARC_RECORD_COUNT = "marcRecordCount";
  public static final String P_RECORDS_PROCESSED_TEXT = "recordsProcessedText";
  public static final String P_FILE_DATE = "fileDate";
  public static final String P_START_TIME = "startTime";
  public static final String P_END_TIME = "endTime";
  public static final String P_REQUEST_TYPE = "requestType";

  private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm");

  JSONObject json = new JSONObject();
  Results results;
  public HistoryEntry(long inputFileLastModified, Results results)  {
    this.results = results;
    json.put(P_RESULTS_BASE_NAME, results.getResultsBaseName());
    json.put(Results.P_INPUT_BASE_NAME, results.getInputBaseName());
    json.put(P_FILE_DATE, format.format(new Date(inputFileLastModified)));
    json.put(P_START_TIME, results.getStartTime());
    json.put(P_END_TIME, results.getEndTime());
    json.put(P_REQUEST_TYPE, results.getRequestType());
    json.put(P_MARC_RECORD_COUNT, results.getMarcRecordCount());
    json.put(P_RECORDS_PROCESSED_TEXT, getRecordsProcessedText());
  }

  public JSONObject asJson() {
    return json;
  }

  private String getRecordsProcessedText() {
    int records = results.getRecordsProcessed();
    return (records == -1 ? "(unknown)" : Integer.toString(records));
  }

}
