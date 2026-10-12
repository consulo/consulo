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

import consulo.application.Application;
import consulo.application.ui.UISettings;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.desktop.awt.ui.impl.tabs.JBTabsImpl;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditorTabbedContainer;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.fileEditor.impl.internal.FileEditorWindowBase;
import consulo.fileEditor.internal.EditorWindowHolder;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.IdeFocusTraversalPolicy;
import consulo.ui.ex.awt.ThreeComponentsSplitter;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tab.JBTabs;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * @author msk
 */
public class DesktopFileEditorWindow extends FileEditorWindowBase implements FileEditorWindow {
    private final JPanel myPanel;
    private final consulo.ui.Component myUIComponent;
    private final DesktopAWTEditorTabbedContainer myTabbedPane;
    private final DesktopFileEditorsSplitters myOwner;

    protected DesktopFileEditorWindow(DesktopFileEditorsSplitters owner) {
        myOwner = owner;
        myPanel = new JPanel(new BorderLayout());
        myPanel.setOpaque(false);
        myUIComponent = TargetAWT.wrap(myPanel);

        myTabbedPane = new DesktopAWTEditorTabbedContainer(this, getManager().getProject());
        myPanel.add(myTabbedPane.getComponent(), BorderLayout.CENTER);

        if (UISettings.getInstance().getScrollTabLayoutInEditor()) {
            setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        }
        else {
            setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        }

        setTabsPlacement(UISettings.getInstance().getEditorTabPlacement());

        myOwner.addWindow(this);
        if (myOwner.getCurrentWindow() == null) {
            myOwner.setCurrentWindow(this, false);
        }
    }

    public boolean isShowing() {
        return myPanel.isShowing();
    }

    JPanel getPanel() {
        return myPanel;
    }

    @Override
    public consulo.ui.Component getUIComponent() {
        return myUIComponent;
    }

    @Override
    protected void disposeTabs() {
        Disposer.dispose(myTabbedPane);
        myPanel.removeAll();
        myPanel.revalidate();
    }

    @Override
    public FileEditorTabbedContainer getContainer() {
        return myTabbedPane;
    }

    @Override
    protected void afterTabsChanged() {
        myPanel.revalidate();
        myPanel.repaint();
    }

    @Override
    protected void afterRemovedFromSplitter() {
        if (UISettings.getInstance().getEditorTabPlacement() == UISettings.PLACEMENT_EDITOR_TAB_NONE) {
            ThreeComponentsSplitter splitter = UIUtil.getParentOfType(ThreeComponentsSplitter.class, myOwner.getComponent());
            if (splitter != null) {
                splitter.revalidate();
                splitter.repaint();
            }
        }
    }

    @Override
    protected void afterPinnedChanged(FileEditorWithProviderComposite composite) {
        JBTabs tabs = myTabbedPane.getTabs();
        if (tabs instanceof JBTabsImpl implTabs) {
            implTabs.doLayout();
        }
    }

    @Override
    protected boolean isEditorFocused(FileEditorWithProviderComposite composite) {
        Component owner = ProjectIdeFocusManager.getInstance(getManager().getProject()).getFocusOwner();
        DesktopFileEditorWithProviderComposite desktopComposite = (DesktopFileEditorWithProviderComposite) composite;
        return owner != null && SwingUtilities.isDescendingFrom(owner, desktopComposite.getSelectedEditor().getComponent());
    }

    @RequiredUIAccess
    @Override
    protected void focusEditor(FileEditorWithProviderComposite composite) {
        JComponent component = ((DesktopFileEditorWithProviderComposite) composite).getComponent();
        IdeFocusManager.getGlobalInstance()
            .doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(component, true));
    }

    @Override
    public @Nullable String getSideTabsSizeLimit() {
        Integer limit = UIUtil.getClientProperty(myTabbedPane.getComponent(), JBTabsImpl.SIDE_TABS_SIZE_LIMIT_KEY);
        return limit == null ? null : String.valueOf(limit);
    }

    @Override
    public void setSideTabsSizeLimit(@Nullable String sideTabsSizeLimit) {
        if (sideTabsSizeLimit == null) {
            return;
        }

        try {
            int limit = Integer.parseInt(sideTabsSizeLimit);
            UIUtil.putClientProperty(myTabbedPane.getComponent(), JBTabsImpl.SIDE_TABS_SIZE_LIMIT_KEY, limit);
        }
        catch (NumberFormatException ignored) {
        }
    }

    @Override
    public FileEditorManagerImpl getManager() {
        return myOwner.getManager();
    }

    @Override
    public int getTabCount() {
        return myTabbedPane.getTabCount();
    }

    @Override
    protected void setForegroundAt(int index, Color color) {
        myTabbedPane.setForegroundAt(index, color);
    }

    @Override
    protected void setWaveColor(int index, @Nullable Color color) {
        myTabbedPane.setWaveColor(index, color);
    }

    @Override
    protected void setIconAt(int index, Image icon) {
        myTabbedPane.setIconAt(index, icon);
    }

    @Override
    protected void setTitleAt(int index, String text) {
        myTabbedPane.setTitleAt(index, text);
    }

    @Override
    protected void setBackgroundColorAt(int index, Color color) {
        myTabbedPane.setBackgroundColorAt(index, color);
    }

    @Override
    protected void setToolTipTextAt(int index, String text) {
        myTabbedPane.setToolTipTextAt(index, text);
    }

    @Override
    protected void setTabLayoutPolicy(int policy) {
        myTabbedPane.setTabLayoutPolicy(policy);
    }

    @Override
    public void setTabsPlacement(int tabPlacement) {
        myTabbedPane.setTabPlacement(tabPlacement);
    }

    @Override
    public DesktopFileEditorsSplitters getOwner() {
        return myOwner;
    }

    boolean isEmptyVisible() {
        return myTabbedPane.isEmptyVisible();
    }

    public Dimension getSize() {
        return myPanel.getSize();
    }

    public DesktopAWTEditorTabbedContainer getTabbedPane() {
        return myTabbedPane;
    }

    @Override
    public void requestFocus(boolean forced) {
        myTabbedPane.requestFocus(forced);
    }

    @Override
    public boolean isValid() {
        return myPanel.isShowing();
    }

    public void setPaintBlocked(boolean blocked) {
        myTabbedPane.setPaintBlocked(blocked);
    }

    protected static class TComp extends JPanel implements UiDataProvider, EditorWindowHolder, IdeFocusTraversalPolicy.PassThroughComponent {
        DesktopFileEditorWithProviderComposite myEditor;
        protected final FileEditorWindow myWindow;

        TComp(DesktopFileEditorWindow window, DesktopFileEditorWithProviderComposite editor) {
            super(new BorderLayout());
            myEditor = editor;
            myWindow = window;
            add(editor.getComponent(), BorderLayout.CENTER);
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    Application.get().invokeLater(() -> {
                        if (!hasFocus()) {
                            return;
                        }
                        JComponent focus = myEditor.getSelectedEditorWithProvider().fileEditor().getPreferredFocusedComponent();
                        if (focus != null && !focus.hasFocus()) {
                            IdeFocusManager.getGlobalInstance().requestFocus(focus, true);
                        }
                    });
                }
            });
        }

        @Override
        public FileEditorWindow getEditorWindow() {
            return myWindow;
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
            sink.lazy(VirtualFile.KEY, () -> {
                VirtualFile virtualFile = myEditor.getFile();
                return virtualFile.isValid() ? virtualFile : null;
            });
            sink.lazy(Project.KEY, () -> myEditor.getFileEditorManager().getProject());
        }
    }

    @Override
    public @Nullable DesktopFileEditorWithProviderComposite getSelectedEditor() {
        TComp comp = (TComp) myTabbedPane.getSelectedComponent();

        if (comp != null) {
            return comp.myEditor;
        }
        return null;
    }

    @RequiredUIAccess
    @Override
    public void setEditor(@Nullable FileEditorWithProviderComposite editor, boolean selectEditor, boolean focusEditor) {
        if (editor != null) {
            onBeforeSetEditor(editor.getFile());

            int index = findEditorIndex(editor);
            if (index != -1) {
                if (selectEditor) {
                    setSelectedEditor(editor, focusEditor);
                }
            }
            else {
                int indexToInsert = getIndexToInsert(editor.getFile());

                VirtualFile file = editor.getFile();
                Image template = PlatformIconGroup.filetypesText();
                myTabbedPane.insertTab(
                    file,
                    Image.empty(template.getWidth(), template.getHeight()),
                    new TComp(this, (DesktopFileEditorWithProviderComposite) editor),
                    null,
                    indexToInsert,
                    this
                );
                trimToSize(UISettings.getInstance().getEditorTabLimit(), file, false);
                if (selectEditor) {
                    setSelectedEditor(editor, focusEditor);
                }
                myOwner.updateFileIconAsync(file);
                myOwner.updateFileColor(file);
            }
            myOwner.setCurrentWindow(this, false);
        }
        myOwner.getComponent().validate();
    }

    protected void onBeforeSetEditor(VirtualFile file) {
    }

    @Override
    protected DesktopFileEditorWithProviderComposite getEditorAt(int i) {
        TComp comp = (TComp) myTabbedPane.getComponentAt(i);
        return comp.myEditor;
    }

    @Override
    public void clear() {
        for (FileEditorWithProviderComposite composite : getEditors()) {
            Disposer.dispose(composite);
        }
    }

    @Override
    public @Nullable DesktopFileEditorWithProviderComposite findFileComposite(VirtualFile file) {
        return (DesktopFileEditorWithProviderComposite) super.findFileComposite(file);
    }
}
