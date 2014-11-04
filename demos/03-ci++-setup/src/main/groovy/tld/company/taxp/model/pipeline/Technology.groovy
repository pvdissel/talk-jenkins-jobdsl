package tld.company.taxp.model.pipeline

/**
 * For example jdk
 */
class Technology {
    def type
    def version

    static final JDK_6 = new Technology(type: "jdk", version: "6u45")

    @Override
    public String toString() {
        "{${type}, ${version}}"
    }
}
