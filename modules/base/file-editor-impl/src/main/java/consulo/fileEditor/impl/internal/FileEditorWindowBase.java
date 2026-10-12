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
package consulo.fileEditor.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.ui.UISettings;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.ScrollingModel;
import consulo.component.util.Iconable;
import consulo.fileEditor.*;
import consulo.fileEditor.event.FileEditorManagerBeforeListener;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.fileEditor.internal.FileEditorWithModifiedIcon;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.util.collection.Stack;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import consulo.virtualFileSystem.fileType.UnknownFileType;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author VISTALL
 * @since 2018-05-11
 */
public abstract class FileEditorWindowBase extends EditorSplitNode implements FileEditorWindow {
    private static final Logger LOG = Logger.getInstance(FileEditorWindowBase.class);

    private boolean myIsDisposed;
    private @Nullable String mySideTabsSizeLimit;

    private final Stack<Pair<String, Integer>> myRemovedTabs = new Stack<>() {
        @Override
        public void push(Pair<String, Integer> pair) {
            if (size() >= UISettings.getInstance().getEditorTabLimit()) {
                remove(0);
            }
            super.push(pair);
        }
    };
    private final AtomicBoolean myTabsHidingInProgress = new AtomicBoolean(false);
    private final Stack<Pair<String, Integer>> myHiddenTabs = new Stack<>();

    protected abstract FileEditorWithProviderComposite getEditorAt(int i);

    protected abstract void setTitleAt(int index, String text);

    protected abstract void setBackgroundColorAt(int index, Color color);

    protected abstract void setToolTipTextAt(int index, String text);

    protected abstract void setForegroundAt(int index, Color color);

    protected abstract void setWaveColor(int index, @Nullable Color color);

    protected abstract void setIconAt(int index, Image icon);

    protected abstract void setTabLayoutPolicy(int policy);

    protected abstract void disposeTabs();

    @Override
    public abstract FileEditorsSplittersBase<?> getOwner();

    @Override
    public abstract FileEditorManagerImpl getManager();

    @Override
    public abstract FileEditorTabbedContainer getContainer();

    protected void detachEditor(FileEditorWithProviderComposite composite) {
    }

    protected void afterTabsChanged() {
    }

    protected void afterRemovedFromSplitter() {
    }

    protected void afterPinnedChanged(FileEditorWithProviderComposite composite) {
    }

    protected boolean isEditorFocused(FileEditorWithProviderComposite composite) {
        return false;
    }

    @RequiredUIAccess
    protected void focusEditor(FileEditorWithProviderComposite composite) {
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> requestFocus(true));
    }

    public @Nullable String getSideTabsSizeLimit() {
        return mySideTabsSizeLimit;
    }

    public void setSideTabsSizeLimit(@Nullable String sideTabsSizeLimit) {
        mySideTabsSizeLimit = sideTabsSizeLimit;
    }

    protected void updateFileName(VirtualFile file, String tabText, String tabTooltip) {
        int index = findEditorIndex(findFileComposite(file));
        if (index != -1) {
            setTitleAt(index, tabText);
            setToolTipTextAt(index, tabTooltip);
        }
    }

    protected void updateFileIcon(VirtualFile file, Image image) {
        int index = findEditorIndex(findFileComposite(file));
        if (index != -1) {
            setIconAt(index, image);
        }
    }

    protected void updateFileBackgroundColor(VirtualFile file) {
        int index = findEditorIndex(findFileComposite(file));
        if (index != -1) {
            ColorValue color = EditorTabPresentationUtil.getEditorTabBackgroundColor(getManager().getProject(), file, this);
            setBackgroundColorAt(index, TargetAWT.to(color));
        }
    }

    /**
     * @return icon which represents file's type and modification status
     */
    @RequiredReadAction
    protected @Nullable Image getFileIcon(VirtualFile file) {
        UIAccess.assetIsNotUIThread();
        if (!file.isValid()) {
            return UnknownFileType.INSTANCE.getIcon();
        }

        Image baseIcon = VirtualFileManager.getInstance().getFileIconNoDefer(file, getManager().getProject(), Iconable.ICON_FLAG_READ_STATUS);

        FileEditorWithProviderComposite composite = findFileComposite(file);

        boolean wantModifiedIcon = false;

        UISettings settings = UISettings.getInstance();
        if (settings.getMarkModifiedTabsWithAsterisk() || !settings.getHideTabsIfNeed()) {
            if (settings.getMarkModifiedTabsWithAsterisk() && composite != null && composite.isModified()) {
                wantModifiedIcon = true;
            }
        }

        if (!wantModifiedIcon && composite != null) {
            for (FileEditor fileEditor : composite.getEditors()) {
                if (fileEditor instanceof FileEditorWithModifiedIcon && fileEditor.isModified()) {
                    wantModifiedIcon = true;
                    break;
                }
            }
        }

        if (wantModifiedIcon) {
            return ImageEffects.layered(baseIcon, PlatformIconGroup.generalModified());
        }

        return baseIcon;
    }

    public int findEditorIndex(@Nullable FileEditorComposite editorToFind) {
        for (int i = 0; i != getTabCount(); ++i) {
            FileEditorWithProviderComposite editor = getEditorAt(i);
            if (editor.equals(editorToFind)) {
                return i;
            }
        }
        return -1;
    }

    public VirtualFile getFileAt(int i) {
        return getEditorAt(i).getFile();
    }

    @Override
    public void closeAllExcept(VirtualFile selectedFile) {
        VirtualFile[] files = getFiles();
        for (VirtualFile file : files) {
            if (!Objects.equals(file, selectedFile) && !isFilePinned(file)) {
                closeFile(file);
            }
        }
    }

    @Override
    public VirtualFile[] getFiles() {
        int tabCount = getTabCount();
        VirtualFile[] res = new VirtualFile[tabCount];
        for (int i = 0; i != tabCount; ++i) {
            res[i] = getEditorAt(i).getFile();
        }
        return res;
    }

    @Override
    public int findFileIndex(VirtualFile fileToFind) {
        for (int i = 0; i != getTabCount(); ++i) {
            VirtualFile file = getFileAt(i);
            if (file.equals(fileToFind)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public FileEditorWithProviderComposite[] getEditors() {
        int tabCount = getTabCount();
        FileEditorWithProviderComposite[] res = new FileEditorWithProviderComposite[tabCount];
        for (int i = 0; i != tabCount; ++i) {
            res[i] = getEditorAt(i);
        }
        return res;
    }

    @Override
    public @Nullable FileEditorWithProviderComposite findFileComposite(VirtualFile file) {
        for (int i = 0; i != getTabCount(); ++i) {
            FileEditorWithProviderComposite editor = getEditorAt(i);
            if (editor.getFile().equals(file)) {
                return editor;
            }
        }
        return null;
    }

    @Override
    public @Nullable VirtualFile getSelectedFile() {
        FileEditorWithProviderComposite editor = getSelectedEditor();
        return editor == null ? null : editor.getFile();
    }

    @Override
    public boolean isFileOpen(VirtualFile file) {
        return findFileComposite(file) != null;
    }

    @Override
    public boolean isDisposed() {
        return myIsDisposed;
    }

    @RequiredUIAccess
    public void dispose() {
        try {
            disposeTabs();
            getOwner().removeWindow(this);
        }
        finally {
            myIsDisposed = true;
        }
    }

    @Override
    public void setAsCurrentWindow(boolean requestFocus) {
        getOwner().setCurrentWindow(this, requestFocus);
    }

    protected void checkConsistency() {
        LOG.assertTrue(getOwner().containsWindow(this), "EditorWindow not in collection");
    }

    public void setSelectedEditor(@Nullable FileEditorComposite editor, boolean focusEditor) {
        if (editor != null) {
            int index = findFileIndex(editor.getFile());
            if (index != -1) {
                getContainer().setSelectedIndex(index, focusEditor);
            }
        }
    }

    protected int getIndexToInsert(VirtualFile file) {
        Integer initialIndex = file.getUserData(INITIAL_INDEX_KEY);
        if (initialIndex != null) {
            return initialIndex;
        }

        if (Registry.is("ide.editor.tabs.open.at.the.end")) {
            return getTabCount();
        }

        int selectedIndex = getContainer().getSelectedIndex();
        return selectedIndex >= 0 ? selectedIndex + 1 : 0;
    }

    @RequiredUIAccess
    public void closeFileToReopen(VirtualFile file) {
        file.putUserData(FileEditorManager.CLOSING_TO_REOPEN, Boolean.TRUE);
        try {
            getManager().closeFile(file, this);
        }
        finally {
            file.putUserData(FileEditorManager.CLOSING_TO_REOPEN, null);
        }
    }

    @RequiredUIAccess
    public void openDroppedFile(VirtualFile file, boolean pinned) {
        getManager().openFileImpl2(UIAccess.current(), this, file, true);
        if (isFileOpen(file)) {
            setFilePinned(file, pinned);
        }
    }

    @RequiredUIAccess
    public void openDroppedFile(VirtualFile file, int index, boolean pinned) {
        file.putUserData(INITIAL_INDEX_KEY, index);
        try {
            openDroppedFile(file, pinned);
        }
        finally {
            file.putUserData(INITIAL_INDEX_KEY, null);
        }
    }

    @Override
    public boolean hasClosedTabs() {
        return !myRemovedTabs.empty();
    }

    @Override
    @RequiredUIAccess
    public void restoreClosedTab() {
        assert hasClosedTabs() : "Nothing to restore";

        Pair<String, Integer> info = myRemovedTabs.pop();
        VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(info.getFirst());
        Integer second = info.getSecond();
        if (file != null) {
            getManager().openFileImpl4(UIAccess.current(), this, file, null, true, true, null, second == null ? -1 : second);
        }
    }

    @RequiredUIAccess
    private void restoreHiddenTabs() {
        while (!myHiddenTabs.isEmpty()) {
            Pair<String, Integer> info = myHiddenTabs.pop();
            myRemovedTabs.remove(info);
            VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(info.getFirst());
            Integer second = info.getSecond();
            if (file != null) {
                getManager().openFileImpl4(UIAccess.current(), this, file, null, true, true, null, second == null ? -1 : second);
            }
        }
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
                        int componentIndex = findEditorIndex(editor);
                        if (componentIndex >= 0) {
                            int indexToSelect = calcIndexToSelect(file, componentIndex);
                            Pair<String, Integer> pair = Pair.create(file.getUrl(), componentIndex);
                            myRemovedTabs.push(pair);
                            if (myTabsHidingInProgress.get()) {
                                myHiddenTabs.push(pair);
                            }
                            getContainer().removeTabAt(componentIndex, indexToSelect, transferFocus);
                            editorManager.disposeComposite(editor);
                        }
                    }

                    if (disposeIfNeeded && getTabCount() == 0) {
                        removeFromSplitter();
                        afterRemovedFromSplitter();
                    }
                    else {
                        afterTabsChanged();
                    }
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

                    getOwner().afterFileClosed(file);
                }
            },
            getOwner()
        );
    }

    @RequiredUIAccess
    protected void removeFromSplitter() {
        if (!inSplitter()) {
            return;
        }

        FileEditorsSplittersBase<?> owner = getOwner();
        if (owner.getCurrentWindow() == this) {
            FileEditorWindowBase[] siblings = findSiblings();
            owner.setCurrentWindow(siblings[0], false);
        }

        owner.removeWindowNode(this);

        dispose();
    }

    private int calcIndexToSelect(VirtualFile fileBeingClosed, int fileIndex) {
        int currentlySelectedIndex = getContainer().getSelectedIndex();
        if (currentlySelectedIndex != fileIndex) {
            return currentlySelectedIndex;
        }
        UISettings uiSettings = UISettings.getInstance();
        if (uiSettings.getActiveMruEditorOnClose()) {
            VirtualFile[] histFiles = EditorHistoryManagerImpl.getInstance(getManager().getProject()).getFiles();
            for (int idx = histFiles.length - 1; idx >= 0; idx--) {
                VirtualFile histFile = histFiles[idx];
                if (histFile.equals(fileBeingClosed)) {
                    continue;
                }
                FileEditorWithProviderComposite editor = findFileComposite(histFile);
                if (editor == null) {
                    continue;
                }
                int histFileIndex = findEditorIndex(editor);
                if (histFileIndex >= 0) {
                    return histFileIndex;
                }
            }
        }
        else if (uiSettings.getActiveRigtEditorOnClose() && fileIndex + 1 < getTabCount()) {
            return fileIndex + 1;
        }

        if (fileIndex > 0) {
            return fileIndex - 1;
        }
        return -1;
    }

    @Override
    @RequiredUIAccess
    public @Nullable FileEditorWindow split(int orientation, boolean forceSplit, @Nullable VirtualFile virtualFile, boolean focusNew) {
        checkConsistency();
        FileEditorManagerImpl fileEditorManager = getManager();
        if (getTabCount() < 1) {
            return null;
        }

        UIAccess uiAccess = UIAccess.current();
        if (!forceSplit && inSplitter()) {
            FileEditorWindowBase target = findSiblings()[0];
            if (virtualFile != null) {
                FileEditor[] editors = fileEditorManager.openFileImpl3(uiAccess, target, virtualFile, focusNew, null, true).first;
                syncCaretIfPossible(editors);
            }
            return target;
        }

        FileEditorWithProviderComposite selectedEditor = getSelectedEditor();
        if (selectedEditor == null) {
            return null;
        }

        FileEditorWindowBase res = getOwner().splitWindow(this, orientation == JSplitPane.VERTICAL_SPLIT);

        VirtualFile file = selectedEditor.getFile();
        VirtualFile nextFile = virtualFile;
        if (nextFile == null) {
            nextFile = fileEditorManager.getProject().getApplication().getExtensionPoint(FileEditorAssociateFinder.class)
                .computeSafeIfAny(finder -> finder.getAssociatedFileToOpen(fileEditorManager.getProject(), file));
        }
        if (nextFile == null) {
            nextFile = file;
        }

        FileEditor[] editors = fileEditorManager.openFileImpl3(uiAccess, res, nextFile, focusNew, null, true).first;
        syncCaretIfPossible(editors);
        if (res.isFileOpen(nextFile)) {
            res.setFilePinned(nextFile, isFilePinned(file));
        }
        if (!focusNew) {
            res.setSelectedEditor(selectedEditor, true);
            focusEditor(selectedEditor);
        }
        return res;
    }

    private void syncCaretIfPossible(FileEditor @Nullable [] toSync) {
        if (toSync == null) {
            return;
        }

        FileEditorWithProviderComposite from = getSelectedEditor();
        if (from == null) {
            return;
        }

        FileEditor caretSource = from.getSelectedEditor();
        if (!(caretSource instanceof TextEditor sourceTextEditor)) {
            return;
        }

        Editor editorFrom = sourceTextEditor.getEditor();
        int offset = editorFrom.getCaretModel().getOffset();
        if (offset <= 0) {
            return;
        }

        int scrollOffset = editorFrom.getScrollingModel().getVerticalScrollOffset();

        for (FileEditor fileEditor : toSync) {
            if (!(fileEditor instanceof TextEditor textEditor)) {
                continue;
            }
            Editor editor = textEditor.getEditor();
            if (editorFrom.getDocument() == editor.getDocument()) {
                editor.getCaretModel().moveToOffset(offset);
                ScrollingModel scrollingModel = editor.getScrollingModel();
                scrollingModel.scrollVertically(scrollOffset);

                UIAccess.current().give(() -> {
                    if (!editor.isDisposed()) {
                        scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
                    }
                });
            }
        }
    }

    @Override
    public FileEditorWindowBase[] findSiblings() {
        checkConsistency();
        EditorSplitBranch branch = getParentBranch();
        if (branch == null) {
            return new FileEditorWindowBase[0];
        }

        List<FileEditorWindowBase> windows = new ArrayList<>();
        FileEditorsSplittersBase.collectWindows(branch, windows);
        windows.remove(this);
        return windows.toArray(new FileEditorWindowBase[windows.size()]);
    }

    @Override
    @RequiredUIAccess
    public void changeOrientation() {
        checkConsistency();
        EditorSplitBranch branch = getParentBranch();
        if (branch != null) {
            branch.setVertical(!branch.isVertical());
            getOwner().updateSplitLayout();
        }
    }

    @Override
    @RequiredUIAccess
    public void unsplit(boolean setCurrent) {
        checkConsistency();
        EditorSplitBranch branch = getParentBranch();
        if (branch == null) {
            return;
        }

        FileEditorWithProviderComposite editorToSelect = getSelectedEditor();
        FileEditorWindowBase[] siblings = findSiblings();

        for (FileEditorWindowBase eachSibling : siblings) {
            FileEditorWithProviderComposite selected = eachSibling.getSelectedEditor();
            if (editorToSelect == null) {
                editorToSelect = selected;
            }
        }

        for (FileEditorWindowBase sibling : siblings) {
            FileEditorWithProviderComposite[] siblingEditors = sibling.getEditors();
            for (FileEditorWithProviderComposite siblingEditor : siblingEditors) {
                if (editorToSelect == null) {
                    editorToSelect = siblingEditor;
                }
                sibling.detachEditor(siblingEditor);
                processSiblingEditor(siblingEditor);
            }
            LOG.assertTrue(sibling != this);
            sibling.dispose();
        }

        getOwner().replaceNode(branch, this);

        if (editorToSelect != null) {
            setSelectedEditor(editorToSelect, true);
        }
        if (setCurrent) {
            getOwner().setCurrentWindow(this, false);
        }
    }

    @RequiredUIAccess
    private void processSiblingEditor(FileEditorWithProviderComposite siblingEditor) {
        if (getTabCount() < UISettings.getInstance().getEditorTabLimit() && findFileComposite(siblingEditor.getFile()) == null) {
            setEditor(siblingEditor, true);
        }
        else {
            getManager().disposeComposite(siblingEditor);
        }
    }

    @Override
    @RequiredUIAccess
    public void unsplitAll() {
        checkConsistency();
        while (inSplitter()) {
            unsplit(true);
        }
    }

    @Override
    public boolean inSplitter() {
        checkConsistency();
        return getParentBranch() != null;
    }

    @Override
    public boolean isFilePinned(VirtualFile file) {
        FileEditorComposite editorComposite = findFileComposite(file);
        return editorComposite != null && editorComposite.isPinned();
    }

    @Override
    public void setFilePinned(VirtualFile file, boolean pinned) {
        FileEditorWithProviderComposite editorComposite = findFileComposite(file);
        if (editorComposite == null) {
            throw new IllegalArgumentException("file is not open: " + file.getPath());
        }
        boolean wasPinned = editorComposite.isPinned();
        editorComposite.setPinned(pinned);
        if (wasPinned != pinned && UIAccess.isUIThread()) {
            afterPinnedChanged(editorComposite);
        }
    }

    protected void trimToSize(int limit, @Nullable VirtualFile fileToIgnore, boolean transferFocus) {
        getManager().getReady(this).doWhenDone(() -> {
            boolean closeNonModifiedFilesFirst = UISettings.getInstance().getCloseNonModifiedFilesFirst();
            FileEditorWithProviderComposite selectedComposite = getSelectedEditor();
            try {
                doTrimSize(limit, fileToIgnore, closeNonModifiedFilesFirst, transferFocus);
            }
            finally {
                setSelectedEditor(selectedComposite, false);
            }
        });
    }

    private void doTrimSize(int limit, @Nullable VirtualFile fileToIgnore, boolean closeNonModifiedFilesFirst, boolean transferFocus) {
        LinkedHashSet<VirtualFile> closingOrder = getTabClosingOrder(closeNonModifiedFilesFirst);
        VirtualFile selectedFile = getSelectedFile();
        if (selectedFile != null && shouldCloseSelected()) {
            defaultCloseFile(selectedFile, transferFocus);
            closingOrder.remove(selectedFile);
        }

        for (VirtualFile file : closingOrder) {
            if (getTabCount() <= limit || getTabCount() == 0 || areAllTabsPinned(fileToIgnore)) {
                return;
            }
            if (fileCanBeClosed(file, fileToIgnore)) {
                defaultCloseFile(file, transferFocus);
            }
        }
    }

    private LinkedHashSet<VirtualFile> getTabClosingOrder(boolean closeNonModifiedFilesFirst) {
        VirtualFile[] allFiles = getFiles();
        Set<VirtualFile> histFiles = EditorHistoryManagerImpl.getInstance(getManager().getProject()).getFileSet();

        LinkedHashSet<VirtualFile> closingOrder = new LinkedHashSet<>();

        for (VirtualFile file : allFiles) {
            if (!histFiles.contains(file)) {
                closingOrder.add(file);
            }
        }

        if (closeNonModifiedFilesFirst) {
            for (VirtualFile file : histFiles) {
                FileEditorWithProviderComposite composite = findFileComposite(file);
                if (composite != null && !getManager().isChanged(composite)) {
                    closingOrder.add(file);
                }
            }

            for (int i = 0; i < getTabCount(); i++) {
                VirtualFile file = getFileAt(i);
                if (!getManager().isChanged(getEditorAt(i))) {
                    closingOrder.add(file);
                }
            }
        }

        closingOrder.addAll(histFiles);

        for (int i = 0; i < getTabCount(); i++) {
            closingOrder.add(getFileAt(i));
        }

        VirtualFile selectedFile = getSelectedFile();
        closingOrder.remove(selectedFile);
        closingOrder.add(selectedFile);
        return closingOrder;
    }

    private boolean shouldCloseSelected() {
        if (!UISettings.getInstance().getReuseNotModifiedTabs() || !getManager().getProject().isInitialized()) {
            return false;
        }

        VirtualFile file = getSelectedFile();
        if (file == null || !isFileOpen(file) || isFilePinned(file)) {
            return false;
        }
        FileEditorWithProviderComposite composite = findFileComposite(file);
        if (composite == null) {
            return false;
        }
        return isEditorFocused(composite) && !getManager().isChanged(composite);
    }

    private boolean areAllTabsPinned(@Nullable VirtualFile fileToIgnore) {
        for (int i = getTabCount() - 1; i >= 0; i--) {
            if (fileCanBeClosed(getFileAt(i), fileToIgnore)) {
                return false;
            }
        }
        return true;
    }

    private void defaultCloseFile(VirtualFile file, boolean transferFocus) {
        closeFile(file, true, transferFocus);
    }

    private boolean fileCanBeClosed(VirtualFile file, @Nullable VirtualFile fileToIgnore) {
        return isFileOpen(file) && !file.equals(fileToIgnore) && !isFilePinned(file);
    }

    @Override
    public String toString() {
        return "EditorWindow: files=" + Arrays.asList(getFiles());
    }
}
