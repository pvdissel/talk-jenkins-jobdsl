package tld.company.taxp.model

import tld.company.taxp.model.pipeline.XLTestJob

class Project {
    String shortName
    String fullName
    Pipeline[] pipelines
    Flow flow = new Flow()
    XLTestJob xlTest
    def test

    boolean hasXlTestJob() {
        if (xlTest == null)
            false
        else if (!hasDeployJob()) {
            println "Project ${shortName} is wrongly configured: there is no deploy job - yet there is an XL test configuration".warn()
            true
        } else
            true
    }

    boolean hasDeployJob() {
        pipelines.any { it.deploy != null }
    }
}
