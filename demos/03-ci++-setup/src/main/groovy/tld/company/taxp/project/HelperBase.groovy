package tld.company.taxp.project

import tld.company.taxp.DescriptionHelper
import tld.company.taxp.JobDslSetting
import tld.company.taxp.OpExResolver
import tld.company.taxp.model.pipeline.ci.LifeCycle

abstract class HelperBase {
    LifeCycle lifeCycle
    List<JobDslSetting> settings
    List messages

    OpExResolver opExResolver
    DescriptionHelper descriptionHelper

    public HelperBase(LifeCycle lifeCycle, List<JobDslSetting> settings, List messages, OpExResolver opExResolver, DescriptionHelper descriptionHelper) {
        this.lifeCycle = lifeCycle
        this.settings = settings
        this.messages = messages
        this.opExResolver = opExResolver
        this.descriptionHelper = descriptionHelper
    }
}
