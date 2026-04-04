# Project Guidelines

## Build

Full project build (per-module compilation is not allowed):
```
cd "R:\consulo-claude-playgroud" && "T:\apache-maven-3.9.12\bin\mvn.cmd" package -Dmaven.test.skip=true
```

**Strictly prohibited:**
- Per-module compilation (`-pl <module>`, `-am`, `-rf`) — breaks the build cache and dependency resolution
- Installing Maven artifacts (`mvn install`) — corrupts the local repository cache

Run coroutine tests:
```
"T:\apache-maven-3.9.12\bin\mvn.cmd" test -pl modules/base/util/util-concurrent-coroutine -Dmaven.build.cache.enabled=false
```

## Async Code Style

Prefer coroutines (`Coroutine.first().then()` with `CoroutineScope.launchAsync()`) over raw `CompletableFuture` chaining (`thenCompose`, `thenApply`, `whenComplete`, etc.).

Coroutines provide:
- Better cancellation support
- Clearer step-by-step logic
- Consistent patterns across the codebase

### Coroutine step types:
- `UIAction` - runs on EDT via `uiAccess.giveAndWait()`. Has `apply(Function)` and `apply(BiFunction<I, Continuation<?>, O>)` variants (the latter for `finishEarly`)
- `ReadLock` - runs under `application.runReadAction()`. Only has `apply(Function<I, O>)`
- `CodeExecution` - runs on coroutine executor thread
- `CompletableFutureStep.await()` - awaits a `CompletableFuture<O>` produced from `Function<I, CompletableFuture<O>>`

### Pattern:
```java
return (CompletableFuture<Result>) CoroutineScope.launchAsync(project.coroutineContext(), () -> {
    return Coroutine
        .first(UIAction.<Void, State>apply((input, continuation) -> {
            // Step 1 on EDT
        }))
        .then(ReadLock.<State, State>apply(state -> {
            // Step 2 under read lock
        }))
        .then(CompletableFutureStep.<State, Result>await(state -> {
            // Step 3 - await async operation
        }))
        .then(UIAction.<Result, Result>apply(result -> {
            // Step 4 on EDT
        }));
}).toFuture();
```

### Early exit with `finishEarly`:
Use `UIAction.apply(BiFunction<I, Continuation<?>, O>)` variant to access `Continuation`, cast to typed `Continuation<T>`, and call `finishEarly(result)`:
```java
@SuppressWarnings("unchecked")
Continuation<MyResult> typed = (Continuation<MyResult>) continuation;
typed.finishEarly(MyResult.EMPTY);
return null;
```

### CompletableFuture method selection:
- **Terminal callbacks** (no further chaining): use `whenComplete(BiConsumer<T, Throwable>)` — handles both success and error, preserves result type
- **Do NOT use `thenAccept`** for terminal callbacks — it only handles success, changes return type to `Void`, and swallows exceptions
- **Chaining to another CF**: use `thenCompose(Function<T, CompletableFuture<U>>)`
- **Synchronous transform**: use `thenApply(Function<T, U>)`
- When migrating from `AsyncResult.doWhenDone()` → always use `whenComplete`, never `thenAccept`

### When raw CompletableFuture is acceptable:
- Simple delegation (just returning another method's future)
- Interface contract requires `CompletableFuture` return type and method body is trivial (1-2 lines)

### CommandProcessor in async code:
- `CommandProcessor` wrapping belongs at the low-level editor infrastructure (`FileEditorManagerImpl.openFileAsync`) where `IdeDocumentHistory` tracking needs command context
- Higher-level async methods (e.g. `LanguageEditorNavigationUtil.openFileWithPsiElementAsync`) should NOT wrap in `CommandProcessor` - they just call `navigateAsync()` directly, which flows down to the low-level code that already handles it

## Copyright Headers

- New or modified files must have the Consulo copyright header with year range `2013-2026`: `Copyright 2013-2026 consulo.io`
- When adapting code from the JetBrains IntelliJ Community codebase (`W:\intellij-community`), add the original JetBrains copyright line **above** the Consulo line
- **Preserve comments** — keep existing comments, Javadoc, and inline documentation from the reference code
