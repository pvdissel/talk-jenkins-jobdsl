class TestLibJobConfig extends JobConfig {

    final libName

    TestLibJobConfig(map) {
        super(String.format('TEST-DSL-%s-%s', map['branchName'].substring(0, 3).toUpperCase(), map['libName'].toUpperCase()), map)
        libName = map['libName']
    }
}

