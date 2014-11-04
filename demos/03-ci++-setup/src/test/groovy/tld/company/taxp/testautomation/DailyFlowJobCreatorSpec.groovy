package tld.company.taxp.testautomation

import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Ignore
import spock.lang.Specification
import tld.company.taxp.AppConfig

@Ignore
class DailyFlowJobCreatorSpec extends Specification {
    def log = null
    def settings = []
    def configs
    JobParent parent = Spy(JobParent)
    JobManagement jobManagement = Mock(JobManagement)
    def creator

    void setup() {
        parent.jm = jobManagement
        configs = new AppConfig(log, settings).readConfigs(new File(AppConfig.TEST_CONFIGS_PATH))
        assert configs
        creator = new DailyFlowJobCreator(log, configs, settings)
    }

    def "Job name is Flow_Deploy_Daily"() {
        when:
        def job = this.creator.create(parent)

        then:
        job.name == 'Flow_Deploy_Daily'
    }

    def "Job is scheduled at Monday to Friday @ 12:00"() {
        when:
        def job = creator.create(parent)

        then:
        def jobXml = new XmlSlurper().parseText(job.xml)
        jobXml.triggers.'hudson.triggers.TimerTrigger'.spec == '0 12 * * 1-5'
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
            // Run Preparation monitors & RunDeck
            ignore(ABORTED) {
            parallel (
            {build("System_Update_Deployment_Status", value: "Initializing..." )},
            {build("System_Update_Monitors", pageName: "mon-eo-1" , target: "default")},
            {build("System_Update_Monitors", pageName: "mon-entrance" , target: "daily")},
            {build("System_Sync_Rundeck_Jobs")},
            {build("System_Set_Hue_Light", on: "true" , light: "1", brightness:"100",saturation:"255",hue:"18310")}
            )
            }

            // Run PreRelease
            ignore(ABORTED) {
            parallel (
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_PRE_DB")}
            )
            }

            // Run CoreDBs
            ignore(ABORTED) {
            parallel (
            {build("System_Update_Deployment_Status", value: "CoreDBs")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_DABC_DB")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_LBB_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_RGN_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_TOF_DB")}
            )
            }

            // Run FE Apps
            ignore(ABORTED) {
            parallel (
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_CA_DB")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_NS_DB")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_SW_DB")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_WSP")}
            )
            parallel (
            {build("System_Update_Deployment_Status", value: "FE Apps")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_ADE_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_ADE")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_CDD")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_MCC")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_SAA_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_SAA")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_TAA_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_TAA")}
            )
            }

            // Run Services
            ignore(ABORTED) {
            parallel (
            {build("System_Update_Deployment_Status", value: "Services")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_AAA")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_ACD")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_AEF")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_AGH")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_BAA")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_BDE_DB")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_CBB")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_DBA")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_DDA_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_DDA")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_EABC")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_EBC")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_EDA")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_EDF")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_GAA")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_LAA")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_LCC")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_OBB_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_OBB")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_ODD")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_PAA")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_PABC")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_PCC_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_PCC")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_PGG_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_PGG")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_RAA")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_RBA_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_RBA")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_RDD_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_RDD")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_SABC")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_SACB")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_SBB")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_SCA")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_SDB")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_SDC_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_SDC")}
            )
            }

            // Run Tools
            ignore(ABORTED) {
            parallel (
            {build("System_Update_Deployment_Status", value: "Tools")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_PFF_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_PFF")}
            )
            }

            // Run Restore Monitors
            ignore(ABORTED) {
            parallel (
            {build("System_Update_Deployment_Status", value: "DONE")},
            {build("System_Update_Monitors", pageName: "mon-entrance" , target: "default")},
            {build("System_Set_Hue_Light", on: "true" , light: "1", brightness:"100",saturation:"255",hue:"25718")}
            )
            }

            // Run Post-Deploy
            ignore(ABORTED) {
            parallel (
            {build("WSP_Gizmo_Report")},
            {build("System_Clean_Deployment_Artifacts_Of_Failed_Builds")},
            {build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "BUILD")}
            )
            }

            // Run Test Automation
            ignore(ABORTED) {
            parallel (
            {build("System_Update_Deployment_Status", value: "TA")},
            {build("XL_Test_Automation", application: "AAA", suiteFilter: "Nightly", browser: "none", branch: "master")},
            {build("XL_Test_Automation", application: "EDA", suiteFilter: "NightlyIE", browser: "iexplore", branch: "master")},
            {build("XL_Test_Automation", application: "EDF", suiteFilter: "Nightly", browser: "none", branch: "master")},
            {build("XL_Test_Automation", application: "RBA", suiteFilter: "Nightly", browser: "none", branch: "master")},
            {build("XL_Test_Automation", application: "RDD", suiteFilter: "Nightly", browser: "none", branch: "master")},
            {build("XL_test_Automation", application: "WSP", suiteFilter: "Nightly,NightlyBrowser,NightlyFirefox", browser:"firefox")}
            )
            }

            // Run Done
            ignore(ABORTED) {
            build("System_Update_Deployment_Status", value: "DONE")
            }""".stripIndent()
    }
}
