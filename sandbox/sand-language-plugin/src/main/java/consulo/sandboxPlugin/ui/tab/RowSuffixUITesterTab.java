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
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.ListBox;
import consulo.ui.Table;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.TextItemRender;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.SplitLayoutPosition;
import consulo.ui.layout.TwoComponentSplitLayout;
import consulo.ui.model.FlatDataModel;
import consulo.ui.style.StandardColors;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
@ExtensionImpl(id = "rowSuffix", order = "after checkBoxTreeTable")
public class RowSuffixUITesterTab implements UITesterTab {
    private static final List<String> ROWS = List.of(
        "Appearance",
        "Code Style",
        "Version Control",
        "Highlighted",
        "Monospaced",
        "Plain"
    );

    private static class HelpDialogDescriptor extends DialogDescriptor {
        private HelpDialogDescriptor() {
            super(LocalizeValue.localizeTODO("Dialog with Help and remembered size"));
        }

        @Override
        @RequiredUIAccess
        public Component createCenterComponent(Disposable uiDisposable) {
            return Label.create(LocalizeValue.localizeTODO("Resize this dialog, close it and open it again"));
        }

        @Override
        public @Nullable String getHelpId() {
            return "platform/uiTester";
        }

        @Override
        public @Nullable String getDimensionServiceKey() {
            return "sand.uiTester.rowSuffix.dialog";
        }

        @Override
        public @Nullable WidthAndHeight getInitialSize() {
            return WidthAndHeight.ofFont(30, 10);
        }
    }

    private final DialogService myDialogService;

    @Inject
    public RowSuffixUITesterTab(DialogService dialogService) {
        myDialogService = dialogService;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Components > Row Suffix");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        Tree<String> tree = Tree.create((TreeModel<String>) (nodeFactory, parentValue) -> {
            if (parentValue != null) {
                for (int i = 0; i < 3; i++) {
                    TreeNode<String> node = nodeFactory.apply(parentValue + " " + i);
                    node.setLeaf(true);
                    node.setRenderer((value, presentation) -> render(value, presentation, true));
                }
                return;
            }

            for (String row : ROWS) {
                TreeNode<String> node = nodeFactory.apply(row);
                node.setRenderer((value, presentation) -> render(value, presentation, false));
            }
        });
        Disposer.register(uiDisposable, tree.destroyHook());

        ListBox<String> listBox = ListBox.create(ROWS);
        listBox.setRenderer((presentation, item) -> {
            String value = item.getValue();
            if (value != null) {
                render(value, presentation, false);
            }
        });

        Table<String> table = Table.create(FlatDataModel.of(ROWS));
        table.addColumn(LocalizeValue.localizeTODO("Name"), row -> row)
            .setWidth(220)
            .setRenderer((presentation, item) -> {
                String value = item.getValue();
                if (value != null) {
                    render(value, presentation, false);
                }
            });
        table.addColumn(LocalizeValue.localizeTODO("Length"), String::length);

        TwoComponentSplitLayout lists = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        lists.setFirstComponent(ScrollableLayout.create(listBox));
        lists.setSecondComponent(ScrollableLayout.create(table));
        lists.setProportion(50);

        TwoComponentSplitLayout split = TwoComponentSplitLayout.create(SplitLayoutPosition.HORIZONTAL);
        split.setFirstComponent(ScrollableLayout.create(tree));
        split.setSecondComponent(lists);
        split.setProportion(34);

        HorizontalLayout buttons = HorizontalLayout.create();
        buttons.add(Button.create(
            LocalizeValue.localizeTODO("Open dialog with Help and remembered size"),
            event -> myDialogService.build(event.getComponent(), new HelpDialogDescriptor()).showAsync()
        ));
        buttons.add(Button.create(LocalizeValue.localizeTODO("Open list popup"), event -> {
            ListPopup popup = JBPopupFactory.getInstance().createListPopup(new BaseListPopupStep<>(null, ROWS));
            popup.setRender((TextItemRender<String>) (presentation, item) -> {
                String value = item.getValue();
                if (value != null) {
                    render(value, presentation, false);
                }
            });
            popup.showBy(event);
        }));

        DockLayout layout = DockLayout.create();
        layout.top(buttons);
        layout.center(split);
        return layout;
    }

    private static void render(String value, TextItemPresentation presentation, boolean child) {
        if (value.startsWith("Appearance")) {
            presentation.append(value, child ? TextAttribute.REGULAR : TextAttribute.REGULAR_BOLD);
        }
        else {
            presentation.append(value);
        }

        if (value.startsWith("Code Style")) {
            presentation.withSuffix(LocalizeValue.empty(), PlatformIconGroup.generalProjectconfigurable());
            presentation.withTooltip(LocalizeValue.localizeTODO("Project settings"));
        }
        else if (value.startsWith("Version Control")) {
            presentation.withSuffix(LocalizeValue.localizeTODO("3 roots"), PlatformIconGroup.generalProjectconfigurable());
        }
        else if (value.startsWith("Highlighted")) {
            presentation.withBackgroundColor(StandardColors.ORANGE.withAlpha(0.35f));
            presentation.withSuffix(LocalizeValue.localizeTODO("module"), null);
        }
        else if (value.startsWith("Monospaced")) {
            presentation.withFont(FontManager.get().createFont("Monospaced", 13, Font.ITALIC));
        }
    }
}
