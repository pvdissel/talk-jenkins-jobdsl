package tld.company.taxp.project

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.helpers.Permissions
import tld.company.taxp.DescriptionHelper
import tld.company.taxp.JobPluginConfigHelper
import tld.company.taxp.model.Project
import tld.company.taxp.model.pipeline.XLTestJob

class XlTestJobBuilder {

    def Job job;

    XlTestJobBuilder(Job job) {
        this.job = job;
    }

    static String createJobName(Project project) {
        "XL_Test_${project.shortName}_FitNesse"
    }

    def createJobFor(XLTestJob xlTestJob) {
        job.description """\
            This job will kick of the FitNesse tests on XL Test.
            ${DescriptionHelper.disclaimer}
            """.stripIndent()
        def jdkVersion = xlTestJob.technology.version
        JobPluginConfigHelper.verifyJdkVersion(jdkVersion)
        job.with {
            jdk "jdk ${jdkVersion}"
            scm xlTestJob.scm.configureScmWithBranchName("\${branch}")
            quietPeriod 0

            authorization {
                permission(Permissions.ItemRead, 'anonymous')
                permission(Permissions.ItemBuild, 'anonymous')
            }

            parameters {
                def defaultBranch = (xlTestJob.scm.type =~ /(?i)svn/ ? "trunk" : "master")

                stringParam("XLTEST_URL")
                stringParam("suiteName")
                stringParam("suiteFilter", "g", """\
                        Tags added to tests in FitNesse, e.g. 'label-DC2' or 'nightly'.
                        These are OR-ed when using multiple tags separated by commas without spaces as in 'nightly,nightlyFirefox'
                        """.stripIndent())
                stringParam("browser", "none")
                stringParam("branch", defaultBranch, "branch or revision of the test materials. E.g. 'master'.")
                stringParam("environment", "TEST2")

                configure { node ->
                    node / 'properties' / 'hudson.model.ParametersDefinitionProperty' / 'parameterDefinitions' << 'org.jvnet.jenkins.plugins.nodelabelparameter.LabelParameterDefinition' {
                        name 'label'
                        description "Specifies which slave to execute the test on. &apos;fitnesse&apos; for most testing, 'TA-CRM' for IE8 CRM testing"
                        defaultValue 'fitnesse'
                        allNodesMatchingLabel false
                        triggerIfResult 'allCases'
                    }
                }
            }

            concurrentBuild()

            steps {
                maven {
                    goals "${xlTestJob.mavenGoals} -Pxltest"
                    if (!xlTestJob.useMavenRootPom) {
                        rootPOM "${xlTestJob.projectDir}/pom.xml"
                    }
                    mavenInstallation "Maven ${xlTestJob.tool.version}"
                    property("skipTests", "false")
                    property("test", "SuiteTest")
                    property("xml.output.dir", "target/fitnesse/xml")
                    property("html.output.dir", "target/fitnesse/html")
                    property("fitnesse.home", "${xlTestJob.fitNesseHome}")
                    property("maven.test.failure.ignore", "true")
                    property("failIfNoTests", "false")
                    property("suite.name", "\${suiteName}")
                    property("suite.filter", "\${suiteFilter}")
                    property("ENVIRONMENT", "\${environment}")
                    property("webdriver.ie.driver", "BrowserExtensions/IEDriverServer.exe")
                    property("webdriver.chrome.driver", "BrowserExtensions/chromedriver.exe")
                }
                gradle("xltestReport -PBUILD_URL=\${BUILD_URL} -Pbrowser=\${browser} -Penvironment=\${environment} -PSPRINT=\${TST_SPRINT} -PsuiteName=\${suiteName} -PsuiteFilter=\${suiteFilter}", null, true) {
                    it / rootBuildScriptDir(xlTestJob.projectDir)
                    it / useWorkspaceAsHome(true)
                }
            }

            publishers {
                publishHtml {
                    report("${xlTestJob.projectDir}/target/fitnesse/html/", "Fitnesse Report", "\${suiteName}.html", true)
                }
            }

            configure {
                it / publishers / 'htmlpublisher.HtmlPublisher' / reportTargets / 'htmlpublisher.HtmlPublisherTarget' << {
                    allowMissing(true)
                }
            }

            // will be enabled when DPI-1877 is provisioned
            if (false) {
                configure { node ->
                    def sshPublisherPlugin = node / 'publishers' / 'jenkins.plugins.publish__over__ssh.BapSshPublisherPlugin'
                    sshPublisherPlugin << {
                        configName 'fitnessImageServer' // this name is configured in Jenkins
                        consolePrefix 'SSH: '
                    }
                    sshPublisherPlugin / 'delegate' / publishers / 'jenkins.plugins.publish__over__ssh.BapSshPublisher' / transfers / 'jenkins.plugins.publish__over__ssh.BapSshTransfer' {
                        remoteDirectory "\$JOB_NAME/\$BUILD_NUMBER"
                        sourceFiles "${xlTestJob.fitNesseHome}/FitNesseRoot/files/testResults/screenshots/**/*.png"
                        makeEmptyDirs true
                        execTimeout 4000
                        usePty true
                    }
                }
            }

            wrappers {
                timestamps()
                colorizeOutput()
            }
        }

        job
    }
}
