package tld.company.taxp

/**
 * All possible settings
 * You can combine these settings as wanted
 */
enum JobDslSetting {
    DEBUG,   // for additional information, eg. stacktraces
    ALL_LIFECYCLES,  // for doing everything for all LifeCycle types,
    SANDBOX, // for excluding all external publishers (eg. artifactory, tagging, emailing)
    MATRIX,  // for creating matrix job variants for all types
    CANARY,  // for running a pre-defined subset of applications, focussed on covering one project for each type
    DISABLED, // all jobs being DISABLED
    TESTRUN //Creates all jobs required to do a TestRun
}
