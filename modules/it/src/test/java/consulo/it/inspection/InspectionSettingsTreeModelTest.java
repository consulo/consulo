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
package consulo.it.inspection;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.HeadlessUIThread;
import consulo.it.TreeTester;
import consulo.language.editor.impl.internal.inspection.setting.InspectionGroupNode;
import consulo.localize.LocalizeValue;
import consulo.language.editor.impl.internal.inspection.setting.InspectionLeafNode;
import consulo.language.editor.impl.internal.inspection.setting.InspectionRow;
import consulo.language.editor.impl.internal.inspection.setting.InspectionTreeModel;
import consulo.language.editor.impl.internal.inspection.setting.InspectionTreeNode;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.TriStateCheckBoxTree;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import consulo.util.lang.ThreeState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class InspectionSettingsTreeModelTest {
    private static final Map<String, String> DESCRIPTIONS = Map.of(
        "ConstantConditions", "<html>Reports <b>null</b> dereferences.</html>",
        "UnusedDeclaration", "<html>Reports unused classes.</html>",
        "Spelling", "<html>Reports typos.</html>",
        "DuplicatedCode", "<html>Reports copy-paste.</html>"
    );

    private final Disposable myDisposable = Disposable.newDisposable();

    @AfterEach
    public void tearDown() {
        Disposer.dispose(myDisposable);
    }

    private static List<InspectionRow> rows() {
        return List.of(
            new InspectionRow("UnusedDeclaration", LocalizeValue.of("Unused declaration"), List.of(LocalizeValue.of("Java"), LocalizeValue.of("Declaration redundancy")), ThreeState.YES, null, false, false),
            new InspectionRow("ConstantConditions", LocalizeValue.of("Constant conditions & exceptions"), List.of(LocalizeValue.of("Java"), LocalizeValue.of("Probable bugs")), ThreeState.UNSURE, null, true, false),
            new InspectionRow("Spelling", LocalizeValue.of("'Typo'"), List.of(LocalizeValue.of("Proofreading")), ThreeState.NO, null, false, false),
            new InspectionRow("DuplicatedCode", LocalizeValue.of("Duplicated code fragment"), List.of(LocalizeValue.of("General")), ThreeState.YES, null, false, true)
        );
    }

    private TreeTester<InspectionTreeNode> create(Application application, InspectionTreeModel model) {
        TreeTester<InspectionTreeNode> tester = TreeTester.createTriStateCheckBoxTree(
            null,
            model,
            application.getInstance(ApplicationTreeExecutorFactory.class).forBackgroundThreadWithoutReadAction(myDisposable)
        );
        Disposer.register(myDisposable, tester.getTree().destroyHook());
        HeadlessUIThread.run(() -> tester.getCheckableTree().setCheckPolicy(CheckBoxTreePolicy.INDEPENDENT));
        return tester.show();
    }

    @Test
    public void groupsComeFirstAndLeavesSortIgnoringLeadingPunctuation(Application application) {
        InspectionTreeModel model = new InspectionTreeModel(DESCRIPTIONS::get);
        model.setRows(rows());

        TreeTester<InspectionTreeNode> tester = create(application, model);
        tester.getTree().expandAll();

        tester.assertStructure("""
            -( ) General
             ( ) Duplicated code fragment %s
            -( ) Java
             -( ) Declaration redundancy
              ( ) Unused declaration
             -( ) Probable bugs
              ( ) Constant conditions & exceptions
            -( ) Proofreading
             ( ) 'Typo'
            """.formatted(InspectionLocalize.inspectionToolAvailabilityInTreeNode1().get()));

        assertThat(model.getVisible()).hasSize(4);
    }

    @Test
    public void filterMatchesNamesGroupsAndDescriptionsWithAllWords(Application application) {
        InspectionTreeModel model = new InspectionTreeModel(DESCRIPTIONS::get);
        model.setRows(rows());
        TreeTester<InspectionTreeNode> tester = create(application, model);

        model.setFilter("java null");
        HeadlessUIThread.run(() -> tester.getTree().refreshAll());
        tester.settle();
        tester.getTree().expandAll();

        tester.assertStructure("""
            -( ) Java
             -( ) Probable bugs
              ( ) Constant conditions & exceptions
            """);
        assertThat(model.getVisible()).extracting(InspectionRow::shortName).containsExactly("ConstantConditions");

        model.setFilter("  ");
        assertThat(model.getFilter()).isNull();
        HeadlessUIThread.run(() -> tester.getTree().refreshAll());
        tester.settle();
        assertThat(model.getVisible()).hasSize(4);
    }

    @Test
    public void visibleRowsOfAGroupCoverEveryLevelBelowIt(Application application) {
        InspectionTreeModel model = new InspectionTreeModel(DESCRIPTIONS::get);
        model.setRows(rows());
        create(application, model);

        assertThat(model.getVisibleRows(new InspectionGroupNode(List.of(LocalizeValue.of("Java")))))
            .extracting(InspectionRow::shortName)
            .containsExactlyInAnyOrder("UnusedDeclaration", "ConstantConditions");
        assertThat(model.getVisibleRows(new InspectionLeafNode("Spelling", LocalizeValue.of("'Typo'"))))
            .extracting(InspectionRow::shortName)
            .containsExactly("Spelling");
        assertThat(model.getVisibleGroups()).containsExactlyInAnyOrder(
            new InspectionGroupNode(List.of(LocalizeValue.of("Java"))),
            new InspectionGroupNode(List.of(LocalizeValue.of("Java"), LocalizeValue.of("Declaration redundancy"))),
            new InspectionGroupNode(List.of(LocalizeValue.of("Java"), LocalizeValue.of("Probable bugs"))),
            new InspectionGroupNode(List.of(LocalizeValue.of("Proofreading"))),
            new InspectionGroupNode(List.of(LocalizeValue.of("General")))
        );
    }

    @Test
    public void checkStatesPushedByValueReachTheBuiltNodes(Application application) {
        InspectionTreeModel model = new InspectionTreeModel(DESCRIPTIONS::get);
        model.setRows(rows());
        TreeTester<InspectionTreeNode> tester = create(application, model);
        tester.getTree().expandAll();
        tester.settle();

        @SuppressWarnings("unchecked")
        TriStateCheckBoxTree<InspectionTreeNode> tree = (TriStateCheckBoxTree<InspectionTreeNode>) tester.getCheckableTree();
        HeadlessUIThread.run(() -> {
            for (InspectionRow row : model.getVisible()) {
                tree.setCheckState(row.toNode(), row.enabled());
            }
            tree.setCheckState(new InspectionGroupNode(List.of(LocalizeValue.of("Java"))), ThreeState.UNSURE);
        });

        tester.assertStructure("""
            -( ) General
             (v) Duplicated code fragment %s
            -(-) Java
             -( ) Declaration redundancy
              (v) Unused declaration
             -( ) Probable bugs
              (-) Constant conditions & exceptions
            -( ) Proofreading
             ( ) 'Typo'
            """.formatted(InspectionLocalize.inspectionToolAvailabilityInTreeNode1().get()));
    }

    @Test
    public void updatedRowsReplaceTheirSnapshotWithoutChangingVisibility(Application application) {
        InspectionTreeModel model = new InspectionTreeModel(DESCRIPTIONS::get);
        model.setRows(rows());
        create(application, model);

        InspectionRow spelling = model.getRow("Spelling");
        assertThat(spelling).isNotNull();
        model.updateRows(List.of(new InspectionRow("Spelling", LocalizeValue.of("'Typo'"), List.of(LocalizeValue.of("Proofreading")), ThreeState.YES, null, true, false)));

        InspectionRow updated = model.getRow("Spelling");
        assertThat(updated).isNotNull();
        assertThat(updated.enabled()).isEqualTo(ThreeState.YES);
        assertThat(updated.modified()).isTrue();
        assertThat(model.getVisible()).hasSize(4).contains(updated);
    }
}
