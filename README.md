## About

Consulo - multi-language ide. Project was started in year 2013 by forking [IDEA Community Edition](https://github.com/JetBrains/intellij-community).

Main goal - to create an **open** IDE, where you don't need to select an IDE for different languages. Instead, provide a standard for language implementation inside the IDE.

## Contributing

If you can't describe the issue, you can use our [forum](https://github.com/orgs/consulo/discussions/), or you can read the [contributing guide](https://github.com/consulo/consulo/blob/master/CONTRIBUTING.md) and report the issue at GitHub

## Building & Running

### Build Status

| JVM           | Github Actions|
| ------------- |-----------------:|
| Java 21       | ![jdk21](https://github.com/consulo/consulo/workflows/jdk21/badge.svg) |
| Java 22       | ![jdk22](https://github.com/consulo/consulo/workflows/jdk22/badge.svg) |
| Java 23       | ![jdk23](https://github.com/consulo/consulo/workflows/jdk23/badge.svg) |

First of all, you need these tools:

 * Maven 3.9+
 * JDK 21+

Then execute from command line:

```sh
mvn package
```

If you want run Consulo from repository
 * as a desktop application

   ```sh
    mvn install

    mvn consulo:run-desktop-awt-fork -pl consulo:consulo-sandbox-desktop-awt
   ```

 * as a web application

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
 * Diagram support [link](https://github.com/consulo/consulo/tree/master/modules/base/graph-api)
 * Web IDE [link](https://github.com/consulo/consulo/tree/master/modules/web)
 * SWT UI [link](https://github.com/consulo/consulo/tree/master/modules/desktop-swt)

## Links

* [Contributing Guide](https://github.com/consulo/consulo/blob/master/CONTRIBUTING.md)
* [Download](https://consulo.app)
* [Issues](https://github.com/consulo/consulo/issues)
* [Forum](https://https://github.com/consulo/consulo/discussions/)
* [Discord Channel](https://discord.gg/Ab3Ka5gTFv)
