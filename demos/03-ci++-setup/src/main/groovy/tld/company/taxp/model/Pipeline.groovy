package tld.company.taxp.model

import tld.company.taxp.model.pipeline.*

class Pipeline {
    def id
    def type
    Technology technology
    Tool tool
    SCM scm
    Ci ci
    Deploy deploy
    def group = 0
}
