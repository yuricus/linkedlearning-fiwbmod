$!

	Template for the top menu bar in the header
	
	see PageParameters class for documentation of full set of arguments
!$
<div class="topmenubar">

	<div class="topmenubarLeft">
		$if(loggedIn)$
			<div class="loggedInLabel">&nbsp;Logged in as <b>$user$</b></div>
		$endif$
	</div>
	
    <div align="center" class="topmenubarCenterDIV navButton" style="left:50%;position:absolute" onclick="\$r('$contextPath$/');"></div>
    
    <div class="topmenubarRight">       
       $toolBarButtons$                                  
       <div onclick="\$r('http://www.fluidops.com');" class="topmenubarRightLogo navButton"></div>
    </div>
</div>