
(1..15).each { projectNumber ->
    job {
        name "project_${projectNumber}"
        def projectName = name.replaceAll('_', ' ').capitalize()
        displayName projectName
        description "CI job for ${projectName}"

        label 'slave'

        scm {
            git("git@some.fancy.githost.tld:project-group/project-${projectNumber}.git", 'master')
        }
        triggers {
            scm('@yearly')
        }

        jdk('jdk 7u25')
        steps {
            gradle('clean build')
        }
        publishers {
            mailer('project-team@company.tld', true, true)
        }
    }
}
