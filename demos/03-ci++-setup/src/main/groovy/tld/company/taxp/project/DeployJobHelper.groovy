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

class DeployJobHelper extends HelperBase implements JobDecorator {
    static String ARTIFACTORY_DC_URL = 'https://artifactory.company.tld/artifactory'
    static String ARTIFACTORY_DC_NAME = '-890069362@1372937695171'
    static String REPOSITORY_DC_KEY = 'company-deploy-local'
    static String SNAPSHOT_REPOSITORY_DC_KEY = 'company-deploy-local'

    DeployJobHelper(LifeCycle lifeCycle, List<JobDslSetting> settings, List messages, OpExResolver opExResolver, DescriptionHelper descriptionHelper) {
        super(lifeCycle, settings, messages, opExResolver, descriptionHelper)
    }

    def decorateJobFor(Project project, Pipeline pipeline, Job job) {
        if (!pipeline.deploy) {
            def msg = "No Deploy config found for pipeline type [${pipeline.type}]" + (pipeline.id ? " with id [${pipeline.id}]" : '')
            throw new IllegalStateException(msg)
        }
        switch (pipeline.type) {
            case ~/(?i)app/:
                decorateAsAppJob(project, pipeline, job)
                break
            case ~/(?i)db/:
                decorateAsDbJob(project, pipeline, job)
                break
            case ~/(?i)api/:
            case ~/(?i)fix/:
                throw new IllegalArgumentException("No deploy pipeline support for pipeline type [${pipeline.type}]" + (pipeline.id ? " with id [${pipeline.id}]" : ''))
            default:
                throw new IllegalStateException("Unknown pipeline type [${pipeline.type}]")
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
        }
    }

    @Override
    public String getJobNameForPipeline(Project project, Pipeline pipeline) {
        switch (pipeline.type) {
            case ~/(?i)app/:
                return lifeCycle == LifeCycle.TEST ? createJobName(project, '', pipeline.id) : createJobName(project, 'App', pipeline.id)
            case ~/(?i)db/:
                return createJobName(project, 'DB', pipeline.id)
            case ~/(?i)api/:
            case ~/(?i)fix/:
                throw new IllegalArgumentException("No deploy pipeline support for pipeline type [${pipeline.type}]" + (pipeline.id ? " with id [${pipeline.id}]" : ''))
            default:
                throw new IllegalStateException("Unknown pipeline type [${pipeline.type}]")
        }
    }

    def decorateAsAppJob(Project project, Pipeline pipeline, Job job) {
        def jdkVersion = pipeline.technology.version
        JobPluginConfigHelper.verifyJdkVersion(jdkVersion)
        job.with {
            name getJobNameForPipeline(project, pipeline)
            jdk "jdk ${jdkVersion}"

            environmentVariables {
                groovy(createEnvInjectScriptContent(project, pipeline))
            }

            wrappers {}

            if (pipeline.tool.type =~ /(?i)maven/) {
                wrappers {
                    toolenv("Maven ${pipeline.tool.version}")
                }
            }

            steps {
                shell createShellDeployScriptContent(project, pipeline)
            }

            configure configureDcArtifactoryPublisher(project, pipeline)

            if (lifeCycle != LifeCycle.TEST) {
                publishers publishersExtendedEmailConfigurer(project)
            }
        }
    }

    def decorateAsDbJob(Project project, Pipeline pipeline, Job job) {
        def jdkVersion = '6u45'
        JobPluginConfigHelper.verifyJdkVersion(jdkVersion)
        job.with {
            name getJobNameForPipeline(project, pipeline)
            jdk "jdk ${jdkVersion}"

            environmentVariables {
                groovy(createEnvInjectScriptContent(project, pipeline))
            }

            wrappers {}

            if (pipeline.tool.type =~ /(?i)maven/) {
                wrappers {
                    toolenv("Maven ${pipeline.tool.version}")
                }
            }

            configure configureDcArtifactoryPublisher(project, pipeline)

            steps {
                shell createShellDeployScriptContent(project, pipeline)
            }

            if (lifeCycle != LifeCycle.TEST) {
                publishers publishersExtendedEmailConfigurer(project)
            }
        }
    }

    private String createEnvInjectScriptContent(Project project, Pipeline pipeline) {
        def envInjectScriptContent
        if (lifeCycle == LifeCycle.TEST) {
            envInjectScriptContent = """\
                            binding.setVariable('scmBase', "${pipeline.scm.base}")
                            return evaluate(new File(binding.getVariable('GROOVY_SCRIPTS_HOME'), 'GroovyEnvInject.groovy'))
                            """.stripIndent()
        } else {
            def branchVariable = lifeCycle == LifeCycle.ACC ? '${ACC_BRANCH}' : '${XPRPRO_BRANCH}'
            envInjectScriptContent = """\
                            binding.setVariable("scmBase", "${pipeline.scm.base}")
                            binding.setVariable('sprint', "${branchVariable}")
                            def scmTagResult = evaluate(new File(binding.getVariable('GROOVY_SCRIPTS_HOME'), "DetermineSCMTag.groovy"))
                            def emergencyVersionResult = evaluate(new File(binding.getVariable('GROOVY_SCRIPTS_HOME'), "DetermineEmergencyVersionNumber.groovy"))
                            return scmTagResult + emergencyVersionResult
                            """.stripIndent()
        }
        return envInjectScriptContent
    }

    private String createJobName(Project project, String postfix, String id) {
        def jobName = (id ? id : project.shortName).toUpperCase()
        if (postfix) {
            jobName = "${jobName}_${postfix}"
        }
        switch (lifeCycle) {
            case LifeCycle.ACC:
                jobName = "Deploy_Acc_${jobName}"
                break
            case LifeCycle.XPRPRO:
                jobName = "Deploy_XprPro_${jobName}"
                break
            case LifeCycle.TEST:
            default:
                jobName = "Deploy_${jobName}"
        }
        return jobName
    }

    private String createShellDeployScriptContent(Project project, Pipeline pipeline) {
        def appCommand = 'ssh-agent sh ${SCRIPTSHOME}/bin/deployitless_app.sh'
        def dbCommand = 'ssh-agent sh ${SCRIPTSHOME}/bin/deployitless_db.sh'
        def appBranchCommand = 'ssh-agent sh ${SCRIPTSHOME}/bin/deployitless_branch_app.sh'
        def dbBranchCommand = 'ssh-agent sh ${SCRIPTSHOME}/bin/deployitless_branch_db.sh'
        def commandLine = []

        if (settings.contains(JobDslSetting.SANDBOX)) {
            messages << 'SANDBOX: skip shell deploy script'
            commandLine << '#'
        }
        switch (pipeline.type) {
            case ~/(?i)app/:
                commandLine << (lifeCycle == LifeCycle.TEST ? appCommand : appBranchCommand)
                break
            case ~/(?i)db/:
                switch (pipeline.technology.type) {
                    case ~/(?i)mongo/:
                        commandLine << (lifeCycle == LifeCycle.TEST ? appCommand : appBranchCommand)
                        break
                    case ~/(?i)oracle/:
                        commandLine << (lifeCycle == LifeCycle.TEST ? dbCommand : dbBranchCommand)
                        if (pipeline.deploy.oracleSubdir.toBoolean()) {
                            commandLine << '-s oracle'
                        }
                        break
                    default:
                        throw new IllegalStateException("Unknown db type [${pipeline.technology.type}]")
                }
                break
            default:
                throw new IllegalStateException("Unknown deploy pipeline type [${pipeline.technology.type}]")
        }

        switch (lifeCycle) {
            case LifeCycle.ACC:
                commandLine << '-b ${ACC_BRANCH}'
                break
            case LifeCycle.XPRPRO:
                commandLine << '-b ${XPRPRO_BRANCH}'
                break
        }

        switch (pipeline.tool.type) {
            case ~/(?i)maven/:
                JobPluginConfigHelper.verifyMavenVersion(pipeline.tool.version)
                commandLine << '-t maven -m ${' + "MAVEN_${pipeline.tool.version.replace('.', '_')}_HOME" + '}'
                break
            case ~/(?i)gradle/:
                commandLine << '-t gradle'
                break
            default:
                throw new IllegalStateException("Unsupported tool type [${pipeline.tool.type}]")
        }

        if (pipeline.deploy.params) {
            commandLine << pipeline.deploy.params.join(' ')
        }
        return commandLine.join(' ')
    }

    private def publishersExtendedEmailConfigurer(Project project) {
        if (settings.contains(JobDslSetting.SANDBOX)) {
            messages << 'SANDBOX: skip email sending'
            return {}
        }
        return { node ->
            extendedEmail('releasemanagement@company.tld') {
                trigger(triggerName: 'Failure',
                        sendToDevelopers: true,
                        sendToRecipientList: true,
                )
            }
        }
    }

    private def configureDcArtifactoryPublisher(Project project, Pipeline pipeline) {
        if (settings.contains(JobDslSetting.SANDBOX)) {
            messages << 'SANDBOX: skip artifact publishing'
            return {}
        }
        return { node ->
            node / 'buildWrappers' / 'org.jfrog.hudson.generic.ArtifactoryGenericConfigurator' {
                details {
                    artifactoryUrl(ARTIFACTORY_DC_URL)
                    artifactoryName(ARTIFACTORY_DC_NAME)
                    repositoryKey(REPOSITORY_DC_KEY)
                    snapshotsRepositoryKey(SNAPSHOT_REPOSITORY_DC_KEY)
                }
                deployPattern(pipeline.deploy.publishedArtifacts.join('\n'))
                deployBuildInfo(true)
                includeEnvVars(false)
            }
        }
    }
}
