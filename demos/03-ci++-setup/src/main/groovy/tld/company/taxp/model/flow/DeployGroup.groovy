package tld.company.taxp.model.flow

enum DeployGroup {
    preRelease('PreRelease'),
    bosDB('BOS_DB'),
    coreDB('CoreDBs'),
    frontendApp('FE Apps'),
    service('Services'),
    tool('Tools'),
    backend('Backends');

    def title

    DeployGroup(title) {
        this.title = title
    }
}
