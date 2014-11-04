package tld.company.taxp.testautomation

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.JobType
import tld.company.taxp.JobCreator
import tld.company.taxp.JobDslSetting
import tld.company.taxp.logging.Logger
import tld.company.taxp.model.Project

import static tld.company.taxp.model.flow.DeployGroup.*
import static tld.company.taxp.model.flow.DeploySchedule.daily

class DailyFlowJobCreator implements JobCreator {
    final String name = 'Flow_Deploy_Daily'
    final JobType type = JobType.BuildFlow
    private final Logger log
    private final List<Project> configs
    private final List<JobDslSetting> settings

    DailyFlowJobCreator(Logger log, List<Project> configs, List<JobDslSetting> settings) {
        this.log = log
        this.configs = configs
        this.settings = settings
    }

    @Override
    Job create(JobParent parent) {
        def creator = new FlowDeployJobCreator(daily, log, configs, settings)
        creator.job {
            triggers {
                // Monday to Friday @ 12:00
                cron('0 12 * * 1-5')
            }

            publishers {
                downstreamParameterized {
                    trigger('Flow_Test_Automation', 'UNSTABLE_OR_BETTER') {
                        predefinedProp('set', 'Daily')
                    }
                }
            }
        }
        creator.withFlowDefinition {
            [
                    'Preparation monitors & RunDeck': [
                            [
                                    'build("System_Update_Deployment_Status", value: "Initializing..." )',
                                    'build("System_Update_Monitors", pageName: "mon-eo-1" , target: "default")',
                                    'build("System_Update_Monitors", pageName: "mon-entrance" , target: "daily")',
                                    'build("System_Sync_Rundeck_Jobs")',
                                    'build("System_Set_Hue_Light", on: "true" , light: "1", brightness:"100",saturation:"255",hue:"18310")',
                            ]
                    ],
                    (preRelease.title)              : [
                            [
                                    'build("Proxy_Version_Artifactory", deployJob: "Deploy_PRE_DB")',
                            ]
                    ] + [collectFor(preRelease)],
                    (bosDB.title)                   :
                            [] + [collectFor(bosDB)],
                    (coreDB.title)                  :
                            [] + [collectFor(coreDB)],
                    (backend.title)                 :
                            [] + collectFor(backend),
                    (frontendApp.title)             : [
                            [
                                    'build("Proxy_Version_Artifactory", deployJob: "Deploy_CA_DB")',
                                    'build("Proxy_Version_Artifactory", deployJob: "Deploy_NS_DB")',
                                    'build("Proxy_Version_Artifactory", deployJob: "Deploy_SW_DB")',
                                    'build("Proxy_Version_Artifactory", deployJob: "Deploy_WSP")'
                            ]
                    ] + [collectFor(frontendApp)],
                    (service.title)                 :
                            [] + [collectFor(service)],
                    (tool.title)                    :
                            [] + [collectFor(tool)],
                    'Restore Monitors'              : [
                            [
                                    'build("System_Update_Deployment_Status", value: "DONE")',
                                    'build("System_Update_Monitors", pageName: "mon-entrance" , target: "default")',
                                    'build("System_Set_Hue_Light", on: "true" , light: "1", brightness:"100",saturation:"255",hue:"25718")',
                            ]
                    ],
                    'Post-Deploy'                   : [
                            [
                                    'build("WSP_Gizmo_Report")',
                                    'build("System_Clean_Deployment_Artifacts_Of_Failed_Builds")',
                                    'build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "BUILD")',
                            ]
                    ],
                    'Test Automation'               :
                            [] + [collectTestAutomation() + ['build("XL_test_Automation", application: "WSP", suiteFilter: "Nightly,NightlyBrowser,NightlyFirefox", browser:"firefox")']],
                    'Done'                          : ['build("System_Update_Deployment_Status", value: "DONE")']
            ]
        }

        return creator.create(parent)
    }
}
