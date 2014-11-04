package tld.company.taxp

import tld.company.taxp.model.OpEx

class LandscapeOpExResolver implements OpExResolver {
    private def appData

    @Override
    OpEx getForProject(String shortName) {
        if (!appData) {
            appData = fetchAppData()
        }

        def application = appData.find { it["Name"] == shortName }
        if (!application) {
            return null
        }
        String[] ldapTeamNames = splitAndTrim(fetchAttributeValue(application, 'ldapTeamName'))
        String[] teamEmails = splitAndTrim(fetchAttributeValue(application, 'teamEmail'))
        return new OpEx(
                applicationShortName: shortName,
                teamEmails: teamEmails,
                ldapTeamNames: ldapTeamNames
        )
    }

    private def splitAndTrim(def value) {
        return value.toString().split(',').collect { it.trim() }
    }

    private def fetchAttributeValue(application, attributeKey) {
        def attributes = application["Attribute"]
        def attribute = attributes.find { it["Name"] == attributeKey }
        return attribute["Value"]
    }

    private def fetchAppData() {
        def apiCallResults = new URL("https://landscape.dev.company.tld/applications").getText(requestProperties: [Accept: 'application/xml'])
        def xml = new XmlSlurper().parseText(apiCallResults)
        return xml["Application"]
    }
}
