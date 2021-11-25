package org.olf.folio.order;

import org.json.JSONObject;

public class ProcessCounters {
  public int recordsProcessed;
  public int recordsImported;
  public int recordsFailed;
  public int recordsSkipped;
  public JSONObject getCountsAsMessage () {
    JSONObject message = new JSONObject();
    JSONObject marcRecords = new JSONObject();
    marcRecords.put("processed", recordsProcessed);
    marcRecords.put("imported", recordsImported);
    marcRecords.put("failed", recordsFailed);
    marcRecords.put("skipped", recordsSkipped);
    message.put("marcRecords", marcRecords);
    return message;
  }
}
