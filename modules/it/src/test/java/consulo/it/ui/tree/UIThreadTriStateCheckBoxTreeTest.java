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
import consulo.it.HeadlessUIThread;
import consulo.it.TreeTester;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.TreeExecutor;
import consulo.ui.TriStateCheckBoxTree;
import consulo.util.lang.ThreeState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static consulo.it.ui.tree.MapTreeModel.ROOT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public class UIThreadTriStateCheckBoxTreeTest extends CheckBoxTreeContractTestCase {
    @Override
    protected TreeExecutor createExecutor(Application application, Disposable parent) {
        return TreeExecutor.uiThread();
    }

    @Override
    protected TreeTester<String> createTester(MapTreeModel model, TreeExecutor executor) {
        return TreeTester.createTriStateCheckBoxTree(ROOT, model, executor);
    }

    private static TriStateCheckBoxTree<String> triState(TreeTester<String> tester) {
        return (TriStateCheckBoxTree<String>) tester.getCheckableTree();
    }

    private static List<String> recordStates(TreeTester<String> tester) {
        List<String> states = new CopyOnWriteArrayList<>();
        tester.getCheckableTree().addCheckListener(event -> states.add(event.getValue() + "=" + event.getState()));
        return states;
    }

    @Test
    public void anUnsureValueShowsMixedAndStaysOnItself(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.FULL_SYNC);
        TriStateCheckBoxTree<String> tree = triState(tester);
        List<String> states = recordStates(tester);

        HeadlessUIThread.run(() -> tree.setCheckState("App", ThreeState.UNSURE));

        assertThat(tree.getOwnCheckState("App")).isEqualTo(ThreeState.UNSURE);
        assertThat(tree.isChecked("App")).isFalse();
        assertThat(tree.getCheckState("App")).isEqualTo(ThreeState.UNSURE);
        assertThat(tree.getCheckState("main")).isEqualTo(ThreeState.UNSURE);
        assertThat(tree.getCheckState("src")).isEqualTo(ThreeState.UNSURE);
        assertThat(tree.getOwnCheckState("Lib")).isEqualTo(ThreeState.NO);
        assertThat(tree.getCheckedValues()).isEmpty();
        assertThat(states).containsExactly("App=UNSURE");
    }

    @Test
    public void aClickMovesUnsureToYesThenToggles(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.INDEPENDENT);
        TriStateCheckBoxTree<String> tree = triState(tester);
        HeadlessUIThread.run(() -> tree.setCheckState("guide", ThreeState.UNSURE));
        List<String> states = recordStates(tester);

        assertThat(tester.userClickCheckBox("docs", "guide")).isTrue();
        assertThat(tree.getOwnCheckState("guide")).isEqualTo(ThreeState.YES);

        assertThat(tester.userClickCheckBox("docs", "guide")).isTrue();
        assertThat(tree.getOwnCheckState("guide")).isEqualTo(ThreeState.NO);

        assertThat(tester.userClickCheckBox("docs", "guide")).isTrue();
        assertThat(tree.getOwnCheckState("guide")).isEqualTo(ThreeState.YES);

        assertThat(states).containsExactly("guide=YES", "guide=NO", "guide=YES");
    }

    @Test
    public void aClickReachesUnsureOnlyWhenEnabled(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.INDEPENDENT);
        TriStateCheckBoxTree<String> tree = triState(tester);
        assertThat(tree.isUnsureEnabled()).isFalse();

        HeadlessUIThread.run(() -> tree.setUnsureEnabled(true));

        tester.userClickCheckBox("docs", "api");
        assertThat(tree.getOwnCheckState("api")).isEqualTo(ThreeState.UNSURE);

        tester.userClickCheckBox("docs", "api");
        assertThat(tree.getOwnCheckState("api")).isEqualTo(ThreeState.YES);

        tester.userClickCheckBox("docs", "api");
        assertThat(tree.getOwnCheckState("api")).isEqualTo(ThreeState.NO);
    }

    @Test
    public void checkingAParentReplacesUnsureChildren(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.FULL_SYNC);
        TriStateCheckBoxTree<String> tree = triState(tester);
        HeadlessUIThread.run(() -> tree.setCheckState("guide", ThreeState.UNSURE));
        List<String> states = recordStates(tester);

        assertThat(tester.userClickCheckBox("docs")).isTrue();

        assertThat(tree.getOwnCheckState("guide")).isEqualTo(ThreeState.YES);
        assertThat(tree.getOwnCheckState("api")).isEqualTo(ThreeState.YES);
        assertThat(tree.getCheckState("docs")).isEqualTo(ThreeState.YES);
        assertThat(states).containsExactlyInAnyOrder("docs=YES", "guide=YES", "api=YES");
    }

    @Test
    public void setCheckStateYesSpreadsLikeSetChecked(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.CASCADE);
        TriStateCheckBoxTree<String> tree = triState(tester);

        HeadlessUIThread.run(() -> tree.setCheckState("main", ThreeState.YES));

        assertThat(tree.getCheckedValues()).containsExactly("main", "App", "Lib");
    }
}
