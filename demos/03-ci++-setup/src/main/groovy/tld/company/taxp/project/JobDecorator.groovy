package tld.company.taxp.project

import javaposse.jobdsl.dsl.Job
import tld.company.taxp.model.Pipeline
import tld.company.taxp.model.Project

interface JobDecorator {
    def decorateJobFor(Project project, Pipeline pipeline, Job job)

    String getJobNameForPipeline(Project project, Pipeline pipeline)
}
