package tld.company.taxp.model.flow

class TypeGroup {
    def id
    def jobNames = []

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        TypeGroup typeGroup = (TypeGroup) o

        if (id != typeGroup.id) return false
        if (jobNames != typeGroup.jobNames) return false

        return true
    }

    int hashCode() {
        int result
        result = (id != null ? id.hashCode() : 0)
        result = 31 * result + (jobNames != null ? jobNames.hashCode() : 0)
        return result
    }

    @Override
    public String toString() {
        return """\
            TypeGroup{
                id=$id,
                jobNames=$jobNames
            }""".stripIndent()
    }
}
