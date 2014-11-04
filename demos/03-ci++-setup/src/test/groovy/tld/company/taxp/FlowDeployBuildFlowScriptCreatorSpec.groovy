package tld.company.taxp

import spock.lang.Ignore
import spock.lang.Specification
import tld.company.taxp.model.flow.OrderGroup
import tld.company.taxp.model.flow.TypeGroup

@Ignore
class FlowDeployBuildFlowScriptCreatorSpec extends Specification {
    def configs
    private creator = new FlowDeployBuildFlowScriptCreator().withIgnoreJobState()

    void setup() {
        configs = loadConfigs()
        assert configs
    }

    def "Defined basic FlowDeploy generates a correct BuildFlow DSL script"() {
        def given = [
                'Pre-Deploy' : [
                        [
                                'build("System_Sync_Rundeck_Jobs")',
                                'build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "DISABLE")',
                        ]
                ],
                'Backend'    : [
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_CRON")',
                ],
                'Service'    : [
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_END")',
                ],
                'Post-Deploy': [
                        'build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "ENABLE")',
                        'build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "BUILD")',
                        'build("System_Clean_Deployment_Artifacts_Of_Failed_Builds")',
                ]
        ]

        def buildFlowScript = creator.createFrom(given)

        expect:
        buildFlowScript == expectedBasicBuildFlowScript()
    }

    private def expectedBasicBuildFlowScript() {
        """\
            // Run Pre-Deploy
            ignore(ABORTED) {
            parallel (
            {build("System_Sync_Rundeck_Jobs")},
            {build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "DISABLE")}
            )
            }

            // Run Backend
            ignore(ABORTED) {
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_CRON")
            }

            // Run Service
            ignore(ABORTED) {
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_END")
            }

            // Run Post-Deploy
            ignore(ABORTED) {
            build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "ENABLE")
            build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "BUILD")
            build("System_Clean_Deployment_Artifacts_Of_Failed_Builds")
            }""".stripIndent()
    }

    def "Defined large FlowDeploy generates a correct BuildFlow DSL script"() {
        def given = [
                'Backend': [
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_1")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_2")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_3")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_4")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_5")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_6")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_7")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_8")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_9")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_10")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_11")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_12")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_13")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_14")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_15")',
                ]
        ]

        def buildFlowScript = creator.createFrom(given)

        expect:
        buildFlowScript == expectedLargeBuildFlowScriptWithCollation()
    }

    private def expectedLargeBuildFlowScriptWithCollation() {
        """\
            // Run Backend
            ignore(ABORTED) {
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_1")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_2")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_3")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_4")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_5")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_6")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_7")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_8")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_9")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_10")
            }

            // Run Backend
            ignore(ABORTED) {
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_11")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_12")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_13")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_14")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_15")
            }""".stripIndent()
    }

    def "Defined FlowDeploy with empty section generates a BuildFlow DSL script without empty section"() {
        def given = [
                'Backend': [
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_1")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_2")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_3")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_4")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_5")',
                ],
                'Service': []
        ]

        def buildFlowScript = creator.createFrom(given)

        expect:
        buildFlowScript == expectedBuildFlowScriptWithoutEmptySections()
    }

    private def expectedBuildFlowScriptWithoutEmptySections() {
        """\
            // Run Backend
            ignore(ABORTED) {
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_1")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_2")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_3")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_4")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_5")
            }""".stripIndent()
    }

    def "Support sequential within a parallel block"() {
        def given = [
                'Backend': [
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_1")',
                        [
                                'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_2")',
                                [
                                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_3")',
                                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_4")',
                                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_5")',
                                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_6")',
                                ],
                                'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_7")',
                        ]
                ],
                'Service': []
        ]

        def buildFlowScript = creator.createFrom(given)

        expect:
        buildFlowScript == expectedBuildFlowScriptWithSequentialWithinParallelBlock()
    }

    private def expectedBuildFlowScriptWithSequentialWithinParallelBlock() {
        """\
            // Run Backend
            ignore(ABORTED) {
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_1")
            parallel (
            {build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_2")},
            {build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_3")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_4")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_5")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_6")},
            {build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_7")}
            )
            }""".stripIndent()
    }

    def "Deplicates are removed"() {
        def given = [
                'Backend': [
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_1")',
                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_1")',
                        [
                                'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_2")',
                                'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_2")',
                                [
                                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_3")',
                                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_3")',
                                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_4")',
                                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_5")',
                                        'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_6")',
                                ],
                                'build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_7")',
                        ]
                ],
                'Service': []
        ]

        def buildFlowScript = creator.createFrom(given)

        expect:
        buildFlowScript == expectedBuildFlowScriptWithoutDuplicated()
    }

    private def expectedBuildFlowScriptWithoutDuplicated() {
        """\
            // Run Backend
            ignore(ABORTED) {
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_1")
            parallel (
            {build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_2")},
            {build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_3")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_4")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_5")
            build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_6")},
            {build("Proxy_Version_Artifactory", deployJob: "Flow_Deploy_7")}
            )
            }""".stripIndent()
    }

    def "OrderGroups keep order of its contains jobs"() {
        def collected = configs.collect {
            creator.generateOrderGroups(it)
        }

        expect:
        collected == [
                [
                        new OrderGroup(id: 0, typeGroups: [
                                new TypeGroup(id: 'DB', jobNames: ['Deploy_TEST_DB']),
                                new TypeGroup(id: 'APP', jobNames: ['Deploy_TEST']),
                        ])
                ],
                [
                        new OrderGroup(id: 0, typeGroups: [
                                new TypeGroup(id: 'DB', jobNames: ['Deploy_TEST2_DB']),
                                new TypeGroup(id: 'APP', jobNames: ['Deploy_TEST2']),
                        ])
                ],
                [
                        new OrderGroup(id: 0, typeGroups: [
                                new TypeGroup(id: 'DB', jobNames: ['Deploy_TEST3_DB']),
                        ]),
                        new OrderGroup(id: 1, typeGroups: [
                                new TypeGroup(id: 'DB', jobNames: [
                                        'Deploy_RGN_DB', 'Deploy_TOF_DB']
                                ),
                        ])
                ],
        ]
    }

    private List loadConfigs() {
        def path = 'src/test/resources/tld/company/taxp/FlowDeployBuildFlowScriptCreatorSpec'
        return new AppConfig(null, []).readConfigs(new File(path))
    }
}
