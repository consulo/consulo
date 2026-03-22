---
name: async-code
description: >
  Use this skill whenever the user writes, reviews, or refactors asynchronous Java code in Consulo —
  including CompletableFuture chains, coroutines, async methods, background tasks, EDT interactions,
  or read-lock operations. Trigger on: "async", "CompletableFuture", "coroutine", "background task",
  "thenCompose", "thenApply", "thenAccept", "whenComplete", "launchAsync", "UIAction", "ReadLock",
  "CodeExecution", "finishEarly", "openFileAsync", "navigateAsync", or any code that needs to run
  off the EDT, under a read lock, or as a multi-step async pipeline.
  Also trigger when the user migrates from AsyncResult or asks how to do something asynchronously.
  MUST be used proactively any time async/concurrent code is being written or reviewed.
---

# Consulo Async Code Style

## Core Rule: Prefer Coroutines over Raw CompletableFuture

Prefer `Coroutine.first().then()` with `CoroutineScope.launchAsync()` over raw `CompletableFuture`
chaining (`thenCompose`, `thenApply`, `whenComplete`, etc.).

Coroutines provide:
- Better cancellation support
- Clearer step-by-step logic
- Consistent patterns across the codebase

---

## Coroutine Step Types

| Step type | Runs on | Notes |
|-----------|---------|-------|
| `UIAction` | EDT via `uiAccess.giveAndWait()` | Two variants: `apply(Function)` and `apply(BiFunction<I, Continuation<?>, O>)` (latter for `finishEarly`) |
| `ReadLock` | Under `application.runReadAction()` | Only has `apply(Function<I, O>)` |
| `CodeExecution` | Coroutine executor thread | General background work |
| `CompletableFutureStep.await()` | Awaits a `CompletableFuture<O>` | Produced from `Function<I, CompletableFuture<O>>` |

---

## Standard Coroutine Pattern

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

---

## Early Exit with `finishEarly`

Use the `UIAction.apply(BiFunction<I, Continuation<?>, O>)` variant to access `Continuation`,
cast to the typed form, and call `finishEarly(result)` to short-circuit the rest of the chain:

```java
@SuppressWarnings("unchecked")
Continuation<MyResult> typed = (Continuation<MyResult>) continuation;
typed.finishEarly(MyResult.EMPTY);
return null;
```

---

## CompletableFuture Method Selection

When raw `CompletableFuture` is used (see below for when that's acceptable), choose the right method:

| Use case | Method | Why |
|----------|--------|-----|
| Terminal callback (last step, no further chaining) | `whenComplete(BiConsumer<T, Throwable>)` | Handles both success and error, preserves result type |
| Chain to another CompletableFuture | `thenCompose(Function<T, CompletableFuture<U>>)` | Flat-maps futures |
| Synchronous transform | `thenApply(Function<T, U>)` | Maps the value |
| Migrating from `AsyncResult.doWhenDone()` | `whenComplete` | Never `thenAccept` |

**Never use `thenAccept` for terminal callbacks** — it only handles success, changes the return
type to `Void`, and silently swallows exceptions.

---

## When Raw CompletableFuture Is Acceptable

Raw `CompletableFuture` (without the coroutine wrapper) is fine only for:
- Simple delegation: the method body just returns another method's future (1–2 lines)
- Interface contract: the interface requires a `CompletableFuture` return type and the
  implementation is trivial

In all other cases, use coroutines.

---

## CommandProcessor in Async Code

`CommandProcessor` wrapping belongs at the **low-level editor infrastructure** layer:
- ✅ `FileEditorManagerImpl.openFileAsync` — this is where `IdeDocumentHistory` tracking
  needs command context, so wrap here
- ❌ `LanguageEditorNavigationUtil.openFileWithPsiElementAsync` and other higher-level
  async methods — do NOT wrap in `CommandProcessor`; just call `navigateAsync()` directly,
  which flows down to the low-level code that already handles it

The rule: command context is an infrastructure concern, not a caller concern.
