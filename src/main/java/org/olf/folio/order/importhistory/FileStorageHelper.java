package org.olf.folio.order.importhistory;


import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;
import org.olf.folio.order.Config;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static java.lang.String.format;

/**
 * The helper is initialized with an incoming MARC file, the file is stored on the file system
 * in the configured directory, using the defined name pattern.
 * <li>[baseName of input file]-['analyze' or 'import']-[a UUID].mrc</li>
 *
 * A subsequently saved results file will be named similarly:
 * <li>[baseName of input file]-['analyze' or 'import']-[same UUID].json</li>
 */
public class FileStorageHelper {

  public static final String REQUEST_ANALYZE = "analyze";
  public static final String REQUEST_IMPORT = "import";
  public static final String EXT_MRC = "mrc";
  public static final String EXT_JSON = "json";

  private String directory;
  private final UUID fileUuid;
  private String inputBaseName;
  private String requestType;

  private static final Logger logger = Logger.getLogger(FileStorageHelper.class);

  private FileStorageHelper () {
    fileUuid = UUID.randomUUID();
  }

  /**
   * The naming pattern for files stored in the upload directory
   * @param extension either .mrc (input) or .json (results/response)
   * @return the full path to the currently processed file with the given extension
   */
  private String fileLocation (String extension) {
    return format("%s%s-%s-%s.%s", directory, inputBaseName, requestType, fileUuid, extension);
  }

  public String resultsBaseName () {
    logger.debug("Storage helper providing resultsBaseName " + FilenameUtils.getBaseName(fileLocation(EXT_JSON)));
    return FilenameUtils.getBaseName(fileLocation(EXT_JSON));
  }

  public static FileStorageHelper storeMarcFile(InputStream marcInput, String inputName, boolean analyzeOnly) {
    FileStorageHelper storage = new FileStorageHelper();
    storage.directory = Config.uploadFilePath;
    storage.requestType = getRequestType(analyzeOnly);
    storage.inputBaseName = FilenameUtils.getBaseName(inputName);
    logger.info(String.format("Initializing file names, storing incoming MARC file [%s]",
            storage.fileLocation(EXT_MRC)));
    FileStorageHelper.writeMarcFile(marcInput, storage.fileLocation(EXT_MRC));
    return storage;
  }

  public String fullPathToMarcFile() {
    return fileLocation(EXT_MRC);
  }

  public String baseNameOfMarcFile() {
    return inputBaseName;
  }

  public String typeOfRequest() {
    return requestType;
  }

  public InputStream getMarcInputStream() throws FileNotFoundException {
    logger.debug("Creating MARC input stream from " + baseNameOfMarcFile());
    return new FileInputStream(fullPathToMarcFile());
  }

  private static void writeMarcFile(InputStream uploadedInputStream,
                             String uploadedFileServerLocation) {
    try {
      int read;
      byte[] bytes = new byte[1024];
      OutputStream out = new FileOutputStream(uploadedFileServerLocation);
      while ((read = uploadedInputStream.read(bytes)) != -1) {
        out.write(bytes, 0, read);
      }
      out.flush();
      out.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void storeResults(Results results) throws Exception {
    logger.debug("Storing analyze/import results with " + results.getRecordsProcessed() + " records.");
    String location = fileLocation(EXT_JSON);
    BufferedWriter writer = new BufferedWriter(new FileWriter(location, false));
    writer.write(results.toJson().toString(2));
    writer.close();
    logger.debug(String.format("Results written to file [%s]", location));
  }

  private static String getRequestType (boolean analyzeOnly) {
    return analyzeOnly ? REQUEST_ANALYZE : REQUEST_IMPORT;
  }

  public String getFilesIdentifier () {
    return fileUuid.toString();
  }

  public static JSONObject getResults(String name) throws IOException {
    Path path = Paths.get(Config.uploadFilePath + name + "." + EXT_JSON);
    JSONObject response;
    logger.debug(String.format("%s exists? %b", path, Files.exists(path)));
    logger.debug("Reading results file at path " + path);
    String resultsFile = Files.readString(path);
    logger.debug("Got results file " + resultsFile);
    if (resultsFile.isEmpty()) {
      response = new JSONObject();
      response.put("error", "empty json");
    } else {
      response = new JSONObject(resultsFile);
    }
    return response;
  }

}
