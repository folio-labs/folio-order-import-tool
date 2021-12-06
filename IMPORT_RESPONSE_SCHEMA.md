## Schema for the service's response

This page is meant to help a developer working on the import log display in the import tool's UI. 

When a user runs a validation request or an import, the service will respond with the outcomes of the process in form of a JSON object, 
which is then received by the UI and rendered by templating.  

This is the format of the output to be rendered (when running `analyze`, the elements related to `import` are not currently populated). 

```
{
  "schema": {
    "name": "importResults",
    "version": "1.0"
  },
  "summary": {
    "type": import or analyze,
    "filesIdentifier": uuid for unique file names,
    "inputBaseName": the base name of the MARC file,
    "resultsBaseName": the base name of the results filed,
    "startInstantUtc": start time of the job, UTC
    "startTime": start time localized,
    "endInstantUtc": finish time for the job, UTC,
    "endTime": finish time localized,
    "recordsProcessed": count 
    "status": partial, done or error,
    "isNotDone": true/false, 
    "fatalError":  message,
    "validation": {
      "hasErrors": true/false,
      "succeeded": count,
      "failed": count
    },
    "import": {
      "hasErrors": true/false,
      "succeeded": count,
      "failed": count
    },
    "hasFlags": true/false,
    "flagged": count
  },
  "records": [
     {
       "recNo": number,
       "imported": false/true,
       "skipped": false/true,
       "hasImportError": false/true,
       "hasValidationErrors": false/true,
       "validationErrors": [
          "msg", 
          "msg",
          "msg"
       ],
       "importError": message,
       "hasFlags": true/false,
       "flags": [
          "msg",
          "msg"
       ]
       "data": {
         "title": title,
         "isbn":  isbn,
         "source": marc record,       
         "poNumber": po number,
         "poUrl": url to Folio Orders UI,
         "instanceHrid", hrid
         "instanceUrl", url to Folio Inventory UI
       }
     }
  ]
}
```