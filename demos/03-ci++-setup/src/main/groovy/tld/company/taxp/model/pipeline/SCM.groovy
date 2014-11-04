package tld.company.taxp.model.pipeline

import tld.company.taxp.model.pipeline.ci.LifeCycle

/**
 * For example SVN or GIT
 */
class SCM {
    def type
    def base

    def configureScm(lifeCycle, relativeTargetDirName = null) {
        return { scm ->
            switch (this.type) {
                case ~/(?i)svn/:
                    def url = this.base
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
                    //git(this.base, branchName)
                    git {
                        remote {
                            url(this.base)
                        }
                        branch(branchName)
                        if (relativeTargetDirName) {
                            relativeTargetDir(relativeTargetDirName)
                        }
                    }
                    break
                default:
                    throw new IllegalStateException("Unknown scm type [${this.type}]")
            }
        }
    }

    def configureScmWithBranchName(branchName, relativeTargetDirName = null) {
        return { scm ->
            switch (this.type) {
                case ~/(?i)svn/:
                    def url = this.base
                    url += '/' + branchName
                    svn(url)
                    break
                case ~/(?i)git/:
                    git {
                        remote {
                            url(this.base)
                        }
                        branch(branchName)
                        if (relativeTargetDirName) {
                            relativeTargetDir(relativeTargetDirName)
                        }
                    }
                    break
                default:
                    throw new IllegalStateException("Unknown scm type [${this.type}]")
            }
        }
    }

    @Override
    public String toString() {
        "{${type}, ${base}}"
    }
}
