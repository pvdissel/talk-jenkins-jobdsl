import CountryPipelineConfig
import DeploymentConfiguration
import UatCountryPipelineConfig

import static ProjectHelper.*

/**
 * Application (country) Release pipeline
 *
 * Reacts on the branch(es):
 * - uat-*
 *
 * Automated Steps:
 * - Commit:
 *   mvn deploy == snapshot version deploy to Nexus
 * - parallel (low prio):
 * -- Integration Test (low prio)
 *   mvn deploy == snapshot version deploy to Nexus
 * -- Inspect  (low prio)
 *   mvn clean install
 *   mvn sonar:sonar (Java inspections)
 *   mvn sonar:sonar (JavaScript inspections)
 * - Deploy to Test
 *   based on snapshot version
 * - Prepare Selenium
 * - Run Selenium
 * - Release Version
 *   mvn release:clean release:prepare release:perform  == version bump and release version to Nexus
 *
 * Manual Steps:
 * - Deploy to UAT
 * - Deploy to Pre-Production
 * - Deploy to Production
 * - Transition from release branch to master (low prio)
 */
def applications = [

        new UatCountryPipelineConfig(country: 'uk', repoUrl: "git@git.company.tld:app01/uk-app.git", branchName: 'uat',
                deployConfig: new DeploymentConfiguration(test: '10.0.0.180', demo: '10.0.0.242', uat: '10.0.0.115')),

        new UatCountryPipelineConfig(country: 'sp', repoUrl: "git@git.company.tld:app01/sp-app.git", branchName: 'uat',
                deployConfig: new DeploymentConfiguration(test: '10.0.0.184', demo: '10.0.0.218', uat: '10.0.0.116')),

        new UatCountryPipelineConfig(country: 'nl', repoUrl: "git@git.company.tld:app01/nl-app.git", branchName: 'uat',
                deployConfig: new DeploymentConfiguration(test: '10.0.0.180', demo: '10.0.0.219', uat: '10.0.0.115')),

        new UatCountryPipelineConfig(country: 'pl', repoUrl: "git@git.company.tld:app01/pl-app.git", branchName: 'uat',
                deployConfig: new DeploymentConfiguration(test: '10.0.0.182', demo: '10.0.0.219', uat: '10.0.0.117')),
]

initialise()

applications.each { config ->
    createApplicationReleasePipeLine(config)
}

def createApplicationReleasePipeLine(final CountryPipelineConfig config) {
    println "Currently set SCM to SVN - this can be changed here when switched to GIT".warn()

    def jobs = [:]
    println "Generating XML of Release Pipeline for ${config.jobNamePrefix}"
    println "Generating XML of Release Pipeline (Manual Deploy Jobs) for ${config.jobNamePrefix}"


    jobs['deployToUat'] = job {
        name "${config.jobNamePrefix}-DEPLOY_TO_UAT"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
        }
        disabled(false)
        scm scmConfig(config)
        steps {
            shell createDeploymentShellScriptFor(config.deployConfig.uatServer, config.country)
        }
    }

    jobs['releaseNewJob'] = job {
        name "${config.jobNamePrefix}-RELEASE"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
        }
        description """\
            Releases the artifact and deploy released version to Nexus.
            After release the pom is set to a new FIX version""".stripIndent()
        disabled(false)
        scm scmConfig(config)
        steps {
            shell addPreBuildShellStep(config)
            groovyCommand(readFileFromWorkspace('src/main/groovy/helperscripts/releaseProject.groovy'), config.groovyVersion) {
                scriptParam('--fix')
            }
        }
        configure manualTrigger(jobs['deployToUat'].name) {
            propertiesFile(PASSTHROUGH_PROPERTIES_FILENAME)
        }
    }

    jobs['runSelenium'] = job {
        name "${config.jobNamePrefix}-RUN_SELENIUM"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
            stringParam('baseUrl', config.getBaseUrlFor("test"), 'Base URL descibing the TEST load balancer')
            stringParam('hubUrl', 'https://10.0.0.80:4444/wd/hub', 'URL describing the location of the Selenium Hub')
        }
        disabled(true)
        scm gitSeleniumConfig()
        steps {
            shell """\
                echo Using baseUrl \$baseUrl
                echo Using hubUrl \$hubUrl
            """.stripIndent()
        }
        steps executeMavenCommand(config, "clean install -DskipTests")
        steps executeMavenCommand(config, "test -P selenium -P ${config.country}-setup", "${config.country}/pom.xml")
        configure manualTrigger(jobs['releaseNewJob'].name)
        configure publishTestNg(config.country)
    }

    jobs['prepareSelenium'] = job {
        name "${config.jobNamePrefix}-PREPARE_SELENIUM"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
        }
        disabled(true)
        scm gitSeleniumConfig()
        steps executeMavenCommand(config, "clean install -DskipTests")
        steps executeMavenCommand(config, "test -P admin-setup -P ${config.country}-setup", "admin/pom.xml")
        configure triggerBlockingBuildWithoutParameters(jobs['runSelenium'].name)
        configure publishTestNg("admin")
    }

    jobs['deployToTest'] = job {
        name "${config.jobNamePrefix}-DEPLOY_TO_TEST"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
        }
        disabled(false)
        steps {
            shell createDeploymentShellScriptFor(config.deployConfig.testServer, config.country)
        }
        configure throttleConcurrentBuildCategories(1, 0, "using-app01-" + config.country + "-cio-test-server")
        configure triggerBlockingBuildWithoutParameters(jobs['prepareSelenium'].name)

        configure manualTrigger(jobs['releaseNewJob'].name)
    }

    jobs['deployToDemo'] = job {
        name "${config.jobNamePrefix}-DEPLOY_TO_DEMO"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
        }
        disabled(false)
        steps {
            shell createDeploymentShellScriptFor(config.deployConfig.demoServer, config.country)
        }
    }

    jobs['unitTestJob'] = job {
        name "${config.jobNamePrefix}-COMMIT"
        description "Start of ${config.country} pipeline, reacts on UAT commits - runs tests and integration tests"
        disabled(false)
        triggers {
            scm('* * * * *')
        }
        configure addRequiredParameterForInitialJob()
        scm scmConfig(config)
        steps executeMavenCommand(config, "clean test integration-test deploy -Pdistribution")
        steps {
            groovyCommand(createArtifactVersionFileInWorkspace(), config.groovyVersion)
        }
        publishers publishJUnit(config)
        configure manualTrigger("${jobs['deployToDemo'].name}")
        publishers {
            downstreamParameterized {
                trigger(jobs['deployToTest'].name, 'SUCCESS') {
                    propertiesFile(PASSTHROUGH_PROPERTIES_FILENAME)
                    currentBuild()
                    gitRevision()
                }
            }
        }
        configure buildWrappers()
        configure mailers()
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
