$!
  HTML code for the SemiWIki wiki edit mode.
  
  Available parameters that can be used for customization
  	    		
  @param templatePageLinks a list of HTML links for included rdf:type templates
  @param preview a container for rendering the preview
  @param textarea a container for the textarea
  @param comment a container for the comment field
  @param submitButton the submit button control element
  @param previewButton the preview button control element
  @param cancelButton the cancel button control element
   
  @author as
!$

<table style="width:100%">
	<tr>
		<td>Need help with the wiki syntax? Have a look <a href='http://www.mediawiki.org/wiki/Help:Formatting'>here</a>.</td>
		<td style="text-align:right">
			$if(templatePageLinks)$
				Edit template page: [ 
					$templatePageLinks; separator=" | "$
	    		]
	    	$endif$
		<td>
	</tr>
</table>

$preview$

$textarea$

<div>Comment</div>
$comment$

<div style="float:left">$submitButton$</div>
<div style="float:left">$previewButton$</div>
<div style="float:left">$cancelButton$</div>
<div style="clear:both" />