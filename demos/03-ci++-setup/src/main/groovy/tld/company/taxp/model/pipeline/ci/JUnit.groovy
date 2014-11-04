package tld.company.taxp.model.pipeline.ci

class JUnit {
    boolean enabled = true
    String glob = "**/surefire-reports/*.xml"
    boolean retainLongStdout = false
    boolean allowClaimingOfFailedTests = false
    boolean publishTestAttachments = false
}

