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

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.AllowLogError;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.TreeTester;
import consulo.localize.LocalizeValue;
import consulo.ui.ComponentItemRender;
import consulo.ui.Label;
import consulo.ui.TableColumn;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeNode;
import consulo.ui.TreeTable;
import consulo.ui.UIAccess;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static consulo.it.ui.tree.MapTreeModel.ROOT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtendWith(HeadlessApplicationExtension.class)
public abstract class TreeTableContractTestCase {
    private static final LocalizeValue SAMPLES = LocalizeValue.of("Samples");

    private final Disposable myDisposable = Disposable.newDisposable("TreeTableContractTestCase");

    private final Map<String, Integer> mySamples = new ConcurrentHashMap<>();
    private final Set<Boolean> myComputedOnUIThread = ConcurrentHashMap.newKeySet();
    private final List<@Nullable String> myComputedFor = new CopyOnWriteArrayList<>();
    private final AtomicInteger myComputations = new AtomicInteger();

    protected abstract TreeExecutor createExecutor(Application application, Disposable parent);

    protected abstract boolean isComputedOnUIThread();

    @AfterEach
    public void tearDown() {
        try {
            assertThat(myComputedOnUIThread).containsExactly(isComputedOnUIThread());
        }
        finally {
            Disposer.dispose(myDisposable);
        }
    }

    private TreeTester<String> create(Application application, MapTreeModel model) {
        TreeTester<String> tester = TreeTester.createTable(null, model, createExecutor(application, myDisposable));
        Disposer.register(myDisposable, tester.getTree().destroyHook());
        tester.getTreeTable().addColumn(SAMPLES, this::samples).setSortable(Comparator.naturalOrder());
        return tester;
    }

    private TreeTester<String> show(Application application, MapTreeModel model) {
        return create(application, model).show();
    }

    private @Nullable Integer samples(@Nullable String value) {
        myComputedOnUIThread.add(UIAccess.isUIThread());
        myComputedFor.add(value);
        myComputations.incrementAndGet();
        return value == null ? null : mySamples.get(value);
    }

    private MapTreeModel project() {
        mySamples.putAll(Map.of(
            "src", 30,
            "docs", 50,
            "README", 10,
            "main", 5,
            "test", 20,
            "App", 7,
            "Lib", 3,
            "guide", 1,
            "api", 9
        ));
        return new MapTreeModel()
            .put(ROOT, "src", "docs", "README")
            .put("src", "main", "test")
            .put("main", "App", "Lib")
            .put("docs", "guide", "api");
    }

    @Test
    public void valuesAreComputedWhereTheExecutorRunsAndCachedOnTheNode(Application application) {
        TreeTester<String> tester = show(application, project());

        tester.assertStructure("""
            +src | 30
            +docs | 50
            README | 10
            """);
        assertThat(myComputedOnUIThread).containsExactly(isComputedOnUIThread());
        assertThat(myComputedFor).doesNotContainNull().containsOnly("src", "docs", "README");

        int computations = myComputations.get();
        tester.dump();
        tester.userSelect("docs").settle();
        tester.dump();

        assertThat(myComputations).hasValue(computations);
    }

    @Test
    public void headerClicksCycleAscendingDescendingAndModelOrderOnEveryLevel(Application application) {
        TreeTester<String> tester = show(application, project());
        tester.getTree().expand(tester.node("src"), 2);
        String modelOrder = """
            -src | 30
             -main | 5
              App | 7
              Lib | 3
             test | 20
            +docs | 50
            README | 10
            """;
        tester.assertStructure(modelOrder);

        tester.userClickHeader(0).assertStructure("""
            README | 10
            -src | 30
             -main | 5
              Lib | 3
              App | 7
             test | 20
            +docs | 50
            """);
        assertThat(tester.getSortColumn()).isZero();
        assertThat(tester.isSortAscending()).isTrue();

        tester.userClickHeader(0).assertStructure("""
            +docs | 50
            -src | 30
             test | 20
             -main | 5
              App | 7
              Lib | 3
            README | 10
            """);
        assertThat(tester.getSortColumn()).isZero();
        assertThat(tester.isSortAscending()).isFalse();

        tester.userClickHeader(0).assertStructure(modelOrder);
        assertThat(tester.getSortColumn()).isEqualTo(-1);
    }

    @Test
    public void aLevelLoadedAfterSortingIsSortedToo(Application application) {
        TreeTester<String> tester = show(application, project());
        tester.userClickHeader(0).userClickHeader(0).settle();

        tester.userExpand("docs");

        tester.assertStructure("""
            -docs | 50
             api | 9
             guide | 1
            +src | 30
            README | 10
            """);
    }

    @Test
    public void rowsWithEqualValuesKeepTheModelOrderAscendingAndDescending(Application application) {
        mySamples.putAll(Map.of("b", 2, "x", 1, "a", 2, "y", 1));
        TreeTester<String> tester = show(application, new MapTreeModel().put(ROOT, "b", "x", "a", "y"));

        tester.sortBy(0, true).assertStructure("""
            x | 1
            y | 1
            b | 2
            a | 2
            """);

        tester.sortBy(0, false).assertStructure("""
            b | 2
            a | 2
            x | 1
            y | 1
            """);

        tester.clearSort().assertStructure("""
            b | 2
            x | 1
            a | 2
            y | 1
            """);
    }

    @Test
    public void rowsWhoseValuesBecomeEqualFallBackToTheModelOrder(Application application) {
        mySamples.putAll(Map.of("b", 3, "x", 2, "a", 1));
        TreeTester<String> tester = show(application, new MapTreeModel().put(ROOT, "b", "x", "a"));
        tester.sortBy(0, true).assertStructure("""
            a | 1
            x | 2
            b | 3
            """);

        mySamples.putAll(Map.of("b", 5, "x", 5, "a", 5));
        tester.getTreeTable().addColumn(LocalizeValue.of("Length"), String::length);

        tester.assertStructure("""
            b | 5 | 1
            x | 5 | 1
            a | 5 | 1
            """);
    }

    @Test
    @AllowLogError("consulo.ui.impl.tree.TreeTableColumns")
    public void aThrowingColumnLeavesItsCellsEmptyAndTheRestOfTheLevelIntact(Application application) {
        TreeTester<String> tester = create(application, project());
        TreeTable<String> table = tester.getTreeTable();
        table.addColumn(LocalizeValue.of("Built"), TreeTableContractTestCase::lengthExceptDocs);
        tester.show();

        table.addColumn(LocalizeValue.of("Added"), TreeTableContractTestCase::lengthExceptDocs);
        tester.sortBy(0, true);

        tester.assertStructure("""
            README | 10 | 6 | 6
            +src | 30 | 3 | 3
            +docs | 50 | |
            """);
    }

    private static int lengthExceptDocs(String value) {
        if (value.equals("docs")) {
            throw new IllegalStateException("No length for " + value);
        }
        return value.length();
    }

    @Test
    public void rowsWithoutAValueSortFirstAscendingAndLastDescending(Application application) {
        mySamples.putAll(Map.of("a", 2, "b", 1));
        MapTreeModel model = new MapTreeModel().put(ROOT, "a", "LICENSE", "b");
        TreeTester<String> tester = show(application, model);

        tester.sortBy(0, true).assertStructure("""
            LICENSE |
            b | 1
            a | 2
            """);

        tester.sortBy(0, false).assertStructure("""
            a | 2
            b | 1
            LICENSE |
            """);

        tester.clearSort().assertStructure("""
            a | 2
            LICENSE |
            b | 1
            """);
    }

    @Test
    public void refreshAllKeepsTheSortTheOpenNodesAndTheSelectionAndRecomputesTheValues(Application application) {
        TreeTester<String> tester = show(application, project());
        tester.getTree().expand(tester.node("src"), 2);
        tester.settle();
        tester.getTree().select(tester.node("src", "main", "App"));
        tester.sortBy(0, true).settle();
        TreeNode<String> main = tester.node("src", "main");
        int computations = myComputations.get();

        mySamples.putAll(Map.of("README", 40, "test", 1, "App", 2));
        tester.getTree().refreshAll();

        tester.assertStructure("""
            -src | 30
             test | 1
             -main | 5
              [App] | 2
              Lib | 3
            README | 40
            +docs | 50
            """);
        assertThat(tester.node("src", "main")).isSameAs(main);
        assertThat(tester.getSortColumn()).isZero();
        assertThat(tester.isSortAscending()).isTrue();
        assertThat(myComputations.get()).isGreaterThan(computations);
    }

    @Test
    public void refreshItemRecomputesTheValueAndMovesTheRow(Application application) {
        TreeTester<String> tester = show(application, project());
        tester.sortBy(0, true).assertStructure("""
            README | 10
            +src | 30
            +docs | 50
            """);

        mySamples.put("README", 99);
        tester.getTree().refreshItem(tester.node("README"), false);

        tester.assertStructure("""
            +src | 30
            +docs | 50
            README | 99
            """);
    }

    @Test
    public void addColumnAfterTheTreeIsShownComputesItsValues(Application application) {
        TreeTester<String> tester = show(application, project());
        tester.getTree().expand(tester.node("src"));
        tester.settle();
        TreeTable<String> table = tester.getTreeTable();
        table.setTreeColumnHeader(LocalizeValue.of("Name"));

        table.addColumn(LocalizeValue.of("Length"), String::length);

        tester.assertStructure("""
            -src | 30 | 3
             +main | 5 | 4
             test | 20 | 4
            +docs | 50 | 4
            README | 10 | 6
            """);
        assertThat(tester.headers()).containsExactly("Name", "Samples", "Length");
        assertThat(table.getColumns()).hasSize(2);

        tester.userClickHeader(1).settle();
        assertThat(tester.getSortColumn()).isEqualTo(-1);
    }

    @Test
    public void aColumnWhichStopsBeingSortableDropsTheSort(Application application) {
        TreeTester<String> tester = show(application, project());
        TableColumn<String, Integer> length = tester.getTreeTable().addColumn(LocalizeValue.of("Length"), String::length)
            .setSortable(Comparator.naturalOrder());
        tester.sortBy(1, false).assertStructure("""
            README | 10 | 6
            +docs | 50 | 4
            +src | 30 | 3
            """);

        length.setSortable(null);

        tester.assertStructure("""
            +src | 30 | 3
            +docs | 50 | 4
            README | 10 | 6
            """);
        assertThat(tester.getSortColumn()).isEqualTo(-1);
    }

    @Test
    public void columnRendersPaintTheCachedValueOnTheUIThread(Application application) {
        Set<Boolean> renderedOnUIThread = ConcurrentHashMap.newKeySet();
        mySamples.putAll(Map.of("a", 2, "b", 1));
        TreeTester<String> tester = create(application, new MapTreeModel().put(ROOT, "a", "b"));
        TreeTable<String> table = tester.getTreeTable();
        table.addColumn(LocalizeValue.of("Share"), mySamples::get).setRender((presentation, item, row) -> {
            renderedOnUIThread.add(UIAccess.isUIThread());
            presentation.append(row + "=" + item.getValue());
        });
        table.addColumn(LocalizeValue.of("Unit"), mySamples::get).setRender((presentation, item) -> {
            renderedOnUIThread.add(UIAccess.isUIThread());
            presentation.append(item.getValue() + " ms");
        });
        table.addColumn(LocalizeValue.of("Bar"), mySamples::get)
            .setRender((ComponentItemRender<Integer>) item -> Label.create(LocalizeValue.of("component")));
        tester.show();

        tester.userSelect("b").assertStructure("""
            a | 2 | a=2 | 2 ms | 2
            [b] | 1 | b=1 | 1 ms | 1
            """);
        assertThat(renderedOnUIThread).containsExactly(true);
    }
}
