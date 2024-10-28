### Languages
| Name | Parsing | Resolving | Completion | Inspections |
| ---- | ------- | --------- | ---------- | ----------- |
| [Java](https://github.com/consulo/consulo-java)             | 🟢 | 🟢 | 🟢 | 🟢
| [C#](https://github.com/consulo/consulo-csharp)             | 🟢 | 🟢 | 🟢 | 🟢
| [JavaScript](https://github.com/consulo/consulo-javascript) | 🔵 | 🔵 | 🔵 | 🔵
| [PHP](https://github.com/consulo/consulo-php)               | 🔵 | 🔵 | 🔵 | 🔴
| [Ruby](https://github.com/consulo/incubating-consulo-ruby)  | 🔵 (incubating but delayed) | 🔴 | 🔴 | 🔴
| [SQL](https://github.com/consulo/consulo-database)          | 🔵 (incubating) | 🔴 | 🔴 | 🔴
| [Dart](https://github.com/consulo/consulo-google-dart)      | 🔵 | 🔵 | 🔵 | 🔵
| [Go](https://github.com/consulo/consulo-google-go)          | 🔵 | 🔵 | 🔵 | 🔵
| [HTML & XML](https://github.com/consulo/consulo-xml)        | 🟢 | 🟢 | 🟢 | 🟢
| [JSON](https://github.com/consulo/consulo-javascript)       | 🟢 | 🟢 | 🟢 | 🟢
| [CSS](https://github.com/consulo/consulo-css)               | 🔵 | 🔵 | 🔵 | 🔵
| [C/C++](https://github.com/consulo/incubating-consulo-cpp)  | 🔵 (incubating) | 🔴 | 🔴 | 🔴

* __Parsing__ — lexical analysis and syntactic tree construction. This provides basic syntax highlighting (comments are grey, string literals are green, etc.)
* __Resolving__ — semantic analysis. Provides links between function calls and function definitions. So you may middle-click on method call and IDE shows you method definition or you may middle-click on method definition and IDE shows you all calls to this method.
* __Inspections__ — deep analysis over resolved syntactic tree. Highlights deprecated method usages, usages of non-initialized or nullable variables, etc. Some inspections provide semi-automatic fixes (shown by pressing Alt+Enter while text cursor is on highlighted spot).
* __Completion__ — interactive help when you're typing new code. For example if you type dot after variable name IDE shows list of fields and methods.

### Language Frameworks 
| Name | Support |
| ---- | ------- |
| Java ▸ [Spring](https://github.com/consulo/incubating-consulo-spring)       | 🔵 (incubating)
| Java ▸ [Hibernate](https://github.com/consulo/incubating-consulo-hibernate) | 🔴 (planned but delayed)
| Java ▸ JPA                                                                  | 🔴 (planned but delayed)
| C# ▸ [Razor](https://github.com/consulo/incubating-consulo-razor)           | 🔵 (incubating)

### Runtime Platforms
| Name | Importing | Running | Debugging |
| ---- | --------- | ------- | --------- |
| [Java](https://github.com/consulo/consulo-java) | [Maven](https://github.com/consulo/consulo-maven) 🟢 / [Gradle](https://github.com/consulo/consulo-gradle) 🟢 | 🟢 | 🟢
| [.NET Framework](https://github.com/consulo/consulo-dotnet-microsoft)   | [MSBuild](https://github.com/consulo/consulo-msbuild) 🟢 | 🟢 | 🟢
| [Mono (.NET Framework)](https://github.com/consulo/consulo-dotnet-mono) | [MSBuild](https://github.com/consulo/consulo-msbuild) 🟢 | 🟢 | 🟢
| [.NET (.NET Core)](https://github.com/consulo/consulo-dotnet-core)      | [MSBuild](https://github.com/consulo/consulo-msbuild) 🟢 | 🟢 | 🔴
| [Unity](https://github.com/consulo/consulo-unity3d)                     | 🟢 | 🟢 | 🟢
| [PHP](https://github.com/consulo/consulo-php)                           | 🔵 | 🔵 | 🔴
| [Node.js](https://github.com/consulo/consulo-nodejs)                    | 🔵 | 🔵 | 🔴

Legend 
  - 🟢 ▸ fully supported
  - 🔵 ▸ partial supported
  - 🔴 ▸ not supported

