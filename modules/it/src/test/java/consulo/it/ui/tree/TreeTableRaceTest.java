/*
 * Copyright 2013-2026 consulo.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.it.ui.tree;

import consulo.disposer.Disposer;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.HeadlessUIThread;
import consulo.it.ManualTreeExecutor;
import consulo.it.TreeTester;
import consulo.localize.LocalizeValue;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.impl.tree.TreeNodeImpl;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static consulo.it.ui.tree.MapTreeModel.ROOT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class TreeTableRaceTest {
    private final ManualTreeExecutor myExecutor = new ManualTreeExecutor();
    private final Map<String, Integer> mySamples = new ConcurrentHashMap<>();
    private final AtomicInteger myComputations = new AtomicInteger();
    private final Set<Boolean> myComputedOnUIThread = ConcurrentHashMap.newKeySet();

    private volatile @Nullable Consumer<String> myComputeHook;
    private @Nullable TreeTester<String> myTester;

    @AfterEach
    public void tearDown() {
        try {
            assertThat(myComputedOnUIThread).containsExactly(false);
        }
        finally {
            TreeTester<String> tester = myTester;
            if (tester != null) {
                Disposer.dispose(tester.getTree().destroyHook());
            }
        }
    }

    private TreeTester<String> create(MapTreeModel model) {
        TreeTester<String> tester = TreeTester.createTable(ROOT, model, myExecutor);
        tester.getTreeTable().addColumn(LocalizeValue.of("Samples"), this::samples).setSortable(Comparator.naturalOrder());
        myTester = tester;
        return tester;
    }

    private Integer samples(String value) {
        myComputedOnUIThread.add(UIAccess.isUIThread());
        myComputations.incrementAndGet();

        Consumer<String> hook = myComputeHook;
        if (hook != null) {
            myComputeHook = null;
            hook.accept(value);
        }
        return mySamples.getOrDefault(value, 0);
    }

    private TreeTester<String> show(MapTreeModel model) {
        TreeTester<String> tester = create(model).bind();
        drain(tester);
        return tester;
    }

    private static Runnable holdUIThread(TreeTester<String> tester) {
        UIAccess access = Objects.requireNonNull(tester.getTree().getUIAccess());
        CountDownLatch held = new CountDownLatch(1);
        CountDownLatch released = new CountDownLatch(1);
        access.give(() -> {
            held.countDown();
            await(released);
        });
        await(held);
        return released::countDown;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new AssertionError("Timed out waiting for the UI thread");
            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private void drain(TreeTester<String> tester) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        do {
            myExecutor.runAll();
            tester.flush();

            if (System.nanoTime() > deadline) {
                throw new AssertionError("The tree did not drain:\n" + tester.dump());
            }
        }
        while (myExecutor.getPendingCount() > 0 || !tester.isIdle());
    }

    @Test
    public void valuesAreOnlyComputedByTheExecutor() {
        mySamples.putAll(Map.of("a", 2, "b", 1));
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a", "b")
            .put("a", "a1");
        TreeTester<String> tester = create(model).bind();
        tester.flush();

        assertThat(myExecutor.getPendingCount()).isPositive();
        assertThat(myComputations).hasValue(0);
        assertThat(tester.dump()).isEmpty();

        drain(tester);

        assertThat(tester.dump()).isEqualTo("""
            +a | 2
            b | 1
            """);
        assertThat(myComputations).hasValue(2);
    }

    @Test
    public void aSortAskedWhileALevelIsBuildingAppliesWhenTheLevelLands() {
        mySamples.putAll(Map.of("a", 1, "a1", 1, "a2", 3, "a3", 2));
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a")
            .put("a", "a1", "a2", "a3");
        TreeTester<String> tester = show(model);

        tester.getTree().expand(tester.node("a"));
        tester.flush();
        assertThat(myExecutor.getPendingCount()).isEqualTo(1);

        tester.userClickHeader(0).userClickHeader(0);
        drain(tester);

        assertThat(tester.dump()).isEqualTo("""
            -a | 1
             a2 | 3
             a3 | 2
             a1 | 1
            """);
    }

    @Test
    public void aColumnAddedWhileALevelIsBuildingIsComputedForThatLevelToo() {
        mySamples.putAll(Map.of("a", 2, "b", 1));
        MapTreeModel model = new MapTreeModel().put(ROOT, "a", "b");
        TreeTester<String> tester = create(model).bind();
        tester.flush();
        myExecutor.runAll();

        tester.getTreeTable().addColumn(LocalizeValue.of("Length"), String::length);
        drain(tester);

        assertThat(tester.dump()).isEqualTo("""
            a | 2 | 1
            b | 1 | 1
            """);
    }

    @Test
    public void aColumnAddedAfterALevelReadTheColumnVersionIsComputedWhenTheLevelLands() {
        mySamples.putAll(Map.of("a", 2, "b", 1));
        MapTreeModel model = new MapTreeModel().put(ROOT, "a", "b");
        TreeTester<String> tester = create(model);
        myComputeHook = value -> {
            HeadlessUIThread.run(() -> tester.getTreeTable().addColumn(LocalizeValue.of("Length"), String::length));
            assertThat(myExecutor.runLast()).isTrue();
        };
        tester.bind().flush();

        myExecutor.runAll();
        tester.flush();

        assertThat(myComputeHook).isNull();
        assertThat(tester.dump()).isEqualTo("""
            a | 2 |
            b | 1 | 1
            """);
        assertThat(myExecutor.getPendingCount()).isEqualTo(1);

        drain(tester);

        assertThat(tester.dump()).isEqualTo("""
            a | 2 | 1
            b | 1 | 1
            """);
    }

    @Test
    public void recomputedValuesReachTheNodeOnlyOnTheUIThread() {
        mySamples.putAll(Map.of("a", 2, "b", 1));
        TreeTester<String> tester = show(new MapTreeModel().put(ROOT, "a", "b"));
        TreeNodeImpl<String> b = (TreeNodeImpl<String>) tester.node("b");

        mySamples.put("b", 5);
        tester.getTree().refreshItem(b, false);

        Runnable release = holdUIThread(tester);
        try {
            assertThat(myExecutor.runAll()).isEqualTo(1);
            assertThat(b.getColumnValues()).containsExactly(1);
        }
        finally {
            release.run();
        }

        drain(tester);
        assertThat(b.getColumnValues()).containsExactly(5);
    }

    @Test
    public void aRecomputeOfAValueWhichARebuildReplacedIsDropped() {
        String first = new String("a");
        String second = new String("a");
        MapTreeModel model = new MapTreeModel().put(ROOT, first);
        TreeTester<String> tester = create(model);
        tester.getTreeTable().addColumn(LocalizeValue.of("Instance"), value -> value == second ? "second" : "first");
        tester.bind();
        drain(tester);
        TreeNode<String> a = tester.node("a");
        assertThat(tester.dump()).isEqualTo("a | 0 | first\n");

        tester.getTree().refreshItem(a, false);
        model.put(ROOT, second);
        tester.getTree().refreshAll();
        assertThat(myExecutor.getPendingCount()).isEqualTo(2);

        Runnable release = holdUIThread(tester);
        try {
            assertThat(myExecutor.runLast()).isTrue();
            assertThat(myExecutor.runNext()).isTrue();
        }
        finally {
            release.run();
        }
        drain(tester);

        assertThat(tester.node("a")).isSameAs(a);
        assertThat(a.getValue()).isSameAs(second);
        assertThat(tester.dump()).isEqualTo("a | 0 | second\n");
    }

    @Test
    public void aRefreshRecomputesTheValuesOnTheExecutor() {
        mySamples.putAll(Map.of("a", 2, "b", 1));
        MapTreeModel model = new MapTreeModel().put(ROOT, "a", "b");
        TreeTester<String> tester = show(model);
        tester.sortBy(0, true);
        drain(tester);
        assertThat(tester.dump()).isEqualTo("""
            b | 1
            a | 2
            """);

        mySamples.put("b", 5);
        tester.getTree().refreshItem(tester.node("b"), false);
        tester.flush();
        assertThat(tester.dump()).isEqualTo("""
            b | 1
            a | 2
            """);

        drain(tester);
        assertThat(tester.dump()).isEqualTo("""
            a | 2
            b | 5
            """);
    }
}
