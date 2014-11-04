package tld.company.taxp.testautomation

import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Ignore
import spock.lang.Specification
import tld.company.taxp.AppConfig

@Ignore
class EveningFlowJobCreatorSpec extends Specification {
    def log = null
    def settings = []
    def configs
    JobParent parent = Spy(JobParent)
    JobManagement jobManagement = Mock(JobManagement)
    def creator

    void setup() {
        configs = new AppConfig(log, settings).readConfigs(new File(AppConfig.TEST_CONFIGS_PATH))
        assert configs
        parent.jm = jobManagement
        creator = new EveningFlowJobCreator(log, configs, settings)
    }

    def "Job name is Flow_Deploy_Evening"() {
        when:
        def job = this.creator.create(parent)

        then:
        job.name == 'Flow_Deploy_Evening'
    }

    def "Job is scheduled at Sunday to Thursday @ 18:00"() {
        when:
        def job = creator.create(parent)

        then:
        def jobXml = new XmlSlurper().parseText(job.xml)
        jobXml.triggers.'hudson.triggers.TimerTrigger'.spec == '0 18 * * 0-4'
    }

    def "Job runs flow deploy"() {
        when:
        def job = creator.create(parent)

        then:
        def jobXml = new XmlSlurper().parseText(job.xml)
        jobXml.dsl == expectedFlowDeployBuildFlowDslScript()
    }

    private String expectedFlowDeployBuildFlowDslScript() {
        """\
            // Run Pre-Deploy
            ignore(ABORTED) {
            parallel (
            {build("System_Sync_Rundeck_Jobs")},
            {build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "DISABLE")}
            )
            }

            // Run Backends
            ignore(ABORTED) {
            build("Proxy_Version_Artifactory", deployJob: "Deploy_CRON_DB")
            }

            // Run Services
            ignore(ABORTED) {
            parallel (
            {build("System_Update_Deployment_Status", value: "Services")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_ECB_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_ECB")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_ECD_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_ECD")}
            )
            }

            // Run Post-Deploy
            ignore(ABORTED) {
            build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "ENABLE")
            build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "BUILD")
            build("System_Clean_Deployment_Artifacts_Of_Failed_Builds")
            }

            // Run Test Automation
            ignore(ABORTED) {
            build("System_Update_Deployment_Status", value: "DONE")
            }""".stripIndent()
    }
}
