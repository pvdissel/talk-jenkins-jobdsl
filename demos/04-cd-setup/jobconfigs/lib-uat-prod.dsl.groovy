import LibJobConfig

import static ProjectHelper.*

/**
 * Library UAT pipeline
 *
 */

def applications = [

        new LibJobConfig(libName: 'pe-uk', repoUrl: 'git@git.company.tld:app01/platform.git', branchName: 'uat-uk'),
        new LibJobConfig(libName: 'pe-pl', repoUrl: 'git@git.company.tld:app01/platform.git', branchName: 'uat-pl'),
        new LibJobConfig(libName: 'pe-sp', repoUrl: 'git@git.company.tld:app01/platform.git', branchName: 'uat-sp'),
        new LibJobConfig(libName: 'pe-nl', repoUrl: 'git@git.company.tld:app01/platform.git', branchName: 'uat-nl'),

        new LibJobConfig(libName: 'pe-uk', repoUrl: 'git@git.company.tld:app01/platform.git', branchName: 'prod-uk'),
        new LibJobConfig(libName: 'pe-pl', repoUrl: 'git@git.company.tld:app01/platform.git', branchName: 'prod-pl'),
        new LibJobConfig(libName: 'pe-sp', repoUrl: 'git@git.company.tld:app01/platform.git', branchName: 'prod-sp'),
        new LibJobConfig(libName: 'pe-nl', repoUrl: 'git@git.company.tld:app01/platform.git', branchName: 'prod-nl'),
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
                       After release the pom is set to a new FIX version""".stripIndent()
        disabled(false)
        scm scmConfig(config)
        steps {
            shell addPreBuildShellStep(config)
        }
        steps executeMavenCommand(config, '--batch-mode clean release:clean release:prepare release:perform -Darguments="-DskipTests"')
    }

    jobs['deployToNexusJob'] = job {
        name "${config.jobNamePrefix}-DEPLOY"
        description "Part of ${config.libName} pipeline, deploys the artificats to Nexus"
        disabled(false)
        scm scmConfig(config)
        steps executeMavenCommand(config, 'clean deploy -Dmaven.test.skip=true')
        configure manualTrigger(jobs['releaseJob'].name)
    }

    jobs['unitTestJob'] = job {
        name "${config.jobNamePrefix}-COMMIT"
        description "Start of ${config.libName} pipeline, reacts on UAT commits - runs tests and integration tests"
        disabled(false)
        triggers {
            scm('* * * * *')
        }
        configure addRequiredParameterForInitialJob()
        scm scmConfig(config)
        steps executeMavenCommand(config, 'clean test  integration-test')
        publishers publishJUnit(config)
        publishers {
            downstreamParameterized {
                trigger(jobs['deployToNexusJob'].name, 'SUCCESS') {
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
    }
    println "Generated jobs are: ${jobs.values()}"
}
