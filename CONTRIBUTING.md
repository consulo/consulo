# Contributing guide

First of all you need select correct repository for report issue. Consulo organization have more that 100 repositories. Please don't create issue in random repository.

If you can't describe issue - use our [forum](https://discuss.consulo.io/) for speak with community.

If you want contribute code, you need setup IDE for it, visit [Plugin-Development-Starter-Guide](https://github.com/consulo/consulo/wiki/Plugin-Development-Starter-Guide)


Here you can found more information for each repository.

## Core
 * [consulo](http://github.com/consulo/consulo) - main repository of **Consulo** organization on **github**. It contains **Consulo** platform code without **any language implementations**. 
 * [consulo-spellchecker](http://github.com/consulo/consulo-spellchecker) - contains **spellchecker** implementation.
 * [consulo-images](http://github.com/consulo/consulo-images) - contains image support. 

## JVM Platform
 * [consulo-java](http://github.com/consulo/consulo-java) - contains **Java** language implementation. Here you can report any problem with **Java** language, **JVM** debugger, etc.

    * [consulo-groovy](http://github.com/consulo/consulo-groovy) - contains **Groovy** implementation.
    * [consulo-junit](http://github.com/consulo/consulo-junit) - contains **JUnit** implementation.
    * [consulo-testng](http://github.com/consulo/consulo-testng) - contains **TestNG** implementation.

## .NET Platform
 * [consulo-dotnet](http://github.com/consulo/consulo-dotnet) - contains **base api** implementation for **.NET** platform. Here you can report problems with **MSIL** language, debugger, etc.

    * [consulo-csharp](http://github.com/consulo/consulo-csharp) - contains **C#** language implementation. Here you can report:
      * any problem with **C#** language
      * .NET debugger evaluation (but not problem with .NET debugger itself)

    * [consulo-unity](http://github.com/consulo/consulo-unity) - contains Unity framework implementation. Here you can report:
      * any problem with **Unity** related functional
      * **UnityScript** language

      Unity plugin have some specific repositories: 
        * [UnityEditorConsuloPlugin](https://github.com/consulo/UnityEditorConsuloPlugin) - here you can found **Consulo** plugin for **UnityEditor** written in **C#**

    * [consulo-nunit](http://github.com/consulo/consulo-nunit) - contains **NUnit** implementation.

## JavaScript Platform
 * [consulo-javascript](http://github.com/consulo/consulo-javascript) - contains **JavaScript**, **ECMAScript**, **JSON** language implementation. Here you can report problems with **JavaScript** language, debugger, etc.
    * [consulo-nodejs](http://github.com/consulo/consulo-nodejs) - contains **NodeJS**, **Mocha** framework implementations.
    
## Python Platform
 * [consulo-python](http://github.com/consulo/consulo-python) - contains **Python** language implementation, and **Jython**, **IronPython** frameworks. Here you can report problems with **Python** language, debugger, etc.

## Go Platform
 * [consulo-google-go](http://github.com/consulo/consulo-google-go) - contains **Go** language implementation. Here you can report problems with **Go** language,debugger, etc.
