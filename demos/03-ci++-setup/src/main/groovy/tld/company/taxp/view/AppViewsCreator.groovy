package tld.company.taxp.view

import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.ViewType
import javaposse.jobdsl.dsl.views.ListView
import javaposse.jobdsl.dsl.views.NestedViewsContext
import tld.company.taxp.model.Project

class AppViewsCreator {
    private final List<Project> projects

    AppViewsCreator(List<Project> projects) {
        this.projects = projects
    }

    def create(JobParent parent) {
        parent.view(type: ViewType.NestedView) {
            name 'Apps'
            views {
                def nestedView = delegate

                def apps = collectProjectAppNames().collate(10)
                apps.each { collatedAppCollection ->
                    createNestedViewForAppCollection(nestedView, collatedAppCollection)
                }
            }
        }
    }

    private List collectProjectAppNames() {
        projects.collect {
            [it.shortName] + it.pipelines.collect { it.id }.findAll()
        }
    }

    private def createNestedViewForAppCollection(NestedViewsContext parentView, List collatedAppCollection) {
        parentView.view(type: ViewType.NestedView) {
            def viewName = "${collatedAppCollection.first().getAt(0)} - ${collatedAppCollection.last().getAt(0)}"
            name viewName
            views {
                def nestedView = delegate

                collatedAppCollection.each { appCollection ->
                    createViewForAppCollection(nestedView, appCollection)
                }
            }
        }
    }

    private def createViewForAppCollection(NestedViewsContext nestedView, List appCollection) {
        nestedView.view(type: ViewType.ListView) {
            name appCollection.first()
            jobs {
                regex "(?i).*(${appCollection.join('|')})_?.*"
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
