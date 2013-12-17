$!
  Test HTML code for the experimental SPARQL interface using Flint
  
  This HTML page is rendered by the SparqlEditorServlet currently used as an iframe 
  for editing queries in configuration form
  
  @param contextPath the contextpath of the web application
  
  @author ango
!$
<!DOCTYPE html>
<html lang="en">
<head>
<title>SPARQL Editor</title>
<meta name="Copyright"
	content="Copyright &copy; 2013 fluid Operations AG" />
<meta http-equiv="Content-Type" content="text/html;charset=utf-8" />
<script type="text/javascript">
	var supportQueryTemplates = true;
</script>
<script type="text/javascript" src="$contextPath$/jquery/jquery.min.js"></script>
<script type="text/javascript" src="$contextPath$/jquery/jquery-ui.min.js"></script>
<script type="text/javascript" src="$contextPath$/flint-editor/codemirror.js"></script>
<script type="text/javascript" src="$contextPath$/flint-editor/sparql10querymode_ll1.js">//</script>
<script type="text/javascript" src="$contextPath$/flint-editor/sparql11querymode_ll1.js">//</script>
<script type="text/javascript" src="$contextPath$/flint-editor/sparql11updatemode_ll1.js">//</script>
<script type="text/javascript" src="$contextPath$/flint-editor/flint-editor-iwb.js">//</script>
<link rel="stylesheet" href="$contextPath$/flint-editor/css/codemirror.css"/>
<link rel="stylesheet" href="$contextPath$/flint-editor/css/sparqlcolors.css"/>
<link rel="stylesheet" href="$contextPath$/flint-editor/css/docs.css"/>

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
		
	}
	
</script>
	
</head>

<body onload="onInit()">
 <p style="font-family:verdana;font-size: 0.7em;" >Write the query in the text area below and click the <i>Done</i> button to paste the query into the configuration field.</p>
	<div id="flint"></div>	
    <textarea id='flint-code' name='query' cols='100' style="display:none;"></textarea>

</body>
</html>