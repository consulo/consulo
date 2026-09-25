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
import consulo.it.ManualTreeExecutor;
import consulo.it.TreeTester;
import consulo.ui.TreeNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static consulo.it.ui.tree.MapTreeModel.ROOT;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-09-25
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class TreeRaceTest {
    private final ManualTreeExecutor myExecutor = new ManualTreeExecutor();

    private TreeTester<String> show(MapTreeModel model) {
        TreeTester<String> tester = TreeTester.create(ROOT, model, myExecutor).bind();
        drain(tester);
        return tester;
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
    public void concurrentAsksShareOneBuild() throws Exception {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a")
            .put("a", "a1");
        TreeTester<String> tester = show(model);
        TreeNode<String> a = tester.node("a");

        tester.getTree().expand(a);
        tester.flush();
        CompletableFuture<TreeNode<String>> found = a.findChild("a1"::equals);
        tester.userExpand("a");

        assertThat(myExecutor.getPendingCount()).isEqualTo(1);
        drain(tester);

        assertThat(model.getBuildCount("a")).isEqualTo(1);
        assertThat(found.get(30, SECONDS).getValue()).isEqualTo("a1");
        assertThat(tester.dump()).isEqualTo("""
            -a
             a1
            """);
    }

    @Test
    public void aBuildOvertakenByARefreshIsDropped() {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a")
            .put("a", "first");
        TreeTester<String> tester = show(model);
        TreeNode<String> a = tester.node("a");

        tester.getTree().expand(a);
        tester.flush();
        tester.getTree().refreshItem(a, true);
        tester.flush();
        assertThat(myExecutor.getPendingCount()).isEqualTo(3);

        model.put("a", "second");
        myExecutor.runLast();
        tester.flush();

        model.put("a", "stale");
        myExecutor.runNext();
        drain(tester);

        assertThat(tester.dump()).isEqualTo("""
            -a
             second
            """);
        assertThat(model.getBuildCount("a")).isEqualTo(2);
    }

    @Test
    public void aBuildOfARemovedNodeIsDropped() {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a", "b")
            .put("a", "a1");
        TreeTester<String> tester = show(model);

        tester.getTree().expand(tester.node("a"));
        tester.flush();
        model.put(ROOT, "b");
        tester.getTree().refreshAll();
        tester.flush();

        myExecutor.runLast();
        tester.flush();
        myExecutor.runNext();
        drain(tester);

        assertThat(tester.dump()).isEqualTo("b\n");
        assertThat(tester.getTree().getExpandedPaths()).isEmpty();
    }

    @Test
    public void aCancelledBuildIsAskedForAgain() {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a")
            .put("a", "a1");
        TreeTester<String> tester = show(model);

        tester.getTree().expand(tester.node("a"));
        tester.flush();
        myExecutor.cancelNext();
        drain(tester);
        assertThat(tester.dump()).isEqualTo("+a\n");

        tester.getTree().expand(tester.node("a"));
        tester.flush();
        drain(tester);

        assertThat(tester.dump()).isEqualTo("""
            -a
             a1
            """);
        assertThat(model.getBuildCount("a")).isEqualTo(1);
    }

    @Test
    public void aBuildFinishedWhileDetachedIsAppliedOnceShownAgain() {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a")
            .put("a", "a1");
        TreeTester<String> tester = show(model);

        tester.getTree().expand(tester.node("a"));
        tester.flush();
        assertThat(myExecutor.getPendingCount()).isEqualTo(1);

        tester.detach();
        myExecutor.runAll();
        tester.flush();
        assertThat(tester.dump()).isEqualTo("+a\n");

        tester.bind();
        drain(tester);

        assertThat(tester.dump()).isEqualTo("""
            -a
             a1
            """);
    }

    @Test
    public void aDisposedTreeIgnoresLateBuilds() {
        MapTreeModel model = new MapTreeModel()
            .put(ROOT, "a")
            .put("a", "a1");
        TreeTester<String> tester = show(model);

        CompletableFuture<?> expanded = tester.getTree().expand(tester.node("a"));
        tester.flush();
        Disposer.dispose(tester.getTree().destroyHook());
        drain(tester);

        assertThat(expanded).isDone();
        assertThat(tester.dump()).isEqualTo("+a\n");
    }
}
