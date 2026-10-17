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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.ui.Button;
import consulo.ui.CheckBoxTree;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.TextAttribute;
import consulo.ui.TextBox;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TreeNodeEditor;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.style.ComponentColors;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
@ExtensionImpl(id = "treeEditing", order = "after checkBoxTree")
public class TreeEditingUITesterTab implements UITesterTab {
    private static final String COMMIT = "#";
    private static final List<String> REPOSITORIES = List.of("consulo", "consulo-java", "consulo-git");
    private static final List<String> REMOTES = List.of("origin", "upstream", "fork");

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components > Tree Editing");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        Map<String, String> remotes = new ConcurrentHashMap<>();
        Map<String, String> branches = new ConcurrentHashMap<>();
        for (String repository : REPOSITORIES) {
            remotes.put(repository, "origin");
            branches.put(repository, "master");
        }

        TreeModel<String> model = (nodeFactory, parentValue) -> {
            if (parentValue == null || parentValue.isEmpty()) {
                for (String repository : REPOSITORIES) {
                    TreeNode<String> node = nodeFactory.apply(repository);
                    node.setLeaf(false);
                    node.setRenderer((value, presentation) -> {
                        presentation.append(value, TextAttribute.REGULAR_BOLD);
                        presentation.append("  master → ", TextAttribute.GRAYED);
                        presentation.append(remotes.get(value) + " : " + branches.get(value));
                    });
                }
            }
            else if (!parentValue.contains(COMMIT)) {
                for (int i = 1; i <= 3; i++) {
                    TreeNode<String> node = nodeFactory.apply(parentValue + COMMIT + i);
                    node.setLeaf(true);
                    node.setRenderer((value, presentation) -> presentation.append("Commit " + value.substring(value.indexOf(COMMIT) + 1)));
                }
            }
        };

        CheckBoxTree<String> tree = CheckBoxTree.create("", model);
        Disposer.register(uiDisposable, tree.destroyHook());
        tree.setCheckableFilter(value -> !value.contains(COMMIT));
        for (String repository : REPOSITORIES) {
            tree.setChecked(repository, true);
        }

        Label status = Label.create(LocalizeValue.localizeTODO("Click a selected repository or press F2"));
        status.setForegroundColor(ComponentColors.DISABLED_TEXT);

        tree.setNodeEditor(new TreeNodeEditor<>() {
            private ComboBox<String> myRemote;
            private TextBox myBranch;

            @Override
            public boolean isEditable(String value) {
                return !value.contains(COMMIT);
            }

            @RequiredUIAccess
            @Override
            public Component createEditor(String value) {
                myRemote = ComboBox.create(REMOTES);
                myRemote.setValue(remotes.get(value));

                myBranch = TextBox.create(branches.get(value));

                HorizontalLayout layout = HorizontalLayout.create(Space.SMALL);
                layout.add(Label.create(LocalizeValue.of(value)));
                layout.add(Label.create(LocalizeValue.of("master →")));
                layout.add(myRemote);
                layout.add(Label.create(LocalizeValue.of(":")));
                layout.add(myBranch);
                return layout;
            }

            @RequiredUIAccess
            @Override
            public void editingStarted(String value, Component editor) {
                myBranch.focus();
                status.setText(LocalizeValue.of("Editing " + value));
            }

            @RequiredUIAccess
            @Override
            public boolean stopEditing(String value, Component editor) {
                String branch = myBranch.getValue();
                if (branch == null || branch.isBlank()) {
                    status.setText(LocalizeValue.of("Branch of " + value + " cannot be empty - editing goes on"));
                    return false;
                }

                String remote = myRemote.getValue();
                if (remote != null) {
                    remotes.put(value, remote);
                }
                branches.put(value, branch.trim());
                status.setText(LocalizeValue.of("Applied " + value + " → " + remotes.get(value) + " : " + branches.get(value)));

                TreeNode<String> node = tree.getSelectedNode();
                if (node != null) {
                    tree.refreshItem(node, false);
                }
                return true;
            }

            @RequiredUIAccess
            @Override
            public void cancelEditing(String value, Component editor) {
                status.setText(LocalizeValue.of("Cancelled " + value));
            }
        });

        Button edit = Button.create(LocalizeValue.localizeTODO("Edit selected"), event -> {
            TreeNode<String> node = tree.getSelectedNode();
            if (node == null || !tree.startEditing(node)) {
                status.setText(LocalizeValue.of("Select a repository first"));
            }
        });
        Button stop = Button.create(LocalizeValue.localizeTODO("Apply"), event -> tree.stopEditing());
        Button cancel = Button.create(LocalizeValue.localizeTODO("Revert"), event -> tree.cancelEditing());

        tree.expandAll();

        DockLayout layout = DockLayout.create();
        layout.top(HorizontalLayout.create().add(edit).add(stop).add(cancel));
        layout.center(ScrollableLayout.create(tree));
        layout.bottom(status);
        return layout;
    }
}
