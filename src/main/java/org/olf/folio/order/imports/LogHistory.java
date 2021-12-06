package org.olf.folio.order.imports;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.olf.folio.order.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Scanner;

public class LogHistory {

  private static final Logger logger = Logger.getLogger(LogHistory.class);
  public static final String P_ENTRIES = "entries";

  public static JSONObject loadDirectory () throws FileNotFoundException {
    JSONObject history = new JSONObject();
    JSONArray logEntries = new JSONArray();
    history.put(P_ENTRIES, logEntries);
    File directoryPath = new File(Config.uploadFilePath);
    File[] files = directoryPath.listFiles();

    if (files != null) {
      Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
      for (File file : files) {
        if (fileCouldBeResultsJson(file)) {
          try {
            JSONObject resultsJson = loadJson(file);
            purgeAgedResultsFiles(resultsJson, file, Config.daysToKeepResults);
            if (hasCompatibleSchema(resultsJson)) {
              if (getAgeOfFileInDays(file) < Config.daysToShowResults) {
                addHistoryEntry(file, resultsJson, logEntries);
              } else {
                logger.info(String.format(
                        "Skipped display of file '%s' since it's more than %d days old ",
                        file.getName(), Config.daysToShowResults));
              }
            } else {
              logger.info(String.format(
                      "File %s skipped due to schema incompatibility. Required schema: '%s', version '%s'.",
                      file.getName(), Results.V_SCHEMA_NAME, Results.V_SCHEMA_VERSION));
            }
          } catch (FileNotFoundException fnfe) {
            logger.error("The file retrieved from directory listing, could not be opened: " + fnfe.getMessage());
          }
          catch (JSONException je) {
            logger.error(String.format(
                    ".json file [%s] in the upload directory did not contain valid JSON [%s], skipping file.",
                    file.getName(), je.getMessage()));
          }
        }
      }
    }
    return history;
  }

  private static void purgeAgedResultsFiles(JSONObject resultsJson, File file, long keepForDays) {
    // make sure it's a results file produced by the tool
    if (jsonHasSchemaDeclaration(resultsJson)) {
      // find its age and delete if old
      long age = getAgeOfFileInDays(file);
      if (age > keepForDays) {
        logger.info(String.format(
                "Results file '%s' is older than %s days, attempting delete.",
                file.getName(),
                keepForDays));
        boolean fileDeleted = file.delete();
        logger.info(String.format("File %s %s", file.getName(),
                ( fileDeleted ? "deleted" : "not deleted" )));
      }
    }
  }

  private static long getAgeOfFileInDays(File file) {
    Instant fileInstant = Instant.ofEpochMilli(file.lastModified());
    Duration age = Duration.between(fileInstant, Instant.now());
    logger.info("File is " + age.toDays() + " days old");
    return age.toDays();
  }

  private static void addHistoryEntry (File file, JSONObject resultsJson, JSONArray logEntries) {
    Results results;
    try {
      results = new Results(resultsJson);
    }
    catch (JSONException | NullPointerException je) {
      logger.error(String.format(
              "Could not parse JSON file [%s] as import results [%s], skipping file. ",
              file.getName(), je.getMessage()));
      return;
    }
    try {
      HistoryEntry entry = new HistoryEntry(file.lastModified(), results);
      logEntries.put(entry.asJson());
    } catch (JSONException je) {
      logger.error(String.format(
              "Error populating import history with another entry: %s.",
              je.getMessage()));
    }
  }

  private static boolean fileCouldBeResultsJson (File file) {
    return file.isFile()
           && file.canRead()
           && FilenameUtils.getExtension(file.getName()).equals("json")
           && (file.getName().contains("-analyze-") || file.getName().contains("-import-"));
  }

  private static JSONObject loadJson(File file) throws FileNotFoundException, JSONException{
    return new JSONObject(loadResults(file));
  }

  private static boolean jsonHasSchemaDeclaration (JSONObject json) {
    return json.has(Results.P_SCHEMA) && json.get(Results.P_SCHEMA) instanceof JSONObject;
  }

  private static boolean jsonHasValidSchemaDeclaration (JSONObject json) {
    if (!json.has(Results.P_SCHEMA)) {
      logger.info("No 'schema' property found in the JSON file.");
      return false;
    }
    if (!(json.get(Results.P_SCHEMA) instanceof JSONObject)) {
      logger.info("JSON file has 'schema' property but the property is not a JSONObject.");
      return false;
    }
    JSONObject schema = json.getJSONObject(Results.P_SCHEMA);
    if (!schema.has(Results.P_SCHEMA_NAME)) {
      logger.info("JSON file has 'schema' property but the schema does not have a 'name' property.");
      return false;
    }
    if (!schema.has(Results.P_SCHEMA_VERSION)) {
      logger.info("JSON file has 'schema' property but the schema does not have a 'version' property.");
      return false;
    }
    return true;
  }

  private static boolean hasCompatibleSchema(JSONObject json) {
    if (!jsonHasValidSchemaDeclaration(json)) {
      return false;
    } else {
      JSONObject schema = json.getJSONObject(Results.P_SCHEMA);
      String schemaName = schema.getString(Results.P_SCHEMA_NAME);
      String schemaVersion = schema.getString(Results.P_SCHEMA_VERSION);
      if (!schemaName.equals(Results.V_SCHEMA_NAME)) {
        logger.info(String.format(
                "The declared schema (%s) is not compatible with required schema name (%s).",
                schemaName, Results.V_SCHEMA_NAME ));
        return false;
      }
      if (!schemaVersion.equals(Results.V_SCHEMA_VERSION)) {
        logger.info(String.format(
                "The declared schema version (%s) is not compatible with the required version (%s).",
                schemaVersion, Results.V_SCHEMA_VERSION ));
        return false;
      }
    }
    return true;
  }

  private static String loadResults(File file) throws FileNotFoundException {
    Scanner sc= new Scanner(file);
    String input;
    StringBuilder sb = new StringBuilder();
    while (sc.hasNextLine()) {
      input = sc.nextLine();
      sb.append(input).append(" ");
    }
    return sb.toString();
  }


}
