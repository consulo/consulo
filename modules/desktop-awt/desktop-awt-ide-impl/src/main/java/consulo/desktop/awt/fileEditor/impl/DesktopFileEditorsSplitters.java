/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package consulo.desktop.awt.fileEditor.impl;

import consulo.application.concurrent.ApplicationConcurrency;
import consulo.dataContext.DataManager;
import consulo.desktop.awt.ui.impl.facade.AWTComponentProviderUtil;
import consulo.desktop.awt.wm.impl.IdePanePanel;
import consulo.desktop.awt.ui.impl.plaf.BasicIdePanePanelUI;
import consulo.disposer.Disposer;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.fileEditor.impl.internal.FileEditorsSplittersBase;
import consulo.fileEditor.impl.internal.text.FileEditorDropHandler;
import consulo.fileEditor.impl.internal.unified.EditorEmptyTextPanel;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.dock.DockManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.IdeFocusTraversalPolicy;
import consulo.ui.ex.awt.tab.JBTabs;
import consulo.ui.ex.awt.util.FocusWatcher;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.keymap.event.KeymapManagerListener;
import consulo.ui.layout.PlaceholderLayout;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ContainerEvent;
import javax.swing.*;

public class DesktopFileEditorsSplitters extends FileEditorsSplittersBase<DesktopFileEditorWindow> {
    private final MyFocusWatcher myFocusWatcher;

    private final IdePanePanel myComponent;
    private final PlaceholderLayout myPlaceholder;
    private final EditorEmptyTextPanel myEmptyTextPanel;

    public DesktopFileEditorsSplitters(Project project,
                                       ApplicationConcurrency applicationConcurrency,
                                       FileEditorManagerImpl manager,
                                       DockManager dockManager,
                                       boolean createOwnDockableContainer) {
        super(applicationConcurrency, project, manager);

        myComponent = new IdePanePanel(new BorderLayout());
        if (createOwnDockableContainer) {
            myComponent.setIslandRole(BasicIdePanePanelUI.EDITOR_ROLE);
        }
        myComponent.setFocusTraversalPolicy(new MyFocusTraversalPolicy());
        myComponent.setTransferHandler(new MyTransferHandler());
        DataManager.registerUiDataProvider(myComponent, sink -> {
            sink.set(KEY, this);
        });

        AWTComponentProviderUtil.putMark(myComponent, this);

        myFocusWatcher = new MyFocusWatcher();

        myEmptyTextPanel = new EditorEmptyTextPanel(project, this, this::showEmptyText) {
            @RequiredUIAccess
            @Override
            protected void afterRefresh() {
                makeTransparent(TargetAWT.to(getComponent()));
            }
        };

        myPlaceholder = PlaceholderLayout.create();
        myPlaceholder.setContent(getSplitComponent());
        myPlaceholder.setPlaceholder(myEmptyTextPanel.getComponent());
        myPlaceholder.setPlaceholderVisibility(this::showEmptyText);

        JComponent placeholderComponent = (JComponent) TargetAWT.to(myPlaceholder);
        placeholderComponent.setOpaque(false);
        myComponent.add(placeholderComponent, BorderLayout.CENTER);

        clear();

        if (createOwnDockableContainer) {
            DesktopDockableEditorTabbedContainer dockable =
                new DesktopDockableEditorTabbedContainer(myManager.getProject(), this, false);
            Disposer.register(manager.getProject(), dockable);
            dockManager.register(dockable);
        }
        KeymapManagerListener keymapListener = keymap -> updatePlaceholder();
        KeymapManager.getInstance().addKeymapManagerListener(keymapListener, this);
    }

    @Override
    public void revalidate() {
        myComponent.revalidate();
    }

    private static void makeTransparent(Component component) {
        if (component instanceof JPanel panel) {
            panel.setOpaque(false);
            for (Component child : panel.getComponents()) {
                makeTransparent(child);
            }
        }
    }

    @RequiredUIAccess
    private void updatePlaceholder() {
        if (showEmptyText()) {
            myEmptyTextPanel.refresh();
        }
        myPlaceholder.testPlaceholder();
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
    protected void setCurrentWindow(@Nullable DesktopFileEditorWindow currentWindow) {
        super.setCurrentWindow(currentWindow);
        updatePlaceholder();
    }

    @Override
    protected void afterSplitLayoutUpdated() {
        myComponent.revalidate();
        myComponent.repaint();
    }

    @Override
    protected DesktopFileEditorWindow[] createArray(int size) {
        return new DesktopFileEditorWindow[size];
    }

    @Override
    public JComponent getComponent() {
        return myComponent;
    }

    @Override
    public void startListeningFocus() {
        myFocusWatcher.install(myComponent);
    }

    @Override
    protected void stopListeningFocus() {
        myFocusWatcher.deinstall(myComponent);
    }

    @Override
    public boolean isShowing() {
        return myComponent.isShowing();
    }

    @Nullable JBTabs getTabsAt(RelativePoint point) {
        Point thisPoint = point.getPoint(myComponent);
        Component c = SwingUtilities.getDeepestComponentAt(myComponent, thisPoint.x, thisPoint.y);
        while (c != null) {
            if (c instanceof JBTabs jbTabs) {
                return jbTabs;
            }
            c = c.getParent();
        }

        return null;
    }

    boolean isEmptyVisible() {
        DesktopFileEditorWindow[] windows = getWindows();
        for (DesktopFileEditorWindow each : windows) {
            if (!each.isEmptyVisible()) {
                return false;
            }
        }
        return true;
    }

    private final class MyFocusTraversalPolicy extends IdeFocusTraversalPolicy {
        @Override
        public final Component getDefaultComponent(Container focusCycleRoot) {
            if (myCurrentWindow != null) {
                DesktopFileEditorWithProviderComposite selectedEditor = myCurrentWindow.getSelectedEditor();
                if (selectedEditor != null) {
                    return IdeFocusTraversalPolicy.getPreferredFocusedComponent(selectedEditor.getComponent(), this);
                }
            }
            return IdeFocusTraversalPolicy.getPreferredFocusedComponent(myComponent, this);
        }
    }

    @RequiredUIAccess
    @Override
    protected DesktopFileEditorWindow createEditorWindow() {
        return new DesktopFileEditorWindow(this);
    }

    private @Nullable DesktopFileEditorWindow findWindowWith(Component component) {
        if (component != null) {
            for (DesktopFileEditorWindow window : myWindows) {
                if (SwingUtilities.isDescendingFrom(component, window.getPanel())) {
                    return window;
                }
            }
        }
        return null;
    }

    public boolean isFloating() {
        return false;
    }

    public boolean isPreview() {
        return false;
    }

    private final class MyFocusWatcher extends FocusWatcher {
        @Override
        protected void focusedComponentChanged(Component component, AWTEvent cause) {
            DesktopFileEditorWindow newWindow = null;

            if (component != null) {
                newWindow = findWindowWith(component);
            }
            else if (cause instanceof ContainerEvent && cause.getID() == ContainerEvent.COMPONENT_REMOVED) {
                // do not change current window in case of child removal as in JTable.removeEditor
                // otherwise Escape in a toolwindow will not focus editor with JTable content
                return;
            }

            setCurrentWindow(newWindow);
            setCurrentWindow(newWindow, false);
        }
    }

    @Override
    public void toFront() {
        ProjectIdeFocusManager.getInstance(myProject).toFront(getComponent());
    }

    private final class MyTransferHandler extends TransferHandler {
        private final FileEditorDropHandler myFileDropHandler = new FileEditorDropHandler(null);

        @Override
        public boolean importData(JComponent comp, Transferable t) {
            if (myFileDropHandler.canHandleDrop(t.getTransferDataFlavors())) {
                myFileDropHandler.handleDrop(t, myManager.getProject(), myCurrentWindow);
                return true;
            }
            return false;
        }

        @Override
        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            return myFileDropHandler.canHandleDrop(transferFlavors);
        }
    }
}
