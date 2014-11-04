class LibJobConfig extends JobConfig {

    final libName

    LibJobConfig(map) {
        super(String.format('APP01-LIB-%s-%s', map['branchName'].substring(0, 3).toUpperCase(), map['libName'].toUpperCase()), map)
        libName = map['libName']
    }
}

