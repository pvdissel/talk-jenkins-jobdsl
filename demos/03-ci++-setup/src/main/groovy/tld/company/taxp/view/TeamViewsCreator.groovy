package tld.company.taxp.view

import javaposse.jobdsl.dsl.JobParent
import javaposse.jobdsl.dsl.ViewType
import javaposse.jobdsl.dsl.views.ListView
import javaposse.jobdsl.dsl.views.NestedViewsContext
import tld.company.taxp.OpExResolver
import tld.company.taxp.model.OpEx
import tld.company.taxp.model.Project

class TeamViewsCreator {
    private final List<Project> projects
    OpExResolver resolver

    TeamViewsCreator(List<Project> projects, OpExResolver resolver) {
        this.projects = projects
        this.resolver = resolver
    }

    def create(JobParent parent) {
        def teams = mapAppsToTeams(collectProjectAppNames())

        parent.view(type: ViewType.NestedView) {
            name 'Teams'
            views {
                def nestedView = delegate
                createTeamsCollatedView(nestedView, teams)
            }
        }
    }

    private def createTeamsCollatedView(NestedViewsContext parentView, Map teams) {
        def collatedTeams = collatedTeams(teams)

        collatedTeams.each { teamSectionName, sectionTeams ->
            if (sectionTeams.size() > 1) {
                parentView.view(type: ViewType.NestedView) {
                    name "Teams ${teamSectionName}"
                    views {
                        def nestedView = delegate
                        sectionTeams.each { teamName, teamOpEx ->
                            createViewForTeamCollection(nestedView, teamName, teamOpEx)
                        }
                    }
                }
            } else {
                sectionTeams.each { teamName, teamOpEx ->
                    createViewForTeamCollection(parentView, teamName, teamOpEx)
                }
            }
        }
    }

    private def createViewForTeamCollection(NestedViewsContext nestedView, String teamName, List<OpEx> teamOpex) {
        nestedView.view(type: ViewType.ListView) {
            name "Team ${teamName}"
            jobs {
                regex "(?i).*(${teamOpex.collect { it.applicationShortName }.join('|')})_?.*"
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

    def mapAppsToTeams(List<String> appNames) {
        appNames.collect {
            resolver.getForProject(it)
        }.findAll().collect { opEx ->
            opEx.ldapTeamNames.collect { teamName ->
                teamName ? [team: cleanTeamName(teamName), opex: opEx] : null
            }.flatten().findAll()
        }.flatten().groupBy {
            it.team
        }.findAll {
            it.key
        }.entrySet().sort { a, b ->
            def n1 = (a.key =~ /\d+/)[-1] as Integer
            def n2 = (b.key =~ /\d+/)[-1] as Integer

            def s1 = a.key.replaceAll(/\d+$/, '').trim()
            def s2 = b.key.replaceAll(/\d+$/, '').trim()

            return n1 == n2 ? s1 <=> s2 : n1 <=> n2
        }.collectEntries {
            [(it.key): it.value*.opex]
        }
    }

    String cleanTeamName(String teamName) {
        teamName.replaceAll(/(?i)team/, '')
                .replaceAll(/(-| )+/, ' ')
                .toLowerCase()
                .trim()
    }

    List collectProjectAppNames() {
        projects.collect {
            it.shortName
        }.findAll()
    }

    def collatedTeams(Map teams) {
        teams.groupBy { team, opex ->
            (team =~ /\d+/)[-1]
        }.entrySet().sort {
            a, b -> a.key <=> b.key
        }.collectEntries {
            [(it.key): it.value]
        }
    }
}
