package tld.company.taxp.sprinthopping

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.JobType
import tld.company.taxp.JobCreator
import tld.company.taxp.JobDslSetting
import tld.company.taxp.logging.Logger
import tld.company.taxp.model.Project

class BranchFromLastStableTagFlowJobCreator implements JobCreator {

    final String name = 'SprintHopping_Create_Branch_From_Last_Stable_Tag'
    final JobType type = JobType.BuildFlow
    private final Logger log
    private final List<Project> configs
    private final List<JobDslSetting> settings
    private List<String> additionalScmUrls = []

    BranchFromLastStableTagFlowJobCreator(Logger log, List<Project> configs, List<JobDslSetting> settings) {
        this.log = log
        this.configs = configs
        this.settings = settings
    }

    BranchFromLastStableTagFlowJobCreator withScmUrls(List<String> additionalScmUrls) {
        this.additionalScmUrls += additionalScmUrls
        return this
    }

    @Override
    Job create(JobParent parent) {
        return parent.job(type: type) {
            name this.name

            buildFlow(createBuildFlowScriptFor(configs, additionalScmUrls))

            // Add emailPublisher
            publishers publishersExtendedEmailConfigurer()
        }
    }

    private String createBuildFlowScriptFor(List<Project> projects, List<String> additionalScmUrls) {
        def flowScript = ''
        flowScript += '''\
            version = build.properties["environment"]["TST_SPRINT"].tokenize(".").get(0)
            out.println "version: " + version

            jobName = "SprintHopping_Auto_Create_Branch"
            out.println "jobName: " + jobName

            // Branch Fitnesse
            ignore(ABORTED) {
                build( "SprintHopping_Create_FitNesse_Branch", BASE_URL: "https://svn.company.tld/group/testautomation/fitnesse", TARGET_BRANCH: version+".x" )
            }
            '''.stripIndent()

        List<String> scmUrls = (additionalScmUrls + getScmUrlsFrom(projects)).unique()
        List buildFlowLines = createBuildFlowLinesFrom(scmUrls)

        buildFlowLines.collate(10, true).each { linesSection ->
            flowScript += withParallelBuildFlow {
                linesSection.join(',\n')
            }
        }
        return flowScript
    }

    private ArrayList createBuildFlowLinesFrom(List<String> scmUrls) {
        def buildFlowLines = []
        scmUrls.each { url ->
            def line = [
                    '       {',
                    'build(',
                    'jobName,',
                    """BASE_URL: "${url}",""",
                    'PATTERN: "-" + version + ".",',
                    'BRANCH_NAME: version + ".x"',
                    ')',
                    '}',
            ]

            buildFlowLines << line.join(' ')
        }
        return buildFlowLines
    }

    private String withParallelBuildFlow(Closure closure) {
        def part = """
ignore(ABORTED) {
    parallel (
${closure.call()}
    )
}
""".stripIndent()
        return part
    }

    private List<String> getScmUrlsFrom(List<Project> projects) {
        return projects.collect { it.pipelines }.flatten().collect { it.scm.base }
    }

    private def publishersExtendedEmailConfigurer() {
        if (settings.contains(JobDslSetting.SANDBOX)) {
            log.note 'SANDBOX: skip email sending'
            return {}
        }
        def recipients = 'dpi@company.tld,teams@company.tld'
        def subjectTpl = 'SprintHopping: Auto Create Branch Completed'
        def contentTpl = '''\
            Dear Teams,

            We would like to inform you that we've completed our branch creation process.
            Please note that branches are based on the last deployed TST release.

            Kind regards,
            DPI
            '''.stripIndent()
        return { node ->
            extendedEmail(recipients, subjectTpl, contentTpl) {
                trigger(triggerName: 'Success',
                        sendToRecipientList: true,
                )
            }
        }
    }
}
