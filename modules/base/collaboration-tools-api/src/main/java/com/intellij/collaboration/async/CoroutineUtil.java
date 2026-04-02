// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.async;

import com.intellij.util.cancelOnDispose;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlin.Unit;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlin.jvm.functions.Function2;
import kotlin.reflect.KClass;
import kotlinx.coroutines.*;
import kotlinx.coroutines.flow.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utility methods for coroutine operations, converted from Kotlin extension functions.
 * <p>
 * Many of these methods operate on Kotlin coroutine types ({@link CoroutineScope}, {@link Flow},
 * {@link StateFlow}) and are meant to be called from Kotlin or Java code that uses these types.
 */
@ApiStatus.Internal
public final class CoroutineUtil {
    private CoroutineUtil() {
    }

    /**
     * Creates a child scope named after the given class.
     *
     * @see com.intellij.platform.util.coroutines.ChildScopeKt#childScope
     */
    public static @Nonnull CoroutineScope childScope(
        @Nonnull CoroutineScope scope,
        @Nonnull KClass<?> owner,
        @Nonnull CoroutineContext context,
        boolean supervisor
    ) {
        String name = owner.getQualifiedName();
        if (name == null) {
            name = owner.toString();
        }
        return ChildScopeKt.childScope(scope, name, context, supervisor);
    }

    public static @Nonnull CoroutineScope childScope(@Nonnull CoroutineScope scope, @Nonnull KClass<?> owner) {
        return childScope(scope, owner, EmptyCoroutineContext.INSTANCE, true);
    }

    /**
     * @deprecated Prefer creating a service to supply a parent scope
     */
    @Deprecated
    public static @Nonnull CoroutineScope disposingMainScope(@Nonnull Disposable parentDisposable) {
        CoroutineScope scope = MainScopeKt.MainScope();
        Disposer.register(parentDisposable, () -> CoroutineScopeKt.cancel(scope, null));
        return scope;
    }

    @ApiStatus.Experimental
    public static @Nonnull Disposable nestedDisposable(@Nonnull CoroutineScope scope) {
        Job job = scope.getCoroutineContext().get(Job.Key);
        if (job == null) {
            throw new IllegalArgumentException("Found no Job in context: " + scope.getCoroutineContext());
        }
        Disposable disposable = Disposer.newDisposable();
        job.invokeOnCompletion(throwable -> {
            Disposer.dispose(disposable);
            return Unit.INSTANCE;
        });
        return disposable;
    }

    public static @Nonnull CoroutineScope cancelledWith(@Nonnull CoroutineScope scope, @Nonnull Disposable disposable) {
        Job job = scope.getCoroutineContext().get(Job.Key);
        if (job == null) {
            throw new IllegalArgumentException("Coroutine scope without a parent job " + scope);
        }
        com.intellij.util.CancelOnDisposeKt.cancelOnDispose(job, disposable);
        return scope;
    }

    public static @Nonnull Job launchNow(
        @Nonnull CoroutineScope scope,
        @Nonnull CoroutineContext context,
        @Nonnull Function2<? super CoroutineScope, ? super kotlin.coroutines.Continuation<? super Unit>, ? extends Object> block
    ) {
        return BuildersKt.launch(scope, context, CoroutineStart.UNDISPATCHED, block);
    }

    public static @Nonnull Job launchNow(
        @Nonnull CoroutineScope scope,
        @Nonnull Function2<? super CoroutineScope, ? super kotlin.coroutines.Continuation<? super Unit>, ? extends Object> block
    ) {
        return launchNow(scope, EmptyCoroutineContext.INSTANCE, block);
    }

    public static <T> @Nonnull StateFlow<T> stateFlowOf(@Nonnull T value) {
        return FlowKt.asStateFlow(StateFlowKt.MutableStateFlow(value));
    }

    /**
     * Maps a StateFlow to another StateFlow using a mapper function.
     */
    @ApiStatus.Experimental
    public static <T, M> @Nonnull StateFlow<M> mapState(
        @Nonnull StateFlow<T> source,
        @Nonnull CoroutineScope scope,
        @Nonnull Function<T, M> mapper
    ) {
        Flow<M> mapped = FlowKt.map(source, (value, continuation) -> mapper.apply(value));
        M initialValue = mapper.apply(source.getValue());
        return FlowKt.stateIn(mapped, scope, SharingStarted.Companion.getEagerly(), initialValue);
    }

    /**
     * Maps a StateFlow without a scope - returns a derived state flow.
     */
    @ApiStatus.Experimental
    public static <T, M> @Nonnull StateFlow<M> mapState(
        @Nonnull StateFlow<T> source,
        @Nonnull Function<T, M> mapper
    ) {
        return new MappedStateFlow<>(source, mapper);
    }

    /**
     * Lazy shared flow that logs all exceptions as errors and never throws (beside cancellation)
     */
    public static <T> @Nonnull SharedFlow<T> modelFlow(
        @Nonnull Flow<T> source,
        @Nonnull CoroutineScope cs,
        @Nonnull Logger log
    ) {
        Flow<T> caught = FlowKt.catch$(source, (collector, throwable, continuation) -> {
            log.error(throwable);
            return Unit.INSTANCE;
        });
        return FlowKt.shareIn(caught, cs, SharingStarted.Companion.getLazily(), 1);
    }

    /**
     * Maps values in the flow to successful results and catches and wraps any exception into a failure result.
     *
     * @deprecated This doesn't work as we expected it to. {@code Flow.catch} doesn't actually prevent the flow from stopping
     */
    @Deprecated
    public static <T> @Nonnull Flow<kotlin.Result<T>> asResultFlow(@Nonnull Flow<T> source) {
        Flow<kotlin.Result<T>> mapped = FlowKt.map(
            source,
            (value, continuation) -> kotlin.Result.Companion.success(value)
        );
        return FlowKt.catch$(mapped, (collector, throwable, continuation) -> {
            collector.emit(kotlin.Result.Companion.failure(throwable), continuation);
            return Unit.INSTANCE;
        });
    }

    /**
     * Cancel the scope, await its completion but ignore the completion exception if any
     */
    public static void cancelAndJoinSilently(
        @Nonnull CoroutineScope cs,
        @Nonnull kotlin.coroutines.Continuation<? super Unit> continuation
    ) {
        Job job = cs.getCoroutineContext().get(Job.Key);
        if (job == null) {
            throw new IllegalStateException("Missing Job in " + cs);
        }
        cancelAndJoinSilently(job, continuation);
    }

    /**
     * Cancel the job, await its completion but ignore the completion exception if any
     */
    public static void cancelAndJoinSilently(
        @Nonnull Job job,
        @Nonnull kotlin.coroutines.Continuation<? super Unit> continuation
    ) {
        try {
            job.cancel(null);
            JobKt.cancelAndJoin(job, continuation);
        }
        catch (Exception ignored) {
        }
    }

    /**
     * Maps a flow of collections filtering elements by a predicate
     */
    public static <T> @Nonnull Flow<List<T>> mapFiltered(
        @Nonnull Flow<? extends Collection<T>> source,
        @Nonnull Predicate<T> predicate
    ) {
        return FlowKt.map(source, (collection, continuation) -> {
            return collection.stream().filter(predicate).toList();
        });
    }

    /**
     * Treats the flow as representing a single list of results, accumulating batches.
     */
    public static <T> @Nonnull Flow<List<T>> collectBatches(@Nonnull Flow<List<T>> source) {
        List<T> result = new ArrayList<>();
        return FlowKt.transform(source, (collector, list, continuation) -> {
            result.addAll(list);
            collector.emit(new ArrayList<>(result), continuation);
            return Unit.INSTANCE;
        });
    }

    public static @Nonnull Flow<Boolean> inverted(@Nonnull Flow<Boolean> source) {
        return FlowKt.map(source, (value, continuation) -> !value);
    }

    /**
     * An analogue of stateIn with SharingStarted.Eagerly, where the default value may never be emitted
     * if a value is already available in the source flow.
     */
    public static <T> @Nonnull StateFlow<T> stateInNow(
        @Nonnull Flow<T> source,
        @Nonnull CoroutineScope cs,
        @Nonnull T defaultValue
    ) {
        MutableStateFlow<T> result = StateFlowKt.MutableStateFlow(defaultValue);
        launchNow(
            cs,
            (scope, continuation) -> {
                FlowKt.collect(
                    source,
                    (value, cont) -> {
                        result.setValue(value);
                        return Unit.INSTANCE;
                    },
                    continuation
                );
                return Unit.INSTANCE;
            }
        );
        return FlowKt.asStateFlow(result);
    }

    @ApiStatus.Internal
    public static <T, M> @Nonnull StateFlow<M> mapStateInNow(
        @Nonnull StateFlow<T> source,
        @Nonnull CoroutineScope scope,
        @Nonnull Function<T, M> mapper
    ) {
        Flow<M> mapped = FlowKt.map(source, (value, continuation) -> mapper.apply(value));
        return stateInNow(mapped, scope, mapper.apply(source.getValue()));
    }

    /**
     * Private implementation of a mapped StateFlow that computes values on-the-fly.
     */
    private static final class MappedStateFlow<T, R> implements StateFlow<R> {
        private final @Nonnull StateFlow<T> source;
        private final @Nonnull Function<T, R> mapper;

        MappedStateFlow(@Nonnull StateFlow<T> source, @Nonnull Function<T, R> mapper) {
            this.source = source;
            this.mapper = mapper;
        }

        @Override
        public R getValue() {
            return mapper.apply(source.getValue());
        }

        @Override
        public @Nonnull List<R> getReplayCache() {
            return source.getReplayCache().stream().map(mapper).toList();
        }

        @Override
        public @Nullable Object collect(
            @Nonnull FlowCollector<? super R> collector,
            @Nonnull kotlin.coroutines.Continuation<? super Unit> continuation
        ) {
            Flow<R> mapped = FlowKt.distinctUntilChanged(FlowKt.map(source, (value, cont) -> mapper.apply(value)));
            return mapped.collect(collector, continuation);
        }
    }
}
