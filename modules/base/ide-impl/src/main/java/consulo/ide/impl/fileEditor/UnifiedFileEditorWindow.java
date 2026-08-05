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
package consulo.ide.impl.fileEditor;

import consulo.application.concurrent.coroutine.ReadLock;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.fileEditor.EditorTabPresentationUtil;
import consulo.fileEditor.FileEditorTabbedContainer;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.event.FileEditorManagerBeforeListener;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.fileEditor.impl.internal.FileEditorWindowBase;
import consulo.fileEditor.impl.internal.FileEditorsSplittersBase;
import consulo.ide.impl.idea.openapi.fileEditor.impl.tabActions.CloseTab;
import consulo.ide.impl.virtualFileSystem.VfsIconUtil;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.TextAttribute;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.impl.internal.action.ActionImplUtil;
import consulo.ui.UIAction;
import consulo.ui.image.Image;
import consulo.ui.layout.TabbedLayout;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class UnifiedFileEditorWindow extends FileEditorWindowBase implements FileEditorWindow, Disposable {
    private static class TabInfo {
        private String myText = "";
        private Image myImage;

        // what the platform answers for the file - the status colour of the text, the fill a
        // EditorTabColorProvider gives the tab
        private ColorValue myForeground;
        private ColorValue myBackground;

        private final Tab myTab;

        private TabInfo(Tab tab) {
            myTab = tab;

            myTab.setRenderer((t, p) -> {
                p.withBackgroundColor(myBackground);

                if (myForeground == null) {
                    p.append(myText);
                }
                else {
                    p.append(myText, new TextAttribute(consulo.ui.font.Font.STYLE_PLAIN, myForeground, null));
                }

                if (myImage != null) {
                    p.withIcon(myImage);
                }
            });
        }

        public void select() {
            myTab.select();
        }

        public void update() {
            myTab.update();
        }
    }

    private final Project myProject;
    private FileEditorManagerImpl myManager;
    private FileEditorsSplittersBase<UnifiedFileEditorWindow> myOwner;

    private TabbedLayout myTabbedLayout = TabbedLayout.create();

    private Map<FileEditorWithProviderComposite, TabInfo> myEditors = new LinkedHashMap<>();

    @RequiredUIAccess
    public UnifiedFileEditorWindow(
        Project project,
        FileEditorManagerImpl manager,
        FileEditorsSplittersBase<UnifiedFileEditorWindow> owner
    ) {
        myProject = project;
        myManager = manager;
        myOwner = owner;

        // the tab actions and the tab popup group work on the window the tab belongs to, and the tab has no data of
        // its own - the layout above the editors is the only place the window can be published from
        myTabbedLayout.putUserData(UiDataProvider.KEY, sink -> {
            sink.set(Project.KEY, myProject);
            sink.set(FileEditorWindow.DATA_KEY, this);
        });

        myOwner.addWindow(this);
        if (myOwner.getCurrentWindow() == null) {
            myOwner.setCurrentWindow(this, false);
        }
    }

    
    @Override
    public Component getUIComponent() {
        return myTabbedLayout;
    }

    @Override
    public int getTabCount() {
        return myEditors.size();
    }

    @Override
    protected FileEditorWithProviderComposite getEditorAt(int i) {
        return myEditors.keySet().toArray(new FileEditorWithProviderComposite[myEditors.size()])[i];
    }

    @Override
    protected void setTitleAt(int index, String text) {
        FileEditorWithProviderComposite editorAt = getEditorAt(index);
        TabInfo tab = myEditors.get(editorAt);
        tab.myText = text;
        tab.update();
    }

    @Override
    protected void setBackgroundColorAt(int index, Color color) {
        TabInfo tab = getTabAt(index);
        tab.myBackground = TargetAWT.from(color);
        tab.update();
    }

    @Override
    protected void setToolTipTextAt(int index, String text) {
    }

    @Override
    protected void setForegroundAt(int index, Color color) {
        TabInfo tab = getTabAt(index);

        // a file with no status of its own is answered with the swing label foreground, which is the colour of
        // the awt laf - a frontend without one does not follow it, and a tab written in it stays dark on a dark
        // theme. it says nothing about the file, so the tab carries no colour of its own and the style decides
        ColorValue foreground = color == null || color.equals(UIUtil.getLabelForeground()) ? null : TargetAWT.from(color);
        if (Objects.equals(tab.myForeground, foreground)) {
            return;
        }

        tab.myForeground = foreground;
        tab.update();
    }

    @Override
    protected void setWaveColor(int index, @Nullable Color color) {
    }

    @Override
    protected void setIconAt(int index, Image icon) {
        TabInfo tab = getTabAt(index);
        tab.myImage = icon;
        tab.update();
    }

    private TabInfo getTabAt(int index) {
        return myEditors.get(getEditorAt(index));
    }

    @Override
    protected void setTabLayoutPolicy(int policy) {
    }

    @Override
    protected void trimToSize(int limit, @Nullable VirtualFile fileToIgnore, boolean transferFocus) {
    }

    
    @Override
    public FileEditorManagerImpl getManager() {
        return myManager;
    }

    @Override
    public @Nullable FileEditorWindow split(int orientation, boolean forceSplit, @Nullable VirtualFile virtualFile, boolean focusNew) {
        return null;
    }

    @Override
    public void unsplit(boolean setCurrent) {
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    
    @Override
    public FileEditorWindow[] findSiblings() {
        return new FileEditorWindow[0];
    }

    @Override
    public @Nullable FileEditorWithProviderComposite getSelectedEditor() {
        if (myEditors.isEmpty()) {
            return null;
        }
        Map.Entry<FileEditorWithProviderComposite, TabInfo> entry = myEditors.entrySet().iterator().next();
        return entry.getKey();
    }

    
    @Override
    public FileEditorsSplittersBase<UnifiedFileEditorWindow> getOwner() {
        return myOwner;
    }

    
    @Override
    public FileEditorTabbedContainer getContainer() {
        return new FileEditorTabbedContainer() {
            @Override
            public ActionCallback setSelectedIndex(int index) {
                return ActionCallback.REJECTED;
            }

            
            @Override
            public ActionCallback setSelectedIndex(int indexToSelect, boolean focusEditor) {
                return ActionCallback.REJECTED;
            }

            @Override
            public int getSelectedIndex() {
                return 0;
            }

            @Override
            public int getTabCount() {
                return 0;
            }

            
            @Override
            public ActionCallback removeTabAt(int componentIndex, int indexToSelect, boolean transferFocus) {
                return ActionCallback.REJECTED;
            }

            @Override
            public void close() {

            }
        };
    }

    @Override
    public VirtualFile getSelectedFile() {
        FileEditorWithProviderComposite selectedEditor = getSelectedEditor();
        if (selectedEditor != null) {
            return selectedEditor.getFile();
        }
        return null;
    }

    @Override
    public boolean inSplitter() {
        return false;
    }

    @Override
    public void closeFile(VirtualFile file, boolean disposeIfNeeded, boolean transferFocus) {
        FileEditorManagerImpl editorManager = getManager();
        editorManager.runChange(
            splitters -> {
                List<FileEditorWithProviderComposite> editors = splitters.findEditorComposites(file);
                if (editors.isEmpty()) {
                    return;
                }
                try {
                    FileEditorWithProviderComposite editor = findFileComposite(file);

                    FileEditorManagerBeforeListener beforePublisher =
                        editorManager.getProject().getMessageBus().syncPublisher(FileEditorManagerBeforeListener.class);

                    beforePublisher.beforeFileClosed(editorManager, file);

                    if (editor != null) {
                        TabInfo tab = myEditors.remove(editor);
                        if (tab != null) {
                            // dropping the map entry alone left the tab and its editor on screen
                            myTabbedLayout.removeTab(tab.myTab);

                            editorManager.disposeComposite(editor);
                        }
                    }
                    else {
                        if (inSplitter()) {
                            //Splitter splitter = (Splitter)myPanel.getParent();
                            //JComponent otherComponent = splitter.getOtherComponent(myPanel);

                            //if (otherComponent != null) {
                            //  IdeFocusManager.findInstance().requestFocus(otherComponent, true);
                            //}
                        }

                        //myPanel.removeAll();
                        if (editor != null) {
                            editorManager.disposeComposite(editor);
                        }
                    }

                    //myPanel.revalidate();
                    //if (myTabbedPane == null) {
                    //  // in tabless mode
                    //  myPanel.repaint();
                    //}
                }
                finally {
                    editorManager.removeSelectionRecord(file, this);

                    editorManager.notifyPublisher(() -> {
                        Project project = editorManager.getProject();
                        if (!project.isDisposed()) {
                            FileEditorManagerListener afterPublisher =
                                project.getMessageBus().syncPublisher(FileEditorManagerListener.class);
                            afterPublisher.fileClosed(editorManager, file);
                        }
                    });

                    ((UnifiedFileEditorsSplitters) splitters).afterFileClosed(file);
                }
            },
            myOwner
        );
    }

    @Override
    public void clear() {
        myEditors.clear();
    }

    @Override
    public void setTabsPlacement(int placement) {
    }

    @Override
    public boolean isFilePinned(VirtualFile file) {
        return false;
    }

    @Override
    public void setFilePinned(VirtualFile file, boolean pinned) {
    }

    @Override
    public boolean isFileOpen(VirtualFile virtualFile) {
        for (FileEditorWithProviderComposite editor : myEditors.keySet()) {
            if (editor.getFile().equals(virtualFile)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void changeOrientation() {
    }

    @Override
    public void unsplitAll() {
    }

    @RequiredUIAccess
    @Override
    public void setEditor(@Nullable FileEditorWithProviderComposite editor, boolean selectEditor, boolean focusEditor) {
        if (editor == null) {

        }
        else {
            FileEditorWithProviderComposite fileComposite = findFileComposite(editor.getFile());
            if (fileComposite == null) {
                Tab tab = myTabbedLayout.createTab();
                TabInfo tabInfo = new TabInfo(tab);
                tabInfo.myText = editor.getFile().getName();
                tabInfo.myImage = VfsIconUtil.getIcon(editor.getFile(), 0, myManager.getProject());

                // the handler has to be known before the tab is rendered, the close affordance is part of the tab
                tab.setCloseHandler((thisTab, component) -> performCloseTab(editor.getFile(), component));

                tab.setPopupGroup(IdeActions.GROUP_EDITOR_TAB_POPUP, ActionPlaces.EDITOR_TAB_POPUP);

                myTabbedLayout.addTab(tab, editor.getUIComponent());
                myEditors.put(editor, tabInfo);

                // a fresh tab carries nothing but the name and the icon, and the colours the platform assigns a
                // file - the vcs status of the text, the fill a EditorTabColorProvider gives - only ever arrived
                // with a later update. the awt container asks for both the moment the tab is inserted
                VirtualFile file = editor.getFile();
                myOwner.updateFileColor(file);
                updateFileBackgroundColorAsync(file);
            }
            else {
                TabInfo tab = myEditors.get(fileComposite);
                assert tab != null;
                tab.select();
            }
        }
    }

    /**
     * The provider reads the file scopes to answer, so the colour is computed under a read lock off the ui
     * thread and only applied back on it.
     */
    private void updateFileBackgroundColorAsync(VirtualFile file) {
        CoroutineScope.launchAsync(
            myProject.coroutineContext(),
            () -> Coroutine
                .first(ReadLock.<Void, ColorValue>apply(ignored ->
                    EditorTabPresentationUtil.getEditorTabBackgroundColor(myProject, file, this)))
                .then(UIAction.<ColorValue, Void>apply(color -> {
                    // the tab may be gone by the time the answer arrives
                    int index = findEditorIndex(findFileComposite(file));
                    if (index != -1) {
                        setBackgroundColorAt(index, TargetAWT.to(color));
                    }
                    return null;
                }))
        );
    }

    @RequiredUIAccess
    private void performCloseTab(VirtualFile file, @Nullable Component component) {
        DataContext dataContext = DataContext.builder()
            .parent(component == null ? null : DataManager.getInstance().getDataContext(component))
            .add(Project.KEY, myProject)
            .add(VirtualFile.KEY, file)
            .add(FileEditorWindow.DATA_KEY, this)
            .build();

        CloseTab closeTab = new CloseTab(component, myProject, file, this);

        AnActionEvent event = AnActionEvent.createFromAnAction(closeTab, null, ActionPlaces.EDITOR_TAB, dataContext);

        ActionImplUtil.performActionDumbAwareWithCallbacks(closeTab, event, dataContext);
    }

    @Override
    public void setAsCurrentWindow(boolean value) {
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void restoreClosedTab() {
    }

    @Override
    public boolean hasClosedTabs() {
        return false;
    }

    @Override
    public void requestFocus(boolean force) {
    }

    @Override
    public void dispose() {
    }
}
