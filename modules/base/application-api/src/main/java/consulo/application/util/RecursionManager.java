// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.SmartList;
import consulo.util.lang.StackOverflowPreventedException;
import consulo.util.lang.ref.SoftReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * A utility to prevent endless recursion and ensure the caching returns stable results if such endless recursion is prevented.<p></p>
 * <p>
 * Imagine a method {@code A()} calls method {@code B()}, which in turn calls {@code C()},
 * which (unexpectedly) calls {@code A()} again (it's just an example; the loop could be shorter or longer).
 * This would normally result in endless recursion and stack overflow. One should avoid situations like these at all cost,
 * but if that's impossible (e.g. due to different plugins unaware of each other yet calling each other),
 * {@code RecursionManager} is to the rescue.<p></p>
 * <p>
 * It helps to track all the computations in the thread stack and return some default value when
 * asked to compute {@code A()} for the second time. {@link #doPreventingRecursion} does precisely this, returning {@code null} when
 * endless recursion would otherwise happen.<p></p>
 * <p>
 * Additionally, imagine all these methods {@code A()}, {@code B()} and {@code C()} cache their results.
 * Note that if not {@code A()} is called first, but {@code B()} or {@code C()}, the endless recursion would stay just the same,
 * but it would be prevented in different places ({@code B()} or {@code C()}, respectively). That'd mean there's 3 situations possible:
 * <ol>
 * <li>{@code C()} calls {@code A()} and gets {@code null} as the result (if {@code A()} is first in the stack)</li>
 * <li>{@code C()} calls {@code A()} which calls {@code B()} and gets {@code null} as the result (if {@code B()} is first in the stack)</li>
 * <li>{@code C()} calls {@code A()} which calls {@code B()} which calls {@code C()} and gets {@code null} as the result (if {@code C()} is first in the stack)</li>
 * </ol>
 * Most likely, the results of {@code C()} would be different in those 3 cases, and it'd be unwise to cache just any of them randomly,
 * whatever is calculated first. In a multi-threaded environment, that'd lead to unpredictability.<p></p>
 * <p>
 * Of the 3 possible scenarios above, caching for {@code C()} makes sense only for the last one, because that's the result we'd get if there were no caching at all.
 * Therefore, if you use any kind of caching in an endless-recursion-prone environment, please ensure you don't cache incomplete results
 * that happen when you're inside the evil recursion loop.
 * {@code RecursionManager} assists in distinguishing this situation and allowing caching outside that loop, but disallowing it inside.<p></p>
 * <p>
 * To prevent caching incorrect values, please create a {@code private static final} field of {@link #createGuard} call, and then use
 * {@link RecursionManager#markStack()} and {@link RecursionGuard.StackStamp#mayCacheNow()}
 * on it.<p></p>
 * <p>
 * Note that the above only helps with idempotent recursion loops, that is, the ones that stabilize after one iteration, so that
 * {@code A()->B()->C()->null} returns the same value as {@code A()->B()->C()->A()->B()->C()->null} etc. If your functions lack that quality
 * (e.g. if they add items to some list), you won't get stable caching results ever, and your code will produce unpredictable results
 * with hard-to-catch bugs. Therefore, please strive for idempotence.
 *
 * @author peter
 */
@SuppressWarnings("UtilityClassWithoutPrivateConstructor")
public class RecursionManager {
    private static final Logger LOG = Logger.getInstance(RecursionManager.class);
    private static final ThreadLocal<CalculationStack> ourStack = ThreadLocal.withInitial(CalculationStack::new);
    private static boolean ourAssertOnPrevention;

    /**
     * Run the given computation, unless it's already running in this thread.
     * This is same as {@link RecursionGuard#doPreventingRecursion(Object, boolean, Supplier)},
     * without a need to bother to create {@link RecursionGuard}.
     */
    @Nullable
    public static <T> T doPreventingRecursion(@Nonnull Object key, boolean memoize, Supplier<T> computation) {
        return createGuard(computation.getClass().getName()).doPreventingRecursion(key, memoize, computation);
    }

    /**
     * @param id just some string to separate different recursion prevention policies from each other
     * @return a helper object which allow you to perform reentrancy-safe computations and check whether caching will be safe.
     * Don't use it unless you need to call it from several places in the code, inspect the computation stack and/or prohibit result caching.
     */
    @Nonnull
    public static <Key> RecursionGuard<Key> createGuard(final String id) {
        return new RecursionGuard<>() {
            @Override
            public <T> T doPreventingRecursion(@Nonnull Key key, boolean memoize, @Nonnull Supplier<T> computation) {
                MyKey realKey = new MyKey(id, key, true);
                CalculationStack stack = ourStack.get();

                if (stack.checkReentrancy(realKey)) {
                    if (ourAssertOnPrevention) {
                        throw new StackOverflowPreventedException("Endless recursion prevention occurred");
                    }
                    return null;
                }

                if (memoize) {
                    MemoizedValue memoized = stack.getMemoizedValue(realKey);
                    if (memoized != null) {
                        for (MyKey noCacheUntil : memoized.dependencies) {
                            stack.prohibitResultCaching(noCacheUntil);
                        }
                        //noinspection unchecked
                        return (T) memoized.value;
                    }
                }

                realKey = new MyKey(id, key, false);

                int sizeBefore = stack.progressMap.size();
                stack.beforeComputation(realKey);
                int sizeAfter = stack.progressMap.size();
                Set<MyKey> preventionsBefore = memoize ? new HashSet<>(stack.preventions) : Collections.emptySet();

                try {
                    T result = computation.get();

                    if (memoize) {
                        stack.maybeMemoize(realKey, result, preventionsBefore);
                    }

                    return result;
                }
                finally {
                    try {
                        stack.afterComputation(realKey, sizeBefore, sizeAfter);
                    }
                    catch (Throwable e) {
                        //noinspection ThrowFromFinallyBlock
                        throw new RuntimeException("Throwable in afterComputation", e);
                    }

                    stack.checkDepth("4");
                }
            }

            @Nonnull
            @Override
            public List<Key> currentStack() {
                ArrayList<Key> result = new ArrayList<>();
                LinkedHashMap<MyKey, Integer> map = ourStack.get().progressMap;
                for (MyKey pair : map.keySet()) {
                    if (pair.guardId.equals(id)) {
                        //noinspection unchecked
                        result.add((Key) pair.userObject);
                    }
                }
                return Collections.unmodifiableList(result);
            }

            @Override
            public void prohibitResultCaching(@Nonnull Object since) {
                MyKey realKey = new MyKey(id, since, false);
                CalculationStack stack = ourStack.get();
                stack.prohibitResultCaching(realKey);
            }
        };
    }

    /**
     * Used in pair with {@link RecursionGuard.StackStamp#mayCacheNow()} to ensure that cached are only the reliable values,
     * not depending on anything incomplete due to recursive prevention policies.
     * A typical usage is this:
     * {@code
     * RecursionGuard.StackStamp stamp = RecursionManager.createGuard("id").markStack();
     * <p>
     * Result result = doComputation();
     * <p>
     * if (stamp.mayCacheNow()) {
     * cache(result);
     * }
     * return result;
     * }
     *
     * @return an object representing the current stack state, managed by {@link RecursionManager}
     */
    @Nonnull
    public static RecursionGuard.StackStamp markStack() {
        int stamp = ourStack.get().reentrancyCount;
        return () -> stamp == ourStack.get().reentrancyCount;
    }

    private static class MyKey {
        final String guardId;
        final Object userObject;
        private final int myHashCode;
        private final boolean myCallEquals;

        MyKey(String guardId, @Nonnull Object userObject, boolean mayCallEquals) {
            this.guardId = guardId;
            this.userObject = userObject;
            // remember user object hashCode to ensure our internal maps consistency
            myHashCode = guardId.hashCode() * 31 + userObject.hashCode();
            myCallEquals = mayCallEquals;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MyKey that && guardId.equals(that.guardId))) {
                return false;
            }
            if (userObject == that.userObject) {
                return true;
            }
            return (myCallEquals || that.myCallEquals)
                && userObject.equals(that.userObject);
        }

        @Override
        public int hashCode() {
            return myHashCode;
        }
    }

    private static class CalculationStack {
        private int reentrancyCount;
        private int depth;
        private final LinkedHashMap<MyKey, Integer> progressMap = new LinkedHashMap<>();
        private final Set<MyKey> preventions = ContainerUtil.newIdentityTroveSet();
        private final Map<MyKey, List<SoftReference<MemoizedValue>>> intermediateCache = ContainerUtil.createSoftMap();
        private int enters;
        private int exits;

        boolean checkReentrancy(MyKey realKey) {
            if (progressMap.containsKey(realKey)) {
                prohibitResultCaching(realKey);
                return true;
            }
            return false;
        }

        @Nullable
        MemoizedValue getMemoizedValue(MyKey realKey) {
            List<SoftReference<MemoizedValue>> refs = intermediateCache.get(realKey);
            if (refs != null) {
                for (SoftReference<MemoizedValue> ref : refs) {
                    MemoizedValue value = SoftReference.dereference(ref);
                    if (value != null && value.isActual(this)) {
                        return value;
                    }
                }
            }
            return null;
        }

        final void beforeComputation(MyKey realKey) {
            enters++;

            if (progressMap.isEmpty()) {
                assert reentrancyCount == 0 : "Non-zero stamp with empty stack: " + reentrancyCount;
            }

            checkDepth("1");

            int sizeBefore = progressMap.size();
            progressMap.put(realKey, reentrancyCount);
            depth++;

            checkDepth("2");

            int sizeAfter = progressMap.size();
            if (sizeAfter != sizeBefore + 1) {
                LOG.error("Key doesn't lead to the map size increase: " + sizeBefore + " " + sizeAfter + " " + realKey.userObject);
            }
        }

        void maybeMemoize(MyKey realKey, @Nullable Object result, Set<MyKey> preventionsBefore) {
            if (preventions.size() > preventionsBefore.size()) {
                List<MyKey> added = ContainerUtil.findAll(preventions, key -> key != realKey && !preventionsBefore.contains(key));
                intermediateCache.computeIfAbsent(realKey, __ -> new SmartList<>())
                    .add(new SoftReference<>(new MemoizedValue(result, added.toArray(new MyKey[0]))));
            }
        }

        final void afterComputation(MyKey realKey, int sizeBefore, int sizeAfter) {
            exits++;
            if (sizeAfter != progressMap.size()) {
                LOG.error("Map size changed: " + progressMap.size() + " " + sizeAfter + " " + realKey.userObject);
            }

            if (depth != progressMap.size()) {
                LOG.error("Inconsistent depth after computation; depth=" + depth + "; map=" + progressMap);
            }

            Integer value = progressMap.remove(realKey);
            depth--;
            if (!preventions.isEmpty()) {
                preventions.remove(realKey);
            }

            if (depth == 0) {
                intermediateCache.clear();
            }

            if (sizeBefore != progressMap.size()) {
                LOG.error("Map size doesn't decrease: " + progressMap.size() + " " + sizeBefore + " " + realKey.userObject);
            }

            reentrancyCount = value;
        }

        private void prohibitResultCaching(MyKey realKey) {
            reentrancyCount++;

            boolean inLoop = false;
            for (Map.Entry<MyKey, Integer> entry : new ArrayList<>(progressMap.entrySet())) {
                if (inLoop) {
                    entry.setValue(reentrancyCount);
                }
                else if (entry.getKey().equals(realKey)) {
                    preventions.add(entry.getKey());
                    inLoop = true;
                }
            }
        }

        private void checkDepth(String s) {
            int oldDepth = depth;
            if (oldDepth != progressMap.size()) {
                depth = progressMap.size();
                throw new AssertionError("_Inconsistent depth " + s + "; depth=" + oldDepth + "; enters=" + enters + "; exits=" + exits + "; map=" + progressMap);
            }
        }
    }

    private static class MemoizedValue {
        final Object value;
        final MyKey[] dependencies;

        MemoizedValue(Object value, MyKey[] dependencies) {
            this.value = value;
            this.dependencies = dependencies;
        }

        boolean isActual(CalculationStack stack) {
            return Stream.of(dependencies).allMatch(stack.progressMap::containsKey);
        }
    }

    @TestOnly
    public static void assertOnRecursionPrevention(@Nonnull Disposable parentDisposable) {
        ourAssertOnPrevention = true;
        Disposer.register(
            parentDisposable,
            () -> {
                //noinspection AssignmentToStaticFieldFromInstanceMethod
                ourAssertOnPrevention = false;
            }
        );
    }

    @TestOnly
    public static void disableAssertOnRecursionPrevention() {
        ourAssertOnPrevention = false;
    }
}
