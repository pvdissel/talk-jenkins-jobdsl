package tld.company.taxp.testautomation

import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.JobType
import spock.lang.Ignore
import spock.lang.Specification
import tld.company.taxp.AppConfig
import tld.company.taxp.model.flow.DeployGroup
import tld.company.taxp.model.flow.DeploySchedule

import static DeployGroup.backend
import static DeployGroup.service

@Ignore
class FlowDeployJobCreatorSpec extends Specification {
    def log = null
    def settings = null
    def configs
    JobParent parent = Spy(JobParent)
    JobManagement jobManagement = Mock(JobManagement)

    void setup() {
        parent.jm = jobManagement
        configs = loadConfigs()
        assert configs
    }

    def "Can create a BuildFlowDSL script for the Flow Deploy"() {
        when:
        def creator = new FlowDeployJobCreator(DeploySchedule.daily, log, configs, settings)
        creator.withFlowDefinition {
            [
                    'Preparation monitors & RunDeck': [
                            'build("System_Update_Deployment_Status", value: "Initializing...")',
                            [
                                    'build("System_Update_Monitors", pageName: "mon-eo-1" , target: "default")',
                                    'build("System_Update_Monitors", pageName: "mon-entrance" , target: "daily")',
                                    'build("System_Sync_Rundeck_Jobs")',
                                    'build("System_Set_Hue_Light", on: "true" , light: "1", brightness:"100",saturation:"255",hue:"18310")'
                            ]
                    ],
                    (backend.title)                 : [
                            'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_CRON")',
                    ] + collectFor(backend),
                    (service.title)                 :
                            [] + [collectFor(service)],
            ]
        }
        def job = creator.create(parent)

        then:
        job.name == 'Flow_Deploy_Daily'
        job.type == JobType.BuildFlow
        job.buildFlowAdded
        def jobXml = new XmlSlurper().parseText(job.xml)
        jobXml.dsl as String == expectedFlowDeployBuildFlowDslScript()
    }

    private String expectedFlowDeployBuildFlowDslScript() {
        """\
            // Run Preparation monitors & RunDeck
            ignore(ABORTED) {
            build("System_Update_Deployment_Status", value: "Initializing...")
            parallel (
            {build("System_Update_Monitors", pageName: "mon-eo-1" , target: "default")},
            {build("System_Update_Monitors", pageName: "mon-entrance" , target: "daily")},
            {build("System_Sync_Rundeck_Jobs")},
            {build("System_Set_Hue_Light", on: "true" , light: "1", brightness:"100",saturation:"255",hue:"18310")}
            )
            }

            // Run Backends
            ignore(ABORTED) {
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_CRON")
            }

            // Run Services
            ignore(ABORTED) {
            parallel (
            {build("System_Update_Deployment_Status", value: "Services")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST2_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST2")},
            {build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST3_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_RGN_DB")
            build("Proxy_Version_Artifactory", deployJob: "Deploy_TOF_DB")}
            )
            }""".stripIndent()
    }

    def "Can decorate the Flow Deploy job"() {
        when:
        def creator = new FlowDeployJobCreator(DeploySchedule.daily, log, configs, settings)
        creator.job {
            triggers {
                // Monday to Friday @ 12:00
                cron('0 12 * * 1-5')
            }
        }
        def job = creator.create(parent)

        then:
        job.name == 'Flow_Deploy_Daily'
        job.type == JobType.BuildFlow
        job.buildFlowAdded
        def jobXml = new XmlSlurper().parseText(job.xml)
        jobXml.dsl
        jobXml.triggers.'hudson.triggers.TimerTrigger'.spec == '0 12 * * 1-5'
    }

    def "Exception is thrown when the buildFlow method is used in the job definition"() {
        when:
        def creator = new FlowDeployJobCreator(DeploySchedule.daily, log, configs, settings)
        creator.job {
            buildFlow("lala")
        }
        creator.create(parent)

        then:
        thrown IllegalStateException
    }

    def "Name set in the job definition is overridden"() {
        when:
        def creator = new FlowDeployJobCreator(DeploySchedule.daily, log, configs, settings)
        creator.job {
            name 'I get overridden!'
        }
        def job = creator.create(parent)

        then:
        job.name == 'Flow_Deploy_Daily'
    }

    def "Name is based on DeploySchedule"() {
        def creator
        def job

        when:
        creator = new FlowDeployJobCreator(DeploySchedule.daily, log, configs, settings)
        job = creator.create(parent)

        then:
        job.name == 'Flow_Deploy_Daily'

        when:
        creator = new FlowDeployJobCreator(DeploySchedule.evening, log, configs, settings)
        job = creator.create(parent)

        then:
        job.name == 'Flow_Deploy_Evening'

        when:
        creator = new FlowDeployJobCreator(DeploySchedule.nightly, log, configs, settings)
        job = creator.create(parent)

        then:
        job.name == 'Flow_Deploy_Nightly'
    }

    def "Build lines of a singe typeGroup are placed in a single list"() {
        def creator = new FlowDeployJobCreator(DeploySchedule.daily, log, configs, settings)

        when:
        def collected = creator.collectFor(DeployGroup.service)

        then:
        collected == [
                'build("System_Update_Deployment_Status", value: "Services")',
                [
                        'build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST_DB")',
                        'build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST")',
                ],
                [
                        'build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST2_DB")',
                        'build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST2")',
                ],
                [
                        'build("Proxy_Version_Artifactory", deployJob: "Deploy_TEST3_DB")',
                        [
                                'build("Proxy_Version_Artifactory", deployJob: "Deploy_RGN_DB")',
                                'build("Proxy_Version_Artifactory", deployJob: "Deploy_TOF_DB")',
                        ]
                ],
        ]
    }

    private List loadConfigs() {
        def path = 'src/test/resources/tld/company/taxp/testautomation/FlowDeployJobCreatorSpec'
        return new AppConfig(null, []).readConfigs(new File(path))
    }
}
