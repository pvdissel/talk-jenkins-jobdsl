package tld.company.taxp.model

import tld.company.taxp.model.flow.DeployGroup
import tld.company.taxp.model.flow.DeploySchedule

import static DeploySchedule.daily
import static DeploySchedule.nightly

class Flow {
    boolean enabled = true
    DeploySchedule[] deploy = [daily, nightly]
    DeployGroup deployGroup = DeployGroup.service
}
