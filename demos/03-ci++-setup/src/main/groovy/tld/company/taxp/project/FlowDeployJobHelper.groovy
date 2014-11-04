package tld.company.taxp.project

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobType
import javaposse.jobdsl.dsl.helpers.Permissions
import tld.company.taxp.DescriptionHelper
import tld.company.taxp.FlowDeployBuildFlowScriptCreator
import tld.company.taxp.JobDslSetting
import tld.company.taxp.OpExResolver
import tld.company.taxp.model.Pipeline
import tld.company.taxp.model.Project
import tld.company.taxp.model.flow.OrderGroup
import tld.company.taxp.model.pipeline.ci.LifeCycle

class FlowDeployJobHelper extends HelperBase implements JobDecorator {
    final static JobType type = JobType.BuildFlow
    private final buildFlowScriptCreator = new FlowDeployBuildFlowScriptCreator()

    FlowDeployJobHelper(LifeCycle lifeCycle, List<JobDslSetting> settings, List messages, OpExResolver opExResolver, DescriptionHelper descriptionHelper) {
        super(lifeCycle, settings, messages, opExResolver, descriptionHelper)
    }

    def decorateJobFor(Project project, Job job) {
        decorateJobFor(project, null, job)
    }

    def decorateJobFor(Project project, Pipeline pipeline, Job job) {
        job.with {
            description generateDescription(project, buildFlowScriptCreator.generateOrderGroups(project))
            logRotator(-1, 30, -1, -1) // days, num, artifactDays, artifactNum
            authorization {
                permission(Permissions.ItemRead, 'anonymous')
            }
            if (settings.contains(JobDslSetting.DISABLED)) {
                disabled true
                messages << 'DISABLED: disabled job'
            }
            label 'slave'
            buildFlow(createBuildFlowScript(project))
        }
        return job
    }

    private String createBuildFlowScript(Project project) {
        def additionalJobs = []
        if (project.hasXlTestJob()) {
            additionalJobs += createTestAutomationBuildLine(project)
        }
        def sectionName = "${project.shortName} (${project.fullName})"
        return buildFlowScriptCreator.createFrom(
                [
                        (sectionName): [] + buildFlowScriptCreator.collectFor(project) + additionalJobs
                ]
        )
    }

    String createTestAutomationBuildLine(Project project) {
        def defaultBranch = (project.xlTest.scm.type =~ /(?i)svn/ ? "trunk" : "master")
        return """build("XL_Test_Automation", application: "${project.shortName}", suiteFilter: "${
            project.xlTest.suiteFilter
        }", browser: "${project.xlTest.browser}", branch: "${defaultBranch}")"""
    }

    String getJobNameForPipeline(Project project, Pipeline pipeline) {
        createJobName(project)
    }

    private def generateDescription(Project project, List<OrderGroup> orderGroups) {

        def orderGroupsHtml = generateOrderGroupsHtml(orderGroups)
        return """
            <p>This is a Flow Deploy job. A Flow Job contains the triggers required to fully Deploy a project to the TST environment.
            Therefore a Flow Deploy job can trigger multiple deployments both sequential and in parallel.</p>
            <p>Flow Jobs are smarter then Deploy jobs; if there is are no changes to the project, nothing will be deployed.
            If for some reason a deployment fails, a JIRA ticket will be automatically created. This JIRA ticket will also contain the
            failure-cases that have been detected by the Deploy Job. If your Flow Job has failed and you want to fix the problem, always
            check <a href="https://company-jira/issues/?filter=14748">JIRA</a>.</p>
            <p>The right to start Flow jobs are reserved to the OpEx team. Only that team fully understands the impact of a Deployment to the TST environment. You can find out which team has OpEx over this application on <a href="https://company-wiki/path/to/page/OpEx-overview">Wiki</a>.</p>
            <h2>Project</h2>
            <ul>
                <li><b>ShortName:</b> ${project.shortName}</li>
                <li><b>FullName:</b> ${project.fullName.replaceAll('_', ' ')}</li>
            </ul>
            ${orderGroupsHtml}
            ${DescriptionHelper.disclaimer}
            """.stripIndent()
    }

    private def generateOrderGroupsHtml(List<OrderGroup> orderGroups) {
        def html = """
            <h2>Deploy Groups</h2>
            <p>Deploy Groups are the groups in which the deploy jobs will be triggered.
            Each group will be run in parallel. Depending on the success of the first group, the second group will be triggered.</p>
            <ul>
            """.stripIndent()

        orderGroups.each { orderGroup ->
            html += "<li> Group ${orderGroup.id} <ul>"
            orderGroup.typeGroups.each { typeGroup ->
                typeGroup.jobNames.each { jobName ->
                    html += "<li>${jobName} </li>"
                }
            }
            html += "</ul></li>"
        }
        html += "</ul>"
        return html
    }

    static String createJobName(Project project) {
        "Flow_Deploy_${project.shortName}"
    }
}
