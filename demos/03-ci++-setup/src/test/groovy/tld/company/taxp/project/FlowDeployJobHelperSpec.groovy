package tld.company.taxp.project

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Ignore
import spock.lang.Specification
import tld.company.taxp.AppConfig
import tld.company.taxp.model.Project
import tld.company.taxp.model.pipeline.ci.LifeCycle

@Ignore
class FlowDeployJobHelperSpec extends Specification {
    def log = null
    def settings = []
    def configs
    JobParent parent = Spy(JobParent)
    JobManagement jobManagement = Mock(JobManagement)
    FlowDeployJobHelper helper

    void setup() {
        parent.jm = jobManagement
        configs = loadConfigs()
        assert configs
        helper = new FlowDeployJobHelper(LifeCycle.TEST, settings, [], null, null)
    }

    private Job createJobFor(Project project) {
        parent.job(type: helper.type) {
            name helper.createJobName(project)
        }
    }

    def "Job gets name based on project shortName"() {
        def project = configs.first()
        def job = createJobFor(project)
        when:
        def decoratedJob = helper.decorateJobFor(project, job)

        then:
        decoratedJob.name == "Flow_Deploy_TEST"
    }

    def "Job runs flow deploy without ignoring triggered jobs result state"() {
        def project = configs.first()
        def job = createJobFor(project)
        when:
        def decoratedJob = helper.decorateJobFor(project, job)

        then:
        def jobXml = new XmlSlurper().parseText(decoratedJob.xml)
        jobXml.dsl == expectedProjectBuildFlowScriptWithoutIgnoringTriggeredJobsResultState()
    }

    private String expectedProjectBuildFlowScriptWithoutIgnoringTriggeredJobsResultState() {
        """\
        // Run TEST (Something ABC)
        build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST_DB")
        build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST")""".stripIndent()
    }

    def "Project with XL Test also triggers XL_Test_Automation"() {
        def project = configs.last()
        def job = createJobFor(project)
        when:
        def decoratedJob = helper.decorateJobFor(project, job)

        then:
        def jobXml = new XmlSlurper().parseText(decoratedJob.xml)
        jobXml.dsl == expectedProjectBuildFlowScriptWithXlTest()
    }

    private String expectedProjectBuildFlowScriptWithXlTest() {
        """\
        // Run TEST2 (Something ABC)
        build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST2_DB")
        build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST2")
        build("XL_Test_Automation", application: "TEST2", suiteFilter: "Nightly", browser: "none", branch: "master")""".stripIndent()
    }

    private List loadConfigs() {
        def path = 'src/test/resources/tld/company/taxp/FlowDeployJobHelperSpec'
        return new AppConfig(null, []).readConfigs(new File(path))
    }
}
