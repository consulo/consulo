## About

Consulo - multi-language ide. Project was started in 2013 year by forking [IDEA Community Edition](https://github.com/JetBrains/intellij-community).

Main goal - create **open** IDE where you don't need select IDE for different languages. Provide a standard for language implementation inside IDE.

## Contributing

If you can't describe issue, you can use our [forum](https://discuss.consulo.io/), or you can read [contributing guide](https://github.com/consulo/consulo/blob/master/CONTRIBUTING.md)  and report issue at GitHub

## Building & Running

### Build Status

| JVM           | [consulo.io](https://ci.consulo.io) | [consulo.io](https://ci.consulo.io) tests |Travis CI|
| ------------- |:-------------:|--------------:|-----------------:|
| Java 8        | [![Build Status](https://ci.consulo.io/job/commit-check/job/consulo+java8/badge/icon)](https://ci.consulo.io/job/commit-check/job/consulo+java8/) | [![Test Status](https://img.shields.io/jenkins/t/https/ci.consulo.io/job/commit-check/job/consulo+java8.svg)](https://ci.consulo.io/job/commit-check/consulo+java8)| [![Build Status](https://travis-matrix-badges.herokuapp.com/repos/consulo/consulo/branches/master/1)](https://travis-ci.org/consulo/consulo) |
| Java 10       | [![Build Status](https://ci.consulo.io/job/commit-check/job/consulo+java10/badge/icon)](https://ci.consulo.io/job/commit-check/job/consulo+java10/) | [![Test Status](https://img.shields.io/jenkins/t/https/ci.consulo.io/job/commit-check/job/consulo+java10.svg)](https://ci.consulo.io/job/commit-check/consulo+java10)| [![Build Status](https://travis-matrix-badges.herokuapp.com/repos/consulo/consulo/branches/master/3)](https://travis-ci.org/consulo/consulo) |
| Java 11       | [![Build Status](https://ci.consulo.io/job/commit-check/job/consulo+java11/badge/icon)](https://ci.consulo.io/job/commit-check/job/consulo+java11/) | [![Test Status](https://img.shields.io/jenkins/t/https/ci.consulo.io/job/commit-check/job/consulo+java10.svg)](https://ci.consulo.io/job/commit-check/consulo+java11)| [![Build Status](https://travis-matrix-badges.herokuapp.com/repos/consulo/consulo/branches/master/4)](https://travis-ci.org/consulo/consulo) |

First of all, you need those tools:

 * Maven 3.3+
 * JDK 1.8

Then execute from command line:

```sh
mvn package
```

If you want run Consulo from repository
 * as desktop application

   ```sh
    mvn install

    mvn consulo:run -pl:consulo-sandbox-desktop
   ```

 * as web application

   first need build web sandbox
   ```
   mvn package -am -pl consulo:consulo-sandbox-web
   ```

   then need start code server (since we used gwt as frontend)

   ```sh
   cd modules/web/web-ui-impl-client

   mvn -am vaadin:run-codeserver
   ```

   and start web server

   ```sh
   cd modules/web/web-bootstrap

   mvn -am jetty:run
   ```

## Sandbox Projects

 * Profiler API [link](https://github.com/consulo/profiler-sandbox)
 * Diagram support [link](https://github.com/consulo/consulo/tree/master/modules/independent/graph-api)

## Links

* [Contributing Guide](https://github.com/consulo/consulo/blob/master/CONTRIBUTING.md)
* [Download](https://github.com/consulo/consulo/wiki/Downloads)
* [Donate](https://github.com/consulo/consulo/wiki/Donation)
* [Issues](https://github.com/consulo/consulo/issues)
* [Forum](https://discuss.consulo.io/)


## Tools

 *  [YourKit Java Profiler](https://www.yourkit.com/java/profiler) with open source license provided by [YourKit](https://www.yourkit.com/)

    ![](https://www.yourkit.com/images/yklogo.png)

    YourKit supports open source projects with its full-featured  [YourKit Java Profiler](https://www.yourkit.com/java/profiler/) and [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/)
