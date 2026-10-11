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
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Button;
import consulo.ui.CheckBoxTree;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
@ExtensionImpl(id = "checkBoxTree", order = "after treeTable")
public class CheckBoxTreeUITesterTab implements UITesterTab {
    private static final String FRUITS = "Fruits";
    private static final String REQUIRED = "Required";

    private static final Map<String, List<String>> CHILDREN = Map.of(
        "", List.of(FRUITS, "Vegetables", "Plugins"),
        FRUITS, List.of("Apple", "Banana", "Cherry"),
        "Vegetables", List.of("Carrot", "Potato", "Tomato"),
        "Potato", List.of("Russet", "Yukon Gold"),
        "Plugins", List.of(REQUIRED, "Optional", "Experimental")
    );

    private static final Set<String> GROUPS = Set.of(FRUITS, "Vegetables", "Plugins");

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components > CheckBoxTree");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        TreeModel<String> model = (nodeFactory, parentValue) -> {
            for (String child : CHILDREN.getOrDefault(parentValue == null ? "" : parentValue, List.of())) {
                TreeNode<String> node = nodeFactory.apply(child);
                node.setLeaf(!CHILDREN.containsKey(child));
                node.setRenderer((value, presentation) -> {
                    presentation.withIcon(GROUPS.contains(value) ? PlatformIconGroup.nodesFolder() : PlatformIconGroup.nodesClass());
                    presentation.append(value);
                    if (value.equals(FRUITS)) {
                        presentation.withTooltip(LocalizeValue.localizeTODO("A group without a check box"));
                    }
                    else if (value.equals(REQUIRED)) {
                        presentation.withTooltip(LocalizeValue.localizeTODO("A check box the user cannot change"));
                    }
                });
            }
        };

        CheckBoxTree<String> tree = CheckBoxTree.create("", model);
        Disposer.register(uiDisposable, tree.destroyHook());

        tree.setCheckableFilter(value -> !value.equals(FRUITS));
        tree.setCheckEnabledFilter(value -> !value.equals(REQUIRED));
        tree.setChecked(REQUIRED, true);
        tree.setChecked("Banana", true);
        tree.expandAll();

        Label checked = Label.create();
        Runnable updateChecked = () -> checked.setText(LocalizeValue.of("Checked: " + String.join(", ", tree.getCheckedValues())));
        updateChecked.run();
        tree.addCheckListener(event -> updateChecked.run());

        ComboBox<CheckBoxTreePolicy> policy = ComboBox.create(
            CheckBoxTreePolicy.CASCADE,
            CheckBoxTreePolicy.INDEPENDENT,
            CheckBoxTreePolicy.FULL_SYNC
        );
        policy.setTextRenderer(value -> {
            if (CheckBoxTreePolicy.INDEPENDENT.equals(value)) {
                return LocalizeValue.localizeTODO("Independent");
            }
            if (CheckBoxTreePolicy.FULL_SYNC.equals(value)) {
                return LocalizeValue.localizeTODO("Full sync");
            }
            return LocalizeValue.localizeTODO("Cascade");
        });
        policy.setValue(tree.getCheckPolicy());
        policy.addValueListener(event -> {
            CheckBoxTreePolicy value = event.getValue();
            if (value != null) {
                tree.setCheckPolicy(value);
            }
        });

        Button clear = Button.create(LocalizeValue.localizeTODO("Uncheck all"), event -> {
            for (String value : tree.getCheckedValues()) {
                tree.setChecked(value, false);
            }
        });

        DockLayout layout = DockLayout.create();
        layout.top(HorizontalLayout.create()
            .add(Label.create(LocalizeValue.localizeTODO("Policy:")))
            .add(policy)
            .add(clear));
        layout.center(ScrollableLayout.create(tree));
        layout.bottom(checked);
        return layout;
    }
}
