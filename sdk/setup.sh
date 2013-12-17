#!/bin/sh

echo Configuring Eclipse for fluidops platform solution development
echo 
echo This script creates a solution project in your eclipse workspace 
echo using this installation. Please specify the desired
echo solution path below, e.g. C:\workspace\MySolution
echo

SolutionPath="$1"
if [ -z "$SolutionPath" ]; then
	read -p "Solution Path: " SolutionPath
fi

DIR="$(dirname $0)"
cd "$DIR"

PLATFORM_HOME="../.."

# determine the application's lib dir (eCM vs. IWB)
if [ -d "$PLATFORM_HOME/fecm" ]; then
export LIB_HOME="$PLATFORM_HOME/fbase"
else
export LIB_HOME="$PLATFORM_HOME/fiwb"
fi

java $JAVA_OPTS -cp "$LIB_HOME/lib/commons/commons-io-2.1.jar:$LIB_HOME/lib/groovy/groovy-all-1.8.8.jar" groovy.ui.GroovyMain resources/CreateSolution "$PLATFORM_HOME" "$SolutionPath"
