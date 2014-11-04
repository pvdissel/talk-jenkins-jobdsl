package tld.company.taxp.project

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.helpers.Permissions
import tld.company.taxp.DescriptionHelper
import tld.company.taxp.JobDslSetting
import tld.company.taxp.JobPluginConfigHelper
import tld.company.taxp.OpExResolver
import tld.company.taxp.model.Pipeline
import tld.company.taxp.model.Project
import tld.company.taxp.model.pipeline.ci.LifeCycle

class CiJobHelper extends HelperBase implements JobDecorator {
    static String ARTIFACTORY_LOCAL_URL = 'https://artifactory.dev.company.tld/artifactory'
    static String ARTIFACTORY_LOCAL_NAME = 'artifactory-local'
    static String REPOSITORY_LOCAL_KEY = 'libs-release-local'
    static String SNAPSHOT_REPOSITORY_LOCAL_KEY = 'libs-snapshot-local'

    CiJobHelper(LifeCycle lifeCycle, List<JobDslSetting> settings, List messages, OpExResolver opExResolver, DescriptionHelper descriptionHelper) {
        super(lifeCycle, settings, messages, opExResolver, descriptionHelper)
    }

    def decorateJobFor(Project project, Pipeline pipeline, Job job) {
        if (!pipeline.ci) {
            def msg = "No CI config found for pipeline type [${pipeline.type}]" + (pipeline.id ? " with id [${pipeline.id}]" : '')
            throw new IllegalStateException(msg)
        }
        switch (pipeline.type) {
            case ~/(?i)app/:
                decorateAsAppJob(project, pipeline, job)
                break
            case ~/(?i)api/:
                decorateAsApiJob(project, pipeline, job)
                break
            case ~/(?i)db/:
                decorateAsDbJob(project, pipeline, job)
                break
            case ~/(?i)fix/:
                decorateAsFixtureJob(project, pipeline, job)
                break
            default:
                throw new IllegalStateException("Unknown pipeline type [${pipeline.type}]" + (pipeline.id ? " with id [${pipeline.id}]" : ''))
        }
        configureJobWithDefaults(project, pipeline, job)
    }

    def configureJobWithDefaults(Project project, Pipeline pipeline, Job job) {
        job.with {
            description descriptionHelper.createHeaders(project, pipeline)
            logRotator(-1, 30, -1, 2) // days, num, artifactDays, artifactNum
            label 'slave'
            authorization {
                permission(Permissions.ItemRead, 'anonymous')
            }
            if (settings.contains(JobDslSetting.DISABLED)) {
                disabled true
                messages << 'DISABLED: disabled job'
            }

            wrappers {
                timestamps()
                colorizeOutput()
            }

            triggers {
                scm lifeCycle == LifeCycle.TEST ? '@hourly' : 'H/15 * * * *'
            }
        }
    }

    public String getJobNameForPipeline(Project project, Pipeline pipeline) {
        def sanitizedFullName = project.fullName.replace(" ", "_")
        switch (pipeline.type) {
            case ~/(?i)app/:
                return lifeCycle == LifeCycle.TEST ? createJobName(project, sanitizedFullName, pipeline.id) : createJobName(project, 'App', pipeline.id)
            case ~/(?i)api/:
                return createJobName(project, 'API', pipeline.id)
            case ~/(?i)db/:
                return createJobName(project, 'DB', pipeline.id)
            case ~/(?i)fix/:
                return createJobName(project, 'Fixtures', pipeline.id)
            default:
                throw new IllegalStateException("Unknown pipeline type [${pipeline.type}]" + (pipeline.id ? " with id [${pipeline.id}]" : ''))
        }
    }

    def decorateAsAppJob(Project project, Pipeline pipeline, Job job) {
        if (!pipeline.deploy && lifeCycle != LifeCycle.TEST) {
            messages << "Skip APP as not in [${LifeCycle.TEST}] lifeCycle and no deploy config available"
            return
        }

        def jdkVersion = pipeline.technology.version
        JobPluginConfigHelper.verifyJdkVersion(jdkVersion)
        job.with {
            name getJobNameForPipeline(project, pipeline)
            jdk "jdk ${jdkVersion}"
            scm scmConfigurer(pipeline)

            if (pipeline.tool.type =~ /(?i)gradle/) {
                steps stepsGradleBuilderConfigurer(pipeline)
            } else {
                // This will enable dependency listing for all maven projects - only if they are not configured yet
                pipeline.ci.maven.goals = pipeline.ci.maven.goals.contains("dependency:list") ? pipeline.ci.maven.goals : "${pipeline.ci.maven.goals} dependency:list"
                pipeline.ci.maven.opts = pipeline.ci.maven.opts.contains("-DoutputFile=dependencies.txt") ? pipeline.ci.maven.opts : "${pipeline.ci.maven.opts} -DoutputFile=dependencies.txt".trim()
                configure configureMavenBuilderConfigurer(pipeline)
            }

            publishers {
                if (pipeline.tool.type =~ /(?i)maven/) {
                    archiveArtifacts('**/dependencies.txt')
                }

                if (pipeline.ci.findBugs) {
                    findbugs('**/findbugsXml.xml', false,
                            JobPluginConfigHelper.staticAnalysisConfigurer(pipeline.ci.findBugs)
                    )
                }
                if (pipeline.ci.pmd) {
                    pmd('**/pmd.xml',
                            JobPluginConfigHelper.staticAnalysisConfigurer(pipeline.ci.pmd)
                    )
                }
                if (pipeline.ci.dry) {
                    dry('**/cpd.xml', pipeline.ci.dry.highThreshold, pipeline.ci.dry.normalThreshold,
                            JobPluginConfigHelper.staticAnalysisConfigurer(pipeline.ci.dry)
                    )
                }

                if (pipeline.ci.junit != null && pipeline.ci.junit.enabled) {
                    archiveJunit(pipeline.ci.junit.glob, pipeline.ci.junit.retainLongStdout,
                            pipeline.ci.junit.allowClaimingOfFailedTests, pipeline.ci.junit.publishTestAttachments)
                }

                if (pipeline.ci.tasks) {
                    tasks(pipeline.ci.tasks.pattern, pipeline.ci.tasks.excludePattern,
                            pipeline.ci.tasks.high, pipeline.ci.tasks.normal, pipeline.ci.tasks.low, pipeline.ci.tasks.ignoreCase,
                            JobPluginConfigHelper.staticAnalysisConfigurer(pipeline.ci.tasks)
                    )
                }
                if (pipeline.ci.cobertura) {
                    cobertura(pipeline.ci.cobertura.coberturaReportFile
                            , JobPluginConfigHelper.coberturaConfigurer(pipeline.ci.cobertura)
                    )
                }
                if (pipeline.ci.publishHtmlReport) {
                    publishHtml {
                        report(pipeline.ci.publishHtmlReport.sourceDir, pipeline.ci.publishHtmlReport.reportName,
                                pipeline.ci.publishHtmlReport.indexPages, pipeline.ci.publishHtmlReport.keepAll)
                    }
                }
            }

            if (pipeline.scm.type =~ /(?i)git/) {
                publishers publishersGitTag(project, pipeline)
            } else {
                configure configureSvnTagPublisher(project, pipeline)
            }
            publishers publishersExtendedEmailConfigurer(project)

            if (lifeCycle != LifeCycle.TEST) {
                publishers {
                    downstreamParameterized {
                        trigger("System_Create_JiraIssue_For_Snapshot_Dependencies", "ALWAYS") {
                            def branchProperty = generateBranchProperty()
                            predefinedProps(['sprint': "\${${branchProperty}}", 'jobName': '${JOB_NAME}', 'buildNumber': '${BUILD_NUMBER}'])
                        }
                    }
                }
            }
        }
    }

    def decorateAsApiJob(Project project, Pipeline pipeline, Job job) {
        if (lifeCycle != LifeCycle.TEST) {
            messages << "Skip API as this is not the [${LifeCycle.TEST}] lifeCycle"
            return
        }

        def jdkVersion = pipeline.technology.version
        JobPluginConfigHelper.verifyJdkVersion(jdkVersion)
        job.with {
            name getJobNameForPipeline(project, pipeline)
            jdk "jdk ${jdkVersion}"
            scm scmConfigurer(pipeline)

            if (pipeline.tool.type =~ /(?i)gradle/) {
                steps stepsGradleBuilderConfigurer(pipeline)
            } else {
                configure configureMavenBuilderConfigurer(pipeline)
            }

            publishers {
                if (pipeline.ci.findBugs) {
                    findbugs('**/findbugsXml.xml', false,
                            JobPluginConfigHelper.staticAnalysisConfigurer(pipeline.ci.findBugs)
                    )
                }
                if (pipeline.ci.pmd) {
                    pmd('**/pmd.xml',
                            JobPluginConfigHelper.staticAnalysisConfigurer(pipeline.ci.pmd)
                    )
                }
                if (pipeline.ci.dry) {
                    dry('**/cpd.xml', pipeline.ci.dry.highThreshold, pipeline.ci.dry.normalThreshold,
                            JobPluginConfigHelper.staticAnalysisConfigurer(pipeline.ci.dry)
                    )
                }
                if (pipeline.ci.junit != null && pipeline.ci.junit.enabled) {
                    archiveJunit(pipeline.ci.junit.glob, pipeline.ci.junit.retainLongStdout,
                            pipeline.ci.junit.allowClaimingOfFailedTests, pipeline.ci.junit.publishTestAttachments)
                }

                if (pipeline.ci.tasks) {
                    tasks(pipeline.ci.tasks.pattern, pipeline.ci.tasks.excludePattern,
                            pipeline.ci.tasks.high, pipeline.ci.tasks.normal, pipeline.ci.tasks.low, pipeline.ci.tasks.ignoreCase,
                            JobPluginConfigHelper.staticAnalysisConfigurer(pipeline.ci.tasks)
                    )
                }
                if (pipeline.ci.cobertura) {
                    cobertura(pipeline.ci.cobertura.coberturaReportFile
                            , JobPluginConfigHelper.coberturaConfigurer(pipeline.ci.cobertura)
                    )
                }
            }
            configure configureLocalArtifactoryPublisher(project, pipeline)
            publishers publishersExtendedEmailConfigurer(project)
        }
    }

    def decorateAsDbJob(Project project, Pipeline pipeline, Job job) {
        LifeCycle lifeCycle = lifeCycle
        if (!pipeline.deploy && lifeCycle != LifeCycle.TEST) {
            messages << "Skip APP as not in [${LifeCycle.TEST}] lifeCycle and no deploy config available"
            return
        }

        def jdkVersion = '6u45'
        JobPluginConfigHelper.verifyJdkVersion(jdkVersion)
        job.with {
            name createJobName(project, 'DB', pipeline.id)
            jdk "jdk ${jdkVersion}"
            scm scmConfigurer(pipeline, "oracle")

            parameters {
                stringParam(
                        'run_db',  // param name
                        'run_db.sql' // param default-value
                )
            }

            if (pipeline.ci.locks && lifeCycle == LifeCycle.TEST) {
                configure configureLocks(pipeline.ci.locks as Set)
            }
            if (pipeline.ci.shell) {
                steps {
                    def ciShell = []
                    def ciShellCmd = pipeline.ci.shell
                    if (lifeCycle != LifeCycle.TEST) {
                        def databasePattern = /.* (.*-db\.dev\.company\.tld).*/
                        pipeline.ci.shell.eachLine { line ->
                            def matcher = (line =~ databasePattern)
                            if (!matcher.matches()) {
                                throw new IllegalStateException("The given pipeline.ci.shell line [${line}] does not match with databasePattern [${databasePattern}]")
                            }
                            def database = matcher[0][1]
                            ciShell << "ssh-agent sh \${SCRIPTSHOME}/bin/build_and_deploy_db.sh -d ${database}"
                        }
                        ciShellCmd = ciShell.join('\n')
                    }
                    shell(createShellScriptContent(ciShellCmd))
                }
            }

            if (pipeline.scm.type =~ /(?i)git/) {
                publishers publishersGitTag(project, pipeline)
            } else {
                configure configureSvnTagPublisher(project, pipeline)
            }
            publishers publishersExtendedEmailConfigurer(project)
        }
    }

    def decorateAsFixtureJob(Project project, Pipeline pipeline, Job job) {
        if (lifeCycle != LifeCycle.TEST) {
            messages << "Skip FIXture as this is not the [${LifeCycle.TEST}] lifeCycle"
            return
        }
        def jdkVersion = pipeline.technology.version
        JobPluginConfigHelper.verifyJdkVersion(jdkVersion)
        job.with {
            name createJobName(project, 'Fixtures', pipeline.id)
            jdk "jdk ${jdkVersion}"
            scm scmConfigurer(pipeline)

            if (pipeline.tool.type =~ /(?i)gradle/) {
                steps stepsGradleBuilderConfigurer(pipeline)
            } else {
                configure configureMavenBuilderConfigurer(pipeline)
            }

            configure configureLocalArtifactoryPublisher(project, pipeline)
            publishers publishersExtendedEmailConfigurer(project)
        }
    }

    private String createJobName(Project project, String postfix, String id) {
        def jobName = id ? "${id}_${postfix}" : "${project.shortName.toUpperCase()}_${postfix}"
        switch (lifeCycle) {
            case LifeCycle.ACC:
                jobName = "Branch_Acc_${jobName}"
                break
            case LifeCycle.XPRPRO:
                jobName = "Branch_XprPro_${jobName}"
                break
            case LifeCycle.TEST:
            default:
                jobName = jobName
        }
        return jobName
    }

    private String generateBranchProperty() {
        switch (lifeCycle) {
            case LifeCycle.TEST:
                return 'TST_SPRINT'
            case LifeCycle.ACC:
                return 'ACC_BRANCH'
            case LifeCycle.XPRPRO:
                return 'XPRPRO_BRANCH'
            default:
                throw new IllegalStateException("Unsupported lifeCycle ${lifeCycle}.")
        }
    }

    private String createCiTagName(Project project, Pipeline pipeline) {
        def prefix = pipeline.id ? pipeline.id : project.shortName
        if (pipeline.type =~ /(?i)db/) {
            prefix += '_DB'
        }
        def branchProperty = generateBranchProperty()
        switch (lifeCycle) {
            case LifeCycle.ACC:
                prefix += '_ACC'
                break
            case LifeCycle.XPRPRO:
                prefix += '_XPR'
                break
        }

        def postfix = ''
        switch (pipeline.scm.type) {
            case ~/(?i)svn/:
                postfix = lifeCycle == LifeCycle.TEST ? "\${env[\'BUILD_NUMBER\']}" : "\${env['${branchProperty}']}-\${env[\'BUILD_NUMBER\']}"
                break
            case ~/(?i)git/:
                postfix = lifeCycle == LifeCycle.TEST ? "\${BUILD_NUMBER}" : "\${${branchProperty}}-\${BUILD_NUMBER}"
                break
            default:
                throw new IllegalStateException("Unknown scm type [${pipeline.scm.type}]")
        }
        return "${prefix}-${postfix}"
    }

    private def configureLocks(Set locks) {
        return { node ->
            def locksNode = node / 'buildWrappers' / 'hudson.plugins.locksandlatches.LockWrapper' / 'locks'
            locks.each { lock ->
                locksNode << 'hudson.plugins.locksandlatches.LockWrapper_-LockWaitConfig' {
                    'name'(lock)
                }
            }
        }
    }

    private String createShellScriptContent(String content) {
        if (settings.contains(JobDslSetting.SANDBOX)) {
            messages << 'SANDBOX: skip shell deploy script'
            def newContent = ''
            content = content.eachLine { line ->
                newContent += "# ${line}\n"
            }
        }
        return content
    }

    private def configureLocalArtifactoryPublisher(Project project, Pipeline pipeline) {
        if (settings.contains(JobDslSetting.SANDBOX)) {
            messages << 'SANDBOX: skip artifact publishing'
            return {}
        }

        return { node ->
            def artNode
            if (pipeline.tool.type == "gradle") {
                artNode = node / 'buildWrappers' / 'org.jfrog.hudson.gradle.ArtifactoryGradleConfigurator'
            } else {
                artNode = node / 'buildWrappers' / 'org.jfrog.hudson.maven3.ArtifactoryMaven3Configurator'
            }

            artNode << {
                deployArtifacts(true)
                deployBuildInfo(false)
                includeEnvVars(false)
                if (pipeline.tool.type == "gradle") {
                    deployMaven(true)
                    evenIfUnstable(false)
                    ivyPattern("[organisation]/[module]/ivy-[revision].xml")
                    artifactPattern("[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]")
                }

                details {
                    artifactoryUrl(ARTIFACTORY_LOCAL_URL)
                    artifactoryName(ARTIFACTORY_LOCAL_NAME)
                    repositoryKey(REPOSITORY_LOCAL_KEY)
                    snapshotsRepositoryKey(SNAPSHOT_REPOSITORY_LOCAL_KEY)
                }

                artifactDeploymentPatterns {
                    includePatterns(pipeline.ci.publishedArtifacts.includePattern)
                    excludePatterns(pipeline.ci.publishedArtifacts.excludePattern)
                    deploymentProperties(pipeline.ci.publishedArtifacts.deploymentProperties)
                }
            }
        }
    }

    private def publishersExtendedEmailConfigurer(Project project) {
        if (settings.contains(JobDslSetting.SANDBOX)) {
            messages << 'SANDBOX: skip email sending'
            return {}
        }
        return { node ->
            extendedEmail(null) {
                trigger(triggerName: 'Unstable',
                        sendToDevelopers: true,
                        sendToRecipientList: true,
                )
                trigger(triggerName: 'Failure',
                        sendToDevelopers: true,
                        sendToRecipientList: true,
                )
                trigger(triggerName: 'StillFailing',
                        sendToDevelopers: true,
                        includeCulprits: true,
                )
                trigger(triggerName: 'StillUnstable',
                        sendToDevelopers: true,
                        includeCulprits: true,
                )
                trigger(triggerName: 'Fixed',
                        sendToDevelopers: true,
                )
            }
        }
    }

    private def publishersGitTag(Project project, Pipeline pipeline) {
        if (settings.contains(JobDslSetting.SANDBOX)) {
            messages << 'SANDBOX: skip tag publishing'
            return {}
        }
        return { node ->
            def tagName = createCiTagName(project, pipeline)
            switch (pipeline.scm.type) {
                case ~/(?i)git/:
                    git {
                        pushOnlyIfSuccess()
                        tag('origin', tagName) {
                            message("NOJIRA Tagged by Jenkins Git Publisher plugin. Build: ${tagName}")
                            create()
                        }
                    }
                    break
                default:
                    throw new IllegalStateException("Unknown scm type [${pipeline.scm.type}]")
            }
        }
    }

    private def configureSvnTagPublisher(Project project, Pipeline pipeline) {
        if (settings.contains(JobDslSetting.SANDBOX)) {
            messages << 'SANDBOX: skip tag publishing'
            return {}
        }
        return { node ->
            def tagName = createCiTagName(project, pipeline)
            switch (pipeline.scm.type) {
                case ~/(?i)svn/:
                    node / 'publishers' / 'hudson.plugins.svn__tag.SvnTagPublisher' {
                        tagBaseURL("${pipeline.scm.base}/hudson-tags/${tagName}")
                        tagComment("NOJIRA Tagged by Hudson svn-tag plugin. Build: ${tagName}")
                        tagDeleteComment('NOJIRA Delete old tag by SvnTag Hudson plugin.')
                    }
                    break
                default:
                    throw new IllegalStateException("Unknown scm type [${pipeline.scm.type}]")
            }
        }
    }

    private def configureMavenBuilderConfigurer(Pipeline pipeline) {
        return { node ->
            switch (pipeline.tool.type) {
                case ~/(?i)maven/:
                    JobPluginConfigHelper.verifyMavenVersion(pipeline.tool.version)

                    // Forces a check for updated releases and snapshots on remote repositories
                    pipeline.ci.maven.goals = pipeline.ci.maven.goals.contains("-U") ? pipeline.ci.maven.goals : "${pipeline.ci.maven.goals} -U"

                    node / 'builders' / 'org.jfrog.hudson.maven3.Maven3Builder' {
                        mavenName("Maven ${pipeline.tool.version}")
                        rootPom(pipeline.ci.maven.rootpom)
                        goals(pipeline.ci.maven.goals)
                        mavenOpts(pipeline.ci.maven.opts)
                    }
                    break
                default:
                    throw new IllegalStateException("Unknown tool type [${pipeline.tool.type}]")
            }
        }
    }

    private def stepsGradleBuilderConfigurer(Pipeline pipeline) {
        return { steps ->
            switch (pipeline.tool.type) {
                case ~/(?i)gradle/:
                    gradle(pipeline.ci.gradle.tasks)
                    break
                default:
                    throw new IllegalStateException("Unknown tool type [${pipeline.tool.type}]")
            }
        }
    }

    private def scmConfigurer(Pipeline pipeline, relativeTargetDirName = null) {
        return { scm ->
            switch (pipeline.scm.type) {
                case ~/(?i)svn/:
                    def url = pipeline.scm.base
                    switch (lifeCycle) {
                        case LifeCycle.ACC:
                            url += '/branches/${ACC_BRANCH}'
                            break
                        case LifeCycle.XPRPRO:
                            url += '/branches/${XPRPRO_BRANCH}'
                            break
                        case LifeCycle.TEST:
                        default:
                            url += '/trunk'
                    }
                    svn(url)
                    break
                case ~/(?i)git/:
                    def branchName
                    switch (lifeCycle) {
                        case LifeCycle.ACC:
                            branchName = '${ACC_BRANCH}'
                            break
                        case LifeCycle.XPRPRO:
                            branchName = '${XPRPRO_BRANCH}'
                            break
                        case LifeCycle.TEST:
                        default:
                            branchName = 'master'
                    }
                    //git(pipeline.scm.base, branchName)
                    git {
                        remote {
                            url(pipeline.scm.base)
                        }
                        branch(branchName)
                        if (relativeTargetDirName) {
                            relativeTargetDir(relativeTargetDirName)
                        }
                    }
                    break
                default:
                    throw new IllegalStateException("Unknown scm type [${pipeline.scm.type}]")
            }
        }
    }
}
