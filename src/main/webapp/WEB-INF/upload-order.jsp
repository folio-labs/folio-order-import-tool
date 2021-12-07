<!DOCTYPE html>
<html>

<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>FOLIO Order import tool</title>
	<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css">
	<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/bulma/0.6.1/css/bulma.min.css">
	<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/font-awesome/4.7.0/css/font-awesome.min.css">
	<script defer src="https://use.fontawesome.com/releases/v5.3.1/js/all.js"></script>
	<link href="https://fonts.googleapis.com/css?family=Special+Elite" rel="stylesheet">
	<link href="https://fonts.googleapis.com/css?family=Old+Standard+TT" rel="stylesheet"> </head>
<style>
.navbar>.container .navbar-brand,
.container>.navbar .navbar-brand {
	margin-left: -1rem;
}

.navbar>.container .navbar-menu,
.container>.navbar .navbar-menu {
	margin-right: -1rem;
}

.right-table th {
	text-align: right !important;
	vertical-align: middle;
}

.right-table td {
	text-align: right !important;
	vertical-align: middle;
}

.is-primary {
	background-color: #dff0d8 !important;
	border-color: #d0e9c6 !important;
	color: #3c763d !important;
}

.is-danger {
	background-color: #f2dede !important;
	border-color: #ebcccc !important;
	color: #a94442 !important;
}

#tabs-with-content .tabs:not (:last-child) {
	margin-bottom: 0;
}

#tabs-with-content .tab-content {
	padding: 1rem;
	display: none;
}

#tabs-with-content .tab-content.is-active {
	display: block;
}
</style>
<%@ page import="java.util.Arrays, org.olf.folio.order.Config" %>
<body class="layout-default" onLoad="javascript:requestImportHistory();">
	<!-- NAVIGATION -->
	<br>
	<div class="container">
		<div class="field">
			<label class="label">FOLIO Order import tool</label>
		</div>
	</div>
	<!--CONTENTS-->
	<div class="container">
		<div class="tile is-ancestor">
			<div class="tile is-vertical is-8">
				<div class="tile">
					<div class="tile is-parent is-vertical">
						<div class="box">
							<!--  left box start -->
							<div id="tabs-with-content">
								<div class="tabs is-left">
									<ul>
										<li><a>Upload Orders</a></li>
										<li><a>Import History</a></li>
										<li><a>View Configuration</a></li>
									</ul>
								</div>
								<div>
									<section class="tab-content">
										<form name="request" id="request">
											<div class="file has-name is-fullwidth">
												<label class="file-label">
												  <input class="file-input" type="file" name="order-file" id="order-file" onchange="showName()"> <span class="file-cta">
														<span class="file-icon"> <i class="fas fa-upload"></i>
													</span> <span class="file-label"> Choose a MARC file... </span> </span> <span class="file-name" id="file-name"> .... </span> </label>
											</div>
										</form>
										<br>
										<br>
										<div class="buttons">
										    <button class="button is-primary" id="analyze" name="analyze" onclick="return sendAnalyzeRequest()">Analyze MARC records</button>
											<button class="button is-primary" id="import" name="import" onclick="return sendImportRequest()">Import MARC records</button>
										</div>
									</section>
								</div>
								<div>
								  <section class="tab-content">
								    <div style="text-align:right;"><h3><a href="javascript:requestImportHistory()">Refresh</a></h3></div>
								    <div id="historyEntries" name="historyEntries">
								    </div>
								  </section>
								</div>
								<div>
									<section class="tab-content">
									  <div id="config">
									    <p class="title">Configuration<span style="color:#f5f5f5"></span></p>
                                           <%
                                              for (String key : Config.KNOWN_PROPERTIES) {
                                                if (!key.contains("password")) {
                                                    String val = (String) getServletContext().getAttribute(key);
                                                    if (val == null) val = "&lt;not specified in properties file&gt;";
                                                    out.println(key + ": " + val);
                                           %>
                                                <br/>
                                           <%   }
                                              }
                                           %>
									  </div>
									</section>
								</div>
							</div>
						</div>
						<div class="tile is-parent">
				            <article class="tile is-child notification is-light">
					            <div class="content"><div id="logContent" class="content"/></div>
				            </article>
			            </div>
						<!-- left box end -->
					</div>
				</div>
			</div>

		</div>
	</div>
	<!-- END CONTENTS-->
	<script src="https://code.jquery.com/jquery-3.2.1.js" integrity="sha256-DZAnKJ/6XZ9si04Hgrsxu/8s717jcIzLy3oi35EouyE=" crossorigin="anonymous"></script>
</body>
<script>
document.addEventListener('DOMContentLoaded', function() {
	// Get all "navbar-burger" elements
	var $navbarBurgers = Array.prototype.slice.call(document.querySelectorAll('.navbar-burger'), 0);
	// Check if there are any navbar burgers
	if($navbarBurgers.length > 0) {
		// Add a click event on each of them
		$navbarBurgers.forEach(function($el) {
			$el.addEventListener('click', function() {
				// Get the target from the "data-target" attribute
				var target = $el.dataset.target;
				var $target = document.getElementById(target);
				// Toggle the class on both the "navbar-burger" and the "navbar-menu"
				$el.classList.toggle('is-active');
				$target.classList.toggle('is-active');
			});
		});
	}
});
</script>
<script src="https://code.jquery.com/jquery-2.2.4.min.js" integrity="sha256-BbhdlvQf/xTY9gja0Dq3HiwQF8LaCRTXxZKRutelT44=" crossorigin="anonymous"></script>
<link rel="stylesheet" type="text/css" href="//cdn.datatables.net/1.10.16/css/jquery.dataTables.css">
<script type="text/javascript" charset="utf8" src="//cdn.datatables.net/1.10.16/js/jquery.dataTables.js"></script>
<script>
function sendImportRequest() {

	$('#import').addClass('is-loading');
	var form_data = new FormData();
	form_data.append('order-file', $('#order-file').get(0).files[0]);
	document.getElementById("logContent").innerHTML = "";
	$.ajax({
		type: "POST",
		processData: false,
		contentType: false,
		data: form_data,
		url: "/import/service/upload",
		success: showImportResponse,
		error: updateFailed
	});
	e.preventDefault();
	return false;
}

function showImportResponse(response) {
	//DISPLAY UPDATED LOG
	$('#import').removeClass('is-loading');
	var source = document.getElementById("importResponseTemplate").innerHTML;
	var template = Handlebars.compile(source);
	var context = response;
	var output = template(context);
	document.getElementById("logContent").innerHTML = output;
	requestImportHistory();
}

function sendAnalyzeRequest() {
	$('#analyze').addClass('is-loading');
	var form_data = new FormData();
	form_data.append('order-file', $('#order-file').get(0).files[0]);
	document.getElementById("logContent").innerHTML = "";
	$.ajax({
		type: "POST",
		processData: false,
		contentType: false,
		data: form_data,
		url: "/import/service/upload?analyzeOnly=true",
		success: showAnalyzeResponse,
		error: updateFailed
	});
	e.preventDefault();
	return false;
}

function showAnalyzeResponse(response) {
	//DISPLAY UPDATED LOG
	$('#analyze').removeClass('is-loading');
	var source = document.getElementById("analyzeResponseTemplate").innerHTML;
	var template = Handlebars.compile(source);
	var context = response;
	var output = template(context);
	document.getElementById("logContent").innerHTML = output;
	requestImportHistory();
}

function updateFailed(response) {
	$('#import').removeClass('is-loading');
	$('#analyze').removeClass('is-loading');
	alert(response.responseText);
}

function requestImportHistory () {
	$.ajax({
		type: "GET",
		processData: false,
		contentType: false,
		url: "/import/service/upload/history",
		success: showHistoryResponse,
		error: updateFailed
	});
	return false;
}

function showHistoryResponse (historyJson) {
  var source = document.getElementById("importHistoryTemplate").innerHTML;
  var template = Handlebars.compile(source);
  var context = historyJson;
  var output = template(context);
  document.getElementById("historyEntries").innerHTML = output;
}

function requestImportResults (importName) {
	$.ajax({
		type: "GET",
		processData: false,
		contentType: false,
		url: "/import/service/upload/results?name="+importName,
		success: showImportResultsResponse,
		error: updateFailed
	});
	return false;
}

function showImportResultsResponse(resultsJson) {
    var source;
    if (resultsJson.summary.import) {
      source = document.getElementById("importResponseTemplate").innerHTML;
    } else {
      source = document.getElementById("analyzeResponseTemplate").innerHTML;
    }
    var template = Handlebars.compile(source);
    var context = resultsJson;
    var output = template(context);
    document.getElementById("logContent").innerHTML = output;
    requestImportHistory();
}

</script>
<script src="js/handlebars-v4.0.2.js"></script>
<script src="js/moment.js"></script>
<script>
Handlebars.registerHelper('formatTime', function(date, format) {
	var mmnt = moment(date);
	return mmnt.format(format);
});
</script>
<script>
let tabsWithContent = (function() {
	let tabs = document.querySelectorAll('.tabs li');
	let tabsContent = document.querySelectorAll('.tab-content');
	let deactivateAllTabs = function() {
		tabs.forEach(function(tab) {
			tab.classList.remove('is-active');
		});
	};
	let hideTabsContent = function() {
		tabsContent.forEach(function(tabContent) {
			tabContent.classList.remove('is-active');
		});
	};
	let activateTabsContent = function(tab) {
		tabsContent[getIndex(tab)].classList.add('is-active');
	};
	let getIndex = function(el) {
		return [...el.parentElement.children].indexOf(el);
	};
	tabs.forEach(function(tab) {
		tab.addEventListener('click', function() {
			deactivateAllTabs();
			hideTabsContent();
			tab.classList.add('is-active');
			activateTabsContent(tab);
		});
	})
	tabs[0].click();
})();
</script>
<script>
function showName() {
	var name = document.getElementById('order-file');
	document.getElementById('file-name').innerHTML = name.files.item(0).name;
}
</script>

<!-- See project''s README for documentation about the schema for the JSON data received here : -->
<script id="importResponseTemplate" type="text/x-handlebars-template">
  {{#if summary.isNotDone}}
     <div style="text-align:right;">
       <a href="javascript:requestImportResults('{{summary.resultsBaseName}}'); requestImportHistory();" title="{{summary.resultsBaseName}}">Refresh</a>
     </div>
  {{/if}}
  <p class="title">Import</p>
  <h4>{{summary.inputBaseName}}  ({{summary.status}})</h4>
  MARC records: {{summary.marcRecords}}
  <br>Records processed: {{summary.recordsProcessed}}
  {{#if summary.validation.hasErrors}}
    <br> Records passed validation: {{summary.validation.succeeded}}
    <br> Records failed validation: {{summary.validation.failed}}
  {{/if}}
  {{#if summary.import.hasErrors}}
    <br> <b> {{summary.import.failed}} record(s) failed to import (fully or partially) </b>
  {{/if}}
  <br> {{summary.import.succeeded}} record(s) imported
  {{#if summary.hasFlags}}
    <br> {{summary.flagged}} record(s) with notes/warnings
  {{/if}}
  {{#each records}}
    <br>
    <br>Rec# {{recNo}} PO number <a href={{data.uiUrlPo}} target=orders>{{data.poNumber}}</a>
    {{#if hasValidationErrors}}<b>failed</b>
      {{#each validationErrors}}
        <br>Error {{this}}
      {{/each}}
    {{/if}}
    {{#if hasImportError}}
      <br>Error: {{importError}}
    {{/if}}
    <br>Title: {{data.title}}
    <br>Instance HRID:  <a href={{data.uiUrlInstance}} target=inventory>{{data.instanceHrid}}</a>
    <br>ISBN: {{data.isbn}}
    {{#each flags}}
      <br> <b>Note</b>: {{this}}
    {{/each}}
    {{#if hasImportError}}
       <br><pre><div class="inner-pre" style="font-size: 11px; height: 100px; width: 1000px;" >{{data.source}}</div></pre>
    {{/if}}
  {{/each}}
</script>

<!-- See project''s README for documentation about the schema for the JSON data received here: -->
<script id="analyzeResponseTemplate" type="text/x-handlebars-template">
  <p class="title">Validation results<span style="color:#f5f5f5"></span></p>
  <h4>{{summary.inputBaseName}}</h4>
  <br>Records processed: {{summary.recordsProcessed}}
  <br>Records passed: {{summary.validation.succeeded}}
  <br>Records failed: {{summary.validation.failed}}
  {{#if summary.hasFlags}}
    <br> {{summary.flagged}} record(s) with notes/warnings
  {{/if}}
  <br>
  {{#each records}}
    <br>Rec# {{recNo}}
    {{#if hasValidationErrors}}<b>failed</b>
      {{#each validationErrors}}
        <br>Error {{this}}
      {{/each}}
    {{/if}}
    <br>Title: {{data.title}}
    <br>ISBN: {{data.isbn}}
    {{#each flags}}
      <br> <b>Note</b>: {{this}}
    {{/each}}
    <br><pre><div class="inner-pre" style="font-size: 11px; height: 100px; width: 1000px;" >{{data.source}}</div></pre>
  {{/each}}
</script>

<script id="importHistoryTemplate" type="text/x-handlebars-template">
<table class="table-look">
  <tr>
    <th style="padding:5px;">Type of<br>request</th>
    <th style="padding:5px;">Name</th>
    <th style="padding:5px;">Started</th>
    <th style="padding:5px;">Ended</th>
    <th style="padding:5px;">Marc<br>records</th>
    <th style="padding:5px;">Records<br>processed</th>
  </tr>
  {{#each entries}}
    <tr>
      <td style="padding:5px;">{{requestType}}</td>
      <td style="padding:5px;"><a href="javascript:requestImportResults('{{resultsBaseName}}')" title="{{resultsBaseName}}">{{inputBaseName}}</a></td>
      <td style="padding:5px;">{{startTime}}</td>
      <td style="padding:5px;">{{endTime}}</td>
      <td style="padding:5px; text-align:right;">{{marcRecordCount}}</td>
      <td style="padding:5px; text-align:right;">{{recordsProcessedText}}</td>
    </tr>
  {{/each}}
</table>
</script>
</html>