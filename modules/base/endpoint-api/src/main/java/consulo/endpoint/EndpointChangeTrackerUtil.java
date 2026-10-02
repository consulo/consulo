// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.endpoint;

import consulo.project.Project;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public final class EndpointChangeTrackerUtil {
    private EndpointChangeTrackerUtil() {
    }

    /**
     * Runs process with disabled PSI changes tracking in opened Endpoints View.
     */
    public static void withExpectedChanges(Project project, Runnable runnable) {
        EndpointChangeTracker publisher = project.getMessageBus().syncPublisher(EndpointChangeTracker.class);
        publisher.setTrackingChanges(false);
        try {
            runnable.run();
        }
        finally {
            publisher.setTrackingChanges(true);
        }
    }

    /**
     * Coroutine friendly {@link #withExpectedChanges}
     */
    public static <T> CompletableFuture<T> withExpectedChangesAsync(
        Project project,
        Supplier<? extends CompletableFuture<T>> action
    ) {
        EndpointChangeTracker publisher = project.getMessageBus().syncPublisher(EndpointChangeTracker.class);
        publisher.setTrackingChanges(false);
        CompletableFuture<T> future;
        try {
            future = action.get();
        }
        catch (RuntimeException | Error e) {
            publisher.setTrackingChanges(true);
            throw e;
        }
        return future.whenComplete((result, error) -> publisher.setTrackingChanges(true));
    }
}
