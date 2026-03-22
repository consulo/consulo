---
name: maven-build
description: >
  Use this skill to build the Consulo project with Maven. Trigger on: "build", "compile",
  "mvn", "maven", "run build", "build project", "check if it compiles", "build the project",
  "run tests", "coroutine tests", or any request to verify the code compiles or tests pass.
---

# Maven Build

## Full project build

```
cd "R:\consulo-claude-playgroud" && "T:\apache-maven-3.9.12\bin\mvn.cmd" package -Dmaven.test.skip=true
```

## Run coroutine tests

```
"T:\apache-maven-3.9.12\bin\mvn.cmd" test -pl modules/base/util/util-concurrent-coroutine -Dmaven.build.cache.enabled=false
```

## Strictly prohibited — never use these

- Per-module build (`-pl`, `-am`, `-rf`) — breaks build cache and dependency resolution
  _(exception: the coroutine test command above is the only approved `-pl` usage)_
- `mvn install` — corrupts the local repository cache
