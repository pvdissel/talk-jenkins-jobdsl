package tld.company.taxp

import tld.company.taxp.model.OpEx

class StaticDataOpExResolver implements OpExResolver {
    def data = [
            'AAA' : new OpEx(
                    applicationShortName: 'AAA',
                    ldapTeamNames: ['Team 5c'],
                    teamEmails: ['Team5c@company.tld']),
            'AAB' : new OpEx(
                    applicationShortName: 'AAB',
                    ldapTeamNames: ['Team2c', 'Team2b'],
                    teamEmails: ['Team2c@company.tld', 'Team2b@company.tld']),
            'ABC' : new OpEx(
                    applicationShortName: 'ABC',
                    ldapTeamNames: ['Team2b', 'Team2c', 'Team13'],
                    teamEmails: ['Team2c@company.tld']),
            'ABD' : new OpEx(
                    applicationShortName: 'ABD',
                    ldapTeamNames: ['Team12b'],
                    teamEmails: ['team12b@company.tld']),
            'ACB' : new OpEx(
                    applicationShortName: 'ACB',
                    ldapTeamNames: ['Team 8a'],
                    teamEmails: ['Team8a@company.tld']),
            'ACD' : new OpEx(
                    applicationShortName: 'ACD',
                    ldapTeamNames: ['Team20'],
                    teamEmails: ['team20@company.tld']),
            'ADD' : new OpEx(
                    applicationShortName: 'ADD',
                    ldapTeamNames: ['Team 5a', 'Team 5c'],
                    teamEmails: ['Team5a@company.tld', 'team5c@company.tld']),
            'ADE' : new OpEx(
                    applicationShortName: 'ADE',
                    ldapTeamNames: ['Team 32'],
                    teamEmails: ['Team32@company.tld']),
            'AEF' : new OpEx(
                    applicationShortName: 'AEF',
                    ldapTeamNames: ['Team 4', 'Team 32'],
                    teamEmails: ['team4@company.tld']),
            'AFE' : new OpEx(
                    applicationShortName: 'AFE',
                    ldapTeamNames: ['Team2b', 'Team2c', 'Team 3', 'Team 6', 'Team1c', 'Team1b', 'Team1d', 'Team 5c', 'Team 5b', 'Team 5a'],
                    teamEmails: ['Team2b@company.tld', 'Team2c@company.tld', 'Team3@company.tld', 'Team5a@company.tld', 'Team5b@company.tld', 'Team_6@company.tld']),
            'BAA' : new OpEx(
                    applicationShortName: 'BAA',
                    ldapTeamNames: [''],
                    teamEmails: ['']),
            'BBB' : new OpEx(
                    applicationShortName: 'BBB',
                    ldapTeamNames: ['Team-5b'],
                    teamEmails: ['team5b@company.tld']),
            'CABC': new OpEx(
                    applicationShortName: 'CABC',
                    ldapTeamNames: ['Team2b'],
                    teamEmails: ['Team2b@company.tld']),
            'CBB' : new OpEx(
                    applicationShortName: 'CBB',
                    ldapTeamNames: ['Team 8b', 'Team 3', 'Team 32'],
                    teamEmails: ['Team8b@company.tld', 'team3@company.tld']),
            'CCCC': new OpEx(
                    applicationShortName: 'CCCC',
                    ldapTeamNames: ['Team 2'],
                    teamEmails: ['team2@company.tld']),
            'CDD' : new OpEx(
                    applicationShortName: 'CDD',
                    ldapTeamNames: ['Team 2'],
                    teamEmails: ['team2@company.tld']),
            'CDE' : new OpEx(
                    applicationShortName: 'CDE',
                    ldapTeamNames: ['Team1d', 'Team1D', 'Team 1d', 'Team 1D'],
                    teamEmails: ['team1d@company.tld']),
            'DAA' : new OpEx(
                    applicationShortName: 'DAA',
                    ldapTeamNames: ['Team1c', 'Team12'],
                    teamEmails: ['Team1c@company.tld']),
            'DBA' : new OpEx(
                    applicationShortName: 'DBA',
                    ldapTeamNames: [''],
                    teamEmails: ['']),
            'DABC': new OpEx(
                    applicationShortName: 'DABC',
                    ldapTeamNames: ['Team 4', 'Team 42', 'Team 6'],
                    teamEmails: ['Team4@company.tld', 'Team_42@company.tld', 'Team6@company.tld']),
            'DBCD': new OpEx(
                    applicationShortName: 'DBCD',
                    ldapTeamNames: ['Team1c', 'Team1d', 'Team 5c', 'Team 4'],
                    teamEmails: ['Team1c@company.tld', 'Team1d@company.tld', 'Team5c@company.tld']),
            'DDA' : new OpEx(
                    applicationShortName: 'DDA',
                    ldapTeamNames: ['team5d'],
                    teamEmails: ['team5d@company.tld']),
            'EABC': new OpEx(
                    applicationShortName: 'EABC',
                    ldapTeamNames: ['Team12'],
                    teamEmails: ['team12@company.tld']),
            'EBC' : new OpEx(
                    applicationShortName: 'EBC',
                    ldapTeamNames: ['Team2b'],
                    teamEmails: ['Team2b@company.tld']),
            'ECB' : new OpEx(
                    applicationShortName: 'ECB',
                    ldapTeamNames: ['Team2b', 'Team2c', 'Team13'],
                    teamEmails: ['gkessels@company.tld']),
            'ECD' : new OpEx(
                    applicationShortName: 'ECD',
                    ldapTeamNames: ['Team 8b'],
                    teamEmails: ['Team8b@company.tld']),
            'EDA' : new OpEx(
                    applicationShortName: 'EDA',
                    ldapTeamNames: ['Team 8b', 'Team 8a'],
                    teamEmails: ['team8b@company.tld', 'team8a@company.tld']),
            'EDF' : new OpEx(
                    applicationShortName: 'EDF',
                    ldapTeamNames: ['Team 42'],
                    teamEmails: ['Team_42@company.tld']),
            'FAA' : new OpEx(
                    applicationShortName: 'FAA',
                    ldapTeamNames: ['Team 5a', 'Team 5c', 'Team 5b'],
                    teamEmails: ['team5@company.tld']),
            'FBB' : new OpEx(
                    applicationShortName: 'FBB',
                    ldapTeamNames: ['Team 4', 'Team 5c'],
                    teamEmails: ['team4@company.tld']),
            'GAA' : new OpEx(
                    applicationShortName: 'GAA',
                    ldapTeamNames: ['Team1c'],
                    teamEmails: ['Team1c@company.tld']),
            'IAA' : new OpEx(
                    applicationShortName: 'IAA',
                    ldapTeamNames: ['Team 4'],
                    teamEmails: ['Team4@company.tld']),
            'IBB' : new OpEx(
                    applicationShortName: 'IBB',
                    ldapTeamNames: ['Team 5c'],
                    teamEmails: ['Team5c@company.tld']),
            'ICC' : new OpEx(
                    applicationShortName: 'ICC',
                    ldapTeamNames: ['Team 32'],
                    teamEmails: ['Team32@company.tld']),
            'LAA' : new OpEx(
                    applicationShortName: 'LAA',
                    ldapTeamNames: ['Team2c'],
                    teamEmails: ['Team2c@company.tld']),
            'LBB' : new OpEx(
                    applicationShortName: 'LBB',
                    ldapTeamNames: ['Team 15', 'Team 5a', 'Team 6', 'Team 5b', 'Team 5c'],
                    teamEmails: ['Team5a@company.tld', 'Team6@company.tld', 'Team5b@company.tld']),
            'LCC' : new OpEx(
                    applicationShortName: 'LCC',
                    ldapTeamNames: [''],
                    teamEmails: ['']),
            'MAA' : new OpEx(
                    applicationShortName: 'MAA',
                    ldapTeamNames: ['Team 2'],
                    teamEmails: ['team2@company.tld']),
            'MBB' : new OpEx(
                    applicationShortName: 'MBB',
                    ldapTeamNames: ['Team 51'],
                    teamEmails: ['Team51@company.tld']),
            'OAA' : new OpEx(
                    applicationShortName: 'OAA',
                    ldapTeamNames: ['Team 8b', 'Team 8a'],
                    teamEmails: ['team8b@company.tld', 'team8a@company.tld']),
            'OBB' : new OpEx(
                    applicationShortName: 'OBB',
                    ldapTeamNames: ['Team 51'],
                    teamEmails: ['Team51@company.tld']),
            'OCC' : new OpEx(
                    applicationShortName: 'OCC',
                    ldapTeamNames: ['Team 5a', 'Team 5c', 'Team 8a'],
                    teamEmails: ['Team5a@company.tld']),
            'ODD' : new OpEx(
                    applicationShortName: 'ODD',
                    ldapTeamNames: ['Team 34'],
                    teamEmails: ['team34@company.tld']),
            'PAA' : new OpEx(
                    applicationShortName: 'PAA',
                    ldapTeamNames: ['Team 2'],
                    teamEmails: ['Team2@company.tld']),
            'PBB' : new OpEx(
                    applicationShortName: 'PBB',
                    ldapTeamNames: ['Team 2'],
                    teamEmails: ['team2@company.tld']),
            'PCC' : new OpEx(
                    applicationShortName: 'PCC',
                    ldapTeamNames: ['Team1d', 'Team1b', 'Team1W'],
                    teamEmails: ['team1d@company.tld', 'team1b@company.tld', 'Team1W@company.tld']),
            'PABC': new OpEx(
                    applicationShortName: 'PABC',
                    ldapTeamNames: ['Team1d', 'Team1W'],
                    teamEmails: ['team1d@company.tld', 'Team1W@company.tld']),
            'PEE' : new OpEx(
                    applicationShortName: 'PEE',
                    ldapTeamNames: ['Team 2'],
                    teamEmails: ['team2@company.tld']),
            'PFF' : new OpEx(
                    applicationShortName: 'PFF',
                    ldapTeamNames: ['Team88'],
                    teamEmails: ['team88@company.tld']),
            'PGG' : new OpEx(
                    applicationShortName: 'PGG',
                    ldapTeamNames: ['Team1c', 'Team1d', 'Team 5c', 'Team 32', 'Team 8a', 'Team 8 c'],
                    teamEmails: ['team1d@company.tld']),
            'PHH' : new OpEx(
                    applicationShortName: 'PHH',
                    ldapTeamNames: ['Team 2'],
                    teamEmails: ['team2@company.tld']),
            'PJJ' : new OpEx(
                    applicationShortName: 'PJJ',
                    ldapTeamNames: ['Team 4', 'Team 6', 'Team 32'],
                    teamEmails: ['Team4@company.tld', 'Team_6@company.tld']),
            'PABC': new OpEx(
                    applicationShortName: 'PABC',
                    ldapTeamNames: ['Team 2'],
                    teamEmails: ['team2@company.tld']),
            'RAA' : new OpEx(
                    applicationShortName: 'RAA',
                    ldapTeamNames: ['Team1c'],
                    teamEmails: ['Team1c@company.tld']),
            'RBBA': new OpEx(
                    applicationShortName: 'RBBA',
                    ldapTeamNames: ['Team 2'],
                    teamEmails: ['Team2@company.tld']),
            'RBA' : new OpEx(
                    applicationShortName: 'RBA',
                    ldapTeamNames: ['Team14'],
                    teamEmails: ['team14@company.tld']),
            'RCC' : new OpEx(
                    applicationShortName: 'RCC',
                    ldapTeamNames: ['Team 3'],
                    teamEmails: ['team3@company.tld']),
            'RDD' : new OpEx(
                    applicationShortName: 'RDD',
                    ldapTeamNames: ['Team 5c'],
                    teamEmails: ['Team5c@company.tld']),
            'SAA' : new OpEx(
                    applicationShortName: 'SAA',
                    ldapTeamNames: ['Team 8b', 'Team 8a', 'Team 32'],
                    teamEmails: ['Team8b@company.tld', 'Team8a@company.tld']),
            'SBA' : new OpEx(
                    applicationShortName: 'SBA',
                    ldapTeamNames: ['Team 3'],
                    teamEmails: ['team3@company.tld']),
            'SBB' : new OpEx(
                    applicationShortName: 'SBB',
                    ldapTeamNames: ['Team 5b', 'Team 5a', 'Team 5c'],
                    teamEmails: ['Team5b@company.tld']),
            'SCA' : new OpEx(
                    applicationShortName: 'SCA',
                    ldapTeamNames: ['Team2b', 'Team2c'],
                    teamEmails: ['team2b@company.tld', 'team2c@company.tld']),
            'SCC' : new OpEx(
                    applicationShortName: 'SCC',
                    ldapTeamNames: [''],
                    teamEmails: ['']),
            'SDA' : new OpEx(
                    applicationShortName: 'SDA',
                    ldapTeamNames: ['Team2b', 'Team2c'],
                    teamEmails: ['Team2b@company.tld', 'Team2c@company.tld']),
            'SDB' : new OpEx(
                    applicationShortName: 'SDB',
                    ldapTeamNames: ['Team1b'],
                    teamEmails: ['team1b@company.tld']),
            'SABC': new OpEx(
                    applicationShortName: 'SABC',
                    ldapTeamNames: ['Team2c', 'Team2b'],
                    teamEmails: ['Team2c@company.tld', 'Team2b@company.tld']),
            'SACB': new OpEx(
                    applicationShortName: 'SACB',
                    ldapTeamNames: ['Team2c'],
                    teamEmails: ['Team2c@company.tld']),
            'TAA' : new OpEx(
                    applicationShortName: 'TAA',
                    ldapTeamNames: ['Team1c', 'Team 1 s'],
                    teamEmails: ['Team1c@company.tld', 'Team1s@company.tld']),
            'TBB' : new OpEx(
                    applicationShortName: 'TBB',
                    ldapTeamNames: [''],
                    teamEmails: ['']),
            'TCC' : new OpEx(
                    applicationShortName: 'TCC',
                    ldapTeamNames: ['Team2c'],
                    teamEmails: ['Team2b@company.tld', 'Team2c@company.tld', 'Team4@company.tld', 'team34@company.tld']),
            'TABC': new OpEx(
                    applicationShortName: 'TABC',
                    ldapTeamNames: [''],
                    teamEmails: ['']),
    ]

    @Override
    OpEx getForProject(String shortName) {
        data[shortName]
    }
}
