demo-03 - Large with flows
==========================

This is a CI++ setup as we have at bol.com, with views per application and per team.

Each application has:
- a CI and Deploy job for each environment (TEST, ACC, XPRPRO) and each part of the application
  (API, APP, DB)
  - where the CI jobs are triggered on each commit
  - and the deploy jobs are triggered manually or by one of the scheduled Flow_Deploy jobs
- a `Flow_Deploy_{APP}` job, which uses the Jenkins Build Flow plugin DSL to define in what
  order the different parts of that application need to be deployed.
  eg. First the DB, then the APP, then the test automation
- a `XL_Test_{APP}_FitNesse` job to run the test automation of this specific application

Then there are three scheduled deployment schedules for the TEST environment:
- Daily, which is every workday around 12:00
- Evening, which is every workday around 18:00
- Nightly, which is daily around midnight

And the process of moving from TEST to ACC to XPRPRO, we call "Sprint Hopping".
This is basicly release branching.
Currently the SprintHopping cycle is the same for most of the applications, but
they are starting to move to a more Continuous Delivery cycle.
To support this SprintHopping, we have jobs with names starting with `SprintHopping_` which
use a combination of the Build Flow plugin DSL and scripts with the System Groovy plugin.

Configuration
-------------

The `appsconfig` directory contains json configuration files, one for each application.

By means of pull/merge-requests, development team members can create/change their
application Jenkins configuration and/or add features to the supporting code.
