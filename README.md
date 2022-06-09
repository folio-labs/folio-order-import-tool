# FOLIO Order import tool

Workaround for importing orders from MARC records until FOLIO Data Import supports importing MARC records to create
orders.

## What does it do?

* It takes an uploaded file that contains MARC records, and creates orders, instances, holdings and items in FOLIO Orders and FOLIO Inventory.
* It uses the 980 field to get fund code, vendor code, price, notes, electronic/print indicator, etc. to create the order.
* It lets FOLIO Orders create the instance, holdings and item records using the "createInventory" value in the order line
* It only asks FOLIO Orders to create items if the material is print
* It uses a property file to determine constants such as locations, fiscal year, note type, and material type and default text for electronic resources

For details, see [What data are populated to FOLIO](#what-data-are-populated-to-folio).

### It has two modes, 'validate only' or 'validate and import'

The MARC file can be run through validation without actually importing anything to FOLIO. Choose a MARC file in the tool's UI and click `Analyze MARC records`. For details about the checks it performs, see [Validation of incoming MARC records](#validation-of-incoming-marc-records)

When running an actual import, the exact same checks are performed for each record, and the tool then acts on the results according to the setting of configuration parameter `onValidationErrors`. See options for this setting in the [configuration table](#how-to-configure-the-service) 

![ui-screen-shot](https://user-images.githubusercontent.com/11644885/146233019-fe7b87d0-a3a0-4372-b135-6a22d14c06e5.png)


Imports can take a while for files with many MARC records, so imports are started in the back-ground. Right after starting an import, the UI will display a status with the state 'started' and a count of MARC records in the file and a 'Refresh' link that you can click to get updates on progress. 

![ui-import-started](https://user-images.githubusercontent.com/11644885/146233538-9b19b43f-2875-491b-b41f-68631bcc66fd.png)

Once the job is finished, clicking the 'Refresh' will display a state of 'done' and detailed results for each MARC record in the file.

## If you want to try it

* Clone the repo.
* The tool expects a properties file to be specified by one of these methods:
    1. A environment variable on the command line, named 'config', like `-Dconfig=/path/to/your.properties`.
    1. A ServletContext attribute, named 'config', specified in the servlet container configuration. Useful for [deploying multiple instances](#multiple-instances) in one servlet container.
    1. If neither of the above 'config' variables are set, it expects to find the properties file here: `/yourhomefolder/order/import.properties`.
* You will have to set your Okapi address, FOLIO tenant, userid and password in the properties, and you may have to adjust the file upload path where it will save
  the uploaded file and store a history of validations and imports, see the [How to configure the service](#how-to-configure-the-service)
* Call: `mvn jetty:run [-Dconfig=path-to-properties-file]`
* If the configuration is okay, it should start a jetty server, and you should be able to point your browser to http://localhost:8888/import and try
  it
* However, unless `exitOnConfigErrors` is set to false in the properties, the service will stop if it detects fatal configuration problems
* Unless `exitOnAccessErrors` is set to false, the service will stop if it fails to log in to Okapi. 
* And, unless `exitOnFailedIdLookups` is set to false, the service will stop if it could not find FOLIO UUIDs for names or codes
  defined in the properties.
* We've included example MARC files, but you will have to update them with your vendor, fund, object codes, etc.
* To effectuate changes of import properties, restart the service

### Docker image

You can build a Docker image using the [Dockerfile](Dockerfile) in this repository.

1. Build the WAR for the webapp: `mvn install`
1. Build the Docker image: `docker build .`
1. Run the container: `docker run -d -p 8080:8080 <imageId>`

This will run a [Jetty](https://hub.docker.com/_/jetty) container with the order import webapp as the root, using the
default configuration in the [import.properties](import.properties) file. This will work against the
FOLIO [folio-snapshot](https://folio-snapshot.dev.folio.org) reference environment.

To override the default configuration, mount your configuration to `/var/lib/jetty/order/import.properties` on the
container e.g.:

    docker run -d -v $(pwd)/order:/var/lib/jetty/order -p 8080:8080 <imageId>

### Multiple instances

You may want to run multiple instances of the tool in the same server environment, using separate configuration files. For example, this would allow separate production and test instances that each point to the appropriate FOLIO API endpoint.

Note that the webapp expects to be running at the root path of its server.  So first set up separate virtual hosts for each instance.

* Add a CNAME my-instance-name.domain.edu referencing the actual server.
* Configure a reverse proxy to point the root path of that virtual host to a distinct path on the actual server.  For example in Apache, 

```
<VirtualHost ....>
  ....
  ServerName my-instance-name.domain.edu
  
  ProxyPreserveHost On
  
  RewriteEngine on
  RewriteRule ^$ /import [L]
  
  <Location />
    ....
   
    RequestHeader set X-Forwarded-Proto "https" env=HTTPS
    ProxyPass http://my-instance-name.domain.edu:8080/my-instance-name/
    ProxyPassReverse http://my-instance-name.domain.edu:8080/my-instance-name/
  </Location>
  
  ....
</VirtualHost>
```
Then configure each app instance to point to a separate properties file using a ServletContext attribute in its servlet configuration.  In Jetty, as an example, define a servlet properties file (in the same directory and with the same filename as the .war, but extension .xml) such as:

```
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE Configure PUBLIC "-//Jetty//Configure//EN" "http://www.eclipse.org/jetty/configure_9_3.dtd">

<Configure class="org.eclipse.jetty.webapp.WebAppContext">
  <Set name="contextPath">/my-instance-name</Set>
  <Set name="war">/usr/share/jetty9/webapps/my-instance-name.war</Set>
  <Get name="ServletContext">
    <Call name="setAttribute">
      <Arg>config</Arg>
      <Arg>/some-folder-with-configuration-files/import-my-instance-name.properties</Arg>
    </Call>
  </Get>
</Configure>
```

## How to configure the service

The startup configuration has parameters controlling FOLIO access, start-up behavior, server settings, process controls, UI settings and static values populated to FOLIO. The table show all parameters except for the static FOLIO data that are described in the subsequent paragraphs.

***FOLIO access settings, startup behavior, server settings for the tool, import process controls, UI settings***

| Name                      | Values                                                                                                                                 | Default                      | What it does                                                                                                                                                                                                                                                                                                          |
|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **FOLIO access**          ||||
| baseOkapiEndpoint         | Protocol and domain of the FOLIO backend REST service<br/>i.e. https://folio-snapshot.dev.folio.org/                                   ||                              |
| tenant                    | FOLIO tenant, i.e. 'diku'                                                                                                              |||
| okapi_username            | The FOLIO user name of the import user i.e. 'diku_admin'                                                                               |||
| okapi_password            | The password for the import user                                                                                                       |||
| **Startup**               ||||
| exitOnConfigurationErrors | boolean ¹                                                                                                                              | true                         | If set to true (the default), the service will exit if it detects fatal problems with the configuration properties -- like missing mandatory properties. <br/>If set to false, it will merely log configuration problems.                                                                                             | 
| exitOnAccessErrors        | boolean ¹                                                                                                                              | true                         | If set to true (the default), the service will exit if it fails to gain access to FOLIO for any reason (wrong URL, tenant, username or service down etc). <br/>If set to false, it will merely log access problems.                                                                                                   |
| exitOnFailedIdLookups     | boolean ¹                                                                                                                              | true                         | If set to true (the default), the service will exit if it fails to resolve configured names of codes to FOLIO UUIDs. <br/>If set to false, it will merely log missing values.                                                                                                                                         |
| **Server**                ||||
| uploadFilePath            | File system path. The parent directory must exist on the server. The given subdirectory will be created if it doesn't exist already. ² | /var/tmp/folio-order-import/ | Directory for storing uploaded MARC files and the outcomes of imports.                                                                                                                                                                                                                                                |
 | daysToKeepResults         | number of days ³                                                                                                                       | 365                          | Results log files older than this number of days will be deleted from the server.                                                                                                                                                                                                                                     |
| marcMapping               | `chi`, `lambda`, or `sigma`                                                                                                            | chi                          | Selects a MARC mapping option. The available mapping options are described in the mapping tables below.                                                                                                                                                                                                               |
| **Processing**            ||||
| purchaseOrderUnit         | `file` or `record`                                                                                                                     | record                       | Instructs the tool to create one purchase order per file or one purchase order per record (default).                                                                                                                                                                                                                  |
| onValidationErrors        | `cancelAll`, `skipFailed` or `attemptImport`                                                                                           | cancelAll                    | If one or more records fail the initial validation check, this setting will cause the service to either cancel the entire import (`cancelAll`), skip the current, failed record (`skipFailed`), or attempt import anyway (`attemptImport`). With the last option, the import itself would presumably eventually fail. |
| onIsbnInvalid             | `removeIsbn`, `reportError` or `doNothing`                                                                                             | reportError                  | Controls if the tool should report error and perhaps skip the record, or remove the ISBN to ingest, or do nothing (which should cause the import to error out later)                                                                                                                                                  | 
| **Tool UI**               |||
| daysToShowResults         | number of days ³                                                                                                                       | 14                           | Results are listed in the UI for this number of days after first created. After that they will be skipped (but not deleted)                                                                                                                                                                                           |
| tzTimeZone                | A [tz time zone](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones), ie `America/Chicago`                                   | Europe/Stockholm             | Sets the time zone of dates in the UI                                                                                                                                                                                                                                                                                 |
| locale                    | Language and country, i.e. `en-US`                                                                                                     | sv-SE                        | Formats dates in the UI                                                                                                                                                                                                                                                                                               | 
| folioUiUrl                | Protocol and domain of FOLIO UI, ie https://folio-snapshot.dev.folio.org/ ²                                                            | none                         | If provided, links to FOLIO's UI will be displayed for records in the import log.                                                                                                                                                                                                                                     |
| folioUiInventoryPath      | Path to the Inventory UI ²                                                                                                             | inventory/view               | Used for a link in the import log to the Instance in UI Inventory.                                                                                                                                                                                                                                                    |
| folioUiOrdersPath         | Path to the Orders UI ²                                                                                                                | orders/view                  | Used for a link in the import log to the order in UI Orders.                                                                                                                                                                                                                                                          |


1) Boolean settings: `true`, `TRUE`, `yes`, `YES`, `y`, `Y`, and `1` will resolve to **true**, while `false`, `FALSE`, `no`, `NO`, `n`, `N`
, and `0` will resolve to **false**
2) file paths, URLs and API paths can be with our without the ending slash (`/`)
3) if the provided config value is not a valid number, the default will apply

## What data are populated to FOLIO
The tool populates FOLIO Orders and Inventory with data from three different sources: 
1) The incoming MARC records
2) Static values defined as configuration parameters
3) Static values hard-coded in the program. 

### Data from the incoming MARC records
The tool provides three, slightly different sets of MARC mappings. All three share a basic set of mappings but then extend that with some mappings of their own. 

The desired mapping is selected at startup by setting the parameter `marcMapping` to either `chi` (default), `lambda` or `sigma`. 

#### Core mapping table

| MARC_fields¹       | Description                    | Target properties                                                                                                                                                                                     | Required                                 | Default                                                                                | Content (incoming)                                                                                     |
|--------------------|--------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------|----------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| 020 $a ($c $q)     | ISBN                           | orderLine.details.productIds[] <br/>instance.identifiers[]                                                                                                                                            | No, but a note will be logged if missing |                                                                                        |
| 020 $z ($c $q)     | Invalid ISBN                   | instance.identifiers[]                                                                                                                                                                                | No                                       |                                                                                        |
| 022 $a ($c $q)     | ISSN                           | orderLine.details.productIds[] <br/>instance.identifiers[]                                                                                                                                            | No                                       |                                                                                        |
| 022 $l ($c $q)     | Linking ISSN                   | instance.identifiers[]                                                                                                                                                                                | No                                       |                                                                                        |
| 022 ($z $y $n)     | Invalid ISSN                   | instance.identifiers[]                                                                                                                                                                                | No                                       |                                                                                        |
| 024 $a             | Other standard identifier      | orderLine.details.productIds[]<br/>instance.identifiers[]                                                                                                                                             | No                                       | 
| 025 $a             | Other standard identifier      | orderLine.details.productIds[]<br/>instance.identifiers[]                                                                                                                                             | No                                       |
| 028 $a             | Publisher/Distributor number   | orderLine.details.productIds[]<br/>instance.identifiers[]                                                                                                                                             | No                                       |
| 035 9$a            | System control number          | instance.identifiers[]                                                                                                                                                                                | No                                       |
| 041 $a             | Languages                      | instance.languages[]                                                                                                                                                                                  | No                                       |                                                                                        |                                                                                                        |                                                                                                                                                                                  
| 100, 700           | Contributors                   | instance.contributors.name /w contributor name type 'Personal name" and contributor type from $4 or 'bkp'                                                                                             | No                                       |                                                                                        |
| 245 $a ($b $c $p)  | Instance title and index title | instance.title, instance.indexTitle (with initial article removed), orderLine.titleOrPackage                                                                                                          | Yes                                      |
| 250 $a             | Edition                        | instance.editions[]                                                                                                                                                                                   | No                                       |                                                                                        |                                                                                                        |
| 336 $a             | Resource type                  | instance.instanceTypeId                                                                                                                                                                               | No                                       | text                                                                                   |                                                                                                        |
| 337$,338$a         | Format                         | instance.instanceFormatIds[]                                                                                                                                                                          | No                                       |                                                                                        | 337$a +  " -- " + 338$a                                                                                |
| 490 $a,l,v,x,3,6,8 | Series                         | instance.series[]                                                                                                                                                                                     | No                                       |                                                                                        |
| 856 $u             | URI                            | instance. electronicAccessUrl[]. uri, holdingsRecord. electronicAccessUrl[]. uri                                                                                                                      | No                                       |
| 856 $x             | User limit                     | orderLine.eResource.userLimit if ELECTRONIC, but see Lambda                                                                                                                                           | No                                       |                                                                                        | Integer                                                                                                |
| 856 $y             | Access provider code           | orderLine. eresource. accessProvider if ELECTRONIC                                                                                                                                                    | No                                       | Vendor code (if 856$y is not present or the code does not resolve to an existing org.) |                                                                                                        |
| 856 $z             | Link text                      | instance. electronicAccessUrl[]. linkText, holdingsRecord. electronicAccessUrl[]. linkText                                                                                                            | No                                       | Static config value text-For-Electronic-Resources (see separate table)                 ||
| 980 $b             | Fund code                      | orderLine. fundDistribution[]. fundCode and (resolved to) .fundId,                                                                                                                                    | Yes                                      |                                                                                        | Fund code must exist in FOLIO for the given fiscal year                                                |
| 980 $c             | Vendor item id                 | orderLine. vendorDetail. referenceNumbers[] .refNumber, refNumberType set to "Vendor internal number", but see 980$u                                                                                  | No                                       |
| 980 $e             | Description                    | orderLine.description                                                                                                                                                                                 | No                                       |
| 980 $f             | Selector                       | orderLine.selector                                                                                                                                                                                    | No                                       |
| 980 $g             | Vendor account                 | orderLine. vendorDetail. vendorAccount                                                                                                                                                                | No                                       |
| 980 $k             | Currency                       | orderLine. cost. currency, invoice.currency                                                                                                                                                           | No                                       | "USD"                                                                                  | Three letter currency code                                                                             |
| 980 $l             | Access provider code           |                                                                                                                                                                                                       |                                          |                                                                                        |                                                                                                        |
| 980 $m             | Price                          | orderLine. cost. listUnitPriceElectronic or orderLine. cost. listUnitPrice                                                                                                                            | Yes                                      |                                                                                        | Format: [9999.99]                                                                                      |
| 980 $n             | Notes                          | Notes of link.type "poLine", domain "orders", and note type from config                                                                                                                               | No                                       |
| 980 $p             | Donor                          | orderLine.donor, Electronic: holdingsRecord.notes[].note /w note type 'Electronic bookplate' and staffOnly false. Physical: item.notes[].note /w note type 'Electronic bookplate' and staffOnly false | No                                       |
| 980 $s             | Bill to                        | order.billTo                                                                                                                                                                                          | No                                       |                                                                                        | Name of existing address in FOLIO                                                                      |
| 980 $t             | Acquisition method             | orderLine.acquisitionMethod                                                                                                                                                                           | No                                       | "Purchase"                                                                             | One of nine allowed strings                                                                            |
| 980 $u             | Reference number type          | orderLine. vendorDetail. referenceNumbers[]. refNumberType                                                                                                                                            | No                                       | "Vendor internal number"                                                               ||
| 980 $v             | Vendor code                    | order.vendor.vendorId (code resolved to id)                                                                                                                                                           | Yes                                      |                                                                                        | Vendor code must exist in FOLIO                                                                        |
| 980 $w             | Rush indicator                 | orderLine.rush                                                                                                                                                                                        | No                                       | false                                                                                  | Values: [RUSH] or nothing                                                                              |
| 980 $y             | Expense class                  | orderLine. fundDistribution. expenseClass                                                                                                                                                             | No                                       |                                                                                        | Code of an existing expense class that must be assigned to a budget in FOLIO for the given fiscal year |
| 980 $z             | Electronic indicator           | orderLine.orderFormat ("Electronic Resource" or "Physical Resource"), holdingsRecord.holdingsTypeId (electronic or physical)                                                                          | No                                       | "Physical resource"                                                                    | Values: [ELECTRONIC] or arbitrary text or nothing                                                      |

1) For the repeatable fields 250, 336, 337, 338, 490, 856 and 980, only the first occurrence is considered

#### `Chi` mapping extension

| MARC_fields¹ | Description       | Target properties | Required | Default | Content (incoming)                             |
|--------------|-------------------|-------------------|----------|---------|------------------------------------------------|
| 980 $o       | Barcode           | item.barcode      | No       |         | A barcode that doesn't exist already in FOLIO. |
| Hard-coded   | Item copy number  | item.copyNumber   |          | 'c.1'   |                                                |

#### `Lambda` mapping extension

| MARC_fields¹  | Description    | Target properties     | Required | Default | Content (incoming)                             |
|---------------|----------------|-----------------------|----------|---------|------------------------------------------------|
| 856 $x        | User limit     | n/a                   | No       |         | This field is not submitted to FOLIO.          |
| 980 $o        | Object code    | orderLine tag list    | Yes      |         | The object code must exist as a Tag in FOLIO.  | 
| 980 $r        | Project code   | orderLine tag list    | No       |         | The project code must exist as a Tag in FOLIO. |

#### `Sigma` mapping extension 

| MARC_fields¹ | Description     | Target properties                                                | Required            | Default                   | Content (incoming) |
|--------------|-----------------|------------------------------------------------------------------|---------------------|---------------------------|--------------------|
| 980 $a       | Location        | orderLine.locations[].id (name resolved to id)                   | Yes                 |                           |                    | 
| 980 $d       | Material type   | orderLine.physical.materialType and item.materialTypeId          | No                  | Configured material type  |                    |
| 980 $q       | Quantity        | orderLine.locations[] .quantityPhysical or .quantityElectronic   | No (defaults to 1)  | 1                         |                    |
| 980 $r       | Loan type       | item.permanentLoanTypeId                                         | Yes                 |                           |                    |
### Pre-configured values, defined as parameters in the startup properties file
| Property name                      | Description                               | Examples                    | Target properties                              | Required                     | Content                                                      |
|------------------------------------|-------------------------------------------|-----------------------------|------------------------------------------------|------------------------------|--------------------------------------------------------------|
| permLocation                       | The name of a FOLIO location              | SECOND FLOOR                | orderLine.locations[].id (name resolved to id) | Yes, if physical resource    | The location must exist in FOLIO. Validated on startup.      |
| permELocation                      | The name of a FOLIO location              | SECOND FLOOR                | orderLine.locations[].id (name resolved to id) | Yes, if electronic resource  | The location must exist in FOLIO. Validated on startup.      |
| fiscalYearCode                     | The code of a FOLIO fiscal year           | FY2022                      | For resolving fund ID                          | Yes                          | Must exist in FOLIO. Validated on startup.                   |
| text For Electronic Resources      | A link text                               | Available to Snapshot Users | instance. electronicAccessEntry[]. linkText    |                              | A default.                                                   |
| noteType                           | The name of a note type for note in 980$n | General note                | notes[].note.typeId (name resolved to id)      | No                           | The note type must exist in FOLIO. Validated on startup.     |
| materialType                       | The name of a material type               | book                        | orderLine. physical. materialType              | Yes                          | The material type must exist in FOLIO. Validated on startup. |


### Static values, hard-coded in the program

| Target properties                                            | Value                                        |
|--------------------------------------------------------------|----------------------------------------------|
| order.orderType                                              | "One-Time"                                   |
| order.reEncumber                                             | true                                         |
| order.approved                                               | true                                         |
| order.workflowStatus                                         | "Open"                                       |
| orderLine.source                                             | "User"                                       |
| orderLine.receiptStatus                                      | "Receipt Not Required" if 980$z = ELECTRONIC |
| orderLine.fundDistribution.funds[].fundDist.distributionType | "percentage"                                 |
| orderLine.fundDistribution.funds[].fundDist.value            | 100                                          |
| instance.source                                              | "FOLIO"                                      |
| instance.discoverySuppress                                   | false                                        |
| IF ELECTRONIC                                                |
| orderLine.cost.quantityElectronic                            | 1                                            |
| orderLine.eResource.activated                                | false                                        |
| orderLine.locations[].quantityElectronic                     | 1                                            |
| holdingsRecord.holdingsTypeId                                | The holdings type ID for Electronic          |
| IF PHYSICAL                                                  |
| orderLine.cost.quantityPhysical                              | 1                                            |
| holdingsRecord.holdingsTypeId                                | The holdings type ID for Physical            |

## Validation of incoming MARC records

Each incoming record will go through following checks on analyze or import:

* The record has a MARC field 980.
* The record provides a fund code that exists in FOLIO Finance.
* The provided fund code is associated with an existing fiscal year (making it a budget) in FOLIO Finance.
* If the record provides an expense class code, it exists in FOLIO Finance where it is associated with the budget.
* The record provides a vendor code that exists in FOLIO Organizations.
* The record provides a price amount.
* If the record provides a bill-to name, an address with that name exists in FOLIO Configurations.
* (when this API comes into production: If the record provides an acquisition method, the method exists in FOLIO Orders)

If any of these checks fail it will be marked as an error.

On top of that the analysis will check if

* The record has an ISBN that is valid (after stripping the ISBN string down to its leading digits)
* If there is no valid ISBN, that the record provides one or more of the following identifiers: ISSN, Publisher or Distributor number, System control number, or Other standard Identifier.

Missing identifiers is not considered an error that should prevent the import, but a flag is raised since it will prevent FOLIO Orders from linking the incoming order with an existing title in FOLIO Inventory, thus triggering the creation of new records in Inventory.

## FOLIO API usage and required user permissions

### API Calls

This is how the tool uses FOLIO's APIs:

* It makes requests to a number of reference data APIs to map reference values (fiscal years, fund codes, budgets, instance types, material types, note
  types, tags)
* It gets the next PO number from Orders
* It posts a purchase order (approved and open) and one line item for each MARC record in a file 
* It retrieves, then put the new/linked Instance from Inventory
* It optionally retrieves, then put the new/linked holdings record 
* It optionally posts notes for the Order

#### APIs used
```
  orders/po-number
  orders/composite-orders
  orders/acquisition-methods¹

  inventory/instances
  holdings-storage/holdings
  inventory/items
  instance-types
  material-types
  contributor-types
  holdings-types
  note-types
  locations

  finance/expense-classes
  finance/funds
  finance/fiscal-years
  finance/budgets
  finance-storage/budget-expense-classes

  organizations-storage/organizations

  notes
  tags

  configurations/entries
````
1) starting from FOLIO release Lotus.

### Required permissions 
```json
{
  "permissions": [
    "configuration.entries.collection.get",
    "finance.budgets.collection.get",
    "finance.expense-classes.collection.get",
    "finance.fiscal-years.collection.get",
    "finance.funds.collection.get",
    "finance-storage.budget-expense-classes.collection.get",
    "inventory.instances.collection.get",
    "inventory.instances.item.get",
    "inventory.instances.item.post",
    "inventory.instances.item.put",
    "inventory.items.collection.get",
    "inventory.items.item.post",
    "inventory.items.item.put",
    "inventory-storage.classification-types.collection.get",
    "inventory-storage.contributor-name-types.collection.get",
    "inventory-storage.contributor-types.collection.get",
    "inventory-storage.identifier-types.collection.get",
    "inventory-storage.holdings.collection.get",
    "inventory-storage.holdings.item.get",
    "inventory-storage.holdings.item.post",
    "inventory-storage.holdings.item.put",
    "inventory-storage.holdings-types.collection.get",
    "inventory-storage.instance-types.collection.get",
    "inventory-storage.items.item.get",
    "inventory-storage.locations.collection.get",
    "inventory-storage.loan-types.collection.get",
    "inventory-storage.material-types.collection.get",
    "invoice.invoices.collection.get",
    "invoice.invoices.item.get",
    "invoice.invoices.item.post",
    "invoice.invoice-lines.item.post",
    "invoice.invoice-lines.collection.get",
    "invoice.invoice-lines.item.get",
    "note.types.collection.get",
    "notes.domain.all",
    "notes.item.post",
    "orders.collection.get",
    "orders.item.get",
    "orders.item.post",
    "orders.po-number.item.get",
    "orders.acquisition-methods.collection.get",
    "organizations-storage.organizations.collection.get",
    "tags.collection.get"
  ]
}
```

### Development notes

Documentation on the schema for communication between the service and the UI can be found in [IMPORT_RESPONSE_SCHEMA](IMPORT_RESPONSE_SCHEMA.md) 
