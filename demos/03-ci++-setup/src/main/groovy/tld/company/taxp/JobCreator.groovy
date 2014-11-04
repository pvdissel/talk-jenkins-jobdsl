package tld.company.taxp

import javaposse.jobdsl.dsl.Job
import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.JobType

interface JobCreator {

    String getName()

    JobType getType()

    Job create(JobParent parent)
}
