// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.application.util.query;

import consulo.application.progress.ProgressManager;
import consulo.util.collection.SmartList;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A Processor + Flow, or the Poor man's Flow: a reactive-stream-like wrapper around {@link Predicate}.
 * <p>
 * Instead of creating the {@link Predicate} manually - create a {@link Plow} and work with the Processor inside a lambda.
 * If your method accepts the processor it is the good idea to return a {@link Plow} instead to avoid
 * the "return value in parameter" semantic
 * <p>
 * Itself {@link Plow} is stateless and
 * <a href="https://projectreactor.io/docs/core/3.3.9.RELEASE/reference/index.html#reactor.hotCold">"Cold"</a>,
 * meaning that the same instance of {@link Plow} could be reused several times (by reevaluating the sources),
 * but depending on the idempotence of the {@code producingFunction} this contract could be violated,
 * so to be careful, and it is better to obtain the new {@link Plow} instance each time.
 */
public final class Plow<T> {
    private final Predicate<Predicate<T>> myProducingFunction;

    private Plow(Predicate<Predicate<T>> producingFunction) {
        myProducingFunction = producingFunction;
    }

    @SuppressWarnings("unchecked")
    public boolean processWith(Predicate<? super T> processor) {
        return myProducingFunction.test((Predicate<T>) processor);
    }

    private void processTo(Predicate<T> processor) {
        myProducingFunction.test(processor);
    }

    public @Nullable T findAny() {
        return find(t -> true);
    }

    public @Nullable T find(Predicate<? super T> test) {
        List<T> found = new ArrayList<>(1);
        processTo(t -> {
            if (test.test(t)) {
                found.add(t);
                return false;
            }
            return true;
        });
        return found.isEmpty() ? null : found.get(0);
    }

    public <C extends Collection<T>> C collectTo(C coll) {
        processTo(t -> {
            coll.add(t);
            return true;
        });
        return coll;
    }

    public List<T> toList() {
        List<T> list = collectTo(new SmartList<>());
        return list.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public Set<T> toSet() {
        Set<T> set = collectTo(new HashSet<>());
        return set.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(set);
    }

    public T[] toArray(T[] array) {
        return collectTo(new ArrayList<>()).toArray(array);
    }

    public <R> Plow<R> transform(Function<Predicate<R>, Predicate<T>> transformation) {
        return new Plow<>(pr -> myProducingFunction.test(transformation.apply(pr)));
    }

    public <R> Plow<R> map(Function<? super T, ? extends R> mapping) {
        return transform(pr -> v -> pr.test(mapping.apply(v)));
    }

    public <R> Plow<R> mapNotNull(Function<? super T, ? extends @Nullable R> mapping) {
        return transform(pr -> v -> {
            @Nullable R mapped = mapping.apply(v);
            return mapped == null || pr.test(mapped);
        });
    }

    public Plow<T> filter(Predicate<? super T> test) {
        return transform(pr -> v -> !test.test(v) || pr.test(v));
    }

    public <R> Plow<R> mapToProcessor(BiPredicate<? super T, Predicate<R>> mapping) {
        return new Plow<>(rProcessor -> myProducingFunction.test(t -> mapping.test(t, rProcessor)));
    }

    public <R> Plow<R> flatMap(Function<? super T, Plow<R>> mapping) {
        return mapToProcessor((t, processor) -> mapping.apply(t).processWith(processor));
    }

    public <R> Plow<R> flatMapSeq(Function<? super T, Stream<R>> mapping) {
        return mapToProcessor((t, processor) -> mapping.apply(t).allMatch(processor));
    }

    public Plow<T> cancellable() {
        return transform(pr -> v -> {
            ProgressManager.checkCanceled();
            return pr.test(v);
        });
    }

    public Plow<T> limit(int n) {
        int[] processedCount = {0};
        return transform(pr -> v -> {
            processedCount[0]++;
            return processedCount[0] <= n && pr.test(v);
        });
    }

    public static <T> Plow<T> empty() {
        return of(pr -> true);
    }

    public static <T> Plow<T> of(Predicate<Predicate<T>> processorCall) {
        return new Plow<>(processorCall);
    }

    @SafeVarargs
    public static <T> Plow<T> ofArray(T... array) {
        return new Plow<>(pr -> Arrays.stream(array).allMatch(pr));
    }

    public static <T> Plow<T> ofIterable(Iterable<T> iterable) {
        return new Plow<>(pr -> {
            for (T t : iterable) {
                if (!pr.test(t)) {
                    return false;
                }
            }
            return true;
        });
    }

    public static <T> Plow<T> ofSequence(Stream<T> sequence) {
        return new Plow<>(pr -> sequence.allMatch(pr));
    }

    @SafeVarargs
    public static <T> Plow<T> concat(Plow<T>... plows) {
        return of(pr -> {
            for (Plow<T> plow : plows) {
                if (!plow.processWith(pr)) {
                    return false;
                }
            }
            return true;
        });
    }
}
