package tld.company.taxp

import tld.company.taxp.model.pipeline.ci.LifeCycle

class RunnerSection {
    def String subject
    def lifeCycles = []
    def Closure closure

    def RunnerSection(String subject, List<LifeCycle> lifeCycles, Closure closure) {
        this.subject = subject
        this.closure = closure
        this.lifeCycles += lifeCycles
    }
}
