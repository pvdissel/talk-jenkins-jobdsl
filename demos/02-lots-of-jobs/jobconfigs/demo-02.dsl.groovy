import javaposse.jobdsl.dsl.ViewType
import javaposse.jobdsl.dsl.views.ListView

(1..15).each { projectNumber ->
    def projectName = "project_${projectNumber}"
    ['CI', 'Deploy to Test', 'TA', 'Deploy to ACC', 'Release'].each { stage ->
        job {
            name "${projectName}_${stage.toLowerCase()}".replaceAll(' ', '_')
            def title = name.replaceAll('_', ' ').replaceAll(/\w+/) { w -> w.capitalize() }
            displayName title
            description "${stage} job for ${title}"

            label 'slave'

            scm {
                git("git@some.fancy.githost.tld:project-group/${projectName}.git", 'master')
            }
            triggers {
                scm('@yearly')
            }

            jdk('jdk 7u25')
            steps {
                gradle('clean build')
            }
            publishers {
                mailer("${projectName}-team@company.tld", true, true)
            }
        }
    }

    view(type: ViewType.ListView) {
        name projectName
        jobs {
            regex "${projectName}_.*"
        }
        statusFilter(ListView.StatusFilter.ALL)
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
}
