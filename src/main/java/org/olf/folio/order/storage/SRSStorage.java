package org.olf.folio.order.storage;

import org.json.JSONObject;
import org.marc4j.MarcJsonWriter;
import org.marc4j.MarcStreamWriter;
import org.marc4j.MarcWriter;
import org.marc4j.marc.ControlField;
import org.marc4j.marc.DataField;
import org.marc4j.marc.MarcFactory;
import org.marc4j.marc.Record;
import org.marc4j.marc.Subfield;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

public class SRSStorage extends FolioAccess {

  public static void storeMarcToSRS(Record record, ByteArrayOutputStream byteArrayOutputStream, UUID snapshotId, UUID recordTableId, String instanceId, String hrid ) throws Exception
  {
    logger.info("Storing MARC to SRS");
    //PREPARING TO ADD THE MARC RECORD TO SOURCE RECORD STORAGE:
    //CONSTRUCTING THE 999 OF THE MARC RECORD for FOLIO:
    DataField field = MarcFactory.newInstance().newDataField();
    field.setTag("999");
    field.setIndicator1('f');
    field.setIndicator2('f');
    Subfield one = MarcFactory.newInstance().newSubfield('i', instanceId );
    Subfield two = MarcFactory.newInstance().newSubfield('s', recordTableId.toString());
    field.addSubfield(one);
    field.addSubfield(two);
    record.addVariableField(field);
    if ( record.getControlNumberField() != null) {
      record.getControlNumberField().setData( hrid );
    }
    else {
      ControlField cf = MarcFactory.newInstance().newControlField("001");
      cf.setData( hrid );
      record.addVariableField(cf);
    }

    //TRANSFORM THE RECORD INTO JSON
    logger.info("MARC RECORD: " + record);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    MarcJsonWriter jsonWriter =  new MarcJsonWriter(baos);
    jsonWriter.setUnicodeNormalization(true);
    jsonWriter.write( record );
    jsonWriter.close();
    String jsonString = baos.toString();
    JSONObject mRecord = new JSONObject(jsonString);
    JSONObject content = new JSONObject();
    content.put("content",mRecord);
    logger.info("MARC TO JSON: " + mRecord);


    //GET THE RAW MARC READY TO POST TO THE API
    ByteArrayOutputStream rawBaos = new ByteArrayOutputStream();
    MarcWriter writer = new MarcStreamWriter(rawBaos);
    writer.write( record );
    JSONObject jsonWithRaw = new JSONObject();
    jsonWithRaw.put("id", instanceId );
    jsonWithRaw.put("content", byteArrayOutputStream );

    //CREATING JOB EXECUTION?
    //I'M NOT ENTIRELY SURE IF THIS IS NECESSARY?
    //WHAT THE CONSEQUENCES OF THIS ARE?
    //TO POST TO SOURCE RECORD STORAGE, A SNAPSHOT ID
    //SEEMS TO BE REQUIRED
    JSONObject jobExecution = new JSONObject();
    jobExecution.put("jobExecutionId", snapshotId.toString());
    jobExecution.put("status", "PARSING_IN_PROGRESS");
    FolioAccess.callApiPostWithUtf8( "source-storage/snapshots",  jobExecution);

    //OBJECT FOR SOURCE RECORD STORAGE API CALL:
    JSONObject sourceRecordStorageObject = new JSONObject();
    sourceRecordStorageObject.put("recordType", "MARC");
    sourceRecordStorageObject.put("snapshotId", snapshotId.toString());
    sourceRecordStorageObject.put("matchedId", instanceId);
    //LINK THE INSTANCE TO SOURCE RECORD STORAGE
    JSONObject externalId = new JSONObject();
    externalId.put("instanceId", instanceId );
    sourceRecordStorageObject.put("externalIdsHolder", externalId);
    //RAW RECORD
    JSONObject rawRecordObject = new JSONObject();
    rawRecordObject.put("id", instanceId );
    rawRecordObject.put("content",jsonWithRaw.toString());
    //PARSED RECORD
    JSONObject parsedRecord = new JSONObject();
    parsedRecord.put("id", instanceId );
    parsedRecord.put("content", mRecord);
    sourceRecordStorageObject.put("rawRecord", rawRecordObject);
    sourceRecordStorageObject.put("parsedRecord", parsedRecord);
    sourceRecordStorageObject.put("id", instanceId );
    //CALL SOURCE RECORD STORAGE POST
    FolioAccess.callApiPostWithUtf8( "source-storage/records", sourceRecordStorageObject);
  }
}
