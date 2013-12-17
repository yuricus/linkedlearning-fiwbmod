import org.apache.commons.io.FileUtils;

import groovy.io.FileType;


/*
 * Script to create an Eclipse project for a platform solution
 * 
 * Usage:
 * 	<Platform_HOME> <SolutionPath>
 * 
 * e.g.
 * "C:\Program Files (x86)\fluid Operations\eCloudManager" "c:\workspace\MySolution"
 * "C:\Program Files (x86)\fluid Operations\Information Workbench" "c:\workspace\MySolution"  
 * 
 */

File platformDir = new File(args[0])
File solDir = new File(args[1])

// validate arguments and retrieve application dir ("fecm" or "fiwb")
String applicationDirectory = validateArguments(solDir, platformDir)	

println "Creating solution Eclipse project: " + solDir.name
println "Solution directory: " + solDir.getCanonicalPath()
println "Installation home: " + platformDir.getCanonicalPath()
solDir.mkdirs()
createBackupOfWorkingSet(platformDir, applicationDirectory)
createProjectStructure(solDir);
createProjectsFile(solDir);
createProjectSettingsFiles(solDir)
createBuilderLaunchConfigurationFile(solDir);
createClassPathFiles(solDir, platformDir, applicationDirectory);
createLaunchConfiguration(solDir, platformDir, applicationDirectory);
createLaunchConfigurationsAnt(solDir, platformDir, applicationDirectory)
createSolutionRef(solDir, platformDir, applicationDirectory);
createCreateSolutionProperties(solDir, applicationDirectory);
copyBuildScript(solDir);


println "Solution successfully created."
println "Open Eclipse workspace '" + solDir.getParentFile().getCanonicalPath() + "' and import your solution:"
println "\t- File=>Import=>Existing Projects into Workspace"
println "\t- Select solution dir as root"
println "\t- Click finish"



/* FUNCTIONS */

/**
 * Validate the arguments of this script. Determines the type of the 
 * installation (Information Workbench or eCloudManager) and returns
 * the applicationDirectory (i.e. either fecm or fiwb)
 * @param solDir
 * @param platformDir
 * @return
 */
String validateArguments(File solDir, File platformDir) {		
	
	if( !platformDir.isDirectory() ) {
		throw new IllegalArgumentException("Invalid fluidOps platform installation directory: " + platformDir.getAbsolutePath() + " does not exist")
	}
	// check if provided dir is from Information Workbench or eCloudManager
	def temp = new File(platformDir, "fecm")
	String applicationDirectory
	
	if (new File(platformDir, "fecm").exists())
		applicationDirectory = "fecm"
	else if (new File(platformDir, "fiwb").exists())
		applicationDirectory = "fiwb"
	else
		throw new IllegalArgumentException("The directory: $platformDir does not look like a valid fluidOps platform directory")
	
	if( !new File(platformDir, applicationDirectory + "/data").isDirectory() || !new File(platformDir, applicationDirectory + "/etc").isDirectory() ) {
		throw new IllegalArgumentException("The directory: $platformDir does not look like a valid fluidOps platform directory")
	}
	
	// TODO: ask if existing dir should be used anyways
	if (solDir.exists()) {
		println "The provided solution directory already exists: " + solDir.getAbsolutePath();
		System.in.withReader {
			print  'Continue (y/n): '
			String line = it.readLine()
			if (!line.equals("y")) {				
				throw new IllegalStateException("Provided solution directory already exists: " + solDir.getAbsolutePath());	
			}				
		}
	}
			
	return applicationDirectory
}

/**
 * Function to create the Eclipse .project file
 * in the provided solDir. The directory name of
 * the solDir is used as name for the project
 */
def createProjectsFile(File solDir) {
	
	def prjName = solDir.name
	File prjFile = new File(solDir, ".project")
	prjFile.withWriter("UTF-8") { writer ->
		def pw = writer.newPrintWriter()
		pw.println """<?xml version="1.0" encoding="UTF-8"?>
<projectDescription>
	<name>$prjName</name>
	<comment></comment>
	<projects>
	</projects>
	<buildSpec>
		<buildCommand>
			<name>org.eclipse.jdt.core.javabuilder</name>
			<arguments>
			</arguments>
		</buildCommand>
		<buildCommand>
			<name>org.eclipse.ui.externaltools.ExternalToolBuilder</name>
			<triggers>auto,full,incremental,</triggers>
			<arguments>
				<dictionary>
					<key>LaunchConfigHandle</key>
					<value>&lt;project&gt;/.externalToolBuilders/Synch_Solution_Files.launch</value>
				</dictionary>
				<dictionary>
					<key>incclean</key>
					<value>true</value>
				</dictionary>
			</arguments>
		</buildCommand>
	</buildSpec>
	<natures>
		<nature>org.eclipse.jdt.groovy.core.groovyNature</nature>
		<nature>org.eclipse.jdt.core.javanature</nature>
	</natures>
</projectDescription>
"""	}
}


/**
 * Function to create the Eclipse builder launch 
 * configuration for synching solution files
 */
def createBuilderLaunchConfigurationFile(File solDir) {
	
	def prjName = solDir.name
	
	File prjFile = new File(solDir, ".externalToolBuilders/Synch_Solution_Files.launch")
	prjFile.withWriter("UTF-8") { writer ->
		def pw = writer.newPrintWriter()
		pw.println """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<launchConfiguration type="org.eclipse.ant.AntBuilderLaunchConfigurationType">
<stringAttribute key="org.eclipse.ant.ui.ATTR_ANT_AUTO_TARGETS" value="pushSolutionFiles,"/>
<stringAttribute key="org.eclipse.ant.ui.ATTR_ANT_MANUAL_TARGETS" value="pushSolutionFiles,"/>
<booleanAttribute key="org.eclipse.ant.ui.ATTR_TARGETS_UPDATED" value="true"/>
<booleanAttribute key="org.eclipse.ant.ui.DEFAULT_VM_INSTALL" value="false"/>
<booleanAttribute key="org.eclipse.debug.ui.ATTR_LAUNCH_IN_BACKGROUND" value="false"/>
<stringAttribute key="org.eclipse.jdt.launching.CLASSPATH_PROVIDER" value="org.eclipse.ant.ui.AntClasspathProvider"/>
<booleanAttribute key="org.eclipse.jdt.launching.DEFAULT_CLASSPATH" value="true"/>
<stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="${prjName}"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_LOCATION" value="\${project_loc:/${prjName}}/build/build.xml"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_RUN_BUILD_KINDS" value="incremental,auto,"/>
<booleanAttribute key="org.eclipse.ui.externaltools.ATTR_TRIGGERS_CONFIGURED" value="true"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_WORKING_DIRECTORY" value="\${project_loc:/${prjName}}/build"/>
</launchConfiguration>"""
	}
}

/**
 * Function to create the Eclipse project settings files
 */
def createProjectSettingsFiles(File solDir) {

	File prjFile = new File(solDir, ".settings/org.eclipse.jdt.groovy.core.prefs")
	prjFile.withWriter("UTF-8") { writer ->
		def pw = writer.newPrintWriter()
		pw.println """eclipse.preferences.version=1
groovy.compiler.level=18"""
	}
}


/**
 * Create the project .classpath file by recursively searching
 * the provided installation directory for *.jar files.
 * 
 * In addition this method creates the ANT include used
 * for classpath
 *
 */
def createClassPathFiles(File solDir, File platformDir, String applicationDirectory) {

	/*
	 * TODO
	 *  - maybe parse backend.conf to retain ordering of jars
	 */
	
	String platform_home = platformDir.getCanonicalPath()
	
	def libs = []
	platformDir.eachFileRecurse(FileType.DIRECTORIES) { File ecmSubdirs ->
		File ecmLibDir = new File(ecmSubdirs, "lib")
		if(ecmLibDir.isDirectory()) {
			ecmLibDir.eachFileRecurse(FileType.FILES) { file  ->
				if(file.name.endsWith(".jar")) {
					libs << file
				}
			}
		}
	}
	println "Building .classpath file, found " + libs.size + " libraries"
	
	File classpathFile = new File(solDir, ".classpath")
	classpathFile.withWriter("UTF-8") { writer ->
		def pw = writer.newPrintWriter()
		pw.println """<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="src" path="resources"/>
	<classpathentry kind="src" path="src"/>
	<classpathentry kind="src" path="test"/>	
	<classpathentry kind="src" path="scripts"/>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER"/>
    <classpathentry kind="con" path="org.eclipse.jdt.junit.JUNIT_CONTAINER/4"/>
	<classpathentry kind="output" path="bin"/>"""
	
		libs.each{ File jar ->
			 pw.println "\t<classpathentry kind=\"lib\" path=\"${jar.absolutePath}\"/>"
		}
		pw.println "</classpath>"
	}
	
	
	/* write build-classpath.xml to solution build directory */
	File buildClasspathFile = new File(solDir, "build/build-classpath.xml")
	println "Building ANT classpath file: " + buildClasspathFile.getCanonicalPath()
	buildClasspathFile.withWriter("UTF-8") { writer ->
		def pw = writer.newPrintWriter()
		pw.println """<project name="build-classpath" basedir=".">
	
	<property name="platform_home" location="${platform_home}" />
	<property name="application_working_dir" location="${platform_home}/${applicationDirectory}" />

	<path id="fluid-base.path">"""
			
			libs.each{ File jar ->
				pw.println "\t<pathelement location=\"" + jar.getCanonicalPath()+ "\" />"
			}
			pw.println "</path>\n</project>"
	}
	
	
}

/**
 * Function to create the project layout:
 *
 *  - build
 *  - src
 *  - test
 *  - resources
 *  - scripts
 *  - data/dbBootstrap
 *  - data/ontologies
 *  - data/wikiBootstrap
 *  - config
 *  - lib/extensions
 *
 * @param solDir
 */
def createProjectStructure(File solDir) {
	
	println "Creating solution directory structure"
	createDirInSolution(solDir, "build")
	createDirInSolution(solDir, "build/launch")
	createDirInSolution(solDir, ".externalToolBuilders")
	createDirInSolution(solDir, ".settings")
	createDirInSolution(solDir, "src")
	createDirInSolution(solDir, "resources")
	createDirInSolution(solDir, "test")
	createDirInSolution(solDir, "scripts")
	createDirInSolution(solDir, "data/dbBootstrap")
	createDirInSolution(solDir, "data/ontologies")
	createDirInSolution(solDir, "data/wikiBootstrap")
	createDirInSolution(solDir, "config")
	createDirInSolution(solDir, "lib/extensions")
}



/**
 * Create a launch configuration for the solution using the platform 
 * directory as working dir
 */
def createLaunchConfiguration(File solDir, File platformDir, String applicationDirectory) {
	
	def projName = solDir.name
	def workingDir = new File(platformDir, applicationDirectory).getCanonicalPath()

	def mainClass = getMainClass(platformDir, applicationDirectory);
	
	File classpathFile = new File(solDir, "Start_" + projName + ".launch")
	classpathFile.withWriter("UTF-8") { writer ->
		def pw = writer.newPrintWriter()
		pw.println """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<launchConfiguration type="org.eclipse.jdt.launching.localJavaApplication">
<listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_PATHS">
<listEntry value="/$projName"/>
</listAttribute>
<listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_TYPES">
<listEntry value="4"/>
</listAttribute>
<mapAttribute key="org.eclipse.debug.core.environmentVariables">
<mapEntry key="com.fluidops.logging.env" value="debug"/>
</mapAttribute>
<listAttribute key="org.eclipse.debug.ui.favoriteGroups">
<listEntry value="org.eclipse.debug.ui.launchGroup.debug"/>
<listEntry value="org.eclipse.debug.ui.launchGroup.run"/>
</listAttribute>
<stringAttribute key="org.eclipse.jdt.launching.MAIN_TYPE" value="$mainClass"/>
<stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="$projName"/>
<stringAttribute key="org.eclipse.jdt.launching.VM_ARGUMENTS" value="-Xmx2048m -Xms512m -XX:PermSize=256m -Dfile.encoding=UTF-8"/>
<stringAttribute key="org.eclipse.jdt.launching.WORKING_DIRECTORY" value="$workingDir"/>
</launchConfiguration>
"""
	}

}


/**
 * Create a launch configurations for ANT tasks: 
 * 
 * a) Build Solution Artifact.launch
 * b) Copy Modified Wiki Pages To Workspace.launch
 * c) Re-Initialize System From Workspace.launch
 * d) Remove User-Edited Wiki Pages From System.launch
 * 
 */
def createLaunchConfigurationsAnt(File solDir, File platformDir, String applicationDirectory) {

	def projName = solDir.name;
	def launchDir = new File(solDir, "build/launch");
	
	createLaunchConfigurationsAntHelp(new File(launchDir, "Build Solution Artifact.launch"), projName, "build");
	createLaunchConfigurationsAntHelp(new File(launchDir, "Copy Modified Wiki Pages To Workspace.launch"), projName, "fetchWikiPages");
	createLaunchConfigurationsAntHelp(new File(launchDir, "Re-Initialize System From Workspace.launch"), projName, "cleanApplicationWorkingDir");
	createLaunchConfigurationsAntHelp(new File(launchDir, "Remove User-Edited Wiki Pages From System.launch"), projName, "cleanUserEditedWikiPages");
}

/**
 * Helper method to write the launchFile for the given targetName
 */
def createLaunchConfigurationsAntHelp(File launchFile, String projName, String targetName) {

	launchFile.withWriter("UTF-8") { writer ->
		def pw = writer.newPrintWriter()
		pw.println """<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<launchConfiguration type="org.eclipse.ant.AntLaunchConfigurationType">
<booleanAttribute key="org.eclipse.ant.ui.DEFAULT_VM_INSTALL" value="false"/>
<stringAttribute key="org.eclipse.debug.core.ATTR_REFRESH_SCOPE" value="\${project}"/>
<listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_PATHS">
<listEntry value="/$projName/build/build.xml"/>
</listAttribute>
<listAttribute key="org.eclipse.debug.core.MAPPED_RESOURCE_TYPES">
<listEntry value="1"/>
</listAttribute>
<listAttribute key="org.eclipse.debug.ui.favoriteGroups">
<listEntry value="org.eclipse.ui.externaltools.launchGroup"/>
</listAttribute>
<stringAttribute key="org.eclipse.jdt.launching.CLASSPATH_PROVIDER" value="org.eclipse.ant.ui.AntClasspathProvider"/>
<stringAttribute key="org.eclipse.jdt.launching.PROJECT_ATTR" value="$projName"/>
<stringAttribute key="org.eclipse.jdt.launching.SOURCE_PATH_PROVIDER" value="org.eclipse.ant.ui.AntClasspathProvider"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_ANT_TARGETS" value="$targetName,"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_LOCATION" value="\${workspace_loc:/$projName/build/build.xml}"/>
<stringAttribute key="org.eclipse.ui.externaltools.ATTR_WORKING_DIRECTORY" value="\${workspace_loc:/$projName/build}"/>
<stringAttribute key="process_factory_id" value="org.eclipse.ant.ui.remoteAntProcessFactory"/>
</launchConfiguration>
"""
	}
}


/**
 * Get the main class extracted from the application directory.
 * fecm => "com.fluidops.ecm.ECM"
 * fiwb => "com.fluidops.iwb.IwbStart" / "IwbComStart"
 * 
 * Note: to decide between IWB CE and EE we inspect the file
 * applicationDir/version.txt, which contains a string like
 *
 * Information Workbench CE 2.6.0.930
 *
 * If this file does not exist, we assume the community edition.
 */
String getMainClass(File platformDir, String applicationDir) {

	if (applicationDir.endsWith("fecm"))
		return "com.fluidops.ecm.ECM";
	
	File versionFile = new File(platformDir, applicationDir + "/version.txt");
	if (!(versionFile.exists()))
		return "com.fluidops.iwb.IwbStart";
	String fileContents = versionFile.text; 
	if (fileContents.contains("EE"))
		return "com.fluidops.iwb.IwbComStart";
	return "com.fluidops.iwb.IwbStart";
}


/**
 * Create a solution.ref file in apps directory of 
 * provided eCM instance
 * 
 * @param solDir
 * @param platformDir
 */
def createSolutionRef(File solDir, File platformDir, String applicationDir) {
	
	File appsDir = new File(platformDir, applicationDir + "/apps")
	try {		
		appsDir.mkdirs();
		File solRefFile = new File(appsDir, "solution.ref")	
		println "Creating solution.ref file: " + solRefFile.getCanonicalPath()
		createSolutionRefHelp(solRefFile, solDir.getCanonicalPath())		
	} catch (IOException e) {
		println "\nWARN: creating solution.ref file in platform home failed: " + e.getMessage()
		println ""
		File solRefFileInSolution = new File(solDir, "solution.ref")
		println "Trying to create a solution.ref file in the solution project workspace: " + solRefFileInSolution.getCanonicalPath()
		createSolutionRefHelp(solRefFileInSolution, solRefFileInSolution.getCanonicalPath());
		println "\n\nATTENTION: Please copy the solution.ref file manually to " + appsDir.getCanonicalPath()
		println "\n\n"
	}	
}

def createSolutionRefHelp(File solRefFile, String solDir) {
	solRefFile.withWriter("UTF-8") { writer ->
		def pw = writer.newPrintWriter()
		pw.println solDir
	}
}

/**
 * Creates a copy of the current working set of the application dir
 * including
 * 
 *  - config/*
 *  - data/wiki
 *  - config.prop
 *  - secrets.xml
 *  
 * The backup is created into application working directory 
 * subfolder "backup-workingset/*"
 * 
 * @param platformDir
 * @param applicationDir
 * @return
 */
def createBackupOfWorkingSet(File platformDir, String applicationDir) {
	
	println "Creating backup of working set (config/*, data/wiki/, config.prop, secrets.xml)"
	File workingDir = new File(platformDir, applicationDir)
	File backupDir = new File(workingDir, "backup-workingset")
	backupDir.mkdirs()
	
	FileUtils.copyDirectory(new File(workingDir, "config"), new File(backupDir, "config"))
	FileUtils.copyDirectory(new File(workingDir, "data/wiki"), new File(backupDir, "data/wiki"))
	copyFileIfExist(workingDir, "config.prop", backupDir)
	copyFileIfExist(workingDir, "secrets.xml", backupDir)
}

def copyFileIfExist(File workingDir, String fileName, File backupDir) {
	File source = new File(workingDir, fileName)
	if (!source.exists()) {
		println "WARN: file " + source.getCanonicalPath() + " does not exist, no backup copy created"
		return;
	}
	FileUtils.copyFile(source, new File(backupDir, fileName))
}

/**
 * Create basic content for solution.properties file
 * 
 * @param solDir
 */
def createCreateSolutionProperties(File solDir, String applicationDir) {
	
	def prjName = solDir.name
	def product = applicationDir.endsWith("fecm") ? "eCloudManager" : "Information Workbench";
	
	File solProps = new File(solDir, "solution.properties")
	solProps.withWriter("UTF-8") { writer ->
		def pw = writer.newPrintWriter()
		pw.println """solution.version=0.1
solution.name=$prjName
solution.longname=$prjName for $product
# Uncomment / define additional settings below
#solution.company=Example
#solution.contact=support@example.com
#solution.buildby=
"""
	}
}


def copy(File src, File dest) {
	def input = src.newDataInputStream()
	def output = dest.newDataOutputStream()
	output << input
	input.close()
	output.close()
}

/**
 * Copies the build scripts (+ ant contrib lib & fluid-sdk) into the solution
 * project
 * 
 * @param solDir
 */
def copyBuildScript(File solDir) {	
		
	copy(new File(".", "/resources/build.xml"), new File(solDir, "build/build.xml"))
	copy(new File(".", "/resources/ant-contrib-1.0b3.jar"), new File(solDir, "build/ant-contrib-1.0b3.jar"))
	copy(new File(".", "/lib/fluid-sdk.jar"), new File(solDir, "build/fluid-sdk.jar"))
}

def createDirInSolution(File solDir, String name) {
	File newFolder = new File(solDir, name)
	if (newFolder.exists())
		return;
	if (!newFolder.mkdirs())
		throw new IllegalStateException("Could not create solution directory: " + name)
}