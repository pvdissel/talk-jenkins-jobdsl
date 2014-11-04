import tld.company.taxp.*
import tld.company.taxp.model.pipeline.ci.LifeCycle
import tld.company.taxp.project.CreateProjectJobs
import tld.company.taxp.sprinthopping.AutoCreateBranchJobCreator
import tld.company.taxp.sprinthopping.BranchFromLastStableTagFlowJobCreator
import tld.company.taxp.testautomation.DailyFlowJobCreator
import tld.company.taxp.testautomation.EveningFlowJobCreator
import tld.company.taxp.testautomation.NightlyFlowJobCreator

// For reference, see:
// - https://github.com/jenkinsci/job-dsl-plugin/wiki/Job-DSL-Commands
// - https://github.com/jenkinsci/job-dsl-plugin/wiki/Job-reference
// - https://github.com/jenkinsci/job-dsl-plugin/wiki/The-Configure-Block

OpExResolver opExResolver = new StaticDataOpExResolver()
DescriptionHelper descriptionHelper = new DescriptionHelper(opExResolver)

// 'binding' is injected by Jenkins JobDSL plugin
def runner = new Runner(binding)
runner.jobs('Projects', [LifeCycle.TEST, LifeCycle.ACC, LifeCycle.XPRPRO]) { jobs, log, configs, settings, lifeCycle ->
    jobs << new CreateProjectJobs(log, configs, settings, lifeCycle, opExResolver, descriptionHelper)
}
runner.jobs('Test Automation Flows', LifeCycle.TEST) { jobs, log, configs, settings, lifeCycle ->
    jobs << new DailyFlowJobCreator(log, configs, settings)
    jobs << new EveningFlowJobCreator(log, configs, settings)
    jobs << new NightlyFlowJobCreator(log, configs, settings)
}
runner.jobs('Sprint Hopping', LifeCycle.TEST) { jobs, log, configs, settings, lifeCycle ->
    def scmUrlsOfProjectsNotInJobDsl = [
            'https://svn.company.tld/group/something-icc',
            'git@git.company.tld:group/something-eabc.git',
            'git@git.company.tld:group/something-sba.git',
            'git@git.company.tld:group/something-dix.git',
    ]
    jobs << new BranchFromLastStableTagFlowJobCreator(log, configs, settings)
            .withScmUrls(scmUrlsOfProjectsNotInJobDsl)
    jobs << new AutoCreateBranchJobCreator()
}
runner.jobDefaults { job ->
    def jdkVersion = '6u45'
    JobPluginConfigHelper.verifyJdkVersion(jdkVersion)

    // TODO: Not sure why yet, but job can be null sometimes..
    if (job) {
        job.with {
            description DescriptionHelper.disclaimer
            jdk "jdk ${jdkVersion}"
            logRotator(-1, 30, -1, 2) // days, num, artifactDays, artifactNum
            label 'slave'

            wrappers {
                timestamps()
                colorizeOutput()
            }
        }
    }
}.run(this)
