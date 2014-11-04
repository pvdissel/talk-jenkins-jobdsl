package tld.company.taxp.project

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.JobType
import tld.company.taxp.*
import tld.company.taxp.logging.Logger
import tld.company.taxp.model.Pipeline
import tld.company.taxp.model.Project
import tld.company.taxp.model.pipeline.ci.LifeCycle
import tld.company.taxp.view.AppViewsCreator
import tld.company.taxp.view.OpsViewsCreator
import tld.company.taxp.view.TeamViewsCreator

class CreateProjectJobs implements JobCreator {
    // TODO: Make name/type more generic somehow?
    final String name = 'CreateProjectJobs'
    final JobType type = JobType.BuildFlow

    private final Logger log
    private final List<Project> configs
    private final List<JobDslSetting> settings
    private final LifeCycle lifeCycle
    private JobParent parent
    private formatter
    private OpExResolver opExResolver
    private DescriptionHelper descriptionHelper

    CreateProjectJobs(Logger log, List<Project> configs, List<JobDslSetting> settings, LifeCycle lifeCycle, OpExResolver opExResolver, DescriptionHelper descriptionHelper) {
        this.log = log
        this.configs = configs
        this.settings = settings
        this.lifeCycle = lifeCycle
        this.opExResolver = opExResolver
        this.descriptionHelper = descriptionHelper
        formatter = new StatusLineFormatter(settings)
    }

    @Override
    Job create(JobParent parent) {
        this.parent = parent
        def successful = this.generatePipelinesForLifecycle(lifeCycle, configs)
        if (!successful) {
            throw new RuntimeException('Error generating jobs, see console output for details')
        }
        if (lifeCycle == LifeCycle.TEST) {
            new AppViewsCreator(configs).create(parent)
            new TeamViewsCreator(configs, new StaticDataOpExResolver()).create(parent)
            new OpsViewsCreator().create(parent)
        }
        // TODO: Make generic somehow?
        return null
    }

    def generatePipelinesForLifecycle(lifeCycle, List<Project> projects) {
        log.note "Starting groovy DSL for [${lifeCycle}] lifeCycle..."
        def results = []
        projects.each { project ->
            log.info "Generating Pipelines for Project [${project.shortName}]"
            project.pipelines.each { pipeline ->
                results += createCiPipeline(project, pipeline, lifeCycle)
                results += createDeployPipeline(project, pipeline, lifeCycle)
            }
            results += createXlTest(project, lifeCycle)
            results += createFlow(project, lifeCycle)
        }

        results.findAll { !it }.empty
    }

    private List getSupportedMatrixJobTypes() {
        return [
                'APP',
                'API',
        ]
    }

    private def createXlTest(Project project, lifeCycle) {
        if (lifeCycle != LifeCycle.TEST) {
            log.info formatter.asSuccessStatusLine("Flows are only valid for the TEST LifeCycle")
            return []
        }
        if (!project.hasXlTestJob()) {
            log.info formatter.asSuccessStatusLine("This project has no XL Test configuration. Skipping")
            return []
        }

        def statusLine = "Creating XL Test job for project [${project.shortName}]"
        def results = []
        Job newJob = parent.job(type: JobType.Freeform) { name XlTestJobBuilder.createJobName(project) }
        results << formatter.executeWithStatusLine(log.&info, statusLine) { messages ->
            new XlTestJobBuilder(newJob).createJobFor(project.xlTest)
        }
        results
    }

    private createFlow(Project project, lifeCycle) {
        if (lifeCycle != LifeCycle.TEST) {
            log.info formatter.asSuccessStatusLine("Flows are only valid for the TEST LifeCycle")
            return []
        }
        if (!project.flow.enabled) {
            log.info formatter.asSuccessStatusLine("This project has disabled the generation of the Flow job. Skipping")
            return []
        }
        log.info "Flowing in [${project.flow.deploy}]"
        boolean hasDbOrApp = project.pipelines.any { it.type =~ /(?i)(db|app)/ }
        if (!hasDbOrApp) {
            log.info formatter.asSuccessStatusLine("This project has no db or app pipelines. Skipping")
            return []
        }
        if (!project.hasDeployJob()) {
            log.info formatter.asSuccessStatusLine("This project has no deploy pipeline segments in any of the pipelines. Skipping")
            return []
        }

        Job theJob = parent.job(type: FlowDeployJobHelper.type) {
            name FlowDeployJobHelper.createJobName(project)
        }
        def statusLine = "Creating Flow Deploy job for project [${project.shortName}]"
        def results = []
        results << formatter.executeWithStatusLine(log.&info, statusLine) { messages ->
            FlowDeployJobHelper flowDeployJobHelper = new FlowDeployJobHelper(lifeCycle, settings, messages, opExResolver, descriptionHelper)
            flowDeployJobHelper.decorateJobFor(project, theJob)
        }
        return results
    }

    private def createCiPipeline(Project project, Pipeline pipeline, LifeCycle lifeCycle) {
        def results = []
        Job theJob = determineCiJob()
        def statusLine = "Creating CI pipeline for type [${pipeline.type}]" + (pipeline.id ? " with id [${pipeline.id}]" : '')
        results << formatter.executeWithStatusLine(log.&info, statusLine) { messages ->
            new CiJobHelper(lifeCycle, settings, messages, opExResolver, descriptionHelper).decorateJobFor(project, pipeline, theJob)
        }

        if (theJob.type == JobType.Matrix) {
            statusLine = "Upgrade CI pipeline for type [${pipeline.type}]" + (pipeline.id ? " with id [${pipeline.id}]" : '') + " to Matrix-style"
            results << formatter.executeWithStatusLine(log.&info, statusLine) { messages ->
                new MatrixCiJobHelper(lifeCycle, settings, messages, opExResolver, descriptionHelper).decorateJobFor(project, pipeline, theJob)
            }
        }
        return results
    }

    private def determineCiJob() {
        if (settings.contains(JobDslSetting.MATRIX)
                && this.getSupportedMatrixJobTypes().find { it ==~ /(?i)pipeline.type/ }
        ) {
            return parent.job(type: JobType.Matrix) { name 'unknown' }
        } else {
            return parent.job(type: JobType.Freeform) { name 'unknown' }
        }
    }

    private def createDeployPipeline(Project project, Pipeline pipeline, LifeCycle lifeCycle) {
        def results = []
        Job theJob = parent.job(type: JobType.Freeform) { name 'unknown' }
        if (pipeline.type =~ /(?i)(api|fix)/) {
            log.info formatter.asSuccessStatusLine("No deploy pipeline support for [${pipeline.type}] pipeline type")
        } else {
            def statusLine = "Creating Deploy pipeline for type [${pipeline.type}]" + (pipeline.id ? " with id [${pipeline.id}]" : '')
            results << formatter.executeWithStatusLine(log.&info, statusLine) { messages ->
                DeployJobHelper deployJobHelper = new DeployJobHelper(lifeCycle, settings, messages, opExResolver, descriptionHelper)
                deployJobHelper.decorateJobFor(project, pipeline, theJob)
            }
        }
        return results
    }
}
