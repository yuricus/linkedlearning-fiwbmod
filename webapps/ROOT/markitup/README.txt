Changes for IWB purposes in jquery.markitup.js:

1. Register the beforeunload function to warn the user leaving the page with unsaved changes in the textarea
			
   'textarea.onchange = function(){$(window).bind('beforeunload', function(){return 'Are you sure you want to leave?';});};'
   
2. 'editorSettings' are adjusted to contain required buttons

3. custom function 'getWidget' called in the custom button 'widget'

4. custom function 'getIESelection(el)' to provide correct text area selection in IE