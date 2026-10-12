/*
 * Copyright 2013-2018 consulo.io
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
package consulo.fileEditor.impl.internal.unified;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.disposer.Disposer;
import consulo.fileEditor.impl.internal.FileDropManager;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.fileEditor.impl.internal.FileEditorsSplittersBase;
import consulo.project.Project;
import consulo.project.ui.wm.dock.DockManager;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.PlaceholderLayout;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class UnifiedFileEditorsSplitters extends FileEditorsSplittersBase<UnifiedFileEditorWindow> {
    private final PlaceholderLayout myRoot;
    private final EditorEmptyTextPanel myEmptyTextPanel;

    @RequiredUIAccess
    public UnifiedFileEditorsSplitters(ApplicationConcurrency applicationConcurrency,
                                       Project project,
                                       FileEditorManagerImpl editorManager,
                                       DockManager dockManager,
                                       boolean createOwnDockableContainer) {
        super(applicationConcurrency, project, editorManager);

        myEmptyTextPanel = new EditorEmptyTextPanel(project, this, this::showEmptyText);

        myRoot = PlaceholderLayout.create();
        myRoot.setContent(getSplitComponent());
        myRoot.setPlaceholder(myEmptyTextPanel.getComponent());
        myRoot.setPlaceholderVisibility(this::showEmptyText);
        myRoot.setDropHandler(
            (component, transfer, checkOnly) -> FileDropManager.getInstance(myProject).drop(transfer, getCurrentWindow(), checkOnly)
        );

        if (createOwnDockableContainer) {
            UnifiedDockableEditorTabbedContainer dockable = new UnifiedDockableEditorTabbedContainer(myManager.getProject(), this, false);
            Disposer.register(editorManager.getProject(), dockable);
            dockManager.register(dockable);
        }
    }

    @Override
    protected UnifiedFileEditorWindow[] createArray(int size) {
        return new UnifiedFileEditorWindow[size];
    }

    @Override
    public Component getUIComponent() {
        return myRoot;
    }

    @RequiredUIAccess
    private void updatePlaceholder() {
        if (showEmptyText()) {
            myEmptyTextPanel.refresh();
        }
        myRoot.testPlaceholder();
    }

    @RequiredUIAccess
    @Override
    public void afterFileOpen(VirtualFile file) {
        updatePlaceholder();
    }

    @RequiredUIAccess
    @Override
    public void afterFileClosed(VirtualFile file) {
        updatePlaceholder();
    }

    @RequiredUIAccess
    @Override
    protected void setCurrentWindow(@Nullable UnifiedFileEditorWindow currentWindow) {
        super.setCurrentWindow(currentWindow);
        updatePlaceholder();
    }

    @RequiredUIAccess
    @Override
    protected UnifiedFileEditorWindow createEditorWindow() {
        return new UnifiedFileEditorWindow(myProject, myManager, this);
    }

    @Override
    public void startListeningFocus() {
    }

    @Override
    public boolean isShowing() {
        return true;
    }
}
