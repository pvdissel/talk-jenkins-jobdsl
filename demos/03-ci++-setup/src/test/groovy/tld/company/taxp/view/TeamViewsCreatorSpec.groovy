package tld.company.taxp.view

import javaposse.jobdsl.dsl.JobManagement
import javaposse.jobdsl.dsl.JobParent
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll
import tld.company.taxp.AppConfig
import tld.company.taxp.OpExResolver
import tld.company.taxp.model.OpEx

@Ignore
class TeamViewsCreatorSpec extends Specification {
    def log = null
    def settings = []
    def configs
    JobParent parent = Spy(JobParent)
    JobManagement jobManagement = Mock(JobManagement)
    TeamViewsCreator creator

    void setup() {
        parent.jm = jobManagement
        configs = new AppConfig(log, settings).readConfigs(new File(AppConfig.TEST_CONFIGS_PATH))
        assert configs
        println configs
        creator = new TeamViewsCreator(configs, createStaticOpExResolver())
    }

    def "See if any error occurs during the generation of the views"() {
        expect:
        creator.create(parent)
    }

    def "Can get map of OpEx of apps per team"() {
        def appsToTeams = creator.mapAppsToTeams(creator.collectProjectAppNames())

        expect:
        appsToTeams == [
                '1' : [
                        new OpEx(
                                applicationShortName: 'AAA',
                                teamEmails: ['team1@company', 'team1@company', 'team1@company'],
                                ldapTeamNames: ['team1', 'team1b', 'team32'],
                        ),
                        new OpEx(
                                applicationShortName: 'SCA',
                                teamEmails: ['team1@company', 'team1b@company', 'team1c@company'],
                                ldapTeamNames: ['team 1', 'team 1b', 'team 1c'],
                        )
                ],
                '1b': [
                        new OpEx(
                                applicationShortName: 'AAA',
                                teamEmails: ['team1@company', 'team1@company', 'team1@company'],
                                ldapTeamNames: ['team1', 'team1b', 'team32'],
                        ),
                        new OpEx(
                                applicationShortName: 'SCA',
                                teamEmails: ['team1@company', 'team1b@company', 'team1c@company'],
                                ldapTeamNames: ['team 1', 'team 1b', 'team 1c'],
                        )
                ],
                '1c': [
                        new OpEx(
                                applicationShortName: 'SCA',
                                teamEmails: ['team1@company', 'team1b@company', 'team1c@company'],
                                ldapTeamNames: ['team 1', 'team 1b', 'team 1c'],
                        )
                ],
                '24': [
                        new OpEx(
                                applicationShortName: 'DBC',
                                teamEmails: ['team24@company'],
                                ldapTeamNames: ['team24'],
                        )
                ],
                '32': [
                        new OpEx(
                                applicationShortName: 'AAA',
                                teamEmails: ['team1@company', 'team1@company', 'team1@company'],
                                ldapTeamNames: ['team1', 'team1b', 'team32'],
                        )
                ]
        ]
    }

    def 'Can get a collated map of OpEx of apps per team'() {
        def teams = creator.mapAppsToTeams(creator.collectProjectAppNames())
        def collatedTeams = creator.collatedTeams(teams)

        expect:
        collatedTeams == [
                '1' : [
                        '1' : [
                                new OpEx(
                                        applicationShortName: 'AAA',
                                        teamEmails: ['team1@company', 'team1@company', 'team1@company'],
                                        ldapTeamNames: ['team1', 'team1b', 'team32'],
                                ),
                                new OpEx(
                                        applicationShortName: 'SCA',
                                        teamEmails: ['team1@company', 'team1b@company', 'team1c@company'],
                                        ldapTeamNames: ['team 1', 'team 1b', 'team 1c'],
                                )
                        ],
                        '1b': [
                                new OpEx(
                                        applicationShortName: 'AAA',
                                        teamEmails: ['team1@company', 'team1@company', 'team1@company'],
                                        ldapTeamNames: ['team1', 'team1b', 'team32'],
                                ),
                                new OpEx(
                                        applicationShortName: 'SCA',
                                        teamEmails: ['team1@company', 'team1b@company', 'team1c@company'],
                                        ldapTeamNames: ['team 1', 'team 1b', 'team 1c'],
                                )
                        ],
                        '1c': [
                                new OpEx(
                                        applicationShortName: 'SCA',
                                        teamEmails: ['team1@company', 'team1b@company', 'team1c@company'],
                                        ldapTeamNames: ['team 1', 'team 1b', 'team 1c'],
                                )
                        ],
                ],
                '24': [
                        '24': [
                                new OpEx(
                                        applicationShortName: 'DBC',
                                        teamEmails: ['team24@company'],
                                        ldapTeamNames: ['team24'],
                                )
                        ],
                ],
                '32': [
                        '32': [
                                new OpEx(
                                        applicationShortName: 'AAA',
                                        teamEmails: ['team1@company', 'team1@company', 'team1@company'],
                                        ldapTeamNames: ['team1', 'team1b', 'team32'],
                                )
                        ],
                ]
        ]
    }

    @Unroll
    def "Team name [#teamName] should be cleaned to [#expectedTeamName]"() {
        when:
        def cleanTeamName = creator.cleanTeamName(teamName)

        then:
        cleanTeamName == expectedTeamName

        where:
        teamName   || expectedTeamName
        'team 1'   || '1'
        'team 1a'  || '1a'
        'TEAM 1A'  || '1a'
        'TEAM  1A' || '1a'
        'TEAM-1A'  || '1a'
        'TEAM1A'   || '1a'
    }

    OpExResolver createStaticOpExResolver() {
        new OpExResolver() {
            def data = [
                    'AAA': new OpEx(
                            applicationShortName: 'AAA',
                            teamEmails: ['team1@company', 'team1@company', 'team1@company'],
                            ldapTeamNames: ['team1', 'team1b', 'team32'],
                    ),
                    'DBC': new OpEx(
                            applicationShortName: 'DBC',
                            teamEmails: ['team24@company'],
                            ldapTeamNames: ['team24'],
                    ),
                    'SCA': new OpEx(
                            applicationShortName: 'SCA',
                            teamEmails: ['team1@company', 'team1b@company', 'team1c@company'],
                            ldapTeamNames: ['team 1', 'team 1b', 'team 1c'],
                    ),
            ]

            @Override
            OpEx getForProject(String shortName) {
                data[shortName]
            }
        }
    }
}
