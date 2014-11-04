import CountryPipelineConfig
import DeploymentConfiguration
import DevelopmentCountryPipelineConfig
import javaposse.jobdsl.dsl.ViewType
import javaposse.jobdsl.dsl.views.DeliveryPipelineView

import static ProjectHelper.*

/**
 * Application (country) Develop pipeline
 *
 * Reacts on the branch(es):
 * - develop
 * - feature-*
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
 * - Deploy to Dev
 *   based on snapshot version
 * - Deploy to Test
 *   based on snapshot version
 * - Prepare Selenium
 * - Run Selenium
 *
 * Manual Steps:
 * - Deploy to Demo (low prio)
 *   based on snapshot version
 * - Transition from develop to release branch (low prio)
 */
def applications = [
        new DevelopmentCountryPipelineConfig(country: 'uk', repoUrl: "git@git.company.tld:app01/uk-app.git", branchName: 'master',
                deployConfig: new DeploymentConfiguration(test: '10.0.0.180', demo: '10.0.0.242', devold: 'app01.dev', dev: '10.0.0.172', uat: '10.0.0.115')),

        new DevelopmentCountryPipelineConfig(country: 'sp', repoUrl: "git@git.company.tld:app01/sp-app.git", branchName: 'master',
                deployConfig: new DeploymentConfiguration(test: '10.0.0.184', demo: '10.0.0.218', devold: 'app01.dev', dev: '10.0.0.176', uat: '10.0.0.119')),

        new DevelopmentCountryPipelineConfig(country: 'nl', repoUrl: "git@git.company.tld:app01/nl-app.git", branchName: 'master',
                deployConfig: new DeploymentConfiguration(test: '10.0.0.180', demo: '10.0.0.219', devold: 'app01.dev', dev: '10.0.0.172', uat: '10.0.0.115')),

        new DevelopmentCountryPipelineConfig(country: 'pl', repoUrl: "git@git.company.tld:app01/pl-app.git", branchName: 'master',
                deployConfig: new DeploymentConfiguration(test: '10.0.0.182', demo: '10.0.0.219', devold: 'app01.dev', dev: '10.0.0.174', uat: '10.0.0.117')),
]

initialise()

def pipelineViews = []

applications.each { config ->
    createApplicationDevelopPipeLine(config, pipelineViews)
}

def createApplicationDevelopPipeLine(final CountryPipelineConfig config, def pipeViews) {

    def jobs = [:]

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
        deliveryPipelineConfiguration('release', 'uat')
        configure mailers()
    }

    jobs['releaseNewJob'] = job {
        name "${config.jobNamePrefix}-RELEASE"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
        }
        description """\
                    Releases the artifact and deploy released version to Nexus.
                    After release the pom is set to a new MINOR version
                    This will allow the triggering of the Deploy to UAT""".stripIndent()
        disabled(false)
        scm scmConfig(config)
        steps {
            shell addPreBuildShellStep(config)
            groovyCommand(readFileFromWorkspace('src/main/groovy/helperscripts/releaseProject.groovy'), config.groovyVersion) {
                scriptParam('--minor')
            }
        }

        configure manualTrigger(jobs['deployToUat'].name) {
            propertiesFile(PASSTHROUGH_PROPERTIES_FILENAME)
        }
        deliveryPipelineConfiguration('uat', 'accept')
        configure mailers()
    }

    jobs['runAdminSelenium'] = job {
        name "${config.jobNamePrefix}-RUN_ADMIN_SELENIUM"
        description "Runs the Admin Selenium tests"
        disabled(false)
        deliveryPipelineConfiguration('qa', 'selenium(admin)')
        configure manualTrigger(jobs['releaseNewJob'].name)
    }

    jobs['runSelenium'] = job {
        name "${config.jobNamePrefix}-RUN_SELENIUM"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
            stringParam('baseUrl', config.getBaseUrlFor("test"), 'Base URL descibing the TEST load balancer')
            stringParam('hubUrl', 'https://10.0.0.80:4444/wd/hub', 'URL describing the location of the Selenium Hub')
        }
        disabled(false)
        scm gitSeleniumConfig()
        steps {
            shell """\
                echo Using baseUrl \$baseUrl
                echo Using hubUrl \$hubUrl
            """.stripIndent()
        }
        //steps executeMavenCommand(config, "clean install -DskipTests")
        steps executeMavenCommand(config, "clean test -P selenium -P ${config.country}-setup -Dmaven.test.failure.ignore=true", "${config.country}/pom.xml")
        configure manualTrigger(jobs['releaseNewJob'].name)
        deliveryPipelineConfiguration('qa', 'selenium')
        configure publishTestNg(config.country)
    }

    jobs['prepareAdminSelenium'] = job {
        name "${config.jobNamePrefix}-PREPARE_ADMIN_SELENIUM"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
        }
        description "Prepares the database for running the Admin tests"
        disabled(false)
        publishers {
            downstreamParameterized {
                trigger(jobs['runAdminSelenium'].name, 'SUCCESS') {
                    currentBuild()
                    gitRevision()
                }
            }
        }
        deliveryPipelineConfiguration('prepare qa', 'selenium(admin)')
    }

    jobs['prepareSelenium'] = job {
        name "${config.jobNamePrefix}-PREPARE_SELENIUM"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
        }
        disabled(false)
        scm gitSeleniumConfig()
        steps executeMavenCommand(config, "clean test -P admin-setup -P ${config.country}-setup -Dmaven.test.failure.ignore=true", "admin/pom.xml")
        configure triggerBlockingBuildWithoutParameters(jobs['runSelenium'].name)
        blockOnUpstreamProjects()
        configure publishTestNg("admin")
        deliveryPipelineConfiguration('prepare qa', 'selenium')
        configure mailers()
    }

    jobs['deployToTest2'] = job {
        name "${config.jobNamePrefix}-DEPLOY_TO_TEST_2"
        description "Deploys to a second test machine to run the Selenium Admin tests"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
        }
        disabled(true)
        steps {
            shell createDeploymentShellScriptFor(config.deployConfig.testServer, config.country)
        }
        publishers {
            downstream(jobs['prepareAdminSelenium'].name, 'SUCCESS')
        }
        deliveryPipelineConfiguration('deploy', 'test2')
        configure mailers()
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
        blockOnUpstreamProjects()
        deliveryPipelineConfiguration('deploy', 'test')
        configure mailers()
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
        deliveryPipelineConfiguration('deploy', 'demo')
        configure mailers()
    }

    jobs['deployToDev'] = job {
        name "${config.jobNamePrefix}-DEPLOY_TO_DEV"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
        }
        disabled(false)
        steps {
            shell createDeploymentShellScriptFor(config.deployConfig.devServer, config.country)
        }
        deliveryPipelineConfiguration('deploy', 'dev')
        configure manualTrigger("${jobs['deployToDemo'].name},${jobs['releaseNewJob'].name}")
        configure mailers()
    }

    jobs['deployToOldDev'] = job {
        name "${config.jobNamePrefix}-DEPLOY_TO_OLD_DEV"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
        }
        disabled(true)
        steps {
            shell createDeployScriptToOldCNO(config, 'integration')
        }
        deliveryPipelineConfiguration('deploy', 'dev (old)')
        configure mailers()
    }

    jobs['inspectJob'] = job {
        name "${config.jobNamePrefix}-INSPECT"
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
        deliveryPipelineConfiguration('qa', 'inspect')
        configure mailers()
    }

    jobs['createDeployToNexusJob'] = job {
        name "${config.jobNamePrefix}-DEPLOY_NEXUS"
        disabled(false)
        scm scmConfig(config)

        steps executeMavenCommand(config, "clean deploy -P distribution -Dmaven.test.skip=true")
        steps {
            groovyCommand(createArtifactVersionFileInWorkspace(), config.groovyVersion)
        }
        publishers {
            downstreamParameterized {
                trigger(jobs['deployToDev'].name, 'SUCCESS') {
                    propertiesFile(PASSTHROUGH_PROPERTIES_FILENAME)
                    currentBuild()
                    gitRevision()
                }
                trigger(jobs['deployToOldDev'].name, 'SUCCESS') {
                    propertiesFile(PASSTHROUGH_PROPERTIES_FILENAME)
                    currentBuild()
                    gitRevision()
                }
                trigger(jobs['deployToTest'].name, 'SUCCESS') {
                    propertiesFile(PASSTHROUGH_PROPERTIES_FILENAME)
                    currentBuild()
                    gitRevision()
                }
            }
        }
        deliveryPipelineConfiguration('deploy', 'artifact')
        configure mailers()
    }

    jobs['integrationTestJob'] = job {
        name "${config.jobNamePrefix}-COMPONENT_TEST"
        disabled(false)
        scm scmConfig(config)
        steps {
            mvnCommands = ['clean', 'integration-test', "--settings ${config.mavenSettingsFilePath}",]
            maven(mvnCommands.join(' '), 'pom.xml') { node ->
                node / 'mavenName' << config.mavenVersion
            }
        }
        publishers publishJUnit(config)
        publishers {
            downstreamParameterized {
                trigger(jobs['createDeployToNexusJob'].name, 'SUCCESS') {
                    currentBuild()
                    gitRevision()
                }
                trigger(jobs['inspectJob'].name, 'SUCCESS') {
                    currentBuild()
                    gitRevision()
                }
            }
        }
        deliveryPipelineConfiguration('commit', 'integration')
        configure mailers()
    }

    jobs['unitTestJob'] = job {
        name "${config.jobNamePrefix}-COMMIT"
        disabled(false)
        configure addRequiredParameterForInitialJob()
        triggers {
            scm('* * * * *')
        }
        scm scmConfig(config)
        steps executeMavenCommand(config, "clean test")
        publishers publishJUnit(config)
        publishers {
            downstreamParameterized {
                trigger(jobs['integrationTestJob'].name, 'SUCCESS') {
                    currentBuild()
                    gitRevision()
                }
            }
        }
        deliveryPipelineConfiguration('commit', 'unit')
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

    pipeViews << jobs['unitTestJob'].name
}

println "Found pipeline views [${pipelineViews}]"

view(type: ViewType.NestedView) {
    name('Delivery Pipelines')
    views {
        def context = delegate
        pipelineViews.each { jobName ->
            context.view(type: ViewType.DeliveryPipelineView) {
                name(jobName)
                pipelineInstances(5)
                showAggregatedPipeline()
                columns(2)
                sorting(DeliveryPipelineView.Sorting.TITLE)
                updateInterval(60)
                enableManualTriggers()
                showAvatars()
                showChangeLog()
                pipelines {
                    component('Pipe', jobName)
                }
            }
        }
    }
}
view(type: ViewType.NestedView) {
    name('Build Pipelines')
    views {
        def context = delegate
        pipelineViews.each { jobName ->
            context.view(type: ViewType.BuildPipelineView) {
                name(jobName)
                title("${jobName} CD Pipeline")
                filterBuildQueue()
                filterExecutors()
                displayedBuilds(5)
                selectedJob(jobName)
                alwaysAllowManualTrigger()
                showPipelineParameters()
                showPipelineDefinitionHeader()
            }
        }
    }
}
