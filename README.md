Cultivating the Jenkins job jungle with Groovy
===================

This presentation repository contains:
- `demos` directory containing demo projects for different situations.
  See the README.md file in the specific demo project directory for details about that setup
- `2014-11-05 - JFall` directory containing the slides in PDF format, as given at JFall conference in The Netherlands @ 2014-11-05
- `jenkins` script to start a Docker container based on a specific demo project.

Requirements
------------

To execute the demos, you need:
- JDK 6+
- Docker


Usage
-----

- For a global overview of all demos, you can open up the `build.gradle` in your favorite IDE
- If you want to start all demos, you can execute the `startDemos` script. The output explains what it does and where the demos are made available. Use the `stopDemos` script to stop all known running demos
- Each demo has its own symlink to the `jenkins` script, as the name of the executing command is used as the name of the Docker container. This makes it possible to run multiple demos at the same time
- When you execute the `jenkins` script without arguments, you will get the script usage information

### OSX or Docker with sudo?

If you're on OSX or cannot execute `docker` without sudo, you can change the `dockerCmd` variable in the `jenkins` script to the correct command to docker.

### Note

As Docker executes as `root`, during the execution of the `jenkins` script you'll be asked to provide your password to become `root`.
All files/directories created during the execution of the `jenkins` script, are located within this current directory as ".<directory>". `jenkins` has a command to clean it all up

Links to resources
------------------

- [Gradle Jenkins JobDSL plugin](https://github.com/pvdissel/gradle-jenkins-jobdsl)
- [Jenkins JobDSL plugin wiki](https://github.com/jenkinsci/job-dsl-plugin/wiki)
- [Continuous Delivery related info](http://continuousdelivery.com)
- GradleSummit2014 - Managing Jenkins With Gradle - Gary Hale
  [video](https://www.youtube.com/watch?v=FGs6_D8ul60)
  [slides](https://speakerdeck.com/ghale/managing-jenkins-with-gradle)
  [Gradle Jenkins Plugin](https://github.com/ghale/gradle-jenkins-plugin)
- Next-gen Continuous Delivery [Pipeline.cd](http://pipeline.cd)

