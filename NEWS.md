## 1.7.2 2022-06-09

* Update Dockerfile to specify Jetty 10 as the base image.

## 1.7.1 2022-06-09

* Sigma mapping: Only sets note type on items if there's a donor. 

## 1.7.0 2022-05-01

* Sets instance.indexTitle to title with initial article removed.

## 1.6.0 2022-05-01

* Escapes all title characters that are CQL special characters.
* Maps name-of-part (245$p) to end of title.
* Maps editions from first 250$a.
* Maps series from first 490, all subfields except $y, $z.
* Maps languages from 041$a.
* Sets holdings type for physical resources, too (as for electronic).
* Maps resource type from first 336$a (default still 'text').
* Maps instance formats from first 337$a " -- " 338$a, concatenated.
* Chi mapping: Sets item copy number to 'c.1'.

## 1.5.1 2022-03-17

* Workaround for CQL parser issue affecting titles with quotes  
* Log4J upgrade for security issue

## 1.5.0 2022-02-02

* Supports ISBNs with last digit of 'X'.
* Lambda mapping: ignores 856$x 'userLimit' field, so it can be used with non-integer values.
* Supports passing import properties file name as ServletContext attribute.
* Chi mapping: Add OCLC number to Instance identifiers.
* Chi mapping: Use only ISBN for product ID (and thus for linking to existing Instance).
* Update only newly created item(s); in the rare case holdings record has previously existing items.

## 1.4.2 2022-01-21

* Fixes premature use of coming 'holdingId' property in JSON response to POST of PO

## 1.4.1 2022-01-20

* Fixes bug in location validation for MARC records with multiple 980s

## 1.4.0 2022-01-19

* Supports one-PO-per-file and multiple-locations-per-MARC-record
* Adds MARC mapping extension 'sigma'

## 1.3.2 2022-01-18

* Bug-fix: Validates Instance ISBNs without including any qualifiers.

## 1.3.1 2021-12-17

* Bug-fix: Removes mistaken attempt to update Item for electronic

## 1.3.0 2021-12-13

* Chi mapping: maps barcode from 980$o
* Provides three sets of MARC-to-FOLIO mappings: Chi, Lambda, Sigma.
* The tool adapts to coming schema change in Orders wrt acquisition method
* The validation logic checks fund, budget, budget expense class, and price.

## 1.2.0 2021-12-09

* Configurable time zone for dates shown in UI
* Configurable locale for dates
* Redundant request to Orders is removed
* Empty results file stored immediately, to avoid exceptions on early "Refresh" in UI

## 1.1.0 2021-12-06

* Runs import jobs asynchronously and provides UI mechanisms to follow progress
* Stores import JSON logs at the file upload path, UI provides listing of import history
* Properly sets default materialType if not provided in properties
* Allows all paths in properties to be with our without the ending slash

## 1.0.0 2021-12-02
