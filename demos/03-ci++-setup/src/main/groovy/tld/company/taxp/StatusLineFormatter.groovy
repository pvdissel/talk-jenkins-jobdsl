package tld.company.taxp

class StatusLineFormatter {
    private final List<JobDslSetting> settings

    def StatusLineFormatter(List<JobDslSetting> settings) {
        this.settings = settings
    }

    def asSuccessStatusLine(String msg) {
        use(StringColoringCategory) {
            return printStatusLine(msg, "SUCCESS".info())
        }
    }

    def printStatusLine(String msg, String status) {
        return msg + (' ' * getPaddingSize(75, msg)) + status
    }

    def executeWithStatusLine(Closure logMethod, String msg, Closure code) {
        def result = true
        use(StringColoringCategory) {
            def output = ''
            output += msg + (' ' * getPaddingSize(75, msg))
            try {
                def messages = []
                // IMPROVE: Catch all output and print below messages
                code.call(messages)
                output += newLine('SUCCESS'.info())
                if (messages) {
                    messages.each {
                        output += newLine printStatusLine('** '.note() + it, 'NOTE'.note())
                    }
                    output += newLine('')
                }
                logMethod output
            } catch (IllegalStateException warning) {
                output += newLine('WARNING'.warn())
                output += newLine('>> '.warn() + warning.message)
                logMethod output
            } catch (Exception error) {
                output += newLine('ERROR'.error())
                output += newLine('!! '.error() + error.message)

                if (settings.contains(JobDslSetting.DEBUG)) {
                    error.stackTrace.findAll {
                        isJobDslRelated(it)
                    }.each { StackTraceElement el ->
                        output += newLine el
                    }
                } else {
                    output += newLine error.stackTrace.find {
                        isJobDslRelated(it)
                    }
                }
                output += newLine ''
                logMethod output
                result = false
            }
        }
        return result
    }

    private def newLine(msg) {
        (msg as String) + '\n'
    }

    private boolean isJobDslRelated(StackTraceElement it) {
        it.fileName =~ /${this.class.canonicalName}/ ||
                it.fileName =~ /.*\.dsl(\.groovy)?/ ||
                it.className =~ /(${this.class.canonicalName}|tld\.company).*/
    }

    private getPaddingSize(int max, String msg) {
        def plainLength = getPlainString(msg).length()
        def paddingSize = max - plainLength
        paddingSize > 0 ? paddingSize : 1
    }

    private getPlainString(String string) {
        // Remove special bash color characters
        return (string =~ /\x1B\[([0-9]{1,2}(;[0-9]{1,2})?)?[m|K]/).replaceAll('')
    }
}
