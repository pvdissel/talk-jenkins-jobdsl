import javaposse.jobdsl.dsl.helpers.AbstractContextHelper
import javaposse.jobdsl.dsl.helpers.common.DownstreamTriggerContext

class ProjectHelper {

    static final String PASSTHROUGH_PROPERTIES_FILENAME = '.jenkins.job.passthrough.properties'
    static final String ARTIFACTVERSION_KEY = 'artifactVersion'

    static void initialise() {
        String.metaClass.warn = { ->
            "\033[4;1m" + delegate + "\033[0m"
        }
    }

    /**
     * This is needed for all release jobs. It makes sure that underlying SCM is used properly.
     */
    static def addPreBuildShellStep(config) {
        if (config.isGit()) {
            return """\
                    git checkout ${config.branchName} || git checkout -b ${config.branchName}
                    git reset --hard origin/${config.branchName}
                   """.stripIndent()
        } else {
            return "echo 'No prebuild step required for SVN'"
        }
    }

    static def executeMavenCommand(config, action, Closure configure = null) {
        executeMavenCommand(config, action, 'pom.xml', configure)
    }

    static def executeMavenCommand(config, action, pomFile, Closure configure = null) {
        def defaultConfigure = { node ->
            node / 'mavenName' << config.mavenVersion
            return node
        }
        def closure = defaultConfigure
        if (configure) {
            closure = configure << defaultConfigure
        }
        return { steps ->
            maven("${action} --settings ${config.mavenSettingsFilePath}", pomFile, closure)
        }
    }

    static def addRequiredParameterForInitialJob() {
        return { job ->
            job / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' / 'hudson.model.StringParameterDefinition' << {
                name('startParamForManual')
                defaultValue('autoDeploy')
                description('needed for automated deployment - please set reason for manual deployment')
            }
        }
    }

    static def manualTrigger(names) {
        return manualTrigger(names, {
            currentBuild()
        })
    }

    static def manualTrigger(names, Closure downstreamTriggerConfigure) {
        return { node ->
            node / publishers / 'au.com.centrumsystems.hudson.plugin.buildpipeline.trigger.BuildPipelineTrigger' {
                configs createParameterConfigFrom(downstreamTriggerConfigure)
                downstreamProjectNames(names)
            }
        }
    }

    private static def createParameterConfigFrom(Closure downstreamClosure) {
        // This closure is copied from PublisherContextHelper#downstreamParameterized() from the
        // original JobDSL plugin code. As this one is not directly reusable from there....
        return { node ->
            DownstreamTriggerContext trigger = new DownstreamTriggerContext()
            AbstractContextHelper.executeInContext(downstreamClosure, trigger)

            if (!trigger.hasParameter()) {
                return configs('class': 'java.util.Collections$EmptyList')
            }

            if (trigger.usingCurrentBuild) {
                'hudson.plugins.parameterizedtrigger.CurrentBuildParameters' ''
            }

            if (trigger.usingPropertiesFile) {
                'hudson.plugins.parameterizedtrigger.FileBuildParameters' {
                    propertiesFile trigger.propFile
                }
            }

            if (trigger.usingGitRevision) {
                'hudson.plugins.git.GitRevisionBuildParameters' {
                    'combineQueuedCommits' trigger.combineQueuedCommits ? 'true' : 'false'
                }
            }

            if (trigger.usingPredefined) {
                'hudson.plugins.parameterizedtrigger.PredefinedBuildParameters' {
                    delegate.createNode('properties', trigger.predefinedProps.join('\n'))
                }
            }

            if (trigger.usingMatrixSubset) {
                'hudson.plugins.parameterizedtrigger.matrix.MatrixSubsetBuildParameters' {
                    filter trigger.matrixSubsetFilter
                }
            }

            if (trigger.usingSubversionRevision) {
                'hudson.plugins.parameterizedtrigger.SubversionRevisionBuildParameters' {}
            }
        }
    }

    static def publishJUnit(JobConfig config) {
        if (!config.hasTests()) {
            return
        }
        return { publishers ->
            archiveJunit('**/target/surefire-reports/*.xml, **/target/failsafe-reports/*.xml, **/target/jasmine/*.xml')
        }
    }

    static def publishTestNg(country) {
        return { node ->
            node / publishers / 'hudson.plugins.testng.Publisher' {
                reportFilenamePattern("${country}/target/surefire-reports/testng-results.xml")
                escapeTestDescp('true')
                escapeExceptionMsg('true')
            }
        }
    }

    static def mailers() {
        return { node ->
            node / 'publishers' << {
                'hudson.tasks.Mailer' {
                    recipients('project-team@company.tld')
                    dontNotifyEveryUnstableBuild('false')
                    sendToIndividuals('true')
                }
            }
        }
    }

    static def buildWrappers() {
        return { node ->
            node / 'buildWrappers' << {
                'hudson.plugins.timestamper.TimestamperBuildWrapper'()
                'hudson.plugins.ansicolor.AnsiColorBuildWrapper' {
                    colorMapName('xterm')
                }
            }
        }
    }

    static
    def throttleConcurrentBuildCategories(int overallConcurentJobs, int concurrentJobsPerNode, String categoryName) {
        return { node ->
            node / 'properties' / 'hudson.plugins.throttleconcurrents.ThrottleJobProperty' << {
                maxConcurrentPerNode(concurrentJobsPerNode)
                maxConcurrentTotal(overallConcurentJobs)
                throttleEnabled('true')
                throttleOption('category')
                'categories' {
                    string(categoryName)
                }
            }
        }
    }

    static def triggerBlockingBuildWithoutParameters(String projectToBuild) {
        return { node ->
            node / 'builders' / 'hudson.plugins.parameterizedtrigger.TriggerBuilder' / 'configs' / 'hudson.plugins.parameterizedtrigger.BlockableBuildTriggerConfig' << {
                configs() {
                    [class: 'empty-list']
                }
                projects(projectToBuild)
                condition('ALWAYS')
                triggerWithNoParameters('false')
                block()
                buildAllNodesWithLabel('false')
            }
        }
    }

    static def scmConfig(JobConfig config) {
        if (config.isGit()) {
            return gitConfig(config)
        }
        return svnConfig(config)
    }

    static def gitConfig(JobConfig config) {
        return { scm ->
            git(config.repoUrl, config.branchName) { node ->
                node / 'authorOrCommitter' << 'true'
                node / 'gitConfigName' << 'Jenkins'
                node / 'gitConfigEmail' << 'jenkins@company.tld'
            }
        }
    }

    static def gitSeleniumConfig() {
        return { scm ->
            git("git@git.company.tld:app01/seleniumtest.git", "master") { node ->
                node / 'authorOrCommitter' << 'true'
                node / 'gitConfigName' << 'Jenkins'
                node / 'gitConfigEmail' << 'jenkins@company.tld'
                node / 'useShallowClone' << 'true'
            }
        }
    }

    static def svnConfig(JobConfig config) {
        return { scm ->
            svn("${config.repoUrl}")
        }
    }

    static def scmBranchName(String name) {
        final svnBranches = 'branches/'
        if (name.startsWith(svnBranches)) {
            name = name.substring(svnBranches.length())
        }
        return name
    }

    static def createArtifactVersionFileInWorkspace() {
        return """\
                def env = System.getenv()
                def workspace = env['WORKSPACE']

                def project = new XmlSlurper().parse(new File("\${workspace}/pom.xml"))
                def projectVersion = project.version.toString()
                println "Found project version = \${projectVersion}"

                def paramName = '${ARTIFACTVERSION_KEY}'
                println "Configuring parameter key '\${paramName}' to value '\${projectVersion}'"

                def propsFilename = '${PASSTHROUGH_PROPERTIES_FILENAME}'
                propsFile = new File(propsFilename)
                propsFile.delete()
                propsFile << "\${paramName}=\${projectVersion}"
                println "Written '\${propsFilename}'"
            """.stripIndent()
    }

    static def createPullDeploymentShellScriptFor(host, country) {
        println("Uploading artificate for '${country}' to host '${host}".warn())
        def CIO_JUMP_MACHINE = "app01@app01.jump.company.tld"
        def s1 = new ExternalSshCommand(CIO_JUMP_MACHINE, "fetch-artifact -c ${country} -v \${artifactVersion}",
                "Step 1: download the file locally or on a CIO machine, for example, UAT").toSshCmd()
        def s2 = new ExternalSshCommand(host, "scp ${CIO_JUMP_MACHINE}:/opt/app01/distributions/distribution-${country}-\${artifactVersion}.jar /opt/app01/distributions",
                "Step 2: scp the file to PROD and have it renamed").toSshCmd()
        def s3 = new ExternalSshCommand(host, "/opt/app01/bin/app-deploy-${country} \${artifactVersion}",
                "Step 3: run normal install command (will skip external download)").toSshCmd()
        return s1 + s2 + s3
    }

    static def createDeploymentShellScriptFor(host, country) {
        println("Deploying '${country}' to host '${host}".warn())
        return new ExternalSshCommand(host, "/opt/app01/bin/app-deploy ${country} \${artifactVersion}",
                "Exectuting remote deployment.").toSshCmd()
    }

    static def blockBuildWhenDownstreamBuilding(Boolean block) {
        return { job ->
            job / 'blockBuildWhenDownstreamBuilding' << block
        }
    }

    @Deprecated
    static def createDeployScriptToOldCNO(JobConfig config, environment) {
        def distributionsPath = "/home/app01/distributions"
        def appServerHomePath = "/opt/appserver"

        def sshDestinationServer = "deployer@app01.company.tld"
        def sshPpkArguments = "-i /var/lib/jenkins/.ssh/id_rsa -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no"

        def nexusUser = "jenkins-app01"
        def nexusPassword = "<password>"

        def distributionSymlinkName = "current_${config.country}"
        def releaseConfigName = environment

        return """\
                # Fail if artifactVersion is not set
                if [ -z \${artifactVersion} ]; then
                   echo "artifactVersion not set!"
                   exit 1
                fi

                echo "Installing artifact version " \${artifactVersion}

                # Undeploy
                ssh ${sshPpkArguments} ${sshDestinationServer} \
                appserver ${config.country} undeploy \
                -s ${distributionsPath}/${distributionSymlinkName} \
                -r ${releaseConfigName}

                # Deploy
                ssh ${sshPpkArguments} ${sshDestinationServer} appserver ${config.country} deploy \
                -s ${distributionsPath}/${distributionSymlinkName} \
                -U ${nexusUser} \
                -P ${nexusPassword} \
                -v \${artifactVersion} \
                -w ${distributionsPath} \
                -r ${releaseConfigName} \
                -p ${appServerHomePath} \
                --update-webresources

                """.stripIndent()
    }
}

class ExternalSshCommand {

    final SSH_OPTIONS
    final remoteCommand
    final description

    ExternalSshCommand(hostToExecuteOn, remoteCommand, description) {
        this.remoteCommand = remoteCommand
        this.description = description
        SSH_OPTIONS = "-tt ${hostToExecuteOn} -i /var/lib/jenkins/.ssh/id_rsa -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no"
    }

    def toSshCmd() {
        return """\
            echo "${description}"
            ssh ${SSH_OPTIONS} sudo su -c \\"${remoteCommand}\\" appserver
        """.stripIndent()
    }
}


