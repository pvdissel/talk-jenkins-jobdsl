package tld.company.taxp.model.flow

class OrderGroup {
    def id
    List<TypeGroup> typeGroups = []

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        OrderGroup that = (OrderGroup) o

        if (id != that.id) return false
        if (typeGroups != that.typeGroups) return false

        return true
    }

    int hashCode() {
        int result
        result = (id != null ? id.hashCode() : 0)
        result = 31 * result + (typeGroups != null ? typeGroups.hashCode() : 0)
        return result
    }

    @Override
    public String toString() {
        return """\
            OrderGroup{
                id=$id,
                typeGroups=$typeGroups
            }""".stripIndent()
    }
}
