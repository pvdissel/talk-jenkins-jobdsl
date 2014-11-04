package tld.company.taxp.model

import groovy.transform.Canonical

@Canonical
class OpEx {
    String applicationShortName
    String[] ldapTeamNames
    String[] teamEmails
}
