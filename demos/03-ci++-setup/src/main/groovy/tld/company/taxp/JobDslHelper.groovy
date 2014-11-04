package tld.company.taxp

import tld.company.taxp.model.pipeline.ci.LifeCycle

class JobDslHelper {
    private Binding bindings
    private final String localPath

    JobDslHelper(Binding bindings, String localPath) {
        this.localPath = localPath
        this.bindings = bindings
    }

    Map<String, String> getConfig() {
        def config = [:]
        config.putAll(System.getenv())
        config.putAll(bindings.getVariables())
        return config
    }

    List<JobDslSetting> getSettings() {
        def settings = []
        if (config.SETTINGS) {
            // SETTINGS is a space- and case-insensitive, comma-separated string of settings
            settings = config.SETTINGS.replaceAll(' ', '').split(',').toList()
            settings = settings.findAll { givenSetting ->
                JobDslSetting.values().find {
                    it.name() =~ /(?i)^${givenSetting}$/
                }
            }.collect { JobDslSetting.valueOf(it.toUpperCase()) }
        }

        if (settings.contains(JobDslSetting.ALL_LIFECYCLES)) {
            settings.add(JobDslSetting.DISABLED)
        }
        return settings
    }

    File getWorkspace() {
        File workspace = new File(localPath)
        if (config.WORKSPACE) {
            workspace = new File("${config.WORKSPACE}/build/jobDsl/workspace");
        }
        return workspace
    }

    LifeCycle getLifeCycle() {
        def jobName = config.JOB_NAME
        def jobLifeCycle = LifeCycle.TEST
        if (jobName) {
            switch (jobName) {
                case ~/(?i).*ACC.*/:
                    jobLifeCycle = LifeCycle.ACC
                    break
                case ~/(?i).*XPRPRO.*/:
                    jobLifeCycle = LifeCycle.XPRPRO
                    break
                default:
                    jobLifeCycle = LifeCycle.TEST
            }
        }
        return jobLifeCycle
    }

    PrintStream getOut() {
        // 'out' is injected by the Jenkins JobDSL plugin
        if (!config.out) {
            return System.out
        }
        return config.out
    }

    def executeWithStatusLine(String msg, Closure code) {
        formatter.executeWithStatusLine(out.&println, msg, code)
    }

    def printStatusLine(String msg, String status) {
        out.println formatter.printStatusLine(msg, status)
    }

    private def getFormatter() {
        new StatusLineFormatter(settings)
    }
}

