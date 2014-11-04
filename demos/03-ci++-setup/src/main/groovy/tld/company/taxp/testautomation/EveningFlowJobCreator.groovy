package tld.company.taxp.testautomation

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.JobType
import tld.company.taxp.JobCreator
import tld.company.taxp.JobDslSetting
import tld.company.taxp.logging.Logger
import tld.company.taxp.model.Project
import tld.company.taxp.model.pipeline.ci.LifeCycle

import static tld.company.taxp.model.flow.DeployGroup.*
import static tld.company.taxp.model.flow.DeploySchedule.evening

class EveningFlowJobCreator implements JobCreator {
    final String name = 'Flow_Deploy_Evening'
    final JobType type = JobType.BuildFlow
    final LifeCycle lifeCycle = LifeCycle.TEST
    private final Logger log
    private final List<Project> configs
    private final List<JobDslSetting> settings

    EveningFlowJobCreator(Logger log, List<Project> configs, List<JobDslSetting> settings) {
        this.log = log
        this.configs = configs
        this.settings = settings
    }

    @Override
    Job create(JobParent parent) {
        def creator = new FlowDeployJobCreator(evening, log, configs, settings)
        creator.job {
            triggers {
                // Sunday to Thursday @ 18:00
                cron('0 18 * * 0-4')
            }

            publishers {
                downstreamParameterized {
                    trigger('Flow_Test_Automation_Legacy', 'UNSTABLE_OR_BETTER', true)
                }
            }
        }
        creator.withFlowDefinition {
            [
                    'Pre-Deploy'       : [
                            [
                                    'build("System_Sync_Rundeck_Jobs")',
                                    'build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "DISABLE")',
                            ]
                    ],
                    (preRelease.title) :
                            [] + [collectFor(preRelease)],
                    (bosDB.title)      :
                            [] + [collectFor(bosDB)],
                    (coreDB.title)     :
                            [] + [collectFor(coreDB)],
                    (backend.title)    : [
                            'build("Proxy_Version_Artifactory", deployJob: "Deploy_CRON_DB")',
                    ] + collectFor(backend),
                    (frontendApp.title):
                            [] + [collectFor(frontendApp)],
                    (service.title)    :
                            [] + [collectFor(service)],
                    (tool.title)       :
                            [] + [collectFor(tool)],
                    'Post-Deploy'      : [
                            'build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "ENABLE")',
                            'build("System_Multi_Jobs", jobSelection: "fitnesseRun" , task: "BUILD")',
                            'build("System_Clean_Deployment_Artifacts_Of_Failed_Builds")',
                    ],
                    'Test Automation'  :
                            [] + [collectTestAutomation()] + 'build("System_Update_Deployment_Status", value: "DONE")'
            ]
        }

        return creator.create(parent)
    }
}
