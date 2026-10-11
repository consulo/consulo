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
import consulo.ui.CheckBox;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.CheckBoxTreeTable;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.HorizontalAlignment;
import consulo.ui.Label;
import consulo.ui.TableItemEditor;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.ValueComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
@ExtensionImpl(id = "checkBoxTreeTable", order = "after checkBoxTree")
public class CheckBoxTreeTableUITesterTab implements UITesterTab {
    private static final String LIBRARIES = "Libraries";
    private static final String REQUIRED = "consulo-core.jar";

    private static final Map<String, List<String>> CHILDREN = Map.of(
        "", List.of(LIBRARIES, "Sources", "Documentation"),
        LIBRARIES, List.of(REQUIRED, "guava.jar", "junit.jar", "assertj.jar"),
        "Sources", List.of("src/main/java", "src/test/java", "generated"),
        "generated", List.of("annotations", "parser"),
        "Documentation", List.of("api-docs", "guide.html")
    );

    private static final Set<String> GROUPS = Set.of(LIBRARIES, "Sources", "Documentation", "generated");

    private static final List<String> SCOPES = List.of("Compile", "Runtime", "Test", "Provided");

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components > CheckBoxTreeTable");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        Map<String, String> scopes = new ConcurrentHashMap<>(Map.of("junit.jar", "Test", "assertj.jar", "Test"));

        TreeModel<String> model = (nodeFactory, parentValue) -> {
            for (String child : CHILDREN.getOrDefault(parentValue == null ? "" : parentValue, List.of())) {
                TreeNode<String> node = nodeFactory.apply(child);
                node.setLeaf(!CHILDREN.containsKey(child));
                node.setRenderer((value, presentation) -> {
                    presentation.withIcon(GROUPS.contains(value) ? PlatformIconGroup.nodesFolder() : PlatformIconGroup.nodesPplib());
                    presentation.append(value);
                    if (value.equals(REQUIRED)) {
                        presentation.withTooltip(LocalizeValue.localizeTODO("A check box the user cannot change"));
                    }
                });
            }
        };

        CheckBoxTreeTable<String> treeTable = CheckBoxTreeTable.create("", model);
        Disposer.register(uiDisposable, treeTable.destroyHook());

        treeTable.setTreeColumnHeader(LocalizeValue.localizeTODO("Entry"));
        treeTable.addColumn(LocalizeValue.localizeTODO("Kind"), value -> GROUPS.contains(value) ? "Group" : "Entry");
        treeTable.addColumn(LocalizeValue.localizeTODO("Length"), String::length)
            .setHorizontalAlignment(HorizontalAlignment.RIGHT)
            .setSortable(Comparator.naturalOrder());
        treeTable.addColumn(LocalizeValue.localizeTODO("Scope"), value -> isLibrary(value) ? scopes.getOrDefault(value, SCOPES.get(0)) : "")
            .setEditor(new TableItemEditor<>() {
                @RequiredUIAccess
                @Override
                public ValueComponent<String> createComponent(String item) {
                    return ComboBox.create(SCOPES);
                }

                @RequiredUIAccess
                @Override
                public void commit(String item, @Nullable String value) {
                    if (value != null) {
                        scopes.put(item, value);
                    }
                }

                @Override
                public boolean isEditable(String item) {
                    return isLibrary(item);
                }
            });

        treeTable.setCheckEnabledFilter(value -> !value.equals(REQUIRED));
        treeTable.setChecked(REQUIRED, true);
        treeTable.setChecked("Sources", true);
        treeTable.expandAll();

        Label checked = Label.create();
        Runnable updateChecked = () -> checked.setText(LocalizeValue.of("Checked: " + String.join(", ", treeTable.getCheckedValues())
            + " | Scopes: " + scopes));
        updateChecked.run();
        treeTable.addCheckListener(event -> updateChecked.run());

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
        policy.setValue(treeTable.getCheckPolicy());
        policy.addValueListener(event -> {
            CheckBoxTreePolicy value = event.getValue();
            if (value != null) {
                treeTable.setCheckPolicy(value);
            }
        });

        CheckBox multipleSelection = CheckBox.create(LocalizeValue.localizeTODO("Multiple selection"));
        multipleSelection.addValueListener(event -> treeTable.setAllowMultipleSelect(Boolean.TRUE.equals(event.getValue())));

        Button clear = Button.create(LocalizeValue.localizeTODO("Uncheck all"), event -> {
            for (String value : treeTable.getCheckedValues()) {
                treeTable.setChecked(value, false);
            }
        });

        Button refresh = Button.create(LocalizeValue.localizeTODO("Refresh"), event -> {
            treeTable.refreshAll();
            updateChecked.run();
        });

        DockLayout layout = DockLayout.create();
        layout.top(HorizontalLayout.create()
            .add(Label.create(LocalizeValue.localizeTODO("Policy:")))
            .add(policy)
            .add(multipleSelection)
            .add(clear)
            .add(refresh));
        layout.center(ScrollableLayout.create(treeTable));
        layout.bottom(checked);
        return layout;
    }

    private static boolean isLibrary(String value) {
        return CHILDREN.get(LIBRARIES).contains(value);
    }
}
