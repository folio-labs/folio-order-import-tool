# FOLIO Order import tool

Workaround for importing orders from MARC records until FOLIO data import supports importing MARC records to create
orders.

## What does it do?

* It takes an uploaded file that contains MARC records and creates orders, instances, holdings and items in FOLIO Orders and FOLIO Inventory.
* It uses the 980 field to get the fund code, vendor code, price, tag, quantity, notes and electronic/print indicator
* It only creates items if the material is print (which should be indicated in the 980$z field) - It looks for the
  values ELECTRONIC or PRINT
* It lets FOLIO Orders create the instance, holdings and item records using the "createInventory" value in the order line
* It uses a property file to determine locations, fiscal year, note type, and
  material type and default text for electronic resources (in case subfield z is missing)

## If you want to try it

* It expects the properties file to be here: `/yourhomefolder/order/import.properties` unless you specify an alternative
  config file as an environment property on the command line: `-DconfigFile=/path/to/your.properties`
* You will have to add the Okapi userid/password, and you may have to adjust the file upload path (where it will save
  the uploaded file)
* clone the repo
* call: `mvn jetty:run [-DconfigFile=path-to-properties-file]`
* It should start a jetty server, and you should be able to point your browser to http://localhost:8888/import and try
  it
* If `exitOnConfigFailes` is set to true in the properties, the service will stop if it detects configuration problems
* If `exitOnFailedIdLookups` is set to true, the service will stop if it could not find FOLIO UUIDs for names or codes
  defined in the properties.
* We've included example MARC files, but you will have to update them with your vendor, fund, object codes
* To effectuate changes of import properties, restart the service

## Operations

#### Configurations in import.properties

A number of configuration parameters control the startup and operation of the service. 

| Name                      | Values                                                                                                                                 | Default                      | What it does                                                                                                                                                                                                                                                                                                          |
|---------------------------|----------------------------------------------------------------------------------------------------------------------------------------|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Startup**               ||||
| exitOnConfigurationErrors | boolean ¹                                                                                                                              | true                         | If true: If the service detects fatal problems with the configuration properties it will exit. False: The service will merely log configuration problems.                                                                                                                                                             | 
| exitOnFailedIdLookups     | boolean ¹                                                                                                                              | true                         | If true: The service looks up FOLIO UUIDs by names of codes of certain properties and will exit if it fails to find any. False: The service will merely log missing values.                                                                                                                                           |
| **Server**                ||||
| uploadFilePath            | File system path. The parent directory must exist on the server. The given subdirectory will be created if it doesn't exist already. ² | /var/tmp/folio-order-import/ | Directory for storing uploaded MARC files and the outcomes of imports.                                                                                                                                                                                                                                                |
 | daysToKeepResults         | number of days ³                                                                                                                       | 365                          | Results log files older than this number of days will be deleted from the server.                                                                                                                                                                                                                                     | 
| **Processing**            ||||
| onValidationErrors        | cancelAll, skipFailed, attemptImport                                                                                                   | cancelAll                    | If one or more records fail the initial validation check, this setting will cause the service to either cancel the entire import (`cancelAll`), skip the current, failed record (`skipFailed`), or attempt import anyway (`attemptImport`). With the last option, the import itself would presumably eventually fail. |
| objectCodeRequired        | boolean ¹                                                                                                                              | true                         | If true, the validation will fail if no object code is found in the incoming MARC.                                                                                                                                                                                                                                    |
| onIsbnInvalid             | removeIsbn, reportError, doNothing                                                                                                     | reportError                  | Controls if the tool should report error and perhaps skip the record, or remove the ISBN to ingest, or do nothing (which should cause the import to error out later)                                                                                                                                                  | 
| **Tool UI**               |||
| daysToShowResults         | number of days ³                                                                                                                       | 14                           | Results are listed in the UI for this number of days after first created. After that they will be skipped (but not deleted)                                                                                                                                                                                           |
| timeZone                  | A [tz time zone](https://en.wikipedia.org/wiki/List_of_tz_database_time_zones), ie `America/Chicago`                                   | Europe/Stockholm             | Sets the time zone of dates in the UI                                                                                                                                                                                                                                                                                 |
| locale                    | Language and country, i.e. `en-US`                                                                                                     | sv-SE                        | Formats dates in the UI                                                                                                                                                                                                                                                                                               | 
| folioUiUrl                | Protocol and domain of FOLIO UI, ie https://folio-snapshot.dev.folio.org/ ²                                                            | none                         | If provided, links to FOLIO's UI will be displayed for records in the import log.                                                                                                                                                                                                                                     |
| folioUiInventoryPath      | Path to the Inventory UI ²                                                                                                             | inventory/view               | Used for a link in the import log to the Instance in UI Inventory.                                                                                                                                                                                                                                                    |
| folioUiOrdersPath         | Path to the Orders UI ²                                                                                                                | orders/view                  | Used for a link in the import log to the order in UI Orders.                                                                                                                                                                                                                                                          |

[1] Boolean settings: `true`, `TRUE`, `yes`, `YES`, `y`, `Y`, and `1` will resolve to **true**, while `false`, `FALSE`, `no`, `NO`, `n`, `N`
, and `0` will resolve to **false**

[2] file paths, URLs and API paths can be with our without the ending slash (`/`) 

[3] if the provided config value is not a valid number, the default will apply

Example import.properties:

```
baseOkapiEndpoint: https://folio-snapshot.dev.folio.org/
exitOnAccessErrors: no
exitOnConfigErrors: no
exitOnFailedIdLookups: no
fiscalYearCode: FY2021
folioUiInventoryPath: inventory/view
folioUiOrdersPath: orders/view
folioUiUrl: https://uchicago-test.folio.indexdata.com/
materialType: unspecified
noteType: General note
objectCodeRequired: no
okapi_username: diku_admin
onIsbnInvalid: reportError
onValidationErrors: attemptImport
permELocation: On Order - Order
permLocation: On Order - Order
tenant: diku
textForElectronicResources: Available to snapshot users
uploadFilePath: /var/tmp/order-import
```


#### User permissions

To access the APIs, the FOLIO user defined in the `import.properties` file needs the following permissions:

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
    "organizations-storage.organizations.collection.get",
    "tags.collection.get"
  ]
}
```

#### API Calls

The service accesses following FOLIO APIs:

* GET calls to initialize reference values (fiscal years, fund codes, budgets, instance types, material types, note
  types)
* Get next PO number (GET /orders/po-number)
* Posts a purchase order (approved and open) and one line item for each MARC record in a file (POST
  /orders/composite-orders)
* Retrieves the purchase order (to get the ID of the instance FOLIO automatically created) (GET
  /orders/composite-orders/theOrderUuid)
* Retrieve the new instance (GET /inventory/instances/theInstanceId)
* Retrieves holdings record FOLIO created (GET /holdings-storage/holdings?query=(instanceId==theInstanceId))
* PUT to /instances (to update the instance with source 'MARC' and add data)  (PUT /inventory/instances/theInstanceId)
* PUT to /holdings (to add 856s to holdings) (PUT /holdings-storage/holdings/...)
* Deprecated: Optionally POST an invoice (POST /invoice/invoices)
* Deprecated: Optionally POST an invoice line (POST /invoice/invoice-lines)

#### Docker image

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

### MARC to FOLIO mappings

| MARC fields               | Description                  | Target properties                                                                                                                                                                                     | Required                                                                   | Default                                                                                | Content (incoming)                                |
|---------------------------|------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------|----------------------------------------------------------------------------------------|---------------------------------------------------|
| 020 $a ($c $q)            | Identifiers                  | instance.identifiers[].value /w type 'ISBN'                                                                                                                                                           | No                                                                         |                                                                                        |
| 020 $z ($c $q)            | Identifiers                  | instance.identifiers[].value /w type 'Invalid ISBN'                                                                                                                                                   | No                                                                         |                                                                                        |
| 022 $a ($c $q)            | Identifiers                  | instance.identifiers[].value /w type 'ISSN'                                                                                                                                                           | No                                                                         |                                                                                        |
| 022 $l ($c $q)            | Identifiers                  | instance.identifiers[].value /w type 'Linking ISSN'                                                                                                                                                   | No                                                                         |                                                                                        |
| 022 ($z $y $n)            | Identifiers                  | instance.identifiers[].value /w type 'Invalid ISSN'                                                                                                                                                   | No                                                                         |                                                                                        |
| 100, 700                  | Contributors                 | instance.contributors.name /w contributor name type 'Personal name" and contributor type from $4 or 'bkp'                                                                                             | No                                                                         |                                                                                        |
| 245 $a ($b $c)            | Instance title               | instance.title, orderLine.titleOrPackage                                                                                                                                                              | Yes                                                                        |
| 856 $u                    | URI                          | instance. electronicAccessUrl[]. uri, holdingsRecord. electronicAccessUrl[]. uri                                                                                                                      | No                                                                         |
| 856 $z                    | Link text                    | instance. electronicAccessUrl[]. linkText, holdingsRecord. electronicAccessUrl[]. linkText                                                                                                            | No                                                                         | Static config value text-For-Electronic-Resources (see separate table)                 ||
| 980 $b                    | Fund code                    | orderLine. fundDistribution[]. fundCode and (resolved to) .fundId,                                                                                                                                    | Yes                                                                        |                                                                                        | Fund code must exist in FOLIO                     |
| 980 $c                    | Vendor item id               | orderLine. vendorDetail. referenceNumbers[] .refNumber, refNumberType set to "Vendor internal number", but see 980$u                                                                                  | No                                                                         |
| 980 $m                    | Price                        | orderLine. cost. listUnitPriceElectronic or orderLine. cost. listUnitPrice                                                                                                                            | Yes                                                                        |                                                                                        | Format: [9999.99]                                 |
| 980 $n                    | Notes                        | Notes of link.type "poLine", domain "orders", and note type from config                                                                                                                               | No                                                                         |
| 980 $o                    | Object code                  | orderLine tag list                                                                                                                                                                                    | Yes - unless optional config property object-Code-Required is set to false |
| 980 $r                    | Project code                 | orderLine tag list                                                                                                                                                                                    | No                                                                         |
| 980 $v                    | Vendor code                  | order.vendor.vendorId (code resolved to id)                                                                                                                                                           | Yes                                                                        |                                                                                        | Vendor code must exist in FOLIO                   |
| 980 $z                    | Electronic indicator         | orderLine.orderFormat ("Electronic Resource" or "Physical Resource")                                                                                                                                  | No                                                                         | "Physical resource"                                                                    | Values: [ELECTRONIC] or arbitrary text or nothing |
| University of Chicago     |
| 035 $a                    | Identifiers                  | instance.identifiers[].value /w type 'System control number'                                                                                                                                          | No                                                                         |
| 856 (first occurrence) $x | User limit                   | orderLine.eResource.userLimit if ELECTRONIC                                                                                                                                                           | No                                                                         |                                                                                        | Integer                                           |
| 856 (first occurrence) $y | Access provider code         | orderLine. eresource. accessProvider if ELECTRONIC                                                                                                                                                    | No                                                                         | Vendor code (if 856$y is not present or the code does not resolve to an existing org.) |                                                   |
| 980 $e                    | Description                  | orderLine.description                                                                                                                                                                                 | No                                                                         |
| 980 $f                    | Selector                     | orderLine.selector                                                                                                                                                                                    | No                                                                         |
| 980 $g                    | Vendor account               | orderLine. vendorDetail. vendorAccount                                                                                                                                                                | No                                                                         |
| 980 $k                    | Currency                     | orderLine. cost. currency, invoice.currency                                                                                                                                                           | No                                                                         | "USD"                                                                                  | Three letter currency code                        |
| 980 $p                    | Donor                        | orderLine.donor, Electronic: holdingsRecord.notes[].note /w note type 'Electronic bookplate' and staffOnly false. Physical: item.notes[].note /w note type 'Electronic bookplate' and staffOnly false | No                                                                         |
| 980 $s                    | Bill to                      | order.billTo                                                                                                                                                                                          | No                                                                         |                                                                                        | Name of existing address in FOLIO                 |
| 980 $t (for now)          | Acquisition method           | orderLine.acquisitionMethod                                                                                                                                                                           | No                                                                         | "Purchase"                                                                             | One of nine allowed strings                       |
| 980 $u                    | Reference number type        | orderLine. vendorDetail. referenceNumbers[]. refNumberType                                                                                                                                            | No                                                                         | "Vendor internal number"                                                               ||
| 980 $w                    | Rush indicator               | orderLine.rush                                                                                                                                                                                        | No                                                                         | false                                                                                  | Values: [RUSH] or nothing                         |
| 980 $y                    | Expense class                | orderLine. fundDistribution. expenseClass                                                                                                                                                             | No                                                                         |                                                                                        | Code of an existing expense class in FOLIO        |
| 980 $v                    | See comments for 980$v above |                                                                                                                                                                                                       |

#### Static values, configured in import.properties

| Property name                      | Description                               | Examples                    | Target properties                              | Required                     | Content                                                      |
|------------------------------------|-------------------------------------------|-----------------------------|------------------------------------------------|------------------------------|--------------------------------------------------------------|
| permLocation                       | The name of a FOLIO location              | SECOND FLOOR                | orderLine.locations[].id (name resolved to id) | Yes, if physical resource    | The location must exist in FOLIO. Validated on startup.      |
| permELocation                      | The name of a FOLIO location              | SECOND FLOOR                | orderLine.locations[].id (name resolved to id) | Yes, if electronic resource  | The location must exist in FOLIO. Validated on startup.      |
| fiscalYearCode                     | The code of a FOLIO fiscal year           | FY2022                      | For resolving fund ID                          | Yes                          | Must exist in FOLIO. Validated on startup.                   |
| text For Electronic Resources      | A link text                               | Available to Snapshot Users | instance. electronicAccessEntry[]. linkText    |                              | A default.                                                   |
| noteType                           | The name of a note type for note in 980$n | General note                | notes[].note.typeId (name resolved to id)      | No                           | The note type must exist in FOLIO. Validated on startup.     |
| materialType                       | The name of a material type               | book                        | orderLine. physical. materialType              | Yes                          | The material type must exist in FOLIO. Validated on startup. |

#### Hard-coded values

| Target properties                                            | Value                                        |
|--------------------------------------------------------------|----------------------------------------------|
| orderLine.cost.currency                                      | "USD" (but see 980$k for UC)                 |
| order.orderType                                              | "One-Time"                                   |
| order.reEncumber                                             | true                                         |
| order.approved                                               | true                                         |
| order.workflowStatus                                         | "Open"                                       |
| orderLine.source                                             | "User"                                       |
| orderLine.receiptStatus                                      | "Receipt Not Required" if 980$z = ELECTRONIC |
| orderLine.fundDistribution.funds[].fundDist.distributionType | "percentage"                                 |
| orderLine.acquisitionMethod                                  | "Purchase" (but see 980$t for UC)            |
| orderLine.fundDistribution.funds[].fundDist.value            | 100                                          |
| instance.source                                              | "MARC"                                       |
| instance.instanceTypeId                                      | UUID for 'text'                              |
| instance.discoverySuppress                                   | false                                        |
| IF ELECTRONIC                                                |
| orderLine.cost.quantityElectronic                            | 1                                            |
| orderLine.eResource.activated                                | false                                        |
| orderLine.locations[].quantityElectronic                     | 1                                            |
| IF PHYSICAL                                                  |
| orderLine.cost.quantityPhysical                              | 1                                            |

### Development

Documentation on the schema for communication between the service and the UI can be found in [IMPORT_RESPONSE_SCHEMA](IMPORT_RESPONSE_SCHEMA.md) 
