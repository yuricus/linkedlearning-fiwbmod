@echo off
ECHO Configuring Eclipse for fluidops platform solution development
ECHO. 
ECHO This script creates a solution project in your eclipse workspace 
ECHO using this installation. Please specify the desired
ECHO solution path below, e.g. C:\workspace\MySolution
ECHO.

REM check if script path is in program files folder
SET folderCheck=
FOR /f "tokens=*" %%G IN ('ECHO "%~dp0" ^| FINDSTR /I "^%programfiles%"') DO SET folderCheck=%~dp0
FOR /f "tokens=*" %%G IN ('ECHO "%~dp0" ^| FINDSTR /I "^%programfiles(x86)%"') DO SET folderCheck=%~dp0

REM check for admin rights, otherwise display eror
IF NOT DEFINED folderCheck goto allowed
net session >nul 2>&1
if NOT %errorLevel% == 0 (
    ECHO.
    ECHO Error: 
    ECHO "%folderCheck%"
    ECHO is only writeable by Administrators.
    ECHO For further information have a look at Help:PlatformSDK#Getting_started
    ECHO.
    PAUSE
    EXIT /B
)

:allowed

set /p SolutionPath=Solution Path: 


set "PLATFORM_HOME=%CD%\..\.."

REM determine the application's lib dir (eCM vs. IWB)
IF EXIST "%PLATFORM_HOME%\fecm" (set "LIB_HOME=%PLATFORM_HOME%\fbase") ELSE (set "LIB_HOME=%PLATFORM_HOME%\fiwb")

java -cp "%LIB_HOME%\lib\commons\commons-io-2.1.jar;%LIB_HOME%\lib\groovy\groovy-all-1.8.8.jar" groovy.ui.GroovyMain resources/CreateSolution "%PLATFORM_HOME%" "%SolutionPath%"

pause;