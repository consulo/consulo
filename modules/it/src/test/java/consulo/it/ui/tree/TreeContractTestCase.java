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
import consulo.it.HeadlessApplicationExtension;
import consulo.it.TreeTester;
import consulo.ui.Tree;
import consulo.ui.TreeExecutor;
import consulo.ui.TreeNode;
import consulo.ui.ex.tree.UITreeState;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static consulo.it.ui.tree.MapTreeModel.ROOT;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
@ExtendWith(HeadlessApplicationExtension.class)
public abstract class TreeContractTestCase {
    private static final long TIMEOUT = 30;

    private final Disposable myDisposable = Disposable.newDisposable("TreeContractTestCase");

    protected abstract TreeExecutor createExecutor(Application application, Disposable parent);

    protected abstract boolean isRenderedOnUIThread();

    protected abstract boolean isCancelledBuildRestarted();

    @AfterEach
    public void tearDown() {
        Disposer.dispose(myDisposable);
    }

    private TreeTester<String> create(Application application, MapTreeModel model) {
        TreeTester<String> tester = TreeTester.create(ROOT, model, createExecutor(application, myDisposable));
        Disposer.register(myDisposable, tester.getTree().destroyHook());
        return tester;
    }

    private TreeTester<String> show(Application application, MapTreeModel model) {
        return create(application, model).show();
    }

    private static MapTreeModel chain(int depth) {
        MapTreeModel model = new MapTreeModel().put(ROOT, "l1");
        for (int level = 1; level < depth; level++) {
            model.put("l" + level, "l" + (level + 1));
        }
        return model;
    }

    private static List<List<String>> paths(Tree<String> tree) {
        return tree.getExpandedPaths().stream().map(path -> path.stream().map(TreeNode::getValue).toList()).toList();
    }

    private static String valueOf(@Nullable TreeNode<String> node) {
        return node == null ? "null" : String.valueOf(node.getValue());
    }

    @Test
    public void showBuildsTheTopLevelOnce(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "src", "docs", "README")
            .put("src", "Main")
            .put("docs");

        TreeTester<String> tester = show(application, model);

        tester.assertStructure("""
            +src
            +docs
            README
            """);
        assertThat(model.getBuildCount(ROOT)).isEqualTo(1);
    }

    @Test
    public void refreshAllBuildsATreeNobodyShowed(Application application) throws Exception {
        MapTreeModel model = new MapTreeModel().put(ROOT, "a");
        TreeTester<String> tester = create(application, model);

        tester.getTree().refreshAll().get(TIMEOUT, SECONDS);

        tester.assertStructure("a\n");
    }

    @Test
    public void workAskedBeforeTheTreeIsShownIsDoneOnceItIs(Application application) {
        TreeTester<String> tester = create(application, chain(3)).detach();

        tester.getTree().expandAll();

        tester.show().assertStructure("""
            -l1
             -l2
              l3
            """);
    }

    @Test
    public void reattachedTreeShowsWhatChangedWhileItWasAway(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a", "b")
            .put("a", "a1");
        TreeTester<String> tester = show(application, model);
        tester.getTree().expand(tester.node("a"));
        tester.settle();
        tester.getTree().select(tester.node("a", "a1"));
        tester.settle();

        tester.detach();
        model.put("a", "a1", "a2");
        tester.getTree().refreshAll();

        tester.show().assertStructure("""
            -a
             [a1]
             a2
            b
            """);
    }

    @Test
    public void openingALevelBuildsItOnce(Application application) throws Exception {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "src")
            .put("src", "Main", "Util");
        TreeTester<String> tester = show(application, model);
        TreeNode<String> src = tester.node("src");

        tester.getTree().expand(src);
        CompletableFuture<TreeNode<String>> found = src.findChild("Util"::equals);
        tester.userExpand("src");

        tester.assertStructure("""
            -src
             Main
             Util
            """);
        assertThat(valueOf(found.get(TIMEOUT, SECONDS))).isEqualTo("Util");
        assertThat(model.getBuildCount("src")).isEqualTo(1);
    }

    @Test
    public void levelFollowsTheComparatorOfTheModel(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "gamma", "alpha", "beta")
            .sortBy(Comparator.comparing(node -> String.valueOf(node.getValue())));

        show(application, model).assertStructure("""
            alpha
            beta
            gamma
            """);
    }

    @Test
    public void rendererRunsWhereTheExecutorRuns(Application application) {
        MapTreeModel model = new MapTreeModel().put(ROOT, "a", "b");

        show(application, model);

        assertThat(model.getRenderedOnUIThread()).containsExactly(isRenderedOnUIThread());
    }

    @Test
    public void anEmptyLevelTurnsTheNodeIntoALeaf(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "docs")
            .put("docs");
        TreeTester<String> tester = show(application, model);
        tester.assertStructure("+docs\n");

        tester.getTree().expand(tester.node("docs"));

        tester.assertStructure("docs\n");
        assertThat(paths(tester.getTree())).isEmpty();
    }

    @Test
    public void refreshTurnsAFilledFolderBackIntoABranch(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "docs")
            .put("docs");
        TreeTester<String> tester = show(application, model);
        tester.getTree().expand(tester.node("docs"));
        tester.assertStructure("docs\n");

        model.put("docs", "guide");
        tester.getTree().refreshItem(tester.node("docs"), true);
        tester.assertStructure("+docs\n");

        tester.getTree().expand(tester.node("docs"));
        tester.assertStructure("""
            -docs
             guide
            """);
    }

    @Test
    public void refreshAllKeepsOpenNodesAndTheSelection(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "src", "test")
            .put("src", "main", "util")
            .put("main", "App");
        TreeTester<String> tester = show(application, model);
        Tree<String> tree = tester.getTree();
        tree.expand(tester.node("src"), 2);
        tester.settle();
        tree.select(tester.node("src", "main", "App"));
        tester.assertStructure("""
            -src
             -main
              [App]
             util
            test
            """);
        TreeNode<String> main = tester.node("src", "main");

        model.put("src", "main", "util", "extra");
        tree.refreshAll();

        tester.assertStructure("""
            -src
             -main
              [App]
             util
             extra
            test
            """);
        assertThat(tester.node("src", "main")).isSameAs(main);
    }

    @Test
    public void refreshItemKeepsWhatWasOpenBelowIt(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "src")
            .put("src", "main")
            .put("main", "App");
        TreeTester<String> tester = show(application, model);
        tester.getTree().expand(tester.node("src"), 2);
        tester.assertStructure("""
            -src
             -main
              App
            """);

        model.put("main", "App", "Lib");
        tester.getTree().refreshItem(tester.node("src"), true);

        tester.assertStructure("""
            -src
             -main
              App
              Lib
            """);
    }

    @Test
    public void droppedNodesLeaveTheExpandedPaths(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a", "b")
            .put("a", "a1")
            .put("a1", "x");
        TreeTester<String> tester = show(application, model);
        tester.getTree().expand(tester.node("a"), 2);
        tester.settle();
        assertThat(paths(tester.getTree())).containsExactly(List.of(ROOT, "a"), List.of(ROOT, "a", "a1"));

        model.put(ROOT, "b");
        tester.getTree().refreshAll();

        tester.assertStructure("b\n");
        assertThat(paths(tester.getTree())).isEmpty();
    }

    @Test
    public void expandedPathsAreVisibleRowsOnly(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a")
            .put("a", "a1")
            .put("a1", "x");
        TreeTester<String> tester = show(application, model);
        tester.getTree().expand(tester.node("a"), 2);
        tester.settle();

        tester.userCollapse("a");
        tester.assertStructure("+a\n");
        assertThat(paths(tester.getTree())).isEmpty();

        tester.userExpand("a");
        tester.assertStructure("""
            -a
             -a1
              x
            """);
    }

    @Test
    public void expandOpensTheRequestedDepth(Application application) {
        TreeTester<String> tester = show(application, chain(6));

        tester.getTree().expand(tester.node("l1"), 2);

        tester.assertStructure("""
            -l1
             -l2
              +l3
            """);
    }

    @Test
    public void expandAllStopsAtItsDepth(Application application) {
        TreeTester<String> tester = show(application, chain(6));

        tester.getTree().expandAll();

        tester.assertStructure("""
            -l1
             -l2
              -l3
               -l4
                +l5
            """);
    }

    @Test
    public void collapseAllClosesHiddenLevelsToo(Application application) {
        TreeTester<String> tester = show(application, chain(4));
        tester.getTree().expand(tester.node("l1"), 3);
        tester.settle();

        tester.getTree().collapseAll();
        tester.assertStructure("+l1\n");

        tester.userExpand("l1");
        tester.assertStructure("""
            -l1
             +l2
            """);
    }

    @Test
    public void everyTransitionIsReportedOnce(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a")
            .put("a", "a1");
        TreeTester<String> tester = show(application, model);
        Tree<String> tree = tester.getTree();
        List<String> events = new CopyOnWriteArrayList<>();
        tree.addExpandListener(event -> events.add("expand " + valueOf(event.getValue())));
        tree.addCollapseListener(event -> events.add("collapse " + valueOf(event.getValue())));
        tree.addSelectListener(event -> events.add("select " + valueOf(event.getValue())));

        tree.expand(tester.node("a"));
        tester.settle();
        tree.expand(tester.node("a"));
        tester.userExpand("a");
        tester.settle();
        tester.userCollapse("a");
        tester.userCollapse("a");
        tree.select(tester.node("a", "a1"));
        tester.settle();
        tester.userSelect("a", "a1");
        tester.settle();

        assertThat(events).containsExactly("expand a", "collapse a", "expand a", "select a1");
    }

    @Test
    public void doubleClickTogglesTheRowWhenTheModelAgrees(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a")
            .put("a", "a1");
        TreeTester<String> tester = show(application, model);
        List<String> clicks = new CopyOnWriteArrayList<>();
        tester.getTree().addDoubleClickListener(event -> clicks.add(valueOf(event.getValue())));

        tester.userDoubleClick("a");
        tester.assertStructure("""
            -a
             a1
            """);

        tester.userDoubleClick("a");
        tester.assertStructure("+a\n");
        assertThat(clicks).containsExactly("a", "a");
    }

    @Test
    public void doubleClickTakenByTheModelLeavesTheRowAlone(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a")
            .put("a", "a1")
            .keepDoubleClicks();
        TreeTester<String> tester = show(application, model);
        List<String> clicks = new CopyOnWriteArrayList<>();
        tester.getTree().addDoubleClickListener(event -> clicks.add(valueOf(event.getValue())));

        tester.userDoubleClick("a");

        tester.assertStructure("+a\n");
        assertThat(clicks).containsExactly("a");
    }

    @Test
    public void selectRevealsTheNodeLevelByLevel(Application application) throws Exception {
        TreeTester<String> tester = show(application, chain(4));
        Tree<String> tree = tester.getTree();
        TreeNode<String> deepest = Objects.requireNonNull(tree.getRootNode()).findChildDeep("l4"::equals).get(TIMEOUT, SECONDS);

        tree.select(deepest);

        tester.assertStructure("""
            -l1
             -l2
              -l3
               [l4]
            """);
        assertThat(tree.getSelectedPath()).extracting(TreeNode::getValue).containsExactly(ROOT, "l1", "l2", "l3", "l4");
    }

    @Test
    public void prebuildDecidesTheLeafBeforeTheNodeIsOpened(Application application) {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "docs", "src")
            .put("docs")
            .put("src", "Main")
            .prebuild("docs")
            .prebuild("src");
        TreeTester<String> tester = show(application, model);

        tester.assertStructure("""
            docs
            +src
            """);

        tester.userExpand("src");
        tester.assertStructure("""
            docs
            -src
             Main
            """);
        assertThat(model.getBuildCount("src")).isEqualTo(1);
    }

    @Test
    public void cancelledBuildIsRestartedByTheExecutor(Application application) {
        assumeTrue(isCancelledBuildRestarted());

        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a")
            .put("a", "a1")
            .cancelBuilds("a", 3);
        TreeTester<String> tester = show(application, model);

        tester.getTree().expand(tester.node("a"));

        tester.assertStructure("""
            -a
             a1
            """);
        assertThat(model.getBuildCount("a")).isEqualTo(4);
    }

    @Test
    public void treeStateRestoresOverlappingPaths(Application application) throws Exception {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a", "b")
            .put("a", "a1", "a2")
            .put("a1", "x")
            .put("a2", "y");
        TreeTester<String> first = show(application, model);
        first.getTree().expand(first.node("a"), 2);
        first.settle();
        first.getTree().select(first.node("a", "a1", "x"));
        first.assertStructure("""
            -a
             -a1
              [x]
             -a2
              y
            b
            """);
        UITreeState state = UITreeState.createOn(first.getTree());

        TreeTester<String> second = show(application, model);
        state.applyTo(second.getTree()).get(TIMEOUT, SECONDS);

        second.assertStructure(first.dump());
        assertThat(model.getBuildCount("a")).isEqualTo(2);
    }
}
