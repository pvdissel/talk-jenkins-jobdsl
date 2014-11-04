package tld.company.taxp.model.pipeline.ci

class StaticAnalysis {
    def healthy
    def unhealthy
    def thresholdLimit
    def defaultEncoding
    def canRunOnFailed
    def useStableBuildAsReference
    def useDeltaValues
    def shouldDetectModules
    def dontComputeNew
    def doNotResolveRelativePaths
    StaticAnalysisThresholds thresholds
}
