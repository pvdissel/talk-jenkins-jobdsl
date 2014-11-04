package tld.company.taxp.view

import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.ViewType
import javaposse.jobdsl.dsl.views.ListView

class OpsViewsCreator {

    def create(JobParent parent) {
        parent.view(type: ViewType.NestedView) {
            name 'Ops'
            views {
                view {
                    name 'Flows'
                    jobs {
                        names 'Flow_Deploy_Daily', 'Flow_Deploy_Evening', 'Flow_Deploy_Nightly'
                    }
                    statusFilter(ListView.StatusFilter.ALL)
                    columns {
                        status()
                        weather()
                        name()
                        lastSuccess()
                        lastFailure()
                        lastDuration()
                        buildButton()
                    }
                }

                view {
                    name 'Sprint Hopping'
                    jobs {
                        regex '(?i)SprintHopping_.*'
                    }
                    statusFilter(ListView.StatusFilter.ALL)
                    columns {
                        status()
                        weather()
                        name()
                        lastSuccess()
                        lastFailure()
                        lastDuration()
                        buildButton()
                    }
                }
            }
        }
    }
}
