package tld.company.taxp

import javaposse.jobdsl.dsl.JobParent
import tld.company.taxp.logging.ColorOutputLogger
import tld.company.taxp.logging.Logger
import tld.company.taxp.model.pipeline.ci.LifeCycle

class Runner {
    private binding
    private jobs = []
    private Logger log
    private dslHelper
    private jobDefaults

    Runner(binding, localPath = AppConfig.DEFAULT_CONFIGS_PATH) {
        this.binding = binding
        dslHelper = new JobDslHelper(binding, localPath)
        log = new ColorOutputLogger(dslHelper.out)
    }

    def jobs(subject = '', LifeCycle lifeCycle, Closure configure) {
        jobs(subject, [lifeCycle], configure)
    }

    def jobs(subject = '', List<LifeCycle> lifeCycles = [LifeCycle.TEST], Closure configure) {
        jobs << new RunnerSection(subject, lifeCycles, configure)
        return this
    }

    def jobDefaults(Closure configure) {
        jobDefaults = configure
        return this
    }

    def run(JobParent parent) {
        def jobLifeCycle = dslHelper.lifeCycle
        def settings = dslHelper.settings
        log.debug = settings.contains(JobDslSetting.DEBUG)

        log.note "ScriptSettings are set to [${settings}]"

        File workspace = dslHelper.workspace
        def configs = new AppConfig(log, settings).readConfigs(workspace)

        def results = []
        if (settings.contains(JobDslSetting.ALL_LIFECYCLES)) {
            LifeCycle.values().each { lifeCycle ->
                results << runSectionFor(lifeCycle, configs, settings, parent)
            }
        } else {
            results << runSectionFor(jobLifeCycle, configs, settings, parent)
        }

        def failedJobCreations = results.findAll { !it }
        if (!failedJobCreations.empty) {
            throw new RunHasFailedJobCreations("There was an error during the creation of [${failedJobCreations.size()}] job(s)")
        }
    }

    private def runSectionFor(lifeCycle, configs, scriptSettings, JobParent parent) {
        def results = []
        jobs.each { RunnerSection section ->
            def introText = "Running jobDsl for [${section.subject}] in lifeCycle [${lifeCycle}]"
            log.note '-' * introText.length()
            log.note introText

            if (!section.lifeCycles.contains(lifeCycle)) {
                log.info "Nothing to do for lifeCycle [${lifeCycle}]"
                return
            }

            def jobCreators = []
            section.closure.call(jobCreators, log, configs, scriptSettings, lifeCycle)
            jobCreators.each { JobCreator creator ->
                results += dslHelper.executeWithStatusLine("Generating job [${creator.name}] with type [${creator.type}]") { messages ->
                    jobDefaults.call(creator.create(parent))
                }
            }
        }
        return results.findAll { !it }.empty
    }
}
