import CountryPipelineConfig
import DeploymentConfiguration
import ProdCountryPipelineConfig

import static ProjectHelper.*

def applications = [

        new ProdCountryPipelineConfig(country: 'uk', repoUrl: "git@git.company.tld:app01/uk-app.git", branchName: 'prod',
                deployConfig: new DeploymentConfiguration(test: '10.0.0.180', demo: '10.0.0.242', uat: '10.0.0.115', prod: '172.28.223.35')),

        new ProdCountryPipelineConfig(country: 'sp', repoUrl: "git@git.company.tld:app01/sp-app.git", branchName: 'prod',
                deployConfig: new DeploymentConfiguration(test: '10.0.0.184', demo: '10.0.0.218', uat: '10.0.0.116', prod: '172.28.223.33')),

        new ProdCountryPipelineConfig(country: 'nl', repoUrl: "git@git.company.tld:app01/nl-app.git", branchName: 'prod',
                deployConfig: new DeploymentConfiguration(test: '10.0.0.180', demo: '10.0.0.219', uat: '10.0.0.115', prod: '172.28.223.35')),

        new ProdCountryPipelineConfig(country: 'pl', repoUrl: "git@git.company.tld:app01/pl-app.git", branchName: 'prod',
                deployConfig: new DeploymentConfiguration(test: '10.0.0.182', demo: '10.0.0.219', uat: '10.0.0.117', prod: '172.28.223.37')),

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


    jobs['deployToProd'] = job {
        name "${config.jobNamePrefix}-DEPLOY_TO_PROD"
        parameters {
            stringParam(ARTIFACTVERSION_KEY, null, 'Version of artifact to deploy')
        }
        disabled(false)
        scm scmConfig(config)
        steps {
            shell createPullDeploymentShellScriptFor(config.deployConfig.prodServer, config.country)
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
        configure manualTrigger(jobs['deployToProd'].name) {
            propertiesFile(PASSTHROUGH_PROPERTIES_FILENAME)
        }
        configure buildWrappers()
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
