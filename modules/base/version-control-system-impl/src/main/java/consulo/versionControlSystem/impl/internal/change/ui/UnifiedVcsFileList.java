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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.CommonActionsManager;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.layout.DockLayout;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.impl.internal.change.action.DeleteUnversionedFilesAction;
import consulo.versionControlSystem.ui.VcsFileList;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileDeleteEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public class UnifiedVcsFileList<T> implements VcsFileList<T> {
    private final Project myProject;
    private final Function<T, FilePath> myToPath;
    private final Predicate<T> myValid;
    private final boolean mySelectable;
    private final @Nullable DeleteProvider myDeleteProvider;

    private final Map<FilePath, T> myItems = new LinkedHashMap<>();
    private final UnifiedChangesTree myTree;
    private final DockLayout myRoot = DockLayout.create();

    private boolean myShowFlatten = true;

    @RequiredUIAccess
    public UnifiedVcsFileList(Project project,
                              List<T> items,
                              Function<T, FilePath> toPath,
                              Predicate<T> valid,
                              boolean selectable,
                              boolean toolbar,
                              @Nullable DeleteProvider deleteProvider,
                              @Nullable Runnable inclusionListener,
                              Disposable parentDisposable) {
        myProject = project;
        myToPath = toPath;
        myValid = valid;
        mySelectable = selectable;
        myDeleteProvider = deleteProvider;

        for (T item : items) {
            myItems.put(toPath.apply(item), item);
        }

        myTree = new UnifiedChangesTree(project, selectable, parentDisposable);
        myTree.setAutoIncludeDefaultList(false);
        if (inclusionListener != null) {
            myTree.addInclusionListener(inclusionListener);
        }
        myTree.setSnapshot(buildSnapshot());
        myTree.setIncluded(myItems.keySet());

        myRoot.putUserData(UiDataProvider.KEY, sink -> {
            myTree.uiDataSnapshot(sink);
            if (myDeleteProvider != null) {
                sink.set(DeleteProvider.KEY, myDeleteProvider);
            }
        });
        if (toolbar || deleteProvider != null) {
            myRoot.top(createToolbar(toolbar).getUIComponent());
        }
        myRoot.center(myTree.getComponent());

        if (deleteProvider != null) {
            project.getApplication().getMessageBus().connect(parentDisposable).subscribe(BulkFileListener.class, new BulkFileListener() {
                @Override
                public void after(List<? extends VFileEvent> events) {
                    if (events.stream().anyMatch(VFileDeleteEvent.class::isInstance)) {
                        UIAccess uiAccess = myRoot.getUIAccess();
                        if (uiAccess != null) {
                            uiAccess.give(UnifiedVcsFileList.this::refresh);
                        }
                    }
                }
            });
        }
    }

    @RequiredUIAccess
    private ActionToolbar createToolbar(boolean treeActions) {
        DefaultActionGroup group = new DefaultActionGroup();
        if (treeActions) {
            TreeExpander expander = TreeExpander.of(myTree.getComponent());
            CommonActionsManager commonActionsManager = CommonActionsManager.getInstance();
            group.add(commonActionsManager.createExpandAllAction(expander));
            group.add(commonActionsManager.createCollapseAllAction(expander));
            group.add(new GroupByDirectoryToggleAction(() -> !myShowFlatten, grouped -> {
                myShowFlatten = !grouped;
                refresh();
            }));
        }
        if (myDeleteProvider != null) {
            group.add(new DeleteUnversionedFilesAction());
        }

        ActionToolbar toolbar = ActionToolbarFactory.getInstance().createActionToolbar(ActionPlaces.TOOLBAR, group, ActionToolbar.Style.HORIZONTAL);
        toolbar.setTargetUIComponent(myRoot);
        return toolbar;
    }

    private ChangesTreeSnapshot buildSnapshot() {
        return new ChangesTreeSnapshotBuilder(myShowFlatten, myProject.getBasePath()).paths(myItems.keySet()).build();
    }

    @RequiredUIAccess
    private void refresh() {
        List<FilePath> included = myTree.getIncludedPaths();
        myItems.values().removeIf(item -> !myValid.test(item));
        myTree.setSnapshot(buildSnapshot());
        myTree.setIncluded(included);
    }

    @Override
    public Component getComponent() {
        return myRoot;
    }

    @Override
    public List<T> getIncluded() {
        Collection<FilePath> paths = mySelectable ? myTree.getIncludedPaths() : myItems.keySet();
        List<T> result = new ArrayList<>();
        for (FilePath path : paths) {
            T item = myItems.get(path);
            if (item != null) {
                result.add(item);
            }
        }
        return result;
    }

    @RequiredUIAccess
    @Override
    public void setIncluded(Collection<T> items) {
        List<FilePath> paths = new ArrayList<>();
        for (T item : items) {
            paths.add(myToPath.apply(item));
        }
        myTree.setIncluded(paths);
    }
}
