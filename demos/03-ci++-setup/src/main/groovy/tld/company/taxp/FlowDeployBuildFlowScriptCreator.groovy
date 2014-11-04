package tld.company.taxp

import tld.company.taxp.model.Project
import tld.company.taxp.model.flow.OrderGroup
import tld.company.taxp.model.flow.TypeGroup
import tld.company.taxp.model.pipeline.ci.LifeCycle
import tld.company.taxp.project.DeployJobHelper

class FlowDeployBuildFlowScriptCreator {
    private final LifeCycle lifeCycle = LifeCycle.TEST
    private final List<JobDslSetting> settings = []
    private final DeployJobHelper deployJobHelper = new DeployJobHelper(lifeCycle, settings, null, null, null)
    private boolean ignoreJobState = false

    def withIgnoreJobState() {
        ignoreJobState = true
        return this
    }

    def createFrom(Map<String, List> flowDef) {
        flowDef.collect { name, items ->
            multiLineTrim(items.unique().findAll { it }.collate(10, true).collect { collatedItems ->
                createSection(name, collatedItems)
            }.findAll { it }.join('\n\n'))
        }.findAll { it }.join('\n\n')
    }

    private String createSection(name, items) {
        if (items.flatten().findAll { it }.empty) {
            return null
        }
        def section = "// Run ${name}\n"
        if (ignoreJobState) {
            section += ignoreJobState {
                items.collect { content ->
                    createSectionParallelContent(content)
                }.findAll { it }.join('\n')
            }
        } else {
            section += items.collect { content ->
                createSectionParallelContent(content)
            }.findAll { it }.join('\n')
        }
        return section
    }

    private def createSectionParallelContent(Collection content) {
        parallel {
            content.collect {
                createSectionContent(it)
            }.unique().findAll().collect { "{${it}}" }.join(',\n')
        }
    }

    private def createSectionParallelContent(String content) {
        createSectionContent(content)
    }

    private def createSectionContent(Collection content) {
        content.collect {
            createSectionContent(it)
        }.unique().findAll().collect { "${it}" }.join('\n')
    }

    private def createSectionContent(String content) {
        content
    }

    private String ignoreJobState(Closure closure) {
        return """\
            ignore(ABORTED) {
                ${closure.call()}
            }""".stripIndent()
    }

    private String parallel(Closure closure) {
        return """\
                parallel (
                    ${closure.call()}
                )""".stripIndent()
    }

    private String multiLineTrim(String text) {
        text.readLines().collect { it.trim() }.join('\n')
    }

    List<String> collectFor(Project project) {
        [
                generateOrderGroups(project).collect {
                    it.typeGroups
                }.flatten()
        ].collect {
            it.jobNames
        }.collect {
            proxyBuildLine(it)
        }.sum()
    }

    List<OrderGroup> generateOrderGroups(Project project) {
        def orderGroups = []
        project.pipelines.each { pipeline ->
            if (!(pipeline.type =~ /(?i)(api|fix)/)) {
                OrderGroup orderGroup = orderGroups.find { it.id == pipeline.group }
                if (!orderGroup) {
                    orderGroup = new OrderGroup()
                    orderGroup.id = pipeline.group
                    orderGroups.add(orderGroup)
                }

                TypeGroup typeGroup = orderGroup.typeGroups.find { it.id == pipeline.type }
                if (!typeGroup) {
                    typeGroup = new TypeGroup()
                    typeGroup.id = pipeline.type
                    orderGroup.typeGroups.add(typeGroup)
                }
                def deployJobName = deployJobHelper.getJobNameForPipeline(project, pipeline)
                typeGroup.jobNames.add(deployJobName)
                typeGroup.jobNames.unique().sort { l, r -> l <=> r }
            }
        }
        orderGroups.sort { orderGroup1, orderGroup2 -> orderGroup1.id <=> orderGroup2.id }
        orderGroups.each {
            it.typeGroups.sort { typeGroup1, typeGroup2 -> typeGroup2.id <=> typeGroup1.id }
        }
        return orderGroups
    }

    private def proxyBuildLine(Collection jobNames) {
        def lines = jobNames.collect {
            if (it instanceof Collection && it.size() == 1) {
                it.first()
            } else {
                it
            }
        }.collect { name ->
            proxyBuildLine(name)
        }
        return lines
    }

    private def proxyBuildLine(def jobName) {
        """build("Proxy_Version_Artifactory", deployJob: "${jobName}")"""
    }
}
