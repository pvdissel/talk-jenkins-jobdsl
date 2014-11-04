package tld.company.taxp.model.pipeline

import tld.company.taxp.model.pipeline.ci.*

class Ci {
    Maven maven
    Gradle gradle
    StaticAnalysis findBugs
    StaticAnalysis pmd
    StaticAnalysisDry dry
    StaticAnalysisTasks tasks
    Cobertura cobertura
    JUnit junit
    def shell
    def locks
    Artifactory publishedArtifacts
    PublishHtmlReport publishHtmlReport
}
