class DeploymentConfiguration {
    final devServerOld
    final devServer
    final testServer
    final demoServer
    final uatServer
    final prodServer

    final homeDirectory = '/opt/app01'

    DeploymentConfiguration(servers) {
        this.devServerOld = servers['devold'] ? "deployer@${servers['devold']}" : "n/a"
        this.devServer = servers['dev'] ? "deployer@${servers['dev']}" : "n/a"
        this.demoServer = servers['demo'] ? "deployer@${servers['demo']}" : "n/a"
        this.testServer = servers['test'] ? "deployer@${servers['test']}" : "n/a"
        this.uatServer = servers['uat'] ? "deployer@${servers['uat']}" : "n/a"
        this.prodServer = servers['prod'] ? "deployer@${servers['prod']}" : "n/a"
    }
}
