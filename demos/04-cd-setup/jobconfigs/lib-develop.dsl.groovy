import LibJobConfig

import static ProjectHelper.*

/**
 * Library Develop pipeline
 *
 * Reacts on the branch(es):
 * - develop
 *
 * Automated Steps:
 * - Commit:
 *   mvn test
 * - parallel:
 * -- Integration Test
 *   mvn deploy == snapshot version deploy to Nexus
 * -- Inspect
 *   mvn clean install
 *   mvn sonar:sonar (Java inspections)
 *   mvn sonar:sonar (JavaScript inspections)
 */

final String MAVEN_SETTINGS_FILE_COMPANY = '/var/lib/jenkins/.m2/settings-company.xml'

def applications = [
        new LibJobConfig(libName: 'platform', repoUrl: 'git@git.company.tld:app01/platform.git', branchName: 'master'),

        new LibJobConfig(libName: 'api-generic', repoUrl: 'git@git.company.tld:app01/api-generic.git', branchName: 'master'),
        new LibJobConfig(libName: 'api-rest-generic', repoUrl: 'git@git.company.tld:app01/api-rest-generic.git', branchName: 'master', hasTests: false),

        new LibJobConfig(libName: 'api-rest-uk', repoUrl: 'git@git.company.tld:app01/uk-api-rest.git', branchName: 'master'),
        new LibJobConfig(libName: 'api-rest-sp', repoUrl: 'git@git.company.tld:app01/sp-api-rest.git', branchName: 'master'),
        new LibJobConfig(libName: 'api-rest-nl', repoUrl: 'git@git.company.tld:app01/nl-api-rest.git', branchName: 'master'),
        new LibJobConfig(libName: 'api-rest-pl', repoUrl: 'git@git.company.tld:app01/pl-api-rest.git', branchName: 'master'),
        new LibJobConfig(libName: 'api-session-uk', repoUrl: 'git@git.company.tld:app01/uk-api-session.git', branchName: 'master'),
        new LibJobConfig(libName: 'api-session-sp', repoUrl: 'git@git.company.tld:app01/sp-api-session.git', branchName: 'master'),
        new LibJobConfig(libName: 'api-session-nl', repoUrl: 'git@git.company.tld:app01/nl-api-session.git', branchName: 'master'),
        new LibJobConfig(libName: 'api-session-pl', repoUrl: 'git@git.company.tld:app01/pl-api-session.git', branchName: 'master'),

        new LibJobConfig(libName: 'bin', repoUrl: 'git@git.company.tld:app01/bin.git', branchName: 'master', hasTests: false),
        new LibJobConfig(libName: 'root', repoUrl: 'git@git.company.tld:app01/root.git', branchName: 'master', hasTests: false)
]

applications.each { config ->
    createApplicationDevelopPipeLine(config)
}

def createApplicationDevelopPipeLine(LibJobConfig config) {

    def jobs = [:]

    jobs['releaseJob'] = job {
        name "${config.jobNamePrefix}-RELEASE"
        description """End of ${config.libName} pipeline.
                       Will release the artifact and deploy released version to Nexus.
                       After release the pom is set to a new MINOR version""".stripIndent()
        disabled(false)
        scm scmConfig(config)
        steps {
            shell addPreBuildShellStep(config)
        }
        if (config.isMinorReleaseType()) {
            steps {
                groovyCommand(readFileFromWorkspace('src/main/groovy/helperscripts/releaseProject.groovy'), config.groovyVersion) {
                    scriptParam('--minor')
                }
            }
        } else {
            steps executeMavenCommand(config, '--batch-mode clean release:clean release:prepare release:perform -Darguments="-DskipTests"')
        }
    }

    jobs['inspectJob'] = job {
        name "${config.jobNamePrefix}-INSPECT"
        description "Part of ${config.libName} component test, runs inspect job for Sonar"
        disabled(false)
        scm scmConfig(config)
        steps executeMavenCommand(config, 'clean install -DskipTests')
        mvnCommand = 'sonar:sonar'
        if (!config.isMinorReleaseType()) {
            mvnCommand += " -Dsonar.branch=${scmBranchName(config.branchName)}"
        }
        steps executeMavenCommand(config, mvnCommand) { node ->
            node / 'jvmOptions' << '-Xmx512m -XX:MaxPermSize=128m'
        }
    }

    jobs['createDeployToNexusJob'] = job {
        name "${config.jobNamePrefix}-DEPLOY"
        description "Part of ${config.libName} pipeline, deploys the artificats to Nexus"
        disabled(false)
        scm scmConfig(config)
        steps executeMavenCommand(config, 'clean deploy -Dmaven.test.skip=true')
        configure manualTrigger(jobs['releaseJob'].name)
    }

    jobs['componentTestJob'] = job {
        name "${config.jobNamePrefix}-COMPONENT_TEST"
        description "Part of ${config.libName}, reacts on commit job. Runs component tests only."
        disabled(false)
        scm scmConfig(config)
        steps executeMavenCommand(config, 'clean integration-test')
        publishers publishJUnit(config)
        publishers {
            downstreamParameterized {
                trigger(jobs['createDeployToNexusJob'].name, 'SUCCESS') {
                    currentBuild()      // Current build parameters
                    gitRevision()       // Pass-through Git commit that was built
                }
                trigger(jobs['inspectJob'].name, 'SUCCESS') {
                    currentBuild()      // Current build parameters
                    gitRevision()       // Pass-through Git commit that was built
                }
            }
        }
    }

    jobs['unitTestJob'] = job {
        name "${config.jobNamePrefix}-COMMIT"
        description "Start of ${config.libName} pipeline, reacts on commits - only runs tests"
        disabled(false)
        triggers {
            scm('* * * * *')
        }
        configure addRequiredParameterForInitialJob()
        scm scmConfig(config)
        steps executeMavenCommand(config, 'clean test')
        publishers publishJUnit(config)
        publishers {
            downstreamParameterized {
                trigger(jobs['componentTestJob'].name, 'SUCCESS') {
                    currentBuild()      // Current build parameters
                    gitRevision()       // Pass-through Git commit that was built
                }
            }
        }
    }


    println "Post processing new jobs to add generic behavior ..."
    jobs.values().each() { job ->
        job.label('slave')
        job.logRotator(
                -1,  // daysToKeepInt
                7,   // numToKeepInt
                -1,  // artifactDaysToKeepInt
                -1   // artifactNumToKeepInt
        )
        job.jdk(config.jdkName)
        job.configure buildWrappers()
        job.configure mailers()
    }
    println "Generated jobs are: ${jobs.values()}"
}
