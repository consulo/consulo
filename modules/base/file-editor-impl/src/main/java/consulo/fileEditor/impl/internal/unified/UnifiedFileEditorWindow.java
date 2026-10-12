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

import consulo.application.concurrent.coroutine.ReadLock;
import consulo.application.ui.UISettings;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.event.FocusChangeListener;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.EditorTabPresentationUtil;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorTabbedContainer;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.impl.internal.CloseTab;
import consulo.fileEditor.impl.internal.FileDropManager;
import consulo.fileEditor.impl.internal.FileEditorManagerImpl;
import consulo.fileEditor.impl.internal.FileEditorWindowBase;
import consulo.fileEditor.impl.internal.FileEditorsSplittersBase;
import consulo.fileEditor.impl.internal.VfsIconUtil;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.DragAndDropTransferHandler;
import consulo.ui.HasFocus;
import consulo.ui.Tab;
import consulo.ui.TextAttribute;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionToolbarFactory;
import consulo.ui.ex.action.CloseAction;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.layout.TabbedLayout;
import consulo.ui.layout.TabbedLayoutStyle;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class UnifiedFileEditorWindow extends FileEditorWindowBase implements FileEditorWindow, Disposable {
    private static class TabInfo {
        private final FileEditorWithProviderComposite myComposite;
        private final Disposable myDisposable = Disposable.newDisposable();

        private String myText = "";
        private Image myImage;

        private ColorValue myForeground;
        private ColorValue myBackground;

        private final Tab myTab;

        private @Nullable ActionToolbar myToolbar;

        private TabInfo(FileEditorWithProviderComposite composite, Tab tab) {
            myComposite = composite;
            myTab = tab;

            myTab.setRenderer((t, p) -> {
                p.withBackgroundColor(myBackground);

                if (myForeground == null) {
                    p.append(myText);
                }
                else {
                    p.append(myText, new TextAttribute(consulo.ui.font.Font.PLAIN, myForeground, null));
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
    private final FileEditorManagerImpl myManager;
    private final FileEditorsSplittersBase<UnifiedFileEditorWindow> myOwner;

    private final TabbedLayout myTabbedLayout = TabbedLayout.create();

    private final List<TabInfo> myTabs = new ArrayList<>();

    private @Nullable FileEditorWithProviderComposite mySelectedEditor;

    private final FileEditorTabbedContainer myContainer = new FileEditorTabbedContainer() {
        @Override
        public ActionCallback setSelectedIndex(int index) {
            return setSelectedIndex(index, true);
        }

        @Override
        public ActionCallback setSelectedIndex(int indexToSelect, boolean focusEditor) {
            if (indexToSelect < 0 || indexToSelect >= myTabs.size()) {
                return ActionCallback.REJECTED;
            }

            TabInfo tabInfo = myTabs.get(indexToSelect);
            mySelectedEditor = tabInfo.myComposite;
            tabInfo.select();
            if (focusEditor) {
                requestFocus(true);
            }
            return ActionCallback.DONE;
        }

        @Override
        public int getSelectedIndex() {
            FileEditorWithProviderComposite selectedEditor = getSelectedEditor();
            return selectedEditor == null ? -1 : findEditorIndex(selectedEditor);
        }

        @Override
        public int getTabCount() {
            return myTabs.size();
        }

        @Override
        public ActionCallback removeTabAt(int componentIndex, int indexToSelect, boolean transferFocus) {
            if (componentIndex < 0 || componentIndex >= myTabs.size()) {
                return ActionCallback.REJECTED;
            }

            TabInfo toSelect = indexToSelect >= 0 && indexToSelect < myTabs.size() ? myTabs.get(indexToSelect) : null;

            removeTab(myTabs.get(componentIndex));

            if (toSelect != null && myTabs.contains(toSelect)) {
                mySelectedEditor = toSelect.myComposite;
                toSelect.select();
                if (transferFocus) {
                    requestFocus(true);
                }
            }
            return ActionCallback.DONE;
        }

        @Override
        public void close() {
            FileEditorWithProviderComposite selectedEditor = getSelectedEditor();
            if (selectedEditor != null) {
                closeFile(selectedEditor.getFile());
            }
        }
    };

    @RequiredUIAccess
    public UnifiedFileEditorWindow(
        Project project,
        FileEditorManagerImpl manager,
        FileEditorsSplittersBase<UnifiedFileEditorWindow> owner
    ) {
        myProject = project;
        myManager = manager;
        myOwner = owner;

        myTabbedLayout.addStyle(TabbedLayoutStyle.NO_PADDING);

        myTabbedLayout.putUserData(UiDataProvider.KEY, sink -> {
            sink.set(Project.KEY, myProject);
            sink.set(FileEditorWindow.DATA_KEY, this);
        });

        myTabbedLayout.setTransferHandler(new TabTransferHandler());
        myTabbedLayout.setDropHandler((component, transfer, checkOnly) -> dropOnContent(transfer, checkOnly));

        myTabbedLayout.addSelectListener(event -> {
            for (TabInfo tabInfo : myTabs) {
                if (tabInfo.myTab == event.getTab()) {
                    mySelectedEditor = tabInfo.myComposite;
                    makeCurrent();
                    return;
                }
            }
        });

        myOwner.addWindow(this);
        if (myOwner.getCurrentWindow() == null) {
            myOwner.setCurrentWindow(this, false);
        }
    }

    @RequiredUIAccess
    private void makeCurrent() {
        if (!isDisposed() && myOwner.containsWindow(this) && myOwner.getCurrentWindow() != this) {
            myOwner.setCurrentWindow(this, false);
        }
    }

    @Override
    public Component getUIComponent() {
        return myTabbedLayout;
    }

    @Override
    public int getTabCount() {
        return myTabs.size();
    }

    @Override
    protected FileEditorWithProviderComposite getEditorAt(int i) {
        return myTabs.get(i).myComposite;
    }

    private @Nullable TabInfo findTab(FileEditorWithProviderComposite composite) {
        for (TabInfo tabInfo : myTabs) {
            if (tabInfo.myComposite == composite) {
                return tabInfo;
            }
        }
        return null;
    }

    @Override
    protected void setTitleAt(int index, String text) {
        TabInfo tab = myTabs.get(index);
        tab.myText = text;
        tab.update();
    }

    @Override
    protected void setBackgroundColorAt(int index, Color color) {
        TabInfo tab = myTabs.get(index);
        tab.myBackground = TargetAWT.from(color);
        tab.update();
    }

    @Override
    protected void setToolTipTextAt(int index, String text) {
    }

    @Override
    protected void setForegroundAt(int index, Color color) {
        TabInfo tab = myTabs.get(index);

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
        TabInfo tab = myTabs.get(index);
        tab.myImage = icon;
        tab.update();
    }

    @Override
    protected void setTabLayoutPolicy(int policy) {
    }

    @Override
    public FileEditorManagerImpl getManager() {
        return myManager;
    }

    @Override
    public @Nullable FileEditorWithProviderComposite getSelectedEditor() {
        if (mySelectedEditor != null && findTab(mySelectedEditor) != null) {
            return mySelectedEditor;
        }

        if (myTabs.isEmpty()) {
            return null;
        }
        return myTabs.get(0).myComposite;
    }

    @Override
    public FileEditorsSplittersBase<UnifiedFileEditorWindow> getOwner() {
        return myOwner;
    }

    @Override
    public FileEditorTabbedContainer getContainer() {
        return myContainer;
    }

    @Override
    protected void disposeTabs() {
        for (TabInfo tabInfo : new ArrayList<>(myTabs)) {
            removeTab(tabInfo);
        }
        mySelectedEditor = null;
    }

    @Override
    protected void detachEditor(FileEditorWithProviderComposite composite) {
        TabInfo tabInfo = findTab(composite);
        if (tabInfo != null) {
            removeTab(tabInfo);
        }
    }

    private void removeTab(TabInfo tabInfo) {
        myTabs.remove(tabInfo);
        Disposer.dispose(tabInfo.myDisposable);
        myTabbedLayout.removeTab(tabInfo.myTab);
        if (mySelectedEditor == tabInfo.myComposite) {
            mySelectedEditor = null;
        }
    }

    @Override
    @RequiredUIAccess
    protected void afterPinnedChanged(FileEditorWithProviderComposite composite) {
        TabInfo tabInfo = findTab(composite);
        ActionToolbar toolbar = tabInfo == null ? null : tabInfo.myToolbar;
        if (toolbar != null) {
            toolbar.updateActionsAsync();
        }
    }

    @RequiredUIAccess
    private void moveTab(TabInfo tabInfo, int index) {
        int from = myTabs.indexOf(tabInfo);
        if (from == -1 || from == index) {
            return;
        }

        myTabs.remove(from);
        myTabs.add(index, tabInfo);
        myTabbedLayout.moveTab(tabInfo.myTab, index);
    }

    private class TabTransferHandler implements DragAndDropTransferHandler<Tab> {
        @Override
        public @Nullable DataTransfer createTransfer(Component component) {
            return null;
        }

        @Override
        public @Nullable DataTransfer createDragTransfer(Component component, List<Tab> items, boolean move) {
            if (items.size() != 1) {
                return null;
            }

            TabInfo tabInfo = findTab(items.get(0));
            if (tabInfo == null) {
                return null;
            }

            EditorTabTransfer transfer = new EditorTabTransfer(UnifiedFileEditorWindow.this, tabInfo.myComposite.getFile(), !move);
            return DataTransfer.builder().put(EditorTabTransfer.TYPE, transfer).build();
        }

        @Override
        @RequiredUIAccess
        public boolean drop(Component component, DropContext<Tab> context) {
            EditorTabTransfer transfer = context.getTransfer().get(EditorTabTransfer.TYPE);
            if (transfer == null) {
                return dropFiles(context.getTransfer(), context.isCheckOnly());
            }

            TabInfo target = findTab(context.getTarget());
            if (target == null || isDisposed()) {
                return false;
            }

            UnifiedFileEditorWindow source = transfer.getWindow();
            if (source.isDisposed() || source.getManager() != myManager) {
                return false;
            }

            int index = myTabs.indexOf(target) + (context.getPosition() == DropPosition.BELOW ? 1 : 0);
            VirtualFile file = transfer.getFile();

            if (source == UnifiedFileEditorWindow.this) {
                FileEditorWithProviderComposite composite = findFileComposite(file);
                TabInfo dragged = composite == null ? null : findTab(composite);
                if (dragged == null) {
                    return false;
                }

                int from = myTabs.indexOf(dragged);
                int to = index > from ? index - 1 : index;
                if (to == from) {
                    return false;
                }

                if (!context.isCheckOnly()) {
                    moveTab(dragged, to);
                }
                return true;
            }

            return dropFromOtherWindow(transfer, index, context.isCheckOnly());
        }
    }

    @RequiredUIAccess
    private boolean dropOnContent(DataTransfer transfer, boolean checkOnly) {
        EditorTabTransfer tabTransfer = transfer.get(EditorTabTransfer.TYPE);
        if (tabTransfer == null) {
            return dropFiles(transfer, checkOnly);
        }

        UnifiedFileEditorWindow source = tabTransfer.getWindow();
        if (source == this || isDisposed() || source.isDisposed() || source.getManager() != myManager) {
            return false;
        }
        return dropFromOtherWindow(tabTransfer, myTabs.size(), checkOnly);
    }

    @RequiredUIAccess
    private boolean dropFromOtherWindow(EditorTabTransfer transfer, int index, boolean checkOnly) {
        UnifiedFileEditorWindow source = transfer.getWindow();
        VirtualFile file = transfer.getFile();
        if (!source.isFileOpen(file)) {
            return false;
        }

        if (checkOnly) {
            return true;
        }

        boolean pinned = source.isFilePinned(file);
        if (!transfer.isCopy()) {
            source.closeFileToReopen(file);
        }
        openDroppedFile(file, index, pinned);
        return true;
    }

    @RequiredUIAccess
    private boolean dropFiles(DataTransfer transfer, boolean checkOnly) {
        return !isDisposed() && FileDropManager.getInstance(myProject).drop(transfer, this, checkOnly);
    }

    private @Nullable TabInfo findTab(Tab tab) {
        for (TabInfo tabInfo : myTabs) {
            if (tabInfo.myTab == tab) {
                return tabInfo;
            }
        }
        return null;
    }

    @Override
    public void clear() {
        disposeTabs();
    }

    @Override
    public void setTabsPlacement(int placement) {
    }

    @RequiredUIAccess
    @Override
    public void setEditor(@Nullable FileEditorWithProviderComposite editor, boolean selectEditor, boolean focusEditor) {
        if (editor == null) {
            return;
        }

        FileEditorWithProviderComposite fileComposite = findFileComposite(editor.getFile());
        if (fileComposite == null) {
            Tab tab = myTabbedLayout.createTab();
            TabInfo tabInfo = new TabInfo(editor, tab);
            tabInfo.myText = editor.getFile().getName();
            tabInfo.myImage = VfsIconUtil.getIcon(editor.getFile(), 0, myManager.getProject());

            tab.setPopupGroup(IdeActions.GROUP_EDITOR_TAB_POPUP, ActionPlaces.EDITOR_TAB_POPUP);

            Component content = editor.getUIComponent();
            content.putUserData(UiDataProvider.KEY, sink -> {
                sink.set(VirtualFile.KEY, editor.getFile());
                sink.set(CloseAction.CloseTarget.KEY, () -> closeFile(editor.getFile()));
            });

            ActionGroup.Builder tabActions = ActionGroup.newImmutableBuilder();
            tabActions.add(new CloseTab(content, myProject, editor.getFile(), this));

            ActionToolbar toolbar = ActionToolbarFactory.getInstance()
                .createActionToolbar(ActionPlaces.EDITOR_TAB, tabActions.build(), ActionToolbar.Style.INPLACE);
            toolbar.setTargetUIComponent(content);
            toolbar.updateActionsAsync();
            tabInfo.myToolbar = toolbar;
            tab.setSuffixComponent(toolbar.getUIComponent());

            listenEditorFocus(editor, tabInfo.myDisposable);

            int index = Math.max(0, Math.min(getIndexToInsert(editor.getFile()), myTabs.size()));
            myTabs.add(index, tabInfo);
            myTabbedLayout.addTab(tab, content, index);

            VirtualFile file = editor.getFile();
            trimToSize(UISettings.getInstance().getEditorTabLimit(), file, false);

            // a file opened into a tab which was already there comes to the front, and one opening a tab of
            // its own has no more reason to stay behind whatever was selected before it
            if (selectEditor && myTabs.contains(tabInfo)) {
                mySelectedEditor = editor;
                tabInfo.select();
            }

            // a fresh tab carries nothing but the name and the icon, and the colours the platform assigns a
            // file - the vcs status of the text, the fill a EditorTabColorProvider gives - only ever arrived
            // with a later update. the awt container asks for both the moment the tab is inserted
            myOwner.updateFileColor(file);
            updateFileBackgroundColorAsync(file);
        }
        else {
            TabInfo tab = findTab(fileComposite);
            assert tab != null;
            if (selectEditor) {
                mySelectedEditor = fileComposite;
            }
            tab.select();
        }

        if (focusEditor) {
            requestFocus(true);
        }

        myOwner.setCurrentWindow(this, false);
    }

    private void listenEditorFocus(FileEditorWithProviderComposite composite, Disposable parentDisposable) {
        for (FileEditor fileEditor : composite.getEditors()) {
            if (fileEditor instanceof TextEditor textEditor && textEditor.getEditor() instanceof EditorEx editorEx) {
                editorEx.addFocusListener(new FocusChangeListener() {
                    @Override
                    @RequiredUIAccess
                    public void focusGained(Editor editor) {
                        makeCurrent();
                    }
                }, parentDisposable);
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

    @Override
    public boolean isValid() {
        return !isDisposed();
    }

    @Override
    public void requestFocus(boolean force) {
        FileEditorWithProviderComposite selectedEditor = getSelectedEditor();
        if (selectedEditor == null) {
            return;
        }

        FileEditor fileEditor = selectedEditor.getSelectedEditor();
        Component component = fileEditor.getPreferredFocusedUIComponent();
        if (component instanceof HasFocus hasFocus) {
            hasFocus.focus();
        }
    }
}
