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
