package tld.company.taxp

import groovy.json.JsonSlurper
import tld.company.taxp.logging.Logger
import tld.company.taxp.model.Pipeline
import tld.company.taxp.model.Project
import tld.company.taxp.model.pipeline.ci.JUnit

class AppConfig {
    static final DEFAULT_CONFIGS_PATH = 'appsconfig'
    static final TEST_CONFIGS_PATH = 'test-appsconfig'
    final List<JobDslSetting> settings
    private final Logger log

    def canaryApps = [
            'AAB',  // svn, maven
            'ABD',  // git, maven : backspin with api, app, db
    ]
    private final def testApps = [
            'ABD'
    ]

    AppConfig(Logger log, settings) {
        this.log = log
        this.settings = settings
    }

    List<Project> readConfigs(File workspace) {
        def files = readConfigFiles(workspace)
        def configs = []
        files.each { file ->
            def slurper = new JsonSlurper()
            Project project = slurper.parseText(file.text)
            project.shortName = file.name.substring(0, file.name.indexOf('.json'))
            sanitize(project)
            configs << project
        }
        return configs
    }

    private void sanitize(Project project) {
        project.shortName = project.shortName.toUpperCase()
        if (!project.fullName) {
            throw new IllegalStateException("Config of project [${project.shortName}] does not have a [fullName] set")
        }
        project.pipelines.each { pipeline ->
            sanitizePipeline(pipeline)
        }
    }

    private void sanitizePipeline(Pipeline pipeline) {
        if (pipeline.id) pipeline.id = pipeline.id.toUpperCase()

        if (pipeline.ci && pipeline.type.equalsIgnoreCase("APP") && !pipeline.ci.junit) {
            pipeline.ci.junit = new JUnit()
        }
    }

    private def readConfigFiles(File workspace) {
        def appConfigFiles = workspace.listFiles().findAll { it.name.endsWith('.json') }
        if (settings.contains(JobDslSetting.CANARY)) {
            log.note "Limiting to the pre-defined subset of applications to the CanarySet: ${canaryApps}"
            appConfigFiles = workspace.listFiles().findAll {
                def fileAppName = it.name.toLowerCase().replaceAll('.json', '').toUpperCase()
                canaryApps.contains(fileAppName) && it.name.endsWith('.json')
            }
        } else if (settings.contains(JobDslSetting.TESTRUN)) {
            log.note "Limiting to the pre-defined subset of applications: ${testRunApps}"
            appConfigFiles = workspace.listFiles().findAll {
                def fileAppName = it.name.toLowerCase().replaceAll('.json', '').toUpperCase()
                testRunApps.contains(fileAppName) && it.name.endsWith('.json')
            }
        }
        return appConfigFiles.sort()
    }

    private def getTestRunApps() {
        return canaryApps + testApps
    }
}
