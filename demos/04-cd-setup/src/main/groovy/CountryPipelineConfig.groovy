abstract class CountryPipelineConfig extends JobConfig {

    final country

    CountryPipelineConfig(map, env) {
        super(String.format("APP01-%s-%s-${env}", map['country'].toUpperCase(), map['branchName'].toUpperCase()), map)
        country = map['country']
    }

    def getBaseUrlFor(env) {
        def prefix = "${country}b2c"
        return "https://${env}.${prefix}.app01.company.tld"
    }
}
