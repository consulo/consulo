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

import consulo.application.*;
import consulo.application.concurrent.ApplicationConcurrency;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.fileEditor.*;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.util.MergingProcessingQueueInUI;
import consulo.project.ui.wm.FrameTitleBuilder;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.image.Image;
import consulo.ui.layout.WrappedLayout;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.Pair;
import consulo.util.xml.serializer.InvalidDataException;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jspecify.annotations.Nullable;
import org.jdom.Element;

import javax.swing.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * This is base class extracted from IDEA AWT/Swing code, unified, and removed awt/swing parts
 *
 * @author VISTALL
 * @since 2018-05-11
 */
public abstract class FileEditorsSplittersBase<W extends FileEditorWindowBase> implements FileEditorsSplitters, Disposable {
    private record EditorTablInfo(String tabTitle, String tabTooltip) {
    }

    private static final Logger LOG = Logger.getInstance(FileEditorsSplittersBase.class);

    
    protected final Project myProject;
    protected final FileEditorManagerImpl myManager;
    private int myInsideChange;

    protected W myCurrentWindow;
    protected final Set<W> myWindows = new CopyOnWriteArraySet<>();
    protected Element mySplittersElement;  // temporarily used during initialization

    private final MergingProcessingQueueInUI<VirtualFile, Pair<W, Image>> myIconUpdater;
    private final MergingProcessingQueueInUI<VirtualFile, EditorTablInfo> myFileNameUpdater;

    private @Nullable EditorSplitNode myRootNode;
    private final EditorSplitLayout mySplitLayout;
    private final UIBuilder myUIBuilder = new UIBuilder();

    protected FileEditorsSplittersBase(ApplicationConcurrency applicationConcurrency,
                                       Project project,
                                       FileEditorManagerImpl manager) {
        myProject = project;
        myManager = manager;

        mySplitLayout = new EditorSplitLayout();

        myIconUpdater = new MergingProcessingQueueInUI<>(applicationConcurrency, project, 200) {
            @Override
            protected void calculateValue(Project project,
                                          VirtualFile key,
                                          Consumer<Pair<W, Image>> consumer) {
                collectFileIcons(key, consumer);
            }

            @Override
            protected void updateValueInsideUI(Project project,
                                               VirtualFile key,
                                               Pair<W, Image> value) {
                value.getFirst().updateFileIcon(key, value.getSecond());
            }
        };

        myFileNameUpdater = new MergingProcessingQueueInUI<>(applicationConcurrency, project, 200) {
            @Override
            protected void calculateValue(Project project, VirtualFile key, Consumer<EditorTablInfo> consumer) {
                String title = EditorTabPresentationUtil.getEditorTabTitle(myProject, key);
                String tooltip = UISettings.getInstance().getShowTabsTooltips() ? getManager().getFileTooltipText(key) : null;
                consumer.accept(new EditorTablInfo(title, tooltip));
            }

            @Override
            protected void updateValueInsideUI(Project project, VirtualFile key, EditorTablInfo value) {
                W[] windows = getWindows();
                for (int i = 0; i != windows.length; ++i) {
                    for (VirtualFile file : windows[i].getFiles()) {
                        if (file.equals(key)) {
                            windows[i].updateFileName(file, value.tabTitle(), value.tabTooltip());
                        }
                    }
                }
            }
        };

        project.getApplication().getMessageBus().connect(this).subscribe(UISettingsListener.class, source -> {
            if (!project.isOpen()) {
                return;
            }

            for (VirtualFile file : getOpenFiles()) {
                updateFileBackgroundColor(file);
                updateFileIconAsync(file);
                updateFileColor(file);
            }
        });
    }

    
    protected abstract W[] createArray(int size);

    @RequiredUIAccess
    protected abstract W createEditorWindow();

    @RequiredUIAccess
    public void createCurrentWindow() {
        LOG.assertTrue(myCurrentWindow == null);
        W window = createEditorWindow();
        setCurrentWindow(window);
        setRootNode(window);
    }

    protected WrappedLayout getSplitComponent() {
        return mySplitLayout.getComponent();
    }

    protected void afterSplitLayoutUpdated() {
    }

    public @Nullable EditorSplitNode getRootNode() {
        return myRootNode;
    }

    @RequiredUIAccess
    protected void setRootNode(@Nullable EditorSplitNode rootNode) {
        myRootNode = rootNode;
        if (rootNode != null) {
            rootNode.setParentBranch(null);
        }
        updateSplitLayout();
    }

    @RequiredUIAccess
    public void updateSplitLayout() {
        mySplitLayout.update(myRootNode, node -> ((FileEditorWindowBase) node).getUIComponent());
        afterSplitLayoutUpdated();
    }

    @RequiredUIAccess
    FileEditorWindowBase splitWindow(FileEditorWindowBase window, boolean vertical) {
        W newWindow = createEditorWindow();
        EditorSplitBranch parent = window.getParentBranch();
        EditorSplitBranch branch = new EditorSplitBranch(vertical, 0.5f, window, newWindow);
        putNode(parent, window, branch);
        updateSplitLayout();
        return newWindow;
    }

    @RequiredUIAccess
    void removeWindowNode(FileEditorWindowBase window) {
        EditorSplitBranch branch = window.getParentBranch();
        if (branch == null) {
            return;
        }

        putNode(branch.getParentBranch(), branch, branch.getOther(window));
        window.setParentBranch(null);
        updateSplitLayout();
    }

    @RequiredUIAccess
    void replaceNode(EditorSplitNode oldNode, EditorSplitNode newNode) {
        putNode(oldNode.getParentBranch(), oldNode, newNode);
        updateSplitLayout();
    }

    private void putNode(@Nullable EditorSplitBranch parent, EditorSplitNode oldNode, EditorSplitNode newNode) {
        if (parent == null) {
            myRootNode = newNode;
            newNode.setParentBranch(null);
        }
        else {
            parent.replace(oldNode, newNode);
        }
    }

    static void collectWindows(EditorSplitNode node, List<FileEditorWindowBase> windows) {
        if (node instanceof EditorSplitBranch branch) {
            collectWindows(branch.getFirst(), windows);
            collectWindows(branch.getSecond(), windows);
        }
        else {
            windows.add((FileEditorWindowBase) node);
        }
    }

    @Override
    public int getSplitCount() {
        EditorSplitNode rootNode = myRootNode;
        if (rootNode == null) {
            return 0;
        }

        List<FileEditorWindowBase> windows = new ArrayList<>();
        collectWindows(rootNode, windows);
        return windows.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public W[] getOrderedWindows() {
        EditorSplitNode rootNode = myRootNode;
        if (rootNode == null) {
            return createArray(0);
        }

        List<FileEditorWindowBase> windows = new ArrayList<>();
        collectWindows(rootNode, windows);

        W[] result = createArray(windows.size());
        for (int i = 0; i < result.length; i++) {
            result[i] = (W) windows.get(i);
        }
        return result;
    }

    @RequiredUIAccess
    @Override
    public @Nullable FileEditorWindow openInRightSplit(VirtualFile file, boolean requestFocus) {
        W window = getCurrentWindow();
        if (window == null) {
            return null;
        }

        EditorSplitBranch branch = window.getParentBranch();
        if (branch != null && branch.getSecond() != window && branch.getSecond() instanceof FileEditorWindowBase rightSplitWindow) {
            myManager.openFileWithProviders(file, requestFocus, rightSplitWindow);
            return rightSplitWindow;
        }

        return window.split(SwingConstants.VERTICAL, true, file, requestFocus);
    }

    @RequiredUIAccess
    @Override
    public void clear() {
        for (W window : myWindows) {
            window.dispose();
        }
        myWindows.clear();
        setRootNode(null);
        setCurrentWindow(null);
    }

    @Override
    public void writeExternal(Element element) {
        EditorSplitNode rootNode = myRootNode;
        if (rootNode == null) {
            return;
        }

        try {
            element.addContent(writeNode(rootNode));
        }
        catch (ProcessCanceledException e) {
            throw e;
        }
        catch (Throwable e) {
            LOG.error(e);
        }
    }

    private Element writeNode(EditorSplitNode node) {
        if (node instanceof EditorSplitBranch branch) {
            Element result = new Element(AsyncConfigTreeReader.SPLITTER);
            result.setAttribute(AsyncConfigTreeReader.SPLIT_ORIENTATION, branch.isVertical() ? "vertical" : "horizontal");
            result.setAttribute(AsyncConfigTreeReader.SPLIT_PROPORTION, Float.toString(branch.getProportion()));

            Element first = new Element(AsyncConfigTreeReader.SPLIT_FIRST);
            first.addContent(writeNode(branch.getFirst()));
            Element second = new Element(AsyncConfigTreeReader.SPLIT_SECOND);
            second.addContent(writeNode(branch.getSecond()));

            result.addContent(first);
            result.addContent(second);
            return result;
        }

        FileEditorWindowBase window = (FileEditorWindowBase) node;
        Element result = new Element(AsyncConfigTreeReader.LEAF);
        String sideTabsSizeLimit = window.getSideTabsSizeLimit();
        if (sideTabsSizeLimit != null) {
            result.setAttribute(AsyncConfigTreeReader.SIDE_TABS_SIZE_LIMIT, sideTabsSizeLimit);
        }

        FileEditorWithProviderComposite[] composites = window.getEditors();
        FileEditorWithProviderComposite selectedEditor = window.getSelectedEditor();
        for (int i = 0; i < composites.length; i++) {
            VirtualFile file = window.getFileAt(i);
            result.addContent(writeComposite(file, composites[i], window.isFilePinned(file), selectedEditor));
        }
        return result;
    }

    private Element writeComposite(VirtualFile file,
                                   FileEditorWithProviderComposite composite,
                                   boolean pinned,
                                   @Nullable FileEditorWithProviderComposite selectedEditor) {
        Element fileElement = new Element(AsyncConfigTreeReader.FILE);
        fileElement.setAttribute(AsyncConfigTreeReader.LEAF_FILE_NAME, file.getName());
        FileEditorHistoryUtil.currentStateAsHistoryEntry(composite).writeExternal(fileElement, myProject);
        fileElement.setAttribute(AsyncConfigTreeReader.PINNED, Boolean.toString(pinned));
        fileElement.setAttribute(AsyncConfigTreeReader.CURRENT_IN_TAB, Boolean.toString(composite.equals(selectedEditor)));
        return fileElement;
    }

    @Override
    public CompletableFuture<?> openFilesAsync(UIAccess uiAccess) {
        Element splittersElement = mySplittersElement;
        if (splittersElement == null) {
            return CompletableFuture.completedFuture(null);
        }

        return myUIBuilder.process(splittersElement, myRootNode, uiAccess).whenCompleteAsync((node, throwable) -> {
            mySplittersElement = null;

            if (throwable != null) {
                LOG.error(throwable);
            }

            if (node != null) {
                setRootNode(node);
                adoptDetachedWindows();
            }

            for (W window : getWindows()) {
                if (window.getTabCount() == 0 && !window.isDisposed() && window.inSplitter()) {
                    window.removeFromSplitter();
                }
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private void adoptDetachedWindows() {
        W[] ordered = getOrderedWindows();
        if (ordered.length == 0) {
            return;
        }

        Set<W> attached = new HashSet<>(Arrays.asList(ordered));
        for (W window : getWindows()) {
            if (attached.contains(window)) {
                continue;
            }

            for (FileEditorWithProviderComposite composite : window.getEditors()) {
                window.detachEditor(composite);
                if (ordered[0].findFileComposite(composite.getFile()) == null) {
                    ordered[0].setEditor(composite, false);
                }
                else {
                    myManager.disposeComposite(composite);
                }
            }
            window.dispose();
        }
    }

    private @Nullable W findContextWindow(@Nullable EditorSplitNode context) {
        if (context == null) {
            return null;
        }

        List<FileEditorWindowBase> windows = new ArrayList<>();
        collectWindows(context, windows);
        return windows.isEmpty() ? null : (W) windows.get(0);
    }

    private final class UIBuilder extends AsyncConfigTreeReader<EditorSplitNode> {
        @Override
        protected CompletableFuture<EditorSplitNode> processFiles(List<Element> fileElements,
                                                                  @Nullable EditorSplitNode context,
                                                                  Element parent,
                                                                  UIAccess uiAccess) {
            return uiAccess.<W>giveAsync(() -> {
                    W window = findContextWindow(context);
                    if (window == null) {
                        window = createEditorWindow();
                    }
                    window.setSideTabsSizeLimit(parent.getAttributeValue(SIDE_TABS_SIZE_LIMIT));
                    return window;
                })
                .thenCompose(window -> reopenFiles(fileElements, window, uiAccess));
        }

        @Override
        protected CompletableFuture<EditorSplitNode> processSplitter(Element element,
                                                                     @Nullable Element firstChild,
                                                                     @Nullable Element secondChild,
                                                                     @Nullable EditorSplitNode context,
                                                                     UIAccess uiAccess) {
            boolean vertical = "vertical".equals(element.getAttributeValue(SPLIT_ORIENTATION));
            float proportion = parseProportion(element.getAttributeValue(SPLIT_PROPORTION));

            EditorSplitNode firstContext = context instanceof EditorSplitBranch branch ? branch.getFirst() : context;
            EditorSplitNode secondContext = context instanceof EditorSplitBranch branch ? branch.getSecond() : null;

            return process(firstChild, firstContext, uiAccess)
                .thenCompose(first -> process(secondChild, secondContext, uiAccess)
                    .thenApplyAsync(second -> combine(context, vertical, proportion, first, second), uiAccess));
        }
    }

    private static float parseProportion(@Nullable String value) {
        if (value == null) {
            return 0.5f;
        }

        try {
            return Float.parseFloat(value);
        }
        catch (NumberFormatException e) {
            return 0.5f;
        }
    }

    private static @Nullable EditorSplitNode combine(@Nullable EditorSplitNode context,
                                                     boolean vertical,
                                                     float proportion,
                                                     @Nullable EditorSplitNode first,
                                                     @Nullable EditorSplitNode second) {
        if (first == null) {
            return second;
        }
        if (second == null || second == first) {
            return first;
        }

        if (context instanceof EditorSplitBranch branch && branch.getFirst() == first && branch.getSecond() == second) {
            branch.setVertical(vertical);
            branch.setProportion(proportion);
            return branch;
        }
        return new EditorSplitBranch(vertical, proportion, first, second);
    }

    private CompletableFuture<EditorSplitNode> reopenFiles(List<Element> fileElements, W window, UIAccess uiAccess) {
        CompletableFuture<?> reopened = CompletableFuture.completedFuture(null);
        VirtualFile focusedFile = null;

        for (int i = 0; i < fileElements.size(); i++) {
            Element file = fileElements.get(i);
            Element historyElement = file.getChild(HistoryEntry.TAG);

            try {
                HistoryEntry entry = HistoryEntry.createLight(myProject, historyElement);
                VirtualFile virtualFile = entry.getFile();
                if (virtualFile == null) {
                    throw new InvalidDataException("No file exists: " + entry.getFilePointer().getUrl());
                }

                FileEditorOpenOptions openOptions = new FileEditorOpenOptions()
                    .withPin(Boolean.valueOf(file.getAttributeValue(AsyncConfigTreeReader.PINNED)))
                    .withIndex(i)
                    .withReopeningEditorsOnStartup();

                reopened = reopened.thenCompose(ignored -> reopenFile(uiAccess, window, virtualFile, entry, openOptions));

                if (Boolean.valueOf(file.getAttributeValue(AsyncConfigTreeReader.CURRENT_IN_TAB))) {
                    focusedFile = virtualFile;
                }
            }
            catch (InvalidDataException e) {
                LOG.warn(e);
            }
            catch (Throwable e) {
                LOG.error("Cannot reopen editor for " + file.getAttributeValue(AsyncConfigTreeReader.LEAF_FILE_NAME), e);
            }
        }

        VirtualFile finalFocusedFile = focusedFile;
        return reopened.<EditorSplitNode>thenApplyAsync(ignored -> {
            if (finalFocusedFile != null) {
                myManager.addSelectionRecord(finalFocusedFile, window);

                FileEditorWithProviderComposite editor = window.findFileComposite(finalFocusedFile);
                if (editor != null) {
                    window.setEditor(editor, true, true);
                }
            }
            else {
                ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(myProject);
                if (toolWindowManager.getActiveToolWindowId() == null) {
                    ToolWindow toolWindow = toolWindowManager.getToolWindow(ToolWindowId.PROJECT_VIEW);
                    if (toolWindow != null) {
                        toolWindow.activate(null);
                    }
                }
            }
            return window;
        }, uiAccess);
    }

    private CompletableFuture<?> reopenFile(UIAccess uiAccess,
                                            W window,
                                            VirtualFile file,
                                            HistoryEntry entry,
                                            FileEditorOpenOptions openOptions) {
        file.putUserData(OPENED_IN_BULK, Boolean.TRUE);
        return myManager.reopenFileAsync(uiAccess, window, file, entry, openOptions).handle((result, throwable) -> {
            file.putUserData(OPENED_IN_BULK, null);
            if (throwable != null) {
                LOG.error("Cannot reopen editor for " + file.getPath(), throwable);
            }
            return null;
        });
    }

    protected void stopListeningFocus() {
    }

    public void afterFileClosed(VirtualFile file) {
    }

    public void afterFileOpen(VirtualFile file) {
    }

    @RequiredUIAccess
    @Override
    public void closeFile(VirtualFile file, boolean moveFocus) {
        List<W> windows = findWindows(file);
        if (!windows.isEmpty()) {
            VirtualFile nextFile = findNextFile(file);
            for (W window : windows) {
                LOG.assertTrue(window.getSelectedEditor() != null);
                window.closeFile(file, false, moveFocus);
                if (window.getTabCount() == 0 && nextFile != null && myProject.isOpen()) {
                    FileEditorWithProviderComposite newComposite = myManager.newEditorComposite(nextFile);
                    window.setEditor(newComposite, moveFocus); // newComposite can be null
                }
            }
            // cleanup windows with no tabs
            for (W window : windows) {
                if (window.isDisposed()) {
                    // call to window.unsplit() which might make its sibling disposed
                    continue;
                }
                if (window.getTabCount() == 0) {
                    window.unsplit(false);
                }
            }
        }
    }

    private @Nullable VirtualFile findNextFile(VirtualFile file) {
        W[] windows = getWindows(); // TODO: use current file as base
        for (int i = 0; i != windows.length; ++i) {
            VirtualFile[] files = windows[i].getFiles();
            for (VirtualFile fileAt : files) {
                if (!Objects.equals(fileAt, file)) {
                    return fileAt;
                }
            }
        }
        return null;
    }

    @Override
    public void setTabsPlacement(int tabPlacement) {
        W[] windows = getWindows();
        for (int i = 0; i != windows.length; ++i) {
            windows[i].setTabsPlacement(tabPlacement);
        }
    }

    @Override
    public void setTabLayoutPolicy(int scrollTabLayout) {
        W[] windows = getWindows();
        for (int i = 0; i != windows.length; ++i) {
            windows[i].setTabLayoutPolicy(scrollTabLayout);
        }
    }

    @Override
    public void trimToSize(int editor_tab_limit) {
        for (W window : myWindows) {
            window.trimToSize(editor_tab_limit, null, true);
        }
    }

    public FileEditorManagerImpl getManager() {
        return myManager;
    }

    @Override
    public void updateFileIconAsync(VirtualFile file) {
        myIconUpdater.queueAdd(file);
    }

    private void collectFileIcons(VirtualFile file, Consumer<Pair<W, Image>> windowIcons) {
        Collection<W> windows = findWindows(file);
        for (W window : windows) {
            Image fileIcon = myProject.getApplication().runReadAction((Supplier<Image>) () -> window.getFileIcon(file));

            windowIcons.accept(Pair.create(window, fileIcon));
        }
    }

    protected boolean showEmptyText() {
        return myCurrentWindow == null || myCurrentWindow.getFiles().length == 0;
    }

    @Override
    public void updateFileColor(VirtualFile file) {
        Collection<W> windows = findWindows(file);
        for (W window : windows) {
            int index = window.findEditorIndex(window.findFileComposite(file));
            LOG.assertTrue(index != -1);
            window.setForegroundAt(index, TargetAWT.to(myManager.getFileColor(file)));
            window.setWaveColor(index, myManager.isProblem(file) ? JBColor.red : null);
        }
    }

    @Override
    public void updateFileBackgroundColor(VirtualFile file) {
        W[] windows = getWindows();
        for (int i = 0; i != windows.length; ++i) {
            windows[i].updateFileBackgroundColor(file);
        }
    }

    @Override
    public @Nullable VirtualFile getCurrentFile() {
        if (myCurrentWindow != null) {
            return myCurrentWindow.getSelectedFile();
        }
        return null;
    }

    
    @Override
    public FileEditorWindow getOrCreateCurrentWindow(VirtualFile file) {
        List<W> windows = findWindows(file);
        if (getCurrentWindow() == null) {
            Iterator<W> iterator = myWindows.iterator();
            if (!windows.isEmpty()) {
                setCurrentWindow(windows.get(0), false);
            }
            else if (iterator.hasNext()) {
                setCurrentWindow(iterator.next(), false);
            }
            else {
                createCurrentWindow();
            }
        }
        else if (!windows.isEmpty()) {
            if (!windows.contains(getCurrentWindow())) {
                setCurrentWindow(windows.get(0), false);
            }
        }
        return getCurrentWindow();
    }

    /**
     * sets the window passed as a current ('focused') window among all splitters. All file openings will be done inside this
     * current window
     *
     * @param window       a window to be set as current
     * @param requestFocus whether to request focus to the editor currently selected in this window
     */
    @Override
    public void setCurrentWindow(@Nullable FileEditorWindow window, boolean requestFocus) {
        FileEditorWithProviderComposite newEditor = window == null ? null : window.getSelectedEditor();

        Runnable fireRunnable = () -> myManager.fireSelectionChanged(newEditor);

        setCurrentWindow((W) window);

        myManager.updateFileName(window == null ? null : window.getSelectedFile());

        if (window != null) {
            FileEditorWithProviderComposite selectedEditor = window.getSelectedEditor();
            if (selectedEditor != null) {
                fireRunnable.run();
            }

            if (requestFocus) {
                window.requestFocus(true);
            }
        }
        else {
            fireRunnable.run();
        }
    }

    protected void setCurrentWindow(@Nullable W currentWindow) {
        if (currentWindow != null && !myWindows.contains(currentWindow)) {
            throw new IllegalArgumentException(currentWindow + " is not a member of this container");
        }
        myCurrentWindow = currentWindow;
    }

    @Override
    public void readExternal(Element element) {
        mySplittersElement = element;
    }

    @Override
    public boolean isInsideChange() {
        return myInsideChange > 0;
    }

    @Override
    public AccessToken increaseChange() {
        myInsideChange++;
        return new AccessToken() {
            @Override
            public void finish() {
                myInsideChange--;
            }
        };
    }

    @Override
    public @Nullable W getCurrentWindow() {
        return myCurrentWindow;
    }

    @Override
    public final void updateFileNameAsync(@Nullable VirtualFile updatedFile) {
        if (updatedFile == null) {
            W[] windows = getWindows();
            for (int i = 0; i != windows.length; ++i) {
                for (VirtualFile file : windows[i].getFiles()) {
                    myFileNameUpdater.queueAdd(file);
                }
            }
        }
        else {
            myFileNameUpdater.queueAdd(updatedFile);
        }

        IdeFrame frame = getFrame(myProject);
        if (frame != null) {
            VirtualFile file = getCurrentFile();

            ReadAction.nonBlocking(() -> {
                File ioFile = file == null ? null : new File(file.getPresentableUrl());
                String fileTitle = null;
                if (file != null) {
                    fileTitle = DumbService.isDumb(myProject) ? file.getName() : FrameTitleBuilder.getInstance().getFileTitle(myProject, file);
                }

                return Pair.create(fileTitle, ioFile);
            }).finishOnUiThread(Application::getDefaultModalityState, pair -> {
                if (pair == null) {
                    return;
                }

                IdeFrame otherFrame = getFrame(myProject);
                if (otherFrame != null) {
                    frame.setFileTitle(pair.getFirst(), pair.getSecond());
                }
            })
            .expireWith(myProject)
            .submitDefault();
        }
    }

    @Override
    
    public VirtualFile[] getOpenFiles() {
        Set<VirtualFile> files = new LinkedHashSet<>();
        for (W myWindow : myWindows) {
            FileEditorWithProviderComposite[] editors = myWindow.getEditors();
            for (FileEditorWithProviderComposite editor : editors) {
                VirtualFile file = editor.getFile();
                // background thread may call this method when invalid file is being removed
                // do not return it here as it will quietly drop out soon
                if (file.isValid()) {
                    files.add(file);
                }
            }
        }
        return VirtualFileUtil.toVirtualFileArray(files);
    }

    @Override
    
    public VirtualFile[] getSelectedFiles() {
        Set<VirtualFile> files = new LinkedHashSet<>();
        for (W window : myWindows) {
            VirtualFile file = window.getSelectedFile();
            if (file != null) {
                files.add(file);
            }
        }
        VirtualFile[] virtualFiles = VirtualFileUtil.toVirtualFileArray(files);
        VirtualFile currentFile = getCurrentFile();
        if (currentFile != null) {
            for (int i = 0; i != virtualFiles.length; ++i) {
                if (Objects.equals(virtualFiles[i], currentFile)) {
                    virtualFiles[i] = virtualFiles[0];
                    virtualFiles[0] = currentFile;
                    break;
                }
            }
        }
        return virtualFiles;
    }

    @Override
    
    @SuppressWarnings("unchecked")
    public FileEditor[] getSelectedEditors() {
        Set<W> windows = new HashSet<>(myWindows);
        FileEditorWindow currentWindow = getCurrentWindow();
        if (currentWindow != null) {
            windows.add((W) currentWindow);
        }
        List<FileEditor> editors = new ArrayList<>();
        for (W window : windows) {
            FileEditorWithProviderComposite composite = window.getSelectedEditor();
            if (composite != null) {
                editors.add(composite.getSelectedEditor());
            }
        }
        return editors.toArray(new FileEditor[editors.size()]);
    }

    public void addWindow(W window) {
        myWindows.add(window);
    }

    public void removeWindow(FileEditorWindowBase window) {
        myWindows.remove(window);
        if (myCurrentWindow == window) {
            myCurrentWindow = null;
        }
    }

    public boolean containsWindow(FileEditorWindowBase window) {
        return myWindows.contains(window);
    }

    //---------------------------------------------------------

    @Override
    public FileEditorWithProviderComposite[] getEditorsComposites() {
        List<FileEditorWithProviderComposite> res = new ArrayList<>();

        for (FileEditorWindow myWindow : myWindows) {
            FileEditorWithProviderComposite[] editors = myWindow.getEditors();
            ContainerUtil.addAll(res, editors);
        }
        return res.toArray(new FileEditorWithProviderComposite[res.size()]);
    }

    //---------------------------------------------------------

    @Override
    
    public List<FileEditorWithProviderComposite> findEditorComposites(VirtualFile file) {
        List<FileEditorWithProviderComposite> res = new ArrayList<>();
        for (FileEditorWindow window : myWindows) {
            FileEditorWithProviderComposite fileComposite = window.findFileComposite(file);
            if (fileComposite != null) {
                res.add(fileComposite);
            }
        }
        return res;
    }

    
    protected List<W> findWindows(VirtualFile file) {
        List<W> res = new ArrayList<>();
        for (W window : myWindows) {
            if (window.findFileComposite(file) != null) {
                res.add(window);
            }
        }
        return res;
    }

    @Override
    
    public W[] getWindows() {
        return myWindows.toArray(createArray(myWindows.size()));
    }

    @Override
    public void dispose() {
        myIconUpdater.dispose();

        stopListeningFocus();
    }

    protected IdeFrame getFrame(Project project) {
        IdeFrame frame = WindowManagerEx.getInstance().getIdeFrame(project);
        LOG.assertTrue(ApplicationManager.getApplication().isUnitTestMode() || frame != null);
        return frame;
    }
}
