package tld.company.taxp.model.pipeline.ci

class Cobertura {
    def coberturaReportFile
    def onlyStable
    def failUnhealthy
    def failUnstable
    def autoUpdateHealth
    def autoUpdateStability
    def zoomCoverageChart
    def failNoReports
    def sourceEncoding
    CoberturaTarget[] targets
}
