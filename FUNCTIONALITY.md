
### Programming Languages
| Name | Parsing | Resolving | Completion | Inspections |
| ---- |:-------:|:---------:|:----------:|:-----------:|
| [Java](https://github.com/consulo/consulo-java)             | 🟢 | 🟢 | 🟢 | 🟢
| [C#](https://github.com/consulo/consulo-csharp)             | 🟢 | 🟢 | 🟢 | 🟢
| [Groovy](https://github.com/consulo/consulo-groovy) | 🔵 | 🔵 | 🔵 | 🔵
| [JavaScript](https://github.com/consulo/consulo-javascript) | 🔵 | 🔵 | 🔵 | 🔵
| [Python](https://github.com/consulo/consulo-python) | 🔵 | 🔵 | 🔵 | 🔵
| [Lua](https://github.com/consulo/consulo-lua) | 🔵 | 🔵 | 🔵 | 🔵
| [PHP](https://github.com/consulo/consulo-php)               | 🔵 | 🔵 | 🔵 | 🔴
| [Ruby](https://github.com/consulo/incubating-consulo-ruby)  | 🔵<br><sup>(incubating but delayed)</sup> | 🔴 | 🔴 | 🔴
| [Dart](https://github.com/consulo/consulo-google-dart)      | 🔵 | 🔵 | 🔵 | 🔵
| [Go](https://github.com/consulo/consulo-google-go)          | 🔵 | 🔵 | 🔵 | 🔵
| [C/C++](https://github.com/consulo/incubating-consulo-cpp)  | 🔵<br><sup>(incubating)</sup> | 🔴 | 🔴 | 🔴

### Other Languages
| Name | Parsing | Resolving | Completion | Inspections |
| ---- |:-------:|:---------:|:----------:|:-----------:|
| [HTML & XML](https://github.com/consulo/consulo-xml)        | 🟢 | 🟢 | 🟢 | 🟢
| [XPath](https://github.com/consulo/consulo-xpath)        | 🟢 | 🟢 | 🟢 | 🟢
| [JSON](https://github.com/consulo/consulo-javascript)       | 🟢 | 🟢 | 🟢 | 🟢
| [CSS](https://github.com/consulo/consulo-css)               | 🔵 | 🔵 | 🔵 | 🔴
| [TOML](https://github.com/consulo/consulo-toml)               | 🟢 | 🟢 | 🔵 | 🔵
| [Markdown](https://github.com/consulo/consulo-markdown)               | 🟢 | 🟢 | 🔵 | 🔵
| [SQL](https://github.com/consulo/consulo-database)          | 🔵<br><sup>(incubating)</sup> | 🔴 | 🔴 | 🔴
| [Regexp](https://github.com/consulo/consulo-regexp)       | 🟢 | 🟢 | 🟢 | 🟢

* __Parsing__ — lexical analysis and syntactic tree construction. This provides basic syntax highlighting (comments are grey, string literals are green, etc.)
* __Resolving__ — semantic analysis. Provides links between function calls and function definitions. So you may middle-click on method call and IDE shows you method definition or you may middle-click on method definition and IDE shows you all calls to this method.
* __Inspections__ — deep analysis over resolved syntactic tree. Highlights deprecated method usages, usages of non-initialized or nullable variables, etc. Some inspections provide semi-automatic fixes (shown by pressing Alt+Enter while text cursor is on highlighted spot).
* __Completion__ — interactive help when you're typing new code. For example if you type dot after variable name IDE shows list of fields and methods.

### Language Frameworks
| Name | Support |
| ---- | :-------: |
| Java ▸ [Lombok](https://github.com/consulo/consulo-lombok)       | 🟢
| Java ▸ [Spring](https://github.com/consulo/incubating-consulo-spring)       | 🔵 <br><sup>(incubating)</sup>
| Java ▸ [Hibernate](https://github.com/consulo/incubating-consulo-hibernate) | 🔴 <br><sup>(planned but delayed)</sup>
| Java ▸ JPA                                                                  | 🔴 <br><sup>(planned but delayed)</sup>
| C# ▸ [Razor](https://github.com/consulo/incubating-consulo-razor)           | 🔵 <br><sup>(incubating)</sup>

### Runtime Platforms
| Name | Importing | Running | Debugging |
| ---- | :---------: |:-------:|:---------:|
| [Java](https://github.com/consulo/consulo-java) | 🟢 [Maven](https://github.com/consulo/consulo-maven) / 🟢 [Gradle](https://github.com/consulo/consulo-gradle) | 🟢 | 🟢
| [.NET Framework](https://github.com/consulo/consulo-dotnet-microsoft)   | 🟢 [MSBuild](https://github.com/consulo/consulo-msbuild) | 🟢 | 🟢
| [Mono (.NET Framework)](https://github.com/consulo/consulo-dotnet-mono) | 🟢 [MSBuild](https://github.com/consulo/consulo-msbuild) | 🟢 | 🟢
| [.NET (.NET Core)](https://github.com/consulo/consulo-dotnet-core)      | 🟢 [MSBuild](https://github.com/consulo/consulo-msbuild) | 🟢 | 🔴
| [Unity](https://github.com/consulo/consulo-unity3d)                     | 🟢 | 🟢 | 🟢
| [PHP](https://github.com/consulo/consulo-php)                           | 🔵 | 🔵 | 🔴
| [Node.js](https://github.com/consulo/consulo-nodejs)                    | 🔵 | 🔵 | 🔴

### Version Control
| Name | Cloning | Committing | Viewing Log|
| ---- | :---------: |:-------:|:-----------:|
| [Git](https://github.com/consulo/consulo-git) |🟢|🟢|🟢|
| [Mercurial](https://github.com/consulo/consulo-mercurial) |🔵|🔵|🔵|
| [Subversion](https://github.com/consulo/consulo-apache-subversion) <br><sup>(support exists, but disabled)</sup>|🔴|🔴|🔴


Legend 
  - 🟢 ▸ fully supported
  - 🔵 ▸ partially supported
  - 🔴 ▸ not supported
