$!
  This HTML page is rendered by the SPARQLServlet if no query is passed
  as parameter.
  
  Arguments of this template:
        
    see ServletPageParameters class for documentation of full set
  
  @author as, andriy.nikolov
!$
<!DOCTYPE html>
<html lang="en">
    <head>
<title>$pageTitle$</title>
<meta name="Copyright"
	content="Copyright &copy; 2013 fluid Operations AG" />
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
$! the following header makes IE select latest document mode !$
<meta http-equiv="X-UA-Compatible" content="IE=Edge,chrome=1" />

<script>
    // Extra check to determine if the browser is IE8 or less,
    // in which case a redirect is performed and a fallback SPARQL UI is used.
    
	var contextPath = "$contextPath$";
	var initQuery = "$initQuery$";
	
	function testIfOldMSIE() {
		if (window.navigator.appName == "Microsoft Internet Explorer")
		{
   			// This is an IE browser. What mode is the engine in?
			if (document.documentMode) // IE8 or later
    	  		engine = document.documentMode;
      			if(engine==8)
      				return true;
      		
	   		else // IE 5-7
			{
      			return true;
			}
		}
	
		return false;
	}
	var oldMSIEIsUsed = testIfOldMSIE();
	
	if(oldMSIEIsUsed) {
		var form = document.createElement("form");
    	form.setAttribute("method", "post");
    	form.setAttribute("action", "");
    	
    	my_tb=document.createElement('INPUT');
		my_tb.type='HIDDEN';
		my_tb.name='initQuery';
		my_tb.value=initQuery;
		form.appendChild(my_tb);

		my_tb=document.createElement('INPUT');
		my_tb.type='HIDDEN';
		my_tb.name='useIE8Fallback';
		my_tb.value='true';
		form.appendChild(my_tb);
    	
    	var body = document.createElement("body");
    	
    	body.appendChild(form);
    	document.appendChild(body);
		form.submit();
	}
</script>

<script type="text/javascript" src="$contextPath$/jquery/jquery.min.js"></script>
<script type="text/javascript" src="$contextPath$/markitup/jquery.markitup.js"></script>
<script type="text/javascript" src="$contextPath$/jquery/jquery-ui.min.js"></script>
<script type="text/javascript" src="$contextPath$/ajax/ajax.js"></script>
<script type="text/javascript" src="$contextPath$/flint-editor/codemirror.js"></script>
<script type="text/javascript" src="$contextPath$/flint-editor/sparql10querymode_ll1.js">//</script>
<script type="text/javascript" src="$contextPath$/flint-editor/sparql11querymode_ll1.js">//</script>
<script type="text/javascript" src="$contextPath$/flint-editor/sparql11updatemode_ll1.js">//</script>
<script type="text/javascript" src="$contextPath$/flint-editor/flint-editor-iwb.js">//</script>
<link rel="stylesheet" href="$contextPath$/flint-editor/css/codemirror.css"/>
<link rel="stylesheet" href="$contextPath$/flint-editor/css/sparqlcolors.css"/>
<link rel="stylesheet" href="$contextPath$/flint-editor/css/docs.css"/>

<link rel="shortcut icon" href="$contextPath$/favicon.ico" />

<link rel='stylesheet' href='$contextPath$/ajax/stylesheet_fajax.css' type='text/css' />
$head$
<link rel='stylesheet' href='$contextPath$/stylesheet_fiwb.css' type='text/css' />
<!--[if lte IE 7]><link rel='stylesheet' href='$contextPath$/css/ie7hacks.css' type='text/css' /><![endif]-->
        
<!--[if IE 8]><link rel='stylesheet' href='$contextPath$/css/ie8hacks.css' type='text/css' /><![endif]-->

<link rel='stylesheet' href='$contextPath$/css/semwiki.css' type='text/css' />
$userScript$       

		<script type="text/javascript">
		
			$! values supported in graph query !$
			var supportedGraphQuery = new Array("auto","application/rdf+xml","text/plain",
					  "text/turtle","text/rdf+n3","application/x-trig","application/trix");
			var supportedTupleQuery = new Array("auto","application/sparql-results+xml",
					  "application/sparql-results+json","text/csv");
					  
			var iwbNamespacePrefixes = $namespaces$;
			var contextPath = "$contextPath$";
			var iwbClasses = $classes$;
			var iwbProperties = $properties$;
			var initQuery = "$initQuery$";
			
			$! function that is executed on submit: checks format validity !$
			function onFormSubmit(form)
			{
				flintEditor.getEditor().getCodeEditor().save();
			
				var query = document.getElementById("flint-code").value;
				var format = form.format.value;
				var infer = document.getElementById("inferCB").checked;
				
		        $! set inference of hidden field according to checkbox selection !$
		        document.getElementById("infer").value = (infer ? 'true' : 'false');
		
				<!-- check supported types for construct queries
				if (query.match(/\bconstruct\b/i) || query.match(/\bdescribe\b/i)) {
					if (!arrayContains(supportedGraphQuery,format)) {
						alert('Format ' + format + ' not supported for graphqueries.')
						return false;
					}
				}
				else if (query.match(/\bselect\b/i)) {
					if (!arrayContains(supportedTupleQuery,format)) {
						alert('Format ' + format + ' not supported for tuple queries.')
						return false;
					}
				}
		
				if(typeof(Storage)!=="undefined") {
					sessionStorage.lastSubmittedQuery = query;
				}
		
				return true;	
			}
			
			$! test if the specified array contains the object !$
			function arrayContains(arr,obj) { 
			    return (arr.indexOf(obj) != -1); 
			} 
			
			function onInit() {
				
				$! define the protoype Array.prototype.indexOf if it is not present maybe needed for ie6 !$
				if (!Array.prototype.indexOf) { 
				  Array.prototype.indexOf = function (obj) { 
				    for (var i = 0, j = this.length; i < j; i++) { 
				        if (this[i] === obj) 
				            return i; 
				    } 
				    return -1; 
				  }; 
				}
		
				if(initQuery==="") {
					if(typeof(Storage)!=="undefined") {
						if(sessionStorage.lastSubmittedQuery) {
							document.getElementById("flint-code").value = sessionStorage.lastSubmittedQuery;
							flintEditor.getEditor().getCodeEditor().setValue(sessionStorage.lastSubmittedQuery);
						}
					}
				} else {
					document.getElementById("flint-code").value = initQuery;
					flintEditor.getEditor().getCodeEditor().setValue(initQuery);
				}
				
			}
		
		</script>
</head>

<body onload="onInit();fluInit();">

	<div id="pageContainer" class="pageContainer">
            <div id="pageHeader">
                
                $topMenuBar$

                $lowerMenuBar$
             </div>

            $! this div contains the desktop content !$
            <div class='licenseNag'>$licenseNagging$</div>

            $! this div contains the desktop content !$
            <div class='pageBody' id='c'>
                       
			<form action="" method="post" onsubmit="return onFormSubmit(this)">
				<label for="query">Enter your query in the text field below.</label>
				<br />
				<div id="flint">
				</div>	
				<br /><br />
				<label for="format">Results format:</label>
				<select name="format" id="format" size="1">
					<option value="auto" title="auto" selected="selected">auto</option>
					<option value="application/sparql-results+xml" title="SPARQL/XML">SPARQL/XML</option>
					<option value="application/sparql-results+json" title="SPARQL/JSON">SPARQL/JSON</option>
					<option value="text/csv" title="SPARQL/CSV">SPARQL/CSV</option>
					<option value="application/rdf+xml" title="RDF/XML">RDF/XML</option>
					<option value="text/plain" title="N-Triples">N-Triples</option>
					<option value="text/turtle" title="Turtle">Turtle</option>
					<option value="text/rdf+n3" title="N3">N3</option>
					<option value="application/x-trig" title="TriG">TriG</option>
					<option value="application/trix" title="TriX">TriX</option>
		
				</select>
				
				$! checkbox and hidden field for inferencing, hidden field is set in onsubmit js code !$
				&nbsp;&nbsp;&nbsp;&nbsp;
				<input type="checkbox" id="inferCB" /> Inferencing (requires repository support)<br>
				<input type="hidden" id="infer" name="infer" value="false" />
				<input type="hidden" id="forceDownload" name="forceDownload" value="true" />
				
				<br/><br/>
				<input type="submit" value="Submit" />
				<input id="reset" type="reset" value="Reset" />
			</form>
		    
		    <textarea id='flint-code' name='query' cols='100' rows='1' style="display:none;">select distinct ?type where { ?x rdf:type ?type }</textarea>

            </div>
	    	<noscript>Please enable JavaScript to display this page.</noscript>
      </div>
</body>
$tracking$
</html>