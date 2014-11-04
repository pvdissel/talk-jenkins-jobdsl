package tld.company.taxp.testautomation

import tld.company.taxp.model.flow.DeployGroup

interface FlowDeployDsl {
    List<String> collectFor(DeployGroup group)
}
