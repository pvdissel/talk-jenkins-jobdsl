package tld.company.taxp.testautomation

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.JobType
import tld.company.taxp.FlowDeployBuildFlowScriptCreator
import tld.company.taxp.JobCreator
import tld.company.taxp.JobDslSetting
import tld.company.taxp.logging.Logger
import tld.company.taxp.model.Project
import tld.company.taxp.model.flow.DeployGroup
import tld.company.taxp.model.flow.DeploySchedule
import tld.company.taxp.model.pipeline.ci.LifeCycle
import tld.company.taxp.project.FlowDeployJobHelper

class FlowDeployJobCreator implements JobCreator, FlowDeployDsl {
    final JobType type = JobType.BuildFlow
    final LifeCycle lifeCycle = LifeCycle.TEST
    private final Logger log
    private final List<Project> configs
    private final List<JobDslSetting> settings
    private final DeploySchedule deploySchedule
    private def flowDefinition = [:]
    private Closure jobDefinition = {}
    private FlowDeployBuildFlowScriptCreator buildFlowScriptCreator = new FlowDeployBuildFlowScriptCreator()

    FlowDeployJobCreator(DeploySchedule deploySchedule, Logger log, List<Project> configs, List<JobDslSetting> settings) {
        this.log = log
        this.configs = configs
        this.settings = settings
        this.deploySchedule = deploySchedule
    }

    @Override
    String getName() {
        "Flow_Deploy_${deploySchedule.name().capitalize()}"
    }

    @Override
    Job create(JobParent parent) {
        def job = parent.job(type: type) {
            name this.name
        }
        jobDefinition.delegate = job
        jobDefinition.resolveStrategy = Closure.DELEGATE_FIRST
        job.with jobDefinition
        job.with {
            name this.name
            buildFlow(buildFlowScriptCreator.withIgnoreJobState().createFrom(flowDefinition))
        }
        return job
    }

//  Groovy 2.1+ arg syntax: @DelegatesTo(JobParent) Closure definition
    def job(Closure definition) {
        this.jobDefinition = definition
    }

//  Groovy 2.1+ arg syntax: @DelegatesTo(FlowDeployDsl) Closure definition
    FlowDeployJobCreator withFlowDefinition(Closure definition) {
        definition.delegate = this
        definition.resolveStrategy = Closure.DELEGATE_FIRST
        flowDefinition = definition.call()
        return this
    }

    @Override
    List<String> collectFor(DeployGroup group) {
        def lines = configs.findAll {
            it.flow.enabled == true
        }.findAll {
            it.flow.deploy.contains(deploySchedule)
        }.findAll {
            it.flow.deployGroup == group
        }.collect { Project project ->
            buildFlowScriptCreator.collectFor(project)
        }
        if (lines) {
            return [
                    """build("System_Update_Deployment_Status", value: "${group.title}")"""
            ] + lines
        } else {
            return lines
        }
    }

    List<String> collectTestAutomation() {
        def helper = new FlowDeployJobHelper(lifeCycle, settings, [], null, null)
        def lines = configs.findAll {
            it.flow.enabled == true
        }.findAll {
            it.flow.deploy.contains(deploySchedule)
        }.findAll {
            it.hasXlTestJob()
        }.collect { Project project ->
            [helper.createTestAutomationBuildLine(project)]
        }
        if (lines) {
            return [
                    """build("System_Update_Deployment_Status", value: "TA")"""
            ] + lines
        } else {
            return lines
        }
    }
}
