package tld.company.taxp.sprinthopping

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.JobType
import tld.company.taxp.JobCreator

class AutoCreateBranchJobCreator implements JobCreator {
    final String name = 'SprintHopping_Auto_Create_Branch'
    final JobType type = JobType.Freeform

    @Override
    Job create(JobParent parent) {
        return parent.job(type: type) {
            name this.name

            wrappers {
                preBuildCleanup()
            }

            parameters {
                stringParam('BASE_URL', 'git@git.company.tld:group/shopfindservice.git', '''\
                    The base svn or git url where to create the branch.
                    Example: https://svn.company.tld/group/partner or git@git.company.tld:group/shopfindservice.git'''.stripIndent()
                )
                stringParam('PATTERN', '-62.', '''\
                    The pattern to look for within the tags.'''.stripIndent()
                )
                stringParam('BRANCH_NAME', '62.x', '''\
                    The name of the branch to be created'''.stripIndent()
                )
            }

            steps {
                shell '''\
                    #!/bin/bash
                    . ${SCRIPTSHOME}/bin/auto_create_branch.sh
                    autoBranchRevision -b ${BASE_URL} -p ${PATTERN} -t ${BRANCH_NAME}
                    '''.stripIndent()

                groovyScriptFile('/opt/jenkins/scripts/groovy-scripts/AutoBranchRevision.groovy') {
                    groovyInstallation 'Groovy 2.1.3'
                    classpath '/opt/jenkins/scripts/groovy-scripts'
                }
            }
        }
    }
}
