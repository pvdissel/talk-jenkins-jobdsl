package tld.company.taxp.model.pipeline

class XLTestJob {

    Technology technology = Technology.JDK_6
    Tool tool = Tool.MAVEN_3

    SCM scm

    /**
     * The location of the FitNesse jobs in the given project
     */
    def projectDir = "."
    def fitNesseHome = "."
    def mavenGoals = "clean test"
    def useMavenRootPom = false

    def suiteFilter = "Nightly"
    def browser = "none"

    @Override
    public String toString() {
        return "XLTestJob{" +
                "projectDir=" + projectDir +
                "fitNesseHome=" + fitNesseHome +
                "mavenGoals=" + mavenGoals +
                "useMavenRootPom=" + useMavenRootPom +
                "suiteFilter=" + suiteFilter +
                "browser=" + browser +
                "job=" + super.toString() +
                '}';
    }
}
