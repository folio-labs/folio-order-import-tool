# order-import-poc
Proof of concept workaround needed until FOLIO supports importing MARC records to create orders.

### What does it do?
* Inserts MARC into source record storage
* Inserts instance
* Inserts holdings
* If it is a print item, it inserts an item.  (for now it knows this using an indicator in the 980 field)
* Using values from the 980 field (code for organization and code for fund) it creates an approved purchase order
* Adds one purchase order line item (hard-coded for now to just be one)
* Opens the purchase order
* Links the 'piece' created by FOLIO to the item UUID

### If you want to try it (maven is needed)
* It expects the properties file to be here: /yourhomefolder/order/import.properties  -- you will have to add the okapi userid/password
* clone the repo
* call: mvn jetty:run    (using this for convenience for testing - won't go live like this - re: security alert)
* It should start a jetty server and you should be able to point your browser to http://localhost:8888/order and try it
* I've included example MARC files with organization and fund codes that are in FOLIO snapshot
* The first call is a bit slow because it initializes reference values/UUIDs

### What isn't working
* While testing, when I "Receive" an order created through this process and change the location, the new location doesn't show up on holdings or item
* I was able to edit my 'electronic' MARC record that was created using this process with quickMARC.  I wasn't able to open the 'print' MARC record

### Lots of areas for improvement including:
* Better way to get data out of the MARC record to include on the instance. 
* Better way to store reference values needed for lookup





