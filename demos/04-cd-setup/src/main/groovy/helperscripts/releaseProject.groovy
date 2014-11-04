package helperscripts

import groovy.transform.ToString
import groovy.util.slurpersupport.GPathResult

import static Constants.*

commandExecutor = { String command ->
    def proc = command.execute()
    proc.consumeProcessOutput(System.out, System.err)
    return proc.waitFor()
}

if (System.getProperty('test')) {
    println 'In TEST mode, will only run the tests'
    runTests()
    return
}

// Jenkins sets the WORKSPACE environment variable within the execution of the job
def workspace = System.getenv("WORKSPACE")
System.exit(main(workspace, args))

//// Only methods below here

int main(String workspace, String... args) {
    if (!workspace) {
        println "System environment variable [WORKSPACE] must be set with the path to the directory containing the maven project"
        return EXIT_FAILURE
    }
    if (!args) {
        println "No arguments given"
        printHelp()
        return EXIT_FAILURE
    }
    try {
        if (args[0] ==~ /(?i)--fix/) {
            return releaseFixVersionOf(workspace)
        }
        if (args[0] ==~ /(?i)--minor/) {
            return releaseMinorVersionOf(workspace)
        }
    } catch (Exception e) {
        println e.message
        return EXIT_FAILURE
    } catch (Error e) {
        println e.message
        return EXIT_FAILURE
    }
    println "Unknown arguments given ${args}"
    printHelp()
    return EXIT_FAILURE
}

def printHelp() {
    println()
    println "Expected [--fix] or [--minor] expected"
    println "  --fix     Release project and increase major.minor.FIX version"
    println "  --minor   Release project and increase major.MINOR[.fix] version"
}

def releaseFixVersionOf(String workspace) {
    findProjectNextFixVersionFor(workspace)
    releaseProjectIn(workspace)
    createPassThroughPropertiesFileFor(workspace)
    return EXIT_SUCCESS
}

def releaseMinorVersionOf(String workspace) {
    findProjectNextMinorVersionFor(workspace)
    releaseProjectIn(workspace)
    createPassThroughPropertiesFileFor(workspace)
    return EXIT_SUCCESS
}

/**
 * Create a properties file useful for passing the 'artifactVersion' property in Jenkins containing the release version.
 * <p/>
 * Reads the <code>${RELEASE_NEXT_VERSION_KEY}</code> property from the file
 * <code>${NEXT_VERSION_PROPERTIES_FILENAME}</code>, and writes the property as <code>${ARTIFACTVERSION_KEY}</code>
 * to the file <code>${PASSTHROUGH_PROPERTIES_FILENAME}</code>.
 *
 * @param workspace Path to project workspace containing the project maven pom.xml
 */
def createPassThroughPropertiesFileFor(String workspace) {
    String version = getPassThroughVersion(workspace)
    writePassThroughPropertiesFile(workspace, version)
}

def String getPassThroughVersion(String workspace) {
    def projectFile = "${workspace}/${NEXT_VERSION_PROPERTIES_FILENAME}"
    def file = new File(projectFile)
    assert file.exists(), "File [${projectFile}] does not exist!"

    def properties = new Properties()
    properties.load(file.newInputStream())
    assertPropertiesFileContainsKey(properties, RELEASE_NEXT_VERSION_KEY, projectFile)

    return properties.getProperty(RELEASE_NEXT_VERSION_KEY)
}

def assertPropertiesFileContainsKey(Properties properties, String versionKey, String projectFile) {
    assert properties.containsKey(versionKey), "File [${projectFile}] does not contain the required property with key [${versionKey}]"
}

def writePassThroughPropertiesFile(String workspace, String version) {
    def projectFile = getPassThroughPropertiesFilename(workspace)
    def file = new File(projectFile)

    def properties = new Properties()
    properties.put(ARTIFACTVERSION_KEY, version)
    properties.store(file.newOutputStream(), null)
}

/**
 * Find the next major.minor.FIX version for a project, based on its maven pom.xml.
 * <p/>
 * Based on the maven pom.xml version, this script will create the next release and development
 * versions for this project. The versions are then written to the properties
 * file <code>${NEXT_VERSION_PROPERTIES_FILENAME}</code> in the same directory as the pom.xml file. The file will
 * have the properties with the keys <code>${RELEASE_NEXT_VERSION_KEY}</code> and
 * <code>${DEVELOP_NEXT_VERSION_KEY}</code> for the release and development versions respectively.
 *
 * Expects a correct version in the pom.xml, correct being:
 * <ul>
 *     <li>Be not empty</li>
 *     <li>Be a snapshot version (eg. major.minor.fix-SNAPSHOT)</li>
 *     <li>Have 2 or 3 version parts (eg. major.minor, or major.minor.fix)</li>
 *     <li>When it does not have a fix part, it will be added and set to '1'</li>
 * </ul>
 * Throws an error on anything else.
 *
 * @param workspace Path to project workspace containing the project maven pom.xml
 */
def findProjectNextFixVersionFor(final String workspace) {
    Versions versions = createNextFixVersionBasedOnPomFromWorkspace(workspace)
    writeVersionsToPropertiesFile(versions, getNextVersionPropertiesFilename(workspace))
}

def createNextFixVersionBasedOnPomFromWorkspace(String workspace) {
    GPathResult project = getMavenProjectFile(workspace)
    String projectVersion = getCurrentProjectVersion(project)
    println "Read current version: ${projectVersion}"
    Versions versions = newProjectFixVersions(projectVersion)
    return versions
}

/**
 * Find the next major.MINOR.fix version for a project, based on its maven pom.xml.
 * <p/>
 * Based on the maven pom.xml version, this script will create the next release and development
 * versions for this project. The versions are then written to the properties
 * file <code>${NEXT_VERSION_PROPERTIES_FILENAME}</code> in the same directory as the pom.xml file. The file will
 * have the properties with the keys <code>${RELEASE_NEXT_VERSION_KEY}</code> and
 * <code>${DEVELOP_NEXT_VERSION_KEY}</code> for the release and development versions respectively.
 *
 * Expects a correct version in the pom.xml, correct being:
 * <ul>
 *     <li>Be not empty</li>
 *     <li>Be a snapshot version (eg. major.minor.fix-SNAPSHOT)</li>
 *     <li>Have 2 or 3 version parts (eg. major.minor, or major.minor.fix)</li>
 *     <li>When it has a fix part, it should have a value of zero '0'</li>
 * </ul>
 * Throws an error on anything else.
 *
 * @param workspace Path to project workspace containing the project maven pom.xml
 */
def findProjectNextMinorVersionFor(final String workspace) {
    Versions versions = createNextMinorVersionBasedOnPomFromWorkspace(workspace)
    writeVersionsToPropertiesFile(versions, getNextVersionPropertiesFilename(workspace))
}

def createNextMinorVersionBasedOnPomFromWorkspace(String workspace) {
    GPathResult project = getMavenProjectFile(workspace)
    String projectVersion = getCurrentProjectVersion(project)
    println "Read current version : ${projectVersion}"
    Versions versions = newProjectMinorVersions(projectVersion)
    return versions
}

/**
 * Run maven-release-plugin clean, prepare, perform.
 * <p/>
 * Versions to use during the release are read from the file <code>${NEXT_VERSION_PROPERTIES_FILENAME}</code>.
 * This file must contain the properties with the keys <code>${RELEASE_NEXT_VERSION_KEY}<code> and
 * <code>${DEVELOP_NEXT_VERSION_KEY}</code>.
 *
 * @param workspace Path to project workspace containing the project maven pom.xml
 */
def releaseProjectIn(final String workspace) {
    Versions versions = getVersions(workspace)
    return releaseWith(versions)
}

//// Generic version methods

def getMavenProjectFile(String workspace) {
    def projectFile = "${workspace}/pom.xml"
    def project
    try {
        project = new XmlSlurper().parse(new File(projectFile))
    } catch (IOException ex) {
        throw new RuntimeException("Unable to read project file [${projectFile}]", ex)
    }
    return project
}

def getCurrentProjectVersion(GPathResult project) {
    def projectVersion
    try {
        projectVersion = project.version.toString()
    } catch (Exception ex) {
        throw new RuntimeException("Unable to get version from pom.xml", ex)
    }
    return projectVersion
}

def newProjectFixVersions(String projectVersion) {
    def semanticVersion = getSemanticVersion(projectVersion)
    verifyValidProjectVersion(projectVersion)
    verifyValidSemanticProjectVersion(semanticVersion)
    def projectReleaseVersion = semanticVersion
    def projectDevelopmentVersion = newDevelopmentFixVersion(semanticVersion)
    return new Versions(projectReleaseVersion, projectDevelopmentVersion)
}

def newProjectMinorVersions(String projectVersion) {
    def semanticVersion = getSemanticVersion(projectVersion)
    verifyValidProjectVersion(projectVersion)
    verifyValidSemanticProjectVersion(semanticVersion)
    String[] parts = splitVersionParts(semanticVersion)
    verifyFixVersionShouldBeZero(parts)
    def projectReleaseVersion = semanticVersion
    def projectDevelopmentVersion = newDevelopmentMinorVersion(semanticVersion)
    return new Versions(projectReleaseVersion, projectDevelopmentVersion)
}

def String getSemanticVersion(String version) {
    def indexOfClassifier = version.indexOf('-')
    return version.substring(0, (indexOfClassifier != -1 ? version.indexOf('-') : version.size()))
}

def verifyValidProjectVersion(String version) {
    assert !version.empty
    assert version.endsWith('-SNAPSHOT')
}

def verifyValidSemanticProjectVersion(String semanticVersion) {
    String[] parts = splitVersionParts(semanticVersion)
    verifyVersionShouldContainMajorMinorAndOptionallyFixParts(parts)
}

def verifyFixVersionShouldBeZero(String[] parts) {
    if (parts.length == 3) {
        assert parts[2] == '0', "major.minor.FIX version part of the version should always be zero when increase MINOR version (${parts} "
    }
}

def newDevelopmentFixVersion(String semanticVersion) {
    version = increaseFixVersion(semanticVersion)
    return "${version}-SNAPSHOT"
}

def newDevelopmentMinorVersion(String semanticVersion) {
    version = increaseMinorVersion(semanticVersion)
    return "${version}-SNAPSHOT"
}

def increaseFixVersion(String version) {
    String[] v = splitVersionParts(version)
    if (v.length > 2) {
        v[2] = v[2].toInteger() + 1
    } else {
        v = [v[0], v[1], 1]
    }
    return v.join('.')
}

def increaseMinorVersion(String version) {
    String[] v = splitVersionParts(version)
    v[1] = v[1].toInteger() + 1
    if (v.length > 2) {
        v[2] = 0
    }
    return v.join('.')
}

def String[] splitVersionParts(String version) {
    return version.split(/\./)
}

def verifyVersionShouldContainMajorMinorAndOptionallyFixParts(String[] parts) {
    assert parts.length >= 2
    assert parts.length <= 3
}

def writeVersionsToPropertiesFile(Versions versions, String propertiesFileName) {
    def paramsFile = new ParameterPropertyFile()
    paramsFile.setFilename(propertiesFileName)
    paramsFile.put(RELEASE_NEXT_VERSION_KEY, versions.release)
    paramsFile.put(DEVELOP_NEXT_VERSION_KEY, versions.development)
    writeToPropertiesFile(paramsFile)
}

def writeToPropertiesFile(ParameterPropertyFile paramsFile) {
    def filename = paramsFile.getFilename()
    def propsFile = new File(filename)
    def properties = new Properties()

    propsFile.delete()
    paramsFile.each { key, value ->
        properties.put(key.toString(), value.toString())
    }
    properties.store(propsFile.newOutputStream(), null)
}

def releaseWith(Versions versions) {
    output "Releasing new version"
    output "  release version:     ${versions.release}"
    output "  development version: ${versions.development}"

    def releaseCommand = [
            '/var/lib/jenkins/tools/hudson.tasks.Maven_MavenInstallation/Maven_3.0.4/bin/mvn',
            '--batch-mode',
            'clean',
            '-Pdistribution',
            'release:clean',
            'release:prepare',
            'release:perform',
            "-Darguments='-Dmaven.test.skip=true'",
            "-DreleaseVersion=${versions.release}",
            "-DdevelopmentVersion=${versions.development}",
            '--settings /var/lib/jenkins/.m2/settings-app01.xml',
            '--global-settings /var/lib/jenkins/.m2/global.settings-company.xml',
    ].join(' ')

    output "Executing command [${releaseCommand}]"
    def exitStatus = executeCommand(releaseCommand)
    if (exitStatus != EXIT_SUCCESS) {
        throw new RuntimeException("Command exitted with error code [${exitStatus}]")
    }
}

def Versions getVersions(String workspace) {
    def projectFile = getNextVersionPropertiesFilename(workspace)
    def file = new File(projectFile)
    assert file.exists(), "File [${projectFile}] does not exist!"

    def properties = new Properties()
    properties.load(file.newInputStream())
    assertPropertiesFileContainsKey(properties, RELEASE_NEXT_VERSION_KEY, projectFile)
    assertPropertiesFileContainsKey(properties, DEVELOP_NEXT_VERSION_KEY, projectFile)

    def releaseVersion = properties.getProperty(RELEASE_NEXT_VERSION_KEY)
    def developmentVersion = properties.getProperty(DEVELOP_NEXT_VERSION_KEY)
    return new Versions(releaseVersion, developmentVersion)
}

def getNextVersionPropertiesFilename(String workspace) {
    return "${workspace}/${NEXT_VERSION_PROPERTIES_FILENAME}"
}

def getPassThroughPropertiesFilename(String workspace) {
    return "${workspace}/${PASSTHROUGH_PROPERTIES_FILENAME}"
}

def executeCommand(String command) {
    commandExecutor(command)
}

def output(String text) {
    if (!System.getProperty('test')) {
        println text
    }
}

//// Generic classes

class Constants {
    static final String NEXT_VERSION_PROPERTIES_FILENAME = '.version.next.properties'
    static final String RELEASE_NEXT_VERSION_KEY = 'releaseVersion'
    static final String DEVELOP_NEXT_VERSION_KEY = 'developVersion'
    static final String PASSTHROUGH_PROPERTIES_FILENAME = '.jenkins.job.passthrough.properties'
    static final String ARTIFACTVERSION_KEY = 'artifactVersion'
    static final int EXIT_SUCCESS = 0
    static final int EXIT_FAILURE = -1
}

@ToString
class Versions {
    final String release
    final String development

    Versions(releaseVersion, developmentVersion) {
        release = releaseVersion
        development = developmentVersion
    }
}

class ParameterPropertyFile implements Map<String, String> {
    @Delegate
    final Map<String, String> properties = [:]

    String filename

    String getFilename() {
        return filename
    }

    void setFilename(String filename) {
        this.filename = filename
    }
}

//// Only tests below here

def runTests() {
    runPassThroughArtifactVersionTests()
    runNextFixVersionTests()
    runNextMinorVersionTests()
    runReleaseVersionTests()
    println "GREEN: it's all good :)"
}

def runPassThroughArtifactVersionTests() {
    println "\nRunning pass-through artifact version tests..."

    testThat("value of property [${RELEASE_NEXT_VERSION_KEY}] from file [${NEXT_VERSION_PROPERTIES_FILENAME}] is used as pass-through version") {
        def releaseVersion = '1.2.3'
        withVersionsPropertiesFile(new Versions(releaseVersion, '1.2.4-SNAPSHOT')) {
            def workspace = new File('').absolutePath
            def version = getPassThroughVersion(workspace)
            assert version == '1.2.3'
        }
    }

    testThat("version is written as value for [${ARTIFACTVERSION_KEY}] property in file [${PASSTHROUGH_PROPERTIES_FILENAME}]") {
        def workspace = new File('').absolutePath
        def releaseVersion = '1.2.3'
        writePassThroughPropertiesFile(workspace, releaseVersion)

        assertWritePropertiesFile(releaseVersion)
    }
}

def runNextFixVersionTests() {
    println "\nRunning next FIX version tests..."

    def expectedVersions = [
            // projectV.    : rel.    , dev.
            '1.2-SNAPSHOT': ['1.2', '1.2.1-SNAPSHOT'],
            '1.2.0-SNAPSHOT': ['1.2.0', '1.2.1-SNAPSHOT'],
            '1.2.3-SNAPSHOT': ['1.2.3', '1.2.4-SNAPSHOT']
    ]
    testValidFixVersions(expectedVersions)

    def invalidVersions = [
            '1',
            '1-SNAPSHOT',
            '1.2',
            '1.2.3.4',
            '1.2.3.4-SNAPSHOT',
    ]
    testInvalidVersions(invalidVersions) { projectVersion ->
        newProjectFixVersions(projectVersion)
    }
}

def runNextMinorVersionTests() {
    println "\nRunning next MINOR version tests..."

    def expectedVersions = [
            // projectV.    : rel.    , dev.
            '1.2-SNAPSHOT': ['1.2', '1.3-SNAPSHOT'],
            '1.2.0-SNAPSHOT': ['1.2.0', '1.3.0-SNAPSHOT'],
    ]
    testValidMinorVersions(expectedVersions)

    def invalidVersions = [
            '1',
            '1-SNAPSHOT',
            '1.2',
            '1.2.3-SNAPSHOT',
            '1.2.3.4',
            '1.2.3.4-SNAPSHOT',
    ]
    testInvalidVersions(invalidVersions) { projectVersion ->
        newProjectMinorVersions(projectVersion)
    }
}

def runReleaseVersionTests() {
    println "\nRunning release version tests..."

    // Command executor mock
    commandExecutor = { String command ->
        return command
    }

    testThat("versions are read correctly from properties file") {
        def versions = new Versions('1.2.3', '1.2.4-SNAPSHOT')
        versionsAreReadCorrectlyFromPropertiesFile(versions)
    }
    testThat("releasing is executing the correct maven command") {
        def versions = new Versions('1.2.3', '1.2.4-SNAPSHOT')
        String expectedCmd = [
                '/var/lib/jenkins/tools/hudson.tasks.Maven_MavenInstallation/Maven_3.0.4/bin/mvn',
                '--batch-mode',
                'clean',
                '-Pdistribution',
                'release:clean',
                'release:prepare',
                'release:perform',
                "-Darguments='-Dmaven.test.skip=true'",
                "-DreleaseVersion=${versions.release}",
                "-DdevelopmentVersion=${versions.development}",
                '--settings /var/lib/jenkins/.m2/settings-app01.xml',
                '--global-settings /var/lib/jenkins/.m2/global.settings-company.xml',
        ].join(' ')
        assert releaseWith(versions) == expectedCmd
    }
}

def withVersionsPropertiesFile(Versions versions, Closure closure) {
    def properties = new Properties()
    properties.put(RELEASE_NEXT_VERSION_KEY, versions.release)
    properties.put(DEVELOP_NEXT_VERSION_KEY, versions.development)

    def propertiesFile = new File(NEXT_VERSION_PROPERTIES_FILENAME)
    properties.store(propertiesFile.newOutputStream(), null)
    assert propertiesFile.exists()

    try {
        closure()
    } finally {
        propertiesFile.delete()
    }
}

def assertWritePropertiesFile(String version) {
    def propertiesFile = new File(PASSTHROUGH_PROPERTIES_FILENAME)
    assert propertiesFile.exists()
    try {
        def properties = new Properties()
        propertiesFile.withInputStream { stream ->
            properties.load(stream)
        }
        assert properties.getProperty(ARTIFACTVERSION_KEY) == version
    } finally {
        assert propertiesFile.delete()
    }
}

def versionsAreReadCorrectlyFromPropertiesFile(Versions versions) {
    withVersionsPropertiesFile(versions) {
        def workspace = new File('').absolutePath
        def actual = getVersions(workspace)
        assert versions.release == actual.release
        assert versions.development == actual.development
    }
}

def testInvalidVersions(ArrayList<String> invalidVersions, Closure closure) {
    invalidVersions.each { projectVersion ->
        testFailure("[${projectVersion}] is invalid") {
            closure(projectVersion)
        }
    }
}

def testValidFixVersions(LinkedHashMap<String, ArrayList<String>> expectedVersions) {
    expectedVersions.each { projectVersion, expected ->
        def (expRel, expDev) = expected
        testThat("[${projectVersion}] is valid and results in releaseVersion [${expRel}], developmentVersion [${expDev}]") {
            Versions versions = newProjectFixVersions(projectVersion)
            assertVersions(versions, expected)
        }
        testThat("the new release [${expRel}] and development [${expDev}] versions are written correctly to properties file") {
            Versions versions = newProjectFixVersions(projectVersion)
            assertWritePropertiesFile(versions)
        }
    }
}

def testValidMinorVersions(LinkedHashMap<String, ArrayList<String>> expectedVersions) {
    expectedVersions.each { projectVersion, expected ->
        def (expRel, expDev) = expected
        testThat("[${projectVersion}] is valid and results in releaseVersion [${expRel}], developmentVersion [${expDev}]") {
            Versions versions = newProjectMinorVersions(projectVersion)
            assertVersions(versions, expected)
        }
        testThat("the new release [${expRel}] and development [${expDev}] versions are written correctly to properties file") {
            Versions versions = newProjectMinorVersions(projectVersion)
            assertWritePropertiesFile(versions)
        }
    }
}

def assertVersions(Versions versions, List<String> expected) {
    def projectReleaseVersion = versions.release
    def projectDevelopmentVersion = versions.development

    def (expRel, expDev) = expected
    assert expRel == projectReleaseVersion
    assert expDev == projectDevelopmentVersion
}

def assertWritePropertiesFile(Versions versions) {
    def testPropertiesFileName = '.test.valid.versions.properties'
    writeVersionsToPropertiesFile(versions, testPropertiesFileName)
    def propertiesFile = new File(testPropertiesFileName)
    assert propertiesFile.exists()
    try {
        def properties = new Properties()
        propertiesFile.withInputStream { stream ->
            properties.load(stream)
        }
        assert properties.getProperty(RELEASE_NEXT_VERSION_KEY) == versions.release
        assert properties.getProperty(DEVELOP_NEXT_VERSION_KEY) == versions.development
    } finally {
        assert propertiesFile.delete()
    }
}

def testThat(String description, Closure closure) {
    print "Test that ${description}: "
    try {
        closure()
    } catch (AssertionError e) {
        println "FAIL"
        throw e
    }
    println "OK"
}

def testFailure(String description, Closure closure) {
    print "Test that ${description}: "
    try {
        closure()
    } catch (AssertionError e) {
        println "OK"
        return
    }
    println "FAIL"
    throw e
}
