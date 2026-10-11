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
package consulo.ide.impl.idea.openapi.roots.libraries.ui.impl;

import consulo.application.Application;
import consulo.content.library.LibraryRootType;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.CheckBoxTreeTable;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.HtmlLabel;
import consulo.ui.TableItemEditor;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.ValueComponent;
import consulo.ui.WidthAndHeight;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFilePresentation;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
class DetectedRootsDialogDescriptor extends DialogDescriptor {
    private final List<SuggestedChildRootInfo> myRoots;

    private @Nullable CheckBoxTreeTable<SuggestedChildRootInfo> myTreeTable;

    private DetectedRootsDialogDescriptor(Collection<SuggestedChildRootInfo> roots) {
        super(ProjectLocalize.libraryDetectedRootsTitle());

        List<SuggestedChildRootInfo> sorted = new ArrayList<>(roots);
        sorted.sort(Comparator.comparing(root -> root.getDetectedRoot().getFile().getPresentableUrl()));
        myRoots = List.copyOf(sorted);
    }

    @RequiredUIAccess
    static CompletableFuture<List<SuggestedChildRootInfo>> showAsync(
        @Nullable Component parent,
        Collection<SuggestedChildRootInfo> roots
    ) {
        DetectedRootsDialogDescriptor descriptor = new DetectedRootsDialogDescriptor(roots);

        DialogService dialogService = Application.get().getInstance(DialogService.class);
        Dialog dialog = parent == null ? dialogService.build(descriptor) : dialogService.build(parent, descriptor);
        return dialog.showAsync().thenApply(value -> descriptor.getChosen());
    }

    @Override
    public @Nullable String getDimensionServiceKey() {
        return "DetectedRootsChooserDialog";
    }

    @Override
    public WidthAndHeight getInitialSize() {
        return WidthAndHeight.ofFont(60, 25);
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        CheckBoxTreeTable<SuggestedChildRootInfo> treeTable = CheckBoxTreeTable.create(new DetectedRootsModel());
        Disposer.register(uiDisposable, treeTable.destroyHook());

        treeTable.setCheckPolicy(CheckBoxTreePolicy.INDEPENDENT);
        treeTable.setTreeColumnHeader(ProjectLocalize.libraryDetectedRootsRootColumn());
        treeTable.addColumn(ProjectLocalize.libraryDetectedRootsTypeColumn(), SuggestedChildRootInfo::getSelectedRootType)
            .setRender((presentation, type, root) -> {
                LibraryRootType value = type.getValue();
                if (value != null) {
                    presentation.append(root.getRootTypeName(value));
                }
            })
            .setEditor(new RootTypeEditor());
        treeTable.setSpeedSearchConverter(node -> {
            SuggestedChildRootInfo root = node.getValue();
            return root == null ? "" : root.getDetectedRoot().getFile().getPresentableUrl();
        });

        for (SuggestedChildRootInfo root : myRoots) {
            treeTable.setChecked(root, true);
        }
        treeTable.addCheckListener(event -> updateOkButtonState());
        myTreeTable = treeTable;

        DockLayout layout = DockLayout.create();
        layout.top(HtmlLabel.create(ProjectLocalize.libraryDetectedRootsDescription()));
        layout.center(ScrollableLayout.create(treeTable));
        return layout;
    }

    @Override
    public boolean doUpdateOkButtonState() {
        CheckBoxTreeTable<SuggestedChildRootInfo> treeTable = myTreeTable;
        return treeTable == null || !treeTable.getCheckedValues().isEmpty();
    }

    private List<SuggestedChildRootInfo> getChosen() {
        CheckBoxTreeTable<SuggestedChildRootInfo> treeTable = myTreeTable;
        return treeTable == null ? List.of() : treeTable.getCheckedValues();
    }

    private static void render(SuggestedChildRootInfo root, TextItemPresentation presentation) {
        VirtualFile file = root.getDetectedRoot().getFile();
        presentation.withIcon(VirtualFilePresentation.getIcon(file));
        presentation.append(file.getPresentableUrl());
    }

    private class DetectedRootsModel implements TreeModel<SuggestedChildRootInfo> {
        @Override
        public void buildChildren(Function<SuggestedChildRootInfo, TreeNode<SuggestedChildRootInfo>> nodeFactory,
                                  @Nullable SuggestedChildRootInfo parentValue) {
            if (parentValue != null) {
                return;
            }

            for (SuggestedChildRootInfo root : myRoots) {
                TreeNode<SuggestedChildRootInfo> node = nodeFactory.apply(root);
                node.setLeaf(true);
                node.setRenderer(DetectedRootsDialogDescriptor::render);
            }
        }
    }

    private static class RootTypeEditor implements TableItemEditor<SuggestedChildRootInfo, LibraryRootType> {
        @RequiredUIAccess
        @Override
        public ValueComponent<LibraryRootType> createComponent(SuggestedChildRootInfo root) {
            List<LibraryRootType> types = new ArrayList<>(root.getDetectedRoot().getTypes());
            types.sort(Comparator.comparing(root::getRootTypeName, String.CASE_INSENSITIVE_ORDER));

            ComboBox<LibraryRootType> comboBox = ComboBox.create(types);
            comboBox.setTextRenderer(type -> type == null ? LocalizeValue.empty() : LocalizeValue.of(root.getRootTypeName(type)));
            return comboBox;
        }

        @RequiredUIAccess
        @Override
        public void commit(SuggestedChildRootInfo root, @Nullable LibraryRootType type) {
            if (type != null) {
                root.setSelectedRootType(type);
            }
        }

        @Override
        public boolean isEditable(SuggestedChildRootInfo root) {
            return root.getDetectedRoot().getTypes().size() > 1;
        }
    }
}
