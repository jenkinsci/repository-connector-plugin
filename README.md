## Repository Connector Plugin

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/repository-connector.svg)](https://plugins.jenkins.io/repository-connector)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/repository-connector-plugin.svg?label=changelog)](https://github.com/jenkinsci/repository-connector-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/repository-connector.svg?color=blue)](https://plugins.jenkins.io/repository-connector)
[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/repository-connector-plugin/master)](https://ci.jenkins.io/job/Plugins/job/repository-connector-plugin/job/master/)

Jenkins [plugin](https://plugins.jenkins.io/) which allows for retrieval or deployment of artifacts to/from a repository such
as [Nexus](https://www.sonatype.com/nexus/repository-oss) or [Artifactory](https://jfrog.com/artifactory/)
and static servers implementing the [repository layout](https://cwiki.apache.org/confluence/display/MAVENOLD/Repository+Layout+-+Final).

### 2.0.0 Changes

With the release of version `2.0.0`  come the following changes, some of which may require jobs to be reconfigured, although none
should cease to work outright. :crossed_fingers:

* Jenkins >= 2.222.4 required
* Artifact deployment re-enabled
* Version resolution build parameters are required to have a name (possible breaking change)
* Separated repository and transfer logging into separate options
* Artifact resolution 'fail on error' can be specified per artifact

##### Credentials

Credentials are now stored in and provided by the [Credentials](https://plugins.jenkins.io/credentials/) plugin. Any previously stored
username/password combinations stored by the plugin configuration are migrated upon startup.

##### Aether Resolution / Deployment

The plugin has been updated to use the same underlying [aether library](https://github.com/apache/maven-resolver) as `maven` itself.
Artifact resolution policies are configured as part of the repository now instead of at the job level. It is also possible to specify
individual deployment endpoints for `snapshots` and `releases` if you are using a repository manager.

During artifact resolution, only the artifact itself will be resolved. Prior versions of the plugin would attempt to resolve all
transitive dependencies as well, but not install them into the local repository. This could lead to issues where resolution of the
artifact itself would succeed but still result in failure because a dependency could not be found.

### Configuration-as-Code

[JCasC](https://plugins.jenkins.io/configuration-as-code) is fully supported. Please visit the please visit the
[wiki](https://github.com/jenkinsci/repository-connector-plugin/wiki/Configuration) for additional details.

### Job DSL

The [jenkins-job-dsl](https://plugins.jenkins.io/job-dsl/) is fully supported using the [dynamic dsl](https://github.com/jenkinsci/job-dsl-plugin/wiki/Dynamic-DSL).
As of `2.0.0`, the built-in dsl provided by the `job-dsl` plugin will no longer work.

### Plugin Configuration

The plugin comes pre-configured with an entry for [maven central](https://repo1.maven.org/maven2) that allows for immediate use.
Select the `Maven Artifact Resolver` option from the `Build Steps` dropdown and and configure accordingly.

For more instructions, please visit the [wiki](https://github.com/jenkinsci/repository-connector-plugin/wiki).

### Other

#### Maven Central Snapshots

If you are looking to resolve `SNAPSHOT` versions from artifacts you would find on [maven central](https://repo1.maven.org/maven2), add
a repository configuration that uses the following endpoint, with the `release` policy disabled.

```
https://oss.sonatype.org/content/repositories/snapshots
```

#### I8N

Google translate was used for all German :de: translations, apologies for anything that is horrendously off. Fixes welcome!
