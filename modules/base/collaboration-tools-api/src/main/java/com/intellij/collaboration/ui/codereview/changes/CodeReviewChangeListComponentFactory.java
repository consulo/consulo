// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.changes;

import com.intellij.collaboration.async.AsyncUtilKt;
import com.intellij.collaboration.ui.codereview.CodeReviewProgressTreeModel;
import com.intellij.collaboration.ui.codereview.CodeReviewProgressTreeModelUtil;
import com.intellij.collaboration.ui.codereview.details.model.CodeReviewChangeListViewModel;
import com.intellij.collaboration.util.RefComparisonChange;
import com.intellij.collaboration.util.RefComparisonChangeKt;
import consulo.application.util.function.Processor;
import consulo.dataContext.DataSink;
import consulo.navigation.Navigatable;
import consulo.ui.ex.ExpandableItemsHandler;
import consulo.ui.ex.awt.ClientProperty;
import consulo.ui.ex.awt.EditSourceOnDoubleClickHandler;
import consulo.ui.ex.awt.tree.SelectionSaver;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kotlinx.coroutines.CoroutineScope;
import org.jetbrains.annotations.Nls;

import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public final class CodeReviewChangeListComponentFactory {
    public static final DataKey<List<RefComparisonChange>> SELECTED_CHANGES =
        DataKey.create("Code.Review.Change.List.Selected.RefComparisonChanges");

    private CodeReviewChangeListComponentFactory() {
    }

    public static @Nonnull AsyncChangesTree createIn(
        @Nonnull CoroutineScope cs,
        @Nonnull CodeReviewChangeListViewModel vm,
        @Nullable CodeReviewProgressTreeModel<?> progressModel,
        @Nls @Nonnull String emptyTextText
    ) {
        AsyncChangesTreeModel treeModel = createTreeModel(vm);
        AsyncChangesTree tree = createTree(cs, vm, treeModel);
        tree.getEmptyText().setText(emptyTextText);

        ClientProperty.put(tree, ExpandableItemsHandler.IGNORE_ITEM_SELECTION, true);
        SelectionSaver.installOn(tree);

        tree.setDoubleClickHandler((Processor<MouseEvent>) e -> {
            if (EditSourceOnDoubleClickHandler.isToggleEvent(tree, e)) {
                return false;
            }
            CodeReviewProgressTreeModelUtil.updateSelectedChangesFromTree(vm, tree);
            vm.showDiffPreview();
            return true;
        });

        tree.setEnterKeyHandler((Processor<KeyEvent>) e -> {
            CodeReviewProgressTreeModelUtil.updateSelectedChangesFromTree(vm, tree);
            vm.showDiffPreview();
            return true;
        });

        if (progressModel != null) {
            CodeReviewProgressTreeModelUtil.setupCodeReviewProgressModel(tree, vm, progressModel);
        }

        tree.rebuildTree();

        TreeSelectionListener selectionListener = e -> CodeReviewProgressTreeModelUtil.updateSelectedChangesFromTree(vm, tree);
        tree.addTreeSelectionListener(selectionListener);

        AsyncUtilKt.launchNow(
            cs,
            continuation -> {
                vm.getSelectionRequests().collect(
                    request -> {
                        handleSelectionRequest(tree, selectionListener, request);
                        return null;
                    },
                    continuation
                );
                return null;
            }
        );

        return tree;
    }

    private static void handleSelectionRequest(
        @Nonnull AsyncChangesTree tree,
        @Nonnull TreeSelectionListener selectionListener,
        @Nonnull CodeReviewChangeListViewModel.SelectionRequest request
    ) {
        // skip selection reset after update to avoid loop
        tree.removeTreeSelectionListener(selectionListener);
        if (request instanceof CodeReviewChangeListViewModel.SelectionRequest.All) {
            tree.invokeAfterRefresh(() -> {
                TreeUtil.selectFirstNode(tree);
                tree.addTreeSelectionListener(selectionListener);
            });
        }
        else if (request instanceof CodeReviewChangeListViewModel.SelectionRequest.OneChange oneChange) {
            tree.invokeAfterRefresh(() -> {
                var currentSelection = VcsTreeModelData.selected(tree).iterateUserObjects(RefComparisonChange.class);
                boolean found = false;
                for (var change : currentSelection) {
                    if (oneChange.getChange().equals(change)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    tree.setSelectedChanges(List.of(oneChange.getChange()));
                }
                tree.addTreeSelectionListener(selectionListener);
            });
        }
    }

    private static @Nonnull AsyncChangesTree createTree(
        @Nonnull CoroutineScope cs,
        @Nonnull CodeReviewChangeListViewModel vm,
        @Nonnull AsyncChangesTreeModel treeModel
    ) {
        return new AsyncChangesTree(vm.getProject(), false, false) {
            @Override
            protected @Nonnull AsyncChangesTreeModel getChangesTreeModel() {
                return treeModel;
            }

            @Override
            public void uiDataSnapshot(@Nonnull DataSink sink) {
                super.uiDataSnapshot(sink);
                VcsTreeModelData.uiDataSnapshot(sink, getProject(), this);
                List<VirtualFile> selectedFiles = getSelectedFiles();
                VirtualFile singleFile = selectedFiles.size() == 1 ? selectedFiles.get(0) : null;
                if (singleFile != null) {
                    sink.set(Navigatable.KEY, new OpenFileDescriptor(getProject(), singleFile));
                }
                sink.set(Navigatable.KEY_OF_ARRAY, ChangesUtil.getNavigatableArray(getProject(), selectedFiles));
                sink.set(SELECTED_CHANGES, getSelectedChanges());
            }

            private @Nonnull List<RefComparisonChange> getSelectedChanges() {
                return VcsTreeModelData.selected(this).userObjects(RefComparisonChange.class);
            }

            private @Nonnull List<VirtualFile> getSelectedFiles() {
                List<VirtualFile> files = new ArrayList<>();
                for (RefComparisonChange change : getSelectedChanges()) {
                    VirtualFile vf = RefComparisonChangeKt.getFilePath(change).getVirtualFile();
                    if (vf != null) {
                        files.add(vf);
                    }
                }
                return files;
            }

            @Override
            protected @Nonnull ChangesGroupingSupport installGroupingSupport() {
                if (vm instanceof CodeReviewChangeListViewModel.WithGrouping groupingVm) {
                    ChangesGroupingSupport gs = new ChangesGroupingSupport(vm.getProject(), this, false);
                    installGroupingSupport(this, gs, () -> groupingVm.getGrouping().getValue(), groupingVm::setGrouping);
                    AsyncUtilKt.launchNow(
                        cs,
                        continuation -> {
                            groupingVm.getGrouping().collect(
                                it -> {
                                    if (!gs.getGroupingKeys().equals(it)) {
                                        gs.setGroupingKeysOrSkip(it);
                                    }
                                    return null;
                                },
                                continuation
                            );
                            return null;
                        }
                    );
                    return gs;
                }
                else {
                    return super.installGroupingSupport();
                }
            }
        };
    }

    private static @Nonnull AsyncChangesTreeModel createTreeModel(@Nonnull CodeReviewChangeListViewModel vm) {
        return new AsyncChangesTreeModel() {
            @Override
            public @Nonnull DefaultTreeModel buildTreeModel(@Nonnull ChangesGroupingPolicyFactory grouping) {
                TreeModelBuilder builder = new TreeModelBuilder(vm.getProject(), grouping);
                for (RefComparisonChange change : vm.getChanges()) {
                    builder.insertChangeNode(
                        RefComparisonChangeKt.getFilePath(change),
                        builder.getMyRoot(),
                        new Node(change)
                    );
                }
                return builder.build();
            }
        };
    }

    private static final class Node extends AbstractChangesBrowserFilePathNode<RefComparisonChange> {
        Node(@Nonnull RefComparisonChange change) {
            super(change, RefComparisonChangeKt.getFileStatus(change));
        }

        @Override
        protected @Nonnull FilePath filePath(@Nonnull RefComparisonChange userObject) {
            return RefComparisonChangeKt.getFilePath(userObject);
        }

        @Override
        protected @Nullable FilePath originPath(@Nonnull RefComparisonChange userObject) {
            return userObject.getFilePathBefore();
        }
    }
}
