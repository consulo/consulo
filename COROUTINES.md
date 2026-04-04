# Consulo Java Coroutine System

## Overview

Consulo implements a pure Java cooperative concurrency system. Coroutines provide lightweight, suspendable execution steps that yield between processing without blocking threads. They replace raw `CompletableFuture` chaining with readable step-based chains.

## Quick Start

```java
CoroutineScope.launchAsync(project.coroutineContext(), () -> {
    return Coroutine
        .first(ReadLock.<Void, PsiFile>apply(ignored -> {
            return PsiDocumentManager.getInstance(project).getPsiFile(document);
        }))
        .then(UIAction.<PsiFile, Void>apply(psiFile -> {
            // update UI on EDT
            return null;
        }));
});
```

## Core Classes

### Coroutine

Entry point for building step chains. Immutable — each `.then()` returns a new instance.

```java
Coroutine
    .first(step1)       // first step
    .then(step2)        // chain more steps
    .then(step3);       // output of step N becomes input of step N+1
```

### CoroutineScope

Launches coroutines and manages their lifecycle.

```java
// Async (non-blocking, returns Continuation for cancellation)
Continuation<?> c = CoroutineScope.launchAsync(context, () -> coroutine);

// Blocking (waits for all coroutines to finish)
CoroutineScope.launch(context, scope -> { /* launch coroutines */ });
```

### Continuation

Represents a running coroutine's execution state. Returned by `launchAsync()`.

```java
Continuation<?> c = CoroutineScope.launchAsync(ctx, () -> coroutine);

c.cancel();              // cancel execution
c.isCancelled();         // check if cancelled
c.isFinished();          // check if done
c.await();               // block until done
c.toFuture();            // convert to CompletableFuture<T>
c.onFinish(cont -> {});  // callback when done
c.onCancel(cont -> {});  // callback when cancelled
c.onError(cont -> {});   // callback on error
```

### CoroutineContext

Provides executor and scheduler. Obtained from `Project` or `Application`:

```java
project.coroutineContext()          // project-scoped
Application.get().coroutineContext() // application-scoped
```

## Step Types

### UIAction — EDT execution

Runs code on EDT via `UIAccess.giveAndWait()`.

```java
// Simple variant
UIAction.<Input, Output>apply(input -> {
    // runs on EDT
    return output;
})

// With Continuation (for finishEarly)
UIAction.<Input, Output>apply((input, continuation) -> {
    if (shouldExit) {
        @SuppressWarnings("unchecked")
        Continuation<FinalType> typed = (Continuation<FinalType>) continuation;
        typed.finishEarly(result);
        return null;
    }
    return output;
})
```

### ReadLock — read action execution

Wraps code in `application.runReadAction()`. Runs on coroutine executor thread.

```java
ReadLock.<Input, Output>apply(input -> {
    // runs under read lock (safe for PSI, VFS access)
    return output;
})
```

Only has `Function` variant (no continuation access).

### WriteLock — write action execution

Wraps code in `application.runWriteAction()`.

```java
// Simple variant
WriteLock.<Input, Output>apply(input -> {
    // runs under write lock
    return output;
})

// With Continuation
WriteLock.<Input, Output>apply((input, continuation) -> {
    return output;
})
```

### OptionalReadLock — try read action

Uses `application.tryRunReadAction()`. Useful when read lock might not be available.

```java
OptionalReadLock.<Input, Output>apply(
    input -> { /* runs if read lock acquired */ return output; },
    () -> { /* runs if read lock NOT acquired */ }
)
```

### CodeExecution — plain execution on executor thread

Runs on the coroutine executor thread without any locks.

```java
CodeExecution.<Input, Output>apply(input -> output)         // transform
CodeExecution.<Input, Output>supply(() -> output)            // ignore input
CodeExecution.<T>consume(input -> { /* side effect */ })     // no output
CodeExecution.<T>run(() -> { /* side effect */ })            // ignore both
```

### CompletableFutureStep — await async operation

Awaits a `CompletableFuture` produced from the input.

```java
CompletableFutureStep.<Input, Output>await(input -> {
    return someAsyncOperation(input); // returns CompletableFuture<Output>
})
```

### Delay — timed pause

Suspends execution for a duration without blocking a thread.

```java
Delay.sleep(200)                          // 200ms
Delay.sleep(5, TimeUnit.SECONDS)          // 5 seconds
```

### Iteration — process collections

Applies a step to each element of an iterable.

```java
// Process each, discard results
Iteration.forEach(stepForEachElement)

// Process each, collect results into List
Iteration.collectEach(stepForEachElement)
```

## Patterns

### Early Exit with finishEarly

Use `UIAction.apply(BiFunction)` or `WriteLock.apply(BiFunction)` to access the `Continuation` and call `finishEarly()`:

```java
Coroutine
    .first(UIAction.<Void, State>apply((input, continuation) -> {
        if (project.isDisposed()) {
            @SuppressWarnings("unchecked")
            Continuation<Result> typed = (Continuation<Result>) continuation;
            typed.finishEarly(Result.EMPTY);
            return null; // ignored
        }
        return new State();
    }))
    .then(ReadLock.<State, Data>apply(state -> {
        // this step is skipped if finishEarly was called
        return loadData(state);
    }))
    .then(UIAction.<Data, Result>apply(data -> {
        return applyToUI(data);
    }));
```

### Cancellation on Dispose

Use `DisposableCoroutineScope` to auto-cancel when a `Disposable` is disposed:

```java
DisposableCoroutineScope.launchAsync(
    project.coroutineContext(),
    myDisposable,  // cancels coroutine when disposed
    () -> Coroutine.first(...)
);
```

### Store and Cancel Later

Store the `Continuation` to cancel manually:

```java
// Launch
myContinuation = CoroutineScope.launchAsync(ctx, () -> coroutine);

// Cancel later
if (myContinuation != null) {
    myContinuation.cancel();
    myContinuation = null;
}
```

### Convert to CompletableFuture

```java
CompletableFuture<Result> future = (CompletableFuture<Result>)
    CoroutineScope.launchAsync(ctx, () -> coroutine).toFuture();
```

### ReadLock → UIAction (background check, then EDT update)

Common pattern for PSI access followed by UI update:

```java
CoroutineScope.launchAsync(project.coroutineContext(), () -> {
    return Coroutine
        .first(ReadLock.<Void, Boolean>apply(ignored -> {
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(doc);
            return psiFile != null && psiFile.isValid();
        }))
        .then(UIAction.<Boolean, Void>apply((valid, continuation) -> {
            if (!valid || editor.isDisposed()) {
                @SuppressWarnings("unchecked")
                Continuation<Void> typed = (Continuation<Void>) continuation;
                typed.finishEarly(null);
                return null;
            }
            // safe to update UI here
            return null;
        }));
});
```

### Delay → UIAction → ReadLock → UIAction (debounced update)

```java
DisposableCoroutineScope.launchAsync(project.coroutineContext(), this, () -> {
    return Coroutine.first(Delay.sleep(200))
        .then(UIAction.apply((o, continuation) -> {
            Editor editor = getEditor();
            if (editor == null) {
                continuation.cancel();
            }
            return editor;
        }))
        .then(ReadLock.apply(editor -> {
            return computeState(editor);
        }))
        .then(UIAction.apply(state -> {
            updateUI(state);
            return null;
        }));
});
```

## Threading Model

| Step | Thread | Lock |
|------|--------|------|
| `UIAction` | EDT (via `UIAccess.giveAndWait()`) | None |
| `ReadLock` | Coroutine executor | Read lock |
| `WriteLock` | Coroutine executor | Write lock |
| `OptionalReadLock` | Coroutine executor | Try read lock |
| `CodeExecution` | Coroutine executor | None |
| `CompletableFutureStep` | Depends on awaited future | None |
| `Delay` | Scheduled executor (async) | None |

Steps execute **sequentially** within a single coroutine. Multiple coroutines execute **concurrently** in the shared executor pool.

## Source Locations

| Component | Path |
|-----------|------|
| Core (Coroutine, Continuation, CoroutineScope) | `modules/base/util/util-concurrent-coroutine/` |
| Steps (CodeExecution, CompletableFutureStep, Delay, Iteration) | `modules/base/util/util-concurrent-coroutine/.../step/` |
| ReadLock, WriteLock, OptionalReadLock, DisposableCoroutineScope | `modules/base/application-api/.../concurrent/coroutine/` |
| UIAction | `modules/base/ui-ex-api/.../coroutine/UIAction.java` |
