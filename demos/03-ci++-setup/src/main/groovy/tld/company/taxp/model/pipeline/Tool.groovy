package tld.company.taxp.model.pipeline

/**
 * For example Maven or Gradle
 */
class Tool {
    def type
    def version

    static final MAVEN_3 = new Tool(type: "maven", version: "3.2.1")

    @Override
    public String toString() {
        "{${type}, ${version}}"
    }
}
