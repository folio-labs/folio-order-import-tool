# order-import-poc
Proof of concept workaround needed until FOLIO supports importing MARC records to create orders.

### What does it do?
* It takes an uploaded file that contains MARC records and creates orders, instances, holdings, items and MARC records for each.
* It uses the 980 field to get the fund code, vendor code, price, tag, quantity, notes and electronic/print indicator
* It only creates items if the material is print (which should be indicated in the 980$z field) - It looks for the values ELECTRONIC or PRINT
* It lets FOLIO create the instance, holdings and item records using the "createInventory" value in the order line
* It optionally creates an invoice
* It does all of this using the FOLIO API
* It uses a property file to determine location, fiscal year, loan type, note type and material type and default text for electronic resources (in case subfield z is missing)

### Optional Invoice import
* The script supports three modes of invoice imports, as configured in import.properties:
  - Never import an invoice: This is default behavior so either leave out any invoice properties or set `importInvoice = false`
  - Import an invoice if invoice data are found in the MARC: Set `importInvoice = true`  and `failIfNoInvoiceData = 'false'`
  - Importing an invoice is mandatory: Set `importInvoice = true` and `failIfNoInvoiceData = true`
* Will import one invoice and one invoice line

### API Calls
* Several get calls to initialize reference values (like instance types, material types, note types)
* Get next PO number (GET orders/po-number)
* Posts a purchase order (approved and open) and one line item for each MARC record in a file (POST orders/composite-orders)
* Retreives the puchase order (to get the ID of the instance FOLIO automatically created) (GET orders/composite-orders/theOrderUuid)
* Retreive the new instance (GET inventory/instances/theinstanceid)
* Posts to snapshots (POST source-storage/snapshots)
* Posts to source record storage (source-storage/records)
* Retreives holdings record FOLIO created (GET holdings-storage/holdings?query=(instanceId==theinstanceid))
* PUT to instances (to update the instance with source 'MARC' and add data)  (PUT inventory/instances/theinstancid)
* PUT to holdings (to add 856s to holdings) (PUT holdings-storage/holdings/...)
* Optionally POST an invoice (POST invoice/invoices)
* Optionally POST an invoice line (POST invoice/invoice-lines)

### If you want to try it 
* It expects the properties file to be here: /yourhomefolder/order/import.properties  -- you will have to add the okapi userid/password and you may have to adjust the file upload path (where it will save the uploaded file)
* clone the repo
* call: mvn jetty:run
* It should start a jetty server and you should be able to point your browser to http://localhost:8888/import and try it
* I've included example MARC files but you will have to update them with your vendor, fund, object codes
* The first call is a bit slow because it initializes reference values/UUIDs
* To effectuate changes of import properties, restart the service

### Mappings
|MARC fields|Description|Target properties|Req.|Default|Content (incoming)|
|-----------|-----------|-----------------|--------|-------|--------|
|020 $a ($c $q)|Identifiers|instance.identifiers[].value /w type 'ISBN'|No| |
|020 $z ($c $q)|Identifiers|instance.identifiers[].value /w type 'Invalid ISBN'|No| |
|022 $a ($c $q)|Identifiers|instance.identifiers[].value /w type 'ISSN'|No| |
|022 $l ($c $q)|Identifiers|instance.identifiers[].value /w type 'Linking ISSN'|No| |
|022 ($z $y $n)|Idenfifiers|instance.identifiers[].value /w type 'Invalid ISSN'|No| |
|100, 700|Contributors|instance.contributors.name /w contributor name type 'Personal name" and contributor type from $4 or 'bkp'|No| |
|245 $a ($b $c)|Instance title|instance.title, orderline.titleOrPackage|Yes|
|856 $u |URI|instance. electronicAccess[]. uri, holdingsRecord.electronicAccess[].uri|No|
|856 $z |Link text|instance. electronicAccess[]. linkText, holdingsRecord. electronicAccess[]. linkText|No|Static config value text-For-Electronic-Resources (see separate table)||
|980 $b |Fund code|orderLine. funDistribution[]. fundCode and (resolved to) .fundId, | Yes| |Fund code must exist in FOLIO|
|980 $c |Vendor item id|orderLine. vendorDetail. referenceNumbers[] .refNumber, refNumberType set to "Vendor internal number", but see 980$u|No|
|980 $m |Price|orderLine.cost.listUnitPriceElectronic or orderLine.cost.listUnitPrice|Yes| |Format: [9999.99]|
|980 $n |Notes|Notes of link.type "poLine", domain "orders", and note type from config|No|
|980 $o |Object code|orderLine tag list|Yes|
|980 $r |Project code|orderLine tag list|No|
|980 $v |Vendor code|order.vendor.vendorId (code resolved to id)|Yes| |Vendor code must exist in FOLIO|
|980 $z |Electronic indicator|orderLine.orderFormat ("Electronic Resource" or "Physical Resource")|No|"Physical resource"|Values: [ELECTRONIC], arbitrary text, nothing|
|UCHICAGO|
|035 $a |Identifiers|instance.identifiers[].value /w type 'System control number'|No|
|980 $f |Selector|orderLine.selector|No|
|980 $g |Vendor account|orderLine.vendorDetail.vendorAccount|No|
|980 $p |Donor|orderLine.donor, holdingsRecord.notes[].note /w note type 'Electronic bookplate' and staffOnly false|No|
|980 $u |Reference number type|orderLine. vendorDetail. referenceNumbers[]. refNumberType|No|"Vendor internal number"||
|980 $w |Rush indicator|orderLine.rush|No|false|Values: [RUSH], nothing|
|INVOICES|
|980 $h|Vendor invoice no|invoice.vendorInvoiceNo|Yes*|
|980 $i|Invoice date|invoice.invoiceDate|Yes*| |Format: [YYYY-MM-DD]|
|980 $j|Sub total|invoiceLine.subTotal|No| |Format: [9999.99]|
|980 $v|See comments for 980$v above| |
`*` if importing invoices

#### Configured static values
|Property name|Description|Examples|Target properties|Required|Content|
|-------------|-------|--------|----------------|--------|-------|
|permELocation|The name of a FOLIO location|SECOND FLOOR|orderLine.locations[].id (name resolved to id)|Yes, if electronic resource|The location must exist in FOLIO|
|permLocation|The name of a FOLIO location|SECOND FLOOR|orderLine.locations[].id (name resolved to id)|Yes, if physical resource|The location must exist in FOLIO|
|text For Electronic Resources|A link text|Available to Snapshot Users|instance. electronicAccess[]. linkText|
|noteType|The name of a note type for note in 980$n|General note|notes[].note.typeId (name resolved to id)|No|The note type must exist in FOLIO|
|materialType|The name of a material type|book|orderLine. physical. materialType| |The material type must exist in FOLIO|
|INVOICES|
|paymentMethod|Payment method code|EFT|invoice.paymentMethod|Yes|One of a list of enumerated values|

#### Hard-coded values
|Target properties|Value|
|-----------------|-----|
|orderLine.cost|"USD"|
|order.orderType|"One-Time"|
|order.reEncumber|false|
|order.approved|true|
|order.workflowStatus|"Open"|
|orderLine.source|"User"|
|orderLine.acquisitionMethod|"Purchase"|
|orderLine.fundDistribution.funds[].fundDist.distributionType|"percentage"|
|orderLine.fundDistribution.funds[].fundDist.value|100|
|instance.source|"MARC"|
|instance.instanceTypeId|UUID for 'text'|
|instance.discoverySuppress|false|
|IF ELECTRONIC|
|orderLine.cost.quantityElectronic|1|
|orderLine.eResource.activated|false|
|orderLine.locations[].quantityElectronic|1|
|IF PHYSICAL|
|orderLine.cost.quantityPhysical|1|
|INVOICES|
|invoice.batchGroupId|UUID of batch group "FOLIO"|
|invoice.source|"API"|
|invoice.status|"Open"|
|invoiceLine.invoiceLineStatus|"Open"|
|invoiceLine.quantity|1|
|invoiceLine.releaseEncumbrance|true|

### Lots of areas for improvement including:
* Better way to get data out of the MARC record to include on the instance. 
* Better way to store reference values needed for lookup
* Current version contains some hard-coded values (e.g. currency: USD)
* If duplicate PO number error - get the next PO number and try again

### What's new?
* 11-16-2020
  - Removed reference to the 001 field.  Wasn't necessary and was causing an error when it was missing.
 
* 9-23-2020
  - Removed the check for enough money in the budget
  - Fixed where the electronic indicator is initialized (needed to be per record)
  
* 7-31-2020
  - Better handling of special characters
  - Handles multiple records in a file
  - Validates each record in the file

* (March/April 2021)
  - Optional import of an invoice, and an invoice line
  - Additional mappings of fields to order and order line.