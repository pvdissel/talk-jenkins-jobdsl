package tld.company.taxp

import tld.company.taxp.model.OpEx

public interface OpExResolver {
    /**
     * @param shortName App shortname
     * @return null if no project found
     */
    OpEx getForProject(String shortName)
}
