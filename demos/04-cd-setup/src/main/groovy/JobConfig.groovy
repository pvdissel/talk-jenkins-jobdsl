abstract class JobConfig {

    final groovyVersion = 'Groovy 2.1.3'
    final jdkName = 'SUN JDK 1.6'
    final mavenVersion = 'Maven 3.0.4'
    final mavenSettingsFilePath = '/var/lib/jenkins/.m2/settings-app01.xml'

    final jobNamePrefix
    final repoUrl
    final branchName
    final tests

    final DeploymentConfiguration deployConfig

    JobConfig(prefix, Map map) {
        jobNamePrefix = prefix
        branchName = map['branchName']
        deployConfig = map['deployConfig']
        if (map['repoUrl'].contains('svn')) {
            repoUrl = "${map['repoUrl']}/${branchName}"
        } else {
            repoUrl = map['repoUrl']
        }
        if (map.containsKey('hasTests')) {
            tests = map['hasTests'].asBoolean()
        } else {
            tests = true
        }
        if (map.containsKey('mavenSettingsFilePath')) {
            mavenSettingsFilePath = map['mavenSettingsFilePath']
        }
    }

    def isGit() {
        repoUrl.contains("git")
    }

    def isMinorReleaseType() {
        if ((isGit() && branchName.equalsIgnoreCase('master')) || (!isGit() && branchName.equalsIgnoreCase('trunk'))) {
            return true;
        } else {
            return false;
        }
    }

    def hasTests() {
        return tests
    }
}
