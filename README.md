# order-import-poc
Proof of concept workaround needed until FOLIO supports importing MARC records to create orders.

### What does it do?
* It takes an uploaded file that contains MARC records and creates orders, instances, holdings, items and MARC records for each.
* It uses the 980 field to get the fund code, vendor code, price, tag, quantity, notes and electronic/print indicator
* It only creates items if the material is print (which should be indicated in the 980$z field) - It looks for the values ELECTRONIC or PRINT
* It lets FOLIO create the instance, holdings and item records using the "createInventory" value in the order line
* It does all of this using the FOLIO API
* It uses a property file to determine location, fiscal year, loan type, note type and material type and default text for electronic resources (in case subfield z is missing)

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

### If you want to try it 
* It expects the properties file to be here: /yourhomefolder/order/import.properties  -- you will have to add the okapi userid/password and you may have to adjust the file upload path (where it will save the uploaded file)
* clone the repo
* call: mvn jetty:run
* It should start a jetty server and you should be able to point your browser to http://localhost:8888/import and try it
* I've included example MARC files but you will have to update them with your vendor, fund, object codes
* The first call is a bit slow because it initializes reference values/UUIDs
* To build the war files you can use:
 * mvn clean package -Ptest       
 * or
 * mvn clean package
 * The -Ptest version will create a war file that will look for an import-test.properties file which can contain properties for your test server.  It will also reflect 'test' on the jsp.

### Lots of areas for improvement including:
* Better way to get data out of the MARC record to include on the instance. 
* Better way to store reference values needed for lookup
* Current version contains some hard-coded values (e.g. currency: USD)
* If duplicate PO number error - get the next PO number and try again

### What's new?
* 05-10-2021
  - build a 2nd war for testing (mvn clean package or mvn clean package -Ptest)

* 11-16-2020
  - Removed reference to the 001 field.  Wasn't necessary and was causing an error when it was missing.
 
* 9-23-2020
  - Removed the check for enough money in the budget
  - Fixed where the electronic indicator is initialized (needed to be per record)
  
* 7-31-2020
  - Better handling of special characters
  - Handles multiple records in a file
  - Validates each record in the file 






