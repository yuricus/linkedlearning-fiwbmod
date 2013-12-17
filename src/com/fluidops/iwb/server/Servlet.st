$!
    Main template for the WikiServlet page.
    
    This template renders the whole screen, top contains infos about viewed
    entity, menu to log in users, plus the "desktop" with Wiki widgets and
    all other optional widgets rendered.

    Arguments of this template:
        
    see ServletPageParameters class for documentation of full set

!$
<!DOCTYPE html>
<html lang="en">
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        $! the following header makes IE select latest document mode !$
        <meta http-equiv="X-UA-Compatible" content="IE=Edge,chrome=1" />
        <title>$pageTitle$</title>
        <script type='text/javascript' src='$contextPath$/ajax/ajax.js'></script>
        <script type='text/javascript' src='$contextPath$/jquery/jquery.min.js'></script>
		<script type='text/javascript' src='$contextPath$/jquery/jquery-ui.min.js'></script>
		
		$! The amchart scripts are always being included to avoid problems with lazy loading, see bug 11163 !$
		<script type='text/javascript' src='$contextPath$/amchart/js/amcharts.js'></script>
		<script type='text/javascript' src='$contextPath$/amchart/js/amstock/amstock.js'></script>
		<script type='text/javascript' src='$contextPath$/amchart/js/amfallback.js'></script>
		

        $if (uriString)$
            <link rel="alternate" title="RDF/XML" type="application/rdf+xml" href="$contextPath$/rdf/?uri=$uriString$"/>
            <link rel='alternate' type='application/rss+xml' title='New info on $pageTitle$' href='$contextPath$/rss.jsp?uri=$uriString$'/>
        $endif$
                
        <link rel="shortcut icon" href="$contextPath$/favicon.ico" />

        <link rel='stylesheet' href='$contextPath$/ajax/stylesheet_fajax.css' type='text/css' />
        $head$
        <link rel='stylesheet' href='$contextPath$/stylesheet_fiwb.css' type='text/css' />
        
        <link rel='stylesheet' href='$contextPath$/css/semwiki.css' type='text/css' />
        
        <!--[if lte IE 7]><link rel='stylesheet' href='$contextPath$/css/ie7hacks.css' type='text/css' /><![endif]-->
        
        <!--[if IE 8]><link rel='stylesheet' href='$contextPath$/css/ie8hacks.css' type='text/css' /><![endif]-->

        $userScript$
    </head>

    <body onload='javascript:fluInit(); $onLoad$'>
        <div id="pageContainer" class="pageContainer">
            <div id="pageHeader">
                
                $topMenuBar$

                $lowerMenuBar$
             </div>

            $! this div contains the desktop content !$
            <div class='licenseNag'>$licenseNagging$</div>
            <div class='pageBody' id='c'>
            
            	$body$
            </div>
	    	<noscript>Please enable JavaScript to display this page.</noscript>       
        </div>
    </body>
    $tracking$
</html>
