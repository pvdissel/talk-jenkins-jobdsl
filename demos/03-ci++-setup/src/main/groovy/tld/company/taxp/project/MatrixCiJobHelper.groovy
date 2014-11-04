package tld.company.taxp.project

import javaposse.jobdsl.dsl.Job
import tld.company.taxp.DescriptionHelper
import tld.company.taxp.JobDslSetting
import tld.company.taxp.JobPluginConfigHelper
import tld.company.taxp.OpExResolver
import tld.company.taxp.model.Pipeline
import tld.company.taxp.model.Project
import tld.company.taxp.model.pipeline.ci.LifeCycle

class MatrixCiJobHelper extends HelperBase implements JobDecorator {

    MatrixCiJobHelper(LifeCycle lifeCycle, List<JobDslSetting> settings, List messages, OpExResolver opExResolver, DescriptionHelper descriptionHelper) {
        super(lifeCycle, settings, messages, opExResolver, descriptionHelper)
    }

    def decorateJobFor(Project project, Pipeline pipeline, Job job) {
        switch (pipeline.type) {
            case ~/(?i)app/:
            case ~/(?i)api/:
                decorateJavaJob(pipeline, job)
                break
            case ~/(?i)db/:
//                decorateDbJob(project, pipeline, messages)
                break
            default:
                throw new IllegalStateException("Unknown pipeline type [${pipeline.type}]")
        }
        return job
    }

    def decorateJavaJob(Pipeline pipeline, Job job) {
        job.with {
            axes {
                jdk(JobPluginConfigHelper.supportedJdkVersions)
            }
        }
    }

    @Override
    public String getJobNameForPipeline(Project project, Pipeline pipeline) {
        return null;
    }
}
