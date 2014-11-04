package tld.company.taxp.testautomation

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.JobType
import tld.company.taxp.JobCreator
import tld.company.taxp.JobDslSetting
import tld.company.taxp.logging.Logger
import tld.company.taxp.model.Project

import static tld.company.taxp.model.flow.DeployGroup.*
import static tld.company.taxp.model.flow.DeploySchedule.nightly

class NightlyFlowJobCreator implements JobCreator {
    final String name = 'Flow_Deploy_Nightly'
    final JobType type = JobType.BuildFlow
    private final Logger log
    private final List<Project> configs
    private final List<JobDslSetting> settings

    NightlyFlowJobCreator(Logger log, List<Project> configs, List<JobDslSetting> settings) {
        this.log = log
        this.configs = configs
        this.settings = settings
    }

    @Override
    Job create(JobParent parent) {
        def creator = new FlowDeployJobCreator(nightly, log, configs, settings)
        creator.job {
            triggers {
                // Monday to Friday @ 02:15
                cron('15 2 * * 1-5')
            }

            publishers {
                downstreamParameterized {
                    trigger('Flow_Test_Automation', 'UNSTABLE_OR_BETTER') {
                        predefinedProp('set', 'Nightly')
                    }
                }
            }
        }
        creator.withFlowDefinition {
            [
                    'Pre-Deploy'       : [
                            [
                                    'build("System_Update_Deployment_Status", value: "Initializing...")',
                                    'build("System_Sync_Rundeck_Jobs")',
                                    'build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "DISABLE")',
                            ]
                    ],
                    'Cleanup'          : [
                            [
                                    // Cleanup twice to remove leftovers results from job chains
                                    'build("Flow_CleanUp")',
                                    'build("Flow_CleanUp")',
                            ],
                            'build("System_Update_Deployment_Status", value: "CleanUp")'
                    ],
                    (preRelease.title) : [
                            [
                                    'build("Proxy_Version_Artifactory", deployJob: "Deploy_PRE_DB")',
                            ]
                    ] + [collectFor(preRelease)],
                    (bosDB.title)      :
                            [] + [collectFor(bosDB)],
                    (coreDB.title)     :
                            [] + [collectFor(coreDB)],
                    (backend.title)    :
                            [] + collectFor(backend),
                    (frontendApp.title): [
                            [
                                    'build("Proxy_Version_Artifactory", deployJob: "Deploy_CA_DB")',
                                    'build("Proxy_Version_Artifactory", deployJob: "Deploy_NS_DB")',
                                    'build("Proxy_Version_Artifactory", deployJob: "Deploy_SW_DB")',
                                    'build("Proxy_Version_Artifactory", deployJob: "Deploy_WSP")'
                            ]
                    ] + [collectFor(frontendApp)],
                    (service.title)    :
                            [] + [collectFor(service)],
                    (tool.title)       :
                            [] + [collectFor(tool)],
                    'Restore Monitors' : [
                            [
                                    'build("System_Update_Deployment_Status", value: "DONE")',
                            ]
                    ],
                    'Post-Deploy'      : [
                            [
                                    'build("WSP_Gizmo_Report")',
                                    'build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "BUILD")',
                                    'build("System_Clean_Deployment_Artifacts_Of_Failed_Builds")',
                            ]
                    ],
                    'Test Automation'  :
                            [] + [collectTestAutomation() + 'build("XL_test_Automation", application: "WSP", suiteFilter: "Nightly,NightlyBrowser,NightlyFirefox", browser:"firefox")'],
                    'Done'             : ['build("System_Update_Deployment_Status", value: "DONE")']
            ]
        }

        return creator.create(parent)
    }
}
