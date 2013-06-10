# Consulo

This is fork of IntelliJ IDEA Community Edition

## Building

To develop Consulo, you can use either IntelliJ IDEA Community Edition or IntelliJ IDEA Ultimate. To build and run the code:

* Make sure you have the Groovy plugin enabled. Parts of IntelliJ IDEA are written in Groovy, and you will get compilation errors if you don't have the plugin enabled.
* Make sure you have the UI Designer plugin enabled. Most of IntelliJ IDEA's UI is built using the UI Designer, and the version you build will not run correctly if you don't have the plugin enabled.
* Open the directory with the source code as a directory-based project
* Configure a JSDK named "IDEA jdk", pointing to an installation of JDK 1.6
* On Windows or Linux, add lib\tools.jar from the JDK installation directory to the classpath of IDEA jdk
* Use Build | Make Project to build the code
* To run the code, use the provided shared run configuration "IDEA".

## Links

* [Tracker](http://napile.myjetbrains.com/youtrack/issues/CO)