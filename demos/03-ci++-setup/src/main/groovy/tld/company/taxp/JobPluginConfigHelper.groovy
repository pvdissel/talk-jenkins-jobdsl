package tld.company.taxp

import tld.company.taxp.model.pipeline.ci.Cobertura
import tld.company.taxp.model.pipeline.ci.StaticAnalysis

class JobPluginConfigHelper {
    static List getSupportedJdkVersions() {
        return [
                '6u45',
                '7u25',
                '7u60',
        ]
    }

    static void verifyJdkVersion(String version) {
        if (!getSupportedJdkVersions().contains(version)) {
            throw new IllegalStateException("Unsupported technology java version [${version}]")
        }
    }

    static List getSupportedMavenVersions() {
        return [
                '2.2.1',
                '3.0.1',
                '3.0.2',
                '3.1.1',
                '3.2.1',
        ]
    }

    static void verifyMavenVersion(String version) {
        if (!getSupportedMavenVersions().contains(version)) {
            throw new IllegalStateException("Unsupported tool maven version [${version}]")
        }
    }

    static def staticAnalysisConfigurer(StaticAnalysis config) {
        if (!config) {
            return {}
        }
        return {
            healthLimits getIntFromString(config.healthy), getIntFromString(config.unhealthy)
            thresholdLimit config.thresholdLimit
            defaultEncoding config.defaultEncoding
            canRunOnFailed config.canRunOnFailed
            useStableBuildAsReference config.useStableBuildAsReference.toBoolean()
            useDeltaValues config.useDeltaValues.toBoolean()
            computeNew !config.dontComputeNew
            shouldDetectModules config.shouldDetectModules.toBoolean()
            thresholds(getThresholdsFor(config))
        }
    }

    static def getThresholdsFor(staticAnalysisPluginConfig) {
        def thresholdsArray = [:]
        if (staticAnalysisPluginConfig.thresholds?.unstableTotal) {
            thresholdsArray << [unstableTotal: [
                    all   : staticAnalysisPluginConfig.thresholds.unstableTotal?.all,
                    high  : staticAnalysisPluginConfig.thresholds.unstableTotal?.high,
                    normal: staticAnalysisPluginConfig.thresholds.unstableTotal?.normal,
                    low   : staticAnalysisPluginConfig.thresholds.unstableTotal?.low
            ]]
        }
        if (staticAnalysisPluginConfig.thresholds?.failedTotal) {
            thresholdsArray << [failedTotal: [
                    all   : staticAnalysisPluginConfig.thresholds.failedTotal?.all,
                    high  : staticAnalysisPluginConfig.thresholds.failedTotal?.high,
                    normal: staticAnalysisPluginConfig.thresholds.failedTotal?.normal,
                    low   : staticAnalysisPluginConfig.thresholds.failedTotal?.low
            ]]
        }
        if (staticAnalysisPluginConfig.thresholds?.unstableNew) {
            thresholdsArray << [unstableNew: [
                    all   : staticAnalysisPluginConfig.thresholds.unstableNew?.all,
                    high  : staticAnalysisPluginConfig.thresholds.unstableNew?.high,
                    normal: staticAnalysisPluginConfig.thresholds.unstableNew?.normal,
                    low   : staticAnalysisPluginConfig.thresholds.unstableNew?.low
            ]]
        }
        if (staticAnalysisPluginConfig.thresholds?.failedNew) {
            thresholdsArray << [failedNew: [
                    all   : staticAnalysisPluginConfig.thresholds.failedNew?.all,
                    high  : staticAnalysisPluginConfig.thresholds.failedNew?.high,
                    normal: staticAnalysisPluginConfig.thresholds.failedNew?.normal,
                    low   : staticAnalysisPluginConfig.thresholds.failedNew?.low
            ]]
        }
        return thresholdsArray
    }

    static def coberturaConfigurer(Cobertura config) {
        if (!config) {
            return {}
        }
        return {
            onlyStable config.onlyStable.toBoolean()
            failUnhealthy config.failUnhealthy.toBoolean()
            failUnstable config.failUnstable.toBoolean()
            autoUpdateHealth config.autoUpdateHealth.toBoolean()
            autoUpdateStability config.autoUpdateStability.toBoolean()
            zoomCoverageChart config.zoomCoverageChart.toBoolean()
            failNoReports config.failNoReports.toBoolean()
            sourceEncoding config.sourceEncoding

            def healthy = config.targets.find { it.type == 'healthy' }
            def unhealthy = config.targets.find { it.type == 'unhealthy' }
            def failing = config.targets.find { it.type == 'failing' }

            methodTarget(toCoberturaPercentage(healthy.method), toCoberturaPercentage(unhealthy.method), toCoberturaPercentage(failing.method))
            lineTarget(toCoberturaPercentage(healthy.line), toCoberturaPercentage(unhealthy.line), toCoberturaPercentage(failing.line))
            conditionalTarget(toCoberturaPercentage(healthy.conditional), toCoberturaPercentage(unhealthy.conditional), toCoberturaPercentage(failing.conditional))
        }
    }

    static int toCoberturaPercentage(String input) {
        int val = getIntFromString(input)
        return val ? val / 100000 : 0
    }

    static def getIntFromString(String input) {
        return input ? input.toInteger() : 0
    }
}
