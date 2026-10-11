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
import consulo.it.HeadlessUIThread;
import consulo.it.TreeTester;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.CheckableTree;
import consulo.ui.TreeExecutor;
import consulo.ui.event.TreeCheckEvent;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.util.lang.ThreeState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static consulo.it.ui.tree.MapTreeModel.ROOT;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
@ExtendWith(HeadlessApplicationExtension.class)
public abstract class CheckBoxTreeContractTestCase {
    private static final String EXPANDED_PROJECT = """
        -( ) src
         -( ) main
          ( ) App
          ( ) Lib
         ( ) test
        -( ) docs
         ( ) guide
         ( ) api
        """;

    private final Disposable myDisposable = Disposable.newDisposable("CheckBoxTreeContractTestCase");

    private final List<String> myEvents = new CopyOnWriteArrayList<>();
    private final List<Boolean> myProgrammatic = new CopyOnWriteArrayList<>();

    protected abstract TreeExecutor createExecutor(Application application, Disposable parent);

    protected TreeTester<String> createTester(MapTreeModel model, TreeExecutor executor) {
        return TreeTester.createCheckBoxTree(ROOT, model, executor);
    }

    protected final List<String> getEvents() {
        return myEvents;
    }

    @AfterEach
    public void tearDown() {
        Disposer.dispose(myDisposable);
    }

    protected final TreeTester<String> create(Application application, MapTreeModel model, CheckBoxTreePolicy policy) {
        TreeTester<String> tester = createTester(model, createExecutor(application, myDisposable));
        Disposer.register(myDisposable, tester.getTree().destroyHook());

        CheckableTree<String> tree = tester.getCheckableTree();
        HeadlessUIThread.run(() -> tree.setCheckPolicy(policy));
        tree.addCheckListener(event -> record(event));
        return tester;
    }

    private void record(TreeCheckEvent<String> event) {
        myEvents.add(event.getValue() + "=" + event.isChecked());
        myProgrammatic.add(event.getInputDetails() instanceof ProgrammaticInputDetails);
    }

    private TreeTester<String> show(Application application, MapTreeModel model, CheckBoxTreePolicy policy) {
        return create(application, model, policy).show();
    }

    protected final TreeTester<String> showExpanded(Application application, MapTreeModel model, CheckBoxTreePolicy policy) {
        TreeTester<String> tester = show(application, model, policy);
        tester.getTree().expandAll();
        tester.settle();
        myEvents.clear();
        myProgrammatic.clear();
        return tester;
    }

    protected static MapTreeModel project() {
        return new MapTreeModel()
            .put(ROOT, "src", "docs")
            .put("src", "main", "test")
            .put("main", "App", "Lib")
            .put("docs", "guide", "api");
    }

    protected static void setChecked(TreeTester<String> tester, String value, boolean checked) {
        CheckableTree<String> tree = tester.getCheckableTree();
        HeadlessUIThread.run(() -> tree.setChecked(value, checked));
    }

    private static CheckBoxTreePolicy only(boolean checkChildren, boolean uncheckChildren, boolean checkParent, boolean uncheckParent) {
        return new CheckBoxTreePolicy(checkChildren, uncheckChildren, checkParent, uncheckParent);
    }

    @Test
    public void theTreeCascadesUnlessToldOtherwise(Application application) {
        TreeTester<String> tester = createTester(project(), createExecutor(application, myDisposable));
        Disposer.register(myDisposable, tester.getTree().destroyHook());

        assertThat(tester.getCheckableTree().getCheckPolicy()).isEqualTo(CheckBoxTreePolicy.CASCADE);
        assertThat(CheckBoxTreePolicy.CASCADE).isEqualTo(new CheckBoxTreePolicy(true, true, false, false));
        assertThat(CheckBoxTreePolicy.INDEPENDENT.isIndependent()).isTrue();
        assertThat(CheckBoxTreePolicy.FULL_SYNC.isIndependent()).isFalse();
    }

    @Test
    public void anIndependentPolicyChangesTheClickedValueAlone(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.INDEPENDENT);

        assertThat(tester.userClickCheckBox("src")).isTrue();
        assertThat(tester.userClickCheckBox("src", "main", "App")).isTrue();

        tester.assertStructure("""
            -(v) src
             -( ) main
              (v) App
              ( ) Lib
             ( ) test
            -( ) docs
             ( ) guide
             ( ) api
            """);
        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("src", "App");
        assertThat(tester.getCheckableTree().getCheckState("main")).isEqualTo(ThreeState.NO);
        assertThat(myEvents).containsExactly("src=true", "App=true");
    }

    @Test
    public void checkingAParentChecksEveryNodeBelowIt(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), only(true, false, false, false));

        tester.userClickCheckBox("src");

        tester.assertStructure("""
            -(v) src
             -(v) main
              (v) App
              (v) Lib
             (v) test
            -( ) docs
             ( ) guide
             ( ) api
            """);
        assertThat(myEvents).containsExactly("src=true", "main=true", "App=true", "Lib=true", "test=true");

        tester.userClickCheckBox("src");

        tester.assertStructure("""
            -(-) src
             -(v) main
              (v) App
              (v) Lib
             (v) test
            -( ) docs
             ( ) guide
             ( ) api
            """);
        assertThat(tester.getCheckableTree().isChecked("src")).isFalse();
        assertThat(tester.getCheckableTree().getCheckState("src")).isEqualTo(ThreeState.UNSURE);
    }

    @Test
    public void uncheckingAParentUnchecksEveryNodeBelowIt(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), only(false, true, false, false));
        setChecked(tester, "src", true);
        setChecked(tester, "main", true);
        setChecked(tester, "App", true);

        tester.userClickCheckBox("src");

        tester.assertStructure(EXPANDED_PROJECT);
        assertThat(tester.getCheckableTree().getCheckedValues()).isEmpty();
        assertThat(myEvents).containsExactly("src=true", "main=true", "App=true", "src=false", "main=false", "App=false");
    }

    @Test
    public void checkingAChildChecksEveryNodeAboveIt(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), only(false, false, true, false));

        tester.userClickCheckBox("src", "main", "App");

        tester.assertStructure("""
            -(-) src
             -(-) main
              (v) App
              ( ) Lib
             ( ) test
            -( ) docs
             ( ) guide
             ( ) api
            """);
        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("src", "main", "App");
        assertThat(myEvents).containsExactly("App=true", "main=true", "src=true");
    }

    @Test
    public void uncheckingAChildUnchecksEveryNodeAboveIt(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), only(false, false, false, true));
        for (String value : List.of("src", "main", "App", "Lib")) {
            setChecked(tester, value, true);
        }

        tester.userClickCheckBox("src", "main", "Lib");

        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("App");
        tester.assertStructure("""
            -(-) src
             -(-) main
              (v) App
              ( ) Lib
             ( ) test
            -( ) docs
             ( ) guide
             ( ) api
            """);
    }

    @Test
    public void cascadeLeavesTheParentsPartiallyChecked(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.CASCADE);

        tester.userClickCheckBox("src", "main", "App");

        tester.assertStructure("""
            -(-) src
             -(-) main
              (v) App
              ( ) Lib
             ( ) test
            -( ) docs
             ( ) guide
             ( ) api
            """);
        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("App");
        assertThat(tester.getCheckableTree().getCheckState("src")).isEqualTo(ThreeState.UNSURE);

        tester.userClickCheckBox("src");
        tester.userClickCheckBox("src", "test");

        tester.assertStructure("""
            -(-) src
             -(v) main
              (v) App
              (v) Lib
             ( ) test
            -( ) docs
             ( ) guide
             ( ) api
            """);
        assertThat(tester.getCheckableTree().isChecked("src")).isTrue();
    }

    @Test
    public void fullSyncKeepsParentsAndChildrenTogether(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.FULL_SYNC);

        tester.userClickCheckBox("src", "main", "App");
        tester.userClickCheckBox("src", "main", "Lib");
        tester.userClickCheckBox("src", "test");

        tester.assertStructure("""
            -(v) src
             -(v) main
              (v) App
              (v) Lib
             (v) test
            -( ) docs
             ( ) guide
             ( ) api
            """);

        tester.userClickCheckBox("src", "main", "App");

        tester.assertStructure("""
            -(-) src
             -(-) main
              ( ) App
              (v) Lib
             (v) test
            -( ) docs
             ( ) guide
             ( ) api
            """);
        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("Lib", "test");
    }

    @Test
    public void childrenBuiltBelowACheckedParentAreChecked(Application application) {
        TreeTester<String> tester = show(application, project(), CheckBoxTreePolicy.CASCADE);

        tester.userClickCheckBox("docs");
        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("docs");

        tester.userExpand("docs").settle();

        tester.assertStructure("""
            +( ) src
            -(v) docs
             (v) guide
             (v) api
            """);
        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("docs", "guide", "api");
        assertThat(myEvents).containsExactly("docs=true", "guide=true", "api=true");
        assertThat(myProgrammatic).containsExactly(false, true, true);
    }

    @Test
    public void childrenBuiltBelowACheckedParentKeepTheirOwnStateWhenThePolicyIsIndependent(Application application) {
        TreeTester<String> tester = show(application, project(), CheckBoxTreePolicy.INDEPENDENT);

        tester.userClickCheckBox("docs");
        tester.userExpand("docs").settle();

        tester.assertStructure("""
            +( ) src
            -(v) docs
             ( ) guide
             ( ) api
            """);
    }

    @Test
    public void aValueCheckedBeforeItsLevelIsBuiltShowsCheckedOnceItIs(Application application) {
        TreeTester<String> tester = show(application, project(), CheckBoxTreePolicy.CASCADE);

        setChecked(tester, "guide", true);
        assertThat(tester.getCheckableTree().getCheckState("guide")).isEqualTo(ThreeState.YES);
        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("guide");

        tester.userExpand("docs").settle();

        tester.assertStructure("""
            +( ) src
            -(-) docs
             (v) guide
             ( ) api
            """);
        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("guide");
    }

    @Test
    public void valuesWithoutACheckBoxArePassedThrough(Application application) {
        TreeTester<String> tester = show(application, project(), CheckBoxTreePolicy.CASCADE);
        CheckableTree<String> tree = tester.getCheckableTree();
        HeadlessUIThread.run(() -> tree.setCheckableFilter(value -> !value.equals("main")));
        tester.getTree().expandAll();
        tester.settle();

        assertThat(tester.userClickCheckBox("src", "main")).isFalse();
        setChecked(tester, "main", true);
        assertThat(tree.isChecked("main")).isFalse();
        assertThat(tree.isCheckable("main")).isFalse();
        assertThat(tree.getCheckState("main")).isEqualTo(ThreeState.NO);

        tester.userClickCheckBox("src");

        tester.assertStructure("""
            -(v) src
             -main
              (v) App
              (v) Lib
             (v) test
            -( ) docs
             ( ) guide
             ( ) api
            """);

        tester.userClickCheckBox("src", "main", "App");

        tester.assertStructure("""
            -(-) src
             -main
              ( ) App
              (v) Lib
             (v) test
            -( ) docs
             ( ) guide
             ( ) api
            """);
        assertThat(tree.getCheckedValues()).containsExactly("src", "Lib", "test");
    }

    @Test
    public void aDisabledCheckBoxIgnoresTheUserButFollowsThePolicy(Application application) {
        TreeTester<String> tester = show(application, project(), CheckBoxTreePolicy.CASCADE);
        CheckableTree<String> tree = tester.getCheckableTree();
        HeadlessUIThread.run(() -> tree.setCheckEnabledFilter(value -> !value.equals("test")));
        tester.getTree().expandAll();
        tester.settle();

        assertThat(tester.userClickCheckBox("src", "test")).isFalse();
        assertThat(tree.isChecked("test")).isFalse();
        assertThat(tree.isCheckEnabled("test")).isFalse();
        assertThat(tree.isCheckable("test")).isTrue();

        tester.userClickCheckBox("src");

        tester.assertStructure("""
            -(v) src
             -(v) main
              (v) App
              (v) Lib
             {v} test
            -( ) docs
             ( ) guide
             ( ) api
            """);

        setChecked(tester, "test", false);
        assertThat(tree.isChecked("test")).isFalse();
    }

    @Test
    public void theListenerHearsEveryChangedValueOnceWithTheDetailsOfTheChange(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.CASCADE);

        tester.userClickCheckBox("src", "main");
        tester.userClickCheckBox("src", "main");

        assertThat(myEvents).containsExactly(
            "main=true", "App=true", "Lib=true",
            "main=false", "App=false", "Lib=false"
        );
        assertThat(myProgrammatic).containsOnly(false);

        myEvents.clear();
        myProgrammatic.clear();
        setChecked(tester, "docs", true);
        setChecked(tester, "docs", true);

        assertThat(myEvents).containsExactly("docs=true", "guide=true", "api=true");
        assertThat(myProgrammatic).containsOnly(true);
    }

    @Test
    public void theCheckStateSurvivesARefresh(Application application) {
        MapTreeModel model = project();
        TreeTester<String> tester = showExpanded(application, model, CheckBoxTreePolicy.CASCADE);
        tester.userClickCheckBox("src", "main", "App");
        tester.userClickCheckBox("docs");
        tester.userClickCheckBox("docs", "api");
        myEvents.clear();

        tester.getTree().refreshAll();

        tester.assertStructure("""
            -(-) src
             -(-) main
              (v) App
              ( ) Lib
             ( ) test
            -(-) docs
             (v) guide
             ( ) api
            """);
        assertThat(myEvents).isEmpty();

        model.put("docs", "guide", "api", "faq");
        model.put("main", "App", "Lib", "Util");
        tester.getTree().refreshAll();

        tester.assertStructure("""
            -(-) src
             -(-) main
              (v) App
              ( ) Lib
              ( ) Util
             ( ) test
            -(-) docs
             (v) guide
             ( ) api
             (v) faq
            """);
        assertThat(myEvents).containsExactly("faq=true");
    }

    @Test
    public void aValueTheTreeNoLongerHoldsForgetsItsState(Application application) {
        MapTreeModel model = project();
        TreeTester<String> tester = showExpanded(application, model, CheckBoxTreePolicy.INDEPENDENT);
        tester.userClickCheckBox("src", "main", "Lib");
        tester.userClickCheckBox("src", "test");
        tester.userClickCheckBox("docs", "api");
        myEvents.clear();

        model.put("main", "App");
        model.put(ROOT, "src");
        tester.getTree().refreshAll();
        tester.settle();

        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("test");
        assertThat(myEvents).containsExactlyInAnyOrder("Lib=false", "api=false");

        model.put("main", "App", "Lib");
        model.put(ROOT, "src", "docs");
        tester.getTree().refreshAll();
        tester.getTree().expandAll();

        tester.assertStructure("""
            -( ) src
             -( ) main
              ( ) App
              ( ) Lib
             (v) test
            -( ) docs
             ( ) guide
             ( ) api
            """);
    }

    @Test
    public void theToggleKeyGivesEverySelectedValueTheStateOfTheLeadOne(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.INDEPENDENT);
        tester.getTree().setAllowMultipleSelect(true);
        tester.settle();
        CheckableTree<String> tree = tester.getCheckableTree();
        HeadlessUIThread.run(() -> tree.setCheckEnabledFilter(value -> !value.equals("guide")));
        setChecked(tester, "App", true);

        tester.userSelect("src", "main", "App");
        tester.userAddToSelection("src", "test");
        tester.userAddToSelection("docs", "guide");
        tester.userAddToSelection("docs", "api");

        assertThat(tester.userPressToggleKey()).isTrue();

        assertThat(tree.getCheckedValues()).containsExactly("App", "test", "api");

        tester.userSelect("docs", "guide");
        assertThat(tester.userPressToggleKey()).isFalse();
        assertThat(tree.isChecked("guide")).isFalse();
    }

    @Test
    public void theCheckedValuesFollowTheTreeOrder(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.INDEPENDENT);

        setChecked(tester, "test", true);
        setChecked(tester, "ghost", true);
        setChecked(tester, "App", true);
        setChecked(tester, "src", true);

        assertThat(tester.getCheckableTree().getCheckedValues()).containsExactly("src", "App", "test", "ghost");
        assertThat(tester.getCheckableTree().getCheckState("ghost")).isEqualTo(ThreeState.YES);
    }

    @Test
    public void aNewCheckableFilterUnchecksTheValuesItRejects(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.INDEPENDENT);
        setChecked(tester, "App", true);
        setChecked(tester, "Lib", true);
        myEvents.clear();

        CheckableTree<String> tree = tester.getCheckableTree();
        HeadlessUIThread.run(() -> tree.setCheckableFilter(value -> !value.equals("App")));

        assertThat(tree.getCheckedValues()).containsExactly("Lib");
        assertThat(myEvents).containsExactly("App=false");
    }

    @Test
    public void anIndependentPolicyShowsNoPartialState(Application application) {
        TreeTester<String> tester = showExpanded(application, project(), CheckBoxTreePolicy.CASCADE);
        tester.userClickCheckBox("src", "main", "App");

        CheckableTree<String> tree = tester.getCheckableTree();
        HeadlessUIThread.run(() -> tree.setCheckPolicy(CheckBoxTreePolicy.INDEPENDENT));

        tester.assertStructure("""
            -( ) src
             -( ) main
              (v) App
              ( ) Lib
             ( ) test
            -( ) docs
             ( ) guide
             ( ) api
            """);
        assertThat(tree.getCheckState("main")).isEqualTo(ThreeState.NO);
    }
}
