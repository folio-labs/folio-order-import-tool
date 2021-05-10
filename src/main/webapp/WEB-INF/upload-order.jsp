<!DOCTYPE html>
<html>

<head>
	<meta charset="utf-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>Order upload Demo</title>
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

<body class="layout-default">
	<!-- NAVIGATION -->
	<nav class="navbar">
		<div class="container">
			<div class="navbar-brand"></div>
		</div>
	</nav>
	<div class="container">
		<div class="field">
		    <label class="label" style="color:red">${testMessage}</label>
			<label class="label">Upload Order:</label>
		</div>
	</div>
	<br>
	<!--CONTENTS-->
	<div class="container">
		<div class="tile is-ancestor">
			<div class="tile is-vertical is-8">
				<div class="tile">
					<div class="tile is-parent is-vertical">
						<div class="box">
							<!--  left box start -->
							<div id="tabs-with-content">
								<div class="tabs is-centered">
									<ul>
										<li><a>Upload File</a></li>
									</ul>
								</div>
								<div>
									<section class="tab-content">
										<form name="request" id="request">
											<div class="file has-name">
												<label class="file-label">
													<input class="file-input" type="file" name="order-file" id="order-file" onchange="showname()"> <span class="file-cta">
														<span class="file-icon"> <i class="fas fa-upload"></i>
													</span> <span class="file-label"> Choose a file... </span> </span> <span class="file-name" id="file-name"> .... </span> </label>
											</div>
										</form>
										<br>
										<br>
										<div class="buttons">
											<button class="button is-primary" id="sendFile" name="sendFile" onclick="return sendRequest()">Send Request</button>
										</div>
									</section>
								</div>
							</div>
						</div>
									<div class="tile is-parent">
				<article class="tile is-child notification is-light">
					<div class="content">
						<p class="title">Response<span style="color:#f5f5f5">6-29-20B</span></p>
						<p class="subtitle"></p>
						<div id="logcontent" class="content" style="font-family: 'Special Elite', cursive;"></div>
					</div>
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
function sendRequest() {

	$('#sendFile').addClass('is-loading');
	var form_data = new FormData();
	form_data.append('order-file', $('#order-file').get(0).files[0]);
	$.ajax({
		type: "POST",
		processData: false,
		contentType: false,
		data: form_data,
		url: "/import/service/upload",
		success: showOrderInfo,
		error: updateFailed
	});
	e.preventDefault();
	return false;
}

function showOrderInfo(response) {
	//DISPLAY UPDATED LOG
	$('#sendFile').removeClass('is-loading');
	var source = document.getElementById("orderTemplate").innerHTML;
	var template = Handlebars.compile(source);
	var context = response;
	var output = template(context);
	document.getElementById("logcontent").innerHTML = output;
	$('#order-file').val(''); 
	$('#file-name').text('...');
}

function updateFailed(response) {
	$('#sendFile').removeClass('is-loading');
	$('#order-file').val(''); 
	$('#file-name').text('...');
	alert(response.responseText);
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
	let deactvateAllTabs = function() {
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
			deactvateAllTabs();
			hideTabsContent();
			tab.classList.add('is-active');
			activateTabsContent(tab);
		});
	})
	tabs[0].click();
})();
</script>
<script>
function showname() {
	var name = document.getElementById('order-file');
	document.getElementById('file-name').innerHTML = name.files.item(0).name;
}
</script>
<script id="orderTemplate" type="text/x-handlebars-template"> 
{{#each this}}
	<br> <b>Created PO</b>: {{PONumber}}
	<br> Title: {{title}}
    <br> 001: {{theOne}}
<br>
     {{#if error}}
	  <br> <b>ERROR:</b> {{error}} 
     <hr>
     <br>
    {{/if}}
{{/each}}

</script>

</html>