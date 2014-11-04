package tld.company.taxp.logging

import tld.company.taxp.StringColoringCategory

class ColorOutputLogger implements Logger {

    final PrintStream out
    boolean debug = true

    ColorOutputLogger(OutputStream out) {
        this.out = new PrintStream(out)
    }

    @Override
    void error(msg) {
        use(StringColoringCategory) {
            out.println containsColor(msg) ? msg : msg.error()
        }
    }

    @Override
    void warn(msg) {
        use(StringColoringCategory) {
            out.println containsColor(msg) ? msg : msg.warn()
        }
    }

    @Override
    void info(msg) {
        use(StringColoringCategory) {
            out.println containsColor(msg) ? msg : msg.info()
        }
    }

    @Override
    void note(msg) {
        use(StringColoringCategory) {
            out.println containsColor(msg) ? msg : msg.note()
        }
    }

    @Override
    void debug(msg) {
        if (isDebug()) {
            out.println msg
        }
    }

    private boolean containsColor(msg) {
        (msg ==~ /(?m)^.*\x1B\[([0-9]{1,2}(;[0-9]{1,2})?)?[m|K].*$/)
    }
}
