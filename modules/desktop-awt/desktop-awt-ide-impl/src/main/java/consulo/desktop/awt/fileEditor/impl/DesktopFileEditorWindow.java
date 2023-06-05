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

import consulo.application.AllIcons;
import consulo.application.ApplicationManager;
import consulo.application.ui.UISettings;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.ScrollingModel;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposer;
import consulo.fileEditor.*;
import consulo.fileEditor.event.FileEditorManagerBeforeListener;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.fileEditor.internal.EditorWindowHolder;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.impl.desktop.awt.migration.AWTComponentProviderUtil;
import consulo.ide.impl.fileEditor.FileEditorWindowBase;
import consulo.ide.impl.idea.openapi.fileEditor.impl.EditorHistoryManagerImpl;
import consulo.ide.impl.idea.openapi.fileEditor.impl.FileEditorManagerImpl;
import consulo.ide.impl.idea.openapi.ui.ThreeComponentsSplitter;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.editor.CommonDataKeys;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.IdeFocusTraversalPolicy;
import consulo.ui.ex.awt.OnePixelSplitter;
import consulo.ui.ex.awt.Splitter;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.image.Image;
import consulo.util.collection.Stack;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static consulo.application.ui.wm.IdeFocusManager.getGlobalInstance;

/**
 * Author: msk
 */
public class DesktopFileEditorWindow extends FileEditorWindowBase implements FileEditorWindow {
  private static final Logger LOG = Logger.getInstance(DesktopFileEditorWindow.class);

  protected JPanel myPanel;
  @Nonnull
  private final DesktopAWTEditorTabbedContainer myTabbedPane;
  private final DesktopFileEditorsSplitters myOwner;

  private boolean myIsDisposed;
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

  protected DesktopFileEditorWindow(final DesktopFileEditorsSplitters owner) {
    myOwner = owner;
    myPanel = new JPanel(new BorderLayout());
    myPanel.setOpaque(false);

    myTabbedPane = new DesktopAWTEditorTabbedContainer(this, getManager().getProject());
    myPanel.add(myTabbedPane.getComponent(), BorderLayout.CENTER);

    // Tab layout policy
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

  void dispose() {
    try {
      disposeTabs();
      myOwner.removeWindow(this);
    }
    finally {
      myIsDisposed = true;
    }
  }

  @Override
  public boolean isDisposed() {
    return myIsDisposed;
  }

  private void disposeTabs() {
    Disposer.dispose(myTabbedPane);
    myPanel.removeAll();
    myPanel.revalidate();
  }

  @Nonnull
  @Override
  public FileEditorTabbedContainer getContainer() {
    return myTabbedPane;
  }

  @Override
  public boolean hasClosedTabs() {
    return !myRemovedTabs.empty();
  }

  @Override
  public void restoreClosedTab() {
    assert hasClosedTabs() : "Nothing to restore";

    final Pair<String, Integer> info = myRemovedTabs.pop();
    final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(info.getFirst());
    final Integer second = info.getSecond();
    if (file != null) {
      getManager().openFileImpl4(UIAccess.get(), this, file, null, true, true, null, second == null ? -1 : second.intValue());
    }
  }

  private void restoreHiddenTabs() {
    while (!myHiddenTabs.isEmpty()) {
      final Pair<String, Integer> info = myHiddenTabs.pop();
      myRemovedTabs.remove(info);
      final VirtualFile file = VirtualFileManager.getInstance().findFileByUrl(info.getFirst());
      final Integer second = info.getSecond();
      if (file != null) {
        getManager().openFileImpl4(UIAccess.get(), this, file, null, true, true, null, second == null ? -1 : second.intValue());
      }
    }
  }

  @Override
  public void closeFile(@Nonnull final VirtualFile file, final boolean disposeIfNeeded, final boolean transferFocus) {
    final FileEditorManagerImpl editorManager = getManager();
    editorManager.runChange(splitters -> {
      final List<FileEditorWithProviderComposite> editors = splitters.findEditorComposites(file);
      if (editors.isEmpty()) return;
      try {
        final DesktopFileEditorWithProviderComposite editor = findFileComposite(file);

        final FileEditorManagerBeforeListener beforePublisher = editorManager.getProject().getMessageBus().syncPublisher(FileEditorManagerBeforeListener.class);

        beforePublisher.beforeFileClosed(editorManager, file);

        if (myTabbedPane != null && editor != null) {
          final int componentIndex = findComponentIndex(editor.getComponent());
          if (componentIndex >= 0) { // editor could close itself on decomposition
            final int indexToSelect = calcIndexToSelect(file, componentIndex);
            Pair<String, Integer> pair = Pair.create(file.getUrl(), componentIndex);
            myRemovedTabs.push(pair);
            if (myTabsHidingInProgress.get()) {
              myHiddenTabs.push(pair);
            }
            myTabbedPane.removeTabAt(componentIndex, indexToSelect, transferFocus);
            editorManager.disposeComposite(editor);
          }
        }
        else {
          if (inSplitter()) {
            Splitter splitter = (Splitter)myPanel.getParent();
            JComponent otherComponent = splitter.getOtherComponent(myPanel);

            if (otherComponent != null) {
              IdeFocusManager.findInstance().requestFocus(otherComponent, true);
            }
          }

          myPanel.removeAll();
        }

        if (disposeIfNeeded && getTabCount() == 0) {
          removeFromSplitter();
          if (UISettings.getInstance().getEditorTabPlacement() == UISettings.TABS_NONE) {
            final DesktopFileEditorsSplitters owner = getOwner();
            final ThreeComponentsSplitter splitter = UIUtil.getParentOfType(ThreeComponentsSplitter.class, owner.getComponent());
            if (splitter != null) {
              splitter.revalidate();
              splitter.repaint();
            }
          }
        }
        else {
          myPanel.revalidate();
          myPanel.repaint();
        }
      }
      finally {
        editorManager.removeSelectionRecord(file, this);

        editorManager.notifyPublisher(() -> {
          final Project project = editorManager.getProject();
          if (!project.isDisposed()) {
            final FileEditorManagerListener afterPublisher = project.getMessageBus().syncPublisher(FileEditorManagerListener.class);
            afterPublisher.fileClosed(editorManager, file);
          }
        });

        ((DesktopFileEditorsSplitters)splitters).afterFileClosed(file);
      }
    }, myOwner);
  }

  private void removeFromSplitter() {
    if (!inSplitter()) return;

    if (myOwner.getCurrentWindow() == this) {
      DesktopFileEditorWindow[] siblings = findSiblings();
      myOwner.setCurrentWindow(siblings[0], false);
    }

    Splitter splitter = (Splitter)myPanel.getParent();
    JComponent otherComponent = splitter.getOtherComponent(myPanel);

    Container parent = splitter.getParent().getParent();
    if (parent instanceof Splitter) {
      Splitter parentSplitter = (Splitter)parent;
      if (parentSplitter.getFirstComponent() == splitter.getParent()) {
        parentSplitter.setFirstComponent(otherComponent);
      }
      else {
        parentSplitter.setSecondComponent(otherComponent);
      }
    }
    else if (AWTComponentProviderUtil.getMark(parent) instanceof FileEditorsSplitters) {
      parent.removeAll();
      parent.add(otherComponent, BorderLayout.CENTER);
      parent.revalidate();
    }
    else {
      throw new IllegalStateException("Unknown container: " + parent);
    }

    dispose();
  }

  private int calcIndexToSelect(VirtualFile fileBeingClosed, final int fileIndex) {
    final int currentlySelectedIndex = myTabbedPane.getSelectedIndex();
    if (currentlySelectedIndex != fileIndex) {
      // if the file being closed is not currently selected, keep the currently selected file open
      return currentlySelectedIndex;
    }
    UISettings uiSettings = UISettings.getInstance();
    if (uiSettings.getActiveMruEditorOnClose()) {
      // try to open last visited file
      final VirtualFile[] histFiles = EditorHistoryManagerImpl.getInstance(getManager().getProject()).getFiles();
      for (int idx = histFiles.length - 1; idx >= 0; idx--) {
        final VirtualFile histFile = histFiles[idx];
        if (histFile.equals(fileBeingClosed)) {
          continue;
        }
        final DesktopFileEditorWithProviderComposite editor = findFileComposite(histFile);
        if (editor == null) {
          continue; // ????
        }
        final int histFileIndex = findComponentIndex(editor.getComponent());
        if (histFileIndex >= 0) {
          // if the file being closed is located before the hist file, then after closing the index of the histFile will be shifted by -1
          return histFileIndex;
        }
      }
    }
    else if (uiSettings.getActiveRigtEditorOnClose() && fileIndex + 1 < myTabbedPane.getTabCount()) {
      return fileIndex + 1;
    }

    // by default select previous neighbour
    if (fileIndex > 0) {
      return fileIndex - 1;
    }
    // do nothing
    return -1;
  }

  @Nonnull
  @Override
  public FileEditorManagerImpl getManager() {
    return myOwner.getManager();
  }

  @Override
  public int getTabCount() {
    return myTabbedPane.getTabCount();
  }

  @Override
  protected void setForegroundAt(final int index, final Color color) {
    myTabbedPane.setForegroundAt(index, color);
  }

  @Override
  protected void setWaveColor(final int index, @Nullable final Color color) {
    myTabbedPane.setWaveColor(index, color);
  }

  @Override
  protected void setIconAt(final int index, final consulo.ui.image.Image icon) {
    myTabbedPane.setIconAt(index, icon);
  }

  @Override
  protected void setTitleAt(final int index, final String text) {
    myTabbedPane.setTitleAt(index, text);
  }

  @Override
  protected void setBackgroundColorAt(final int index, final Color color) {
    myTabbedPane.setBackgroundColorAt(index, color);
  }

  @Override
  protected void setToolTipTextAt(final int index, final String text) {
    myTabbedPane.setToolTipTextAt(index, text);
  }

  @Override
  protected void setTabLayoutPolicy(final int policy) {
    myTabbedPane.setTabLayoutPolicy(policy);
  }

  @Override
  public void setTabsPlacement(final int tabPlacement) {
    myTabbedPane.setTabPlacement(tabPlacement);
  }

  @Override
  public void setAsCurrentWindow(final boolean requestFocus) {
    myOwner.setCurrentWindow(this, requestFocus);
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

  @Nonnull
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

  protected static class TComp extends JPanel implements DataProvider, EditorWindowHolder, IdeFocusTraversalPolicy.PassThroughComponent {
    @Nonnull
    final DesktopFileEditorWithProviderComposite myEditor;
    protected final FileEditorWindow myWindow;

    TComp(@Nonnull DesktopFileEditorWindow window, @Nonnull DesktopFileEditorWithProviderComposite editor) {
      super(new BorderLayout());
      myEditor = editor;
      myWindow = window;
      add(editor.getComponent(), BorderLayout.CENTER);
      addFocusListener(new FocusAdapter() {
        @Override
        public void focusGained(FocusEvent e) {
          ApplicationManager.getApplication().invokeLater(() -> {
            if (!hasFocus()) return;
            final JComponent focus = myEditor.getSelectedEditorWithProvider().getFileEditor().getPreferredFocusedComponent();
            if (focus != null && !focus.hasFocus()) {
              IdeFocusManager.getGlobalInstance().requestFocus(focus, true);
            }
          });
        }
      });
    }

    @Nonnull
    @Override
    public FileEditorWindow getEditorWindow() {
      return myWindow;
    }

    @Override
    public Object getData(@Nonnull Key<?> dataId) {
      if (CommonDataKeys.VIRTUAL_FILE == dataId) {
        final VirtualFile virtualFile = myEditor.getFile();
        return virtualFile.isValid() ? virtualFile : null;
      }
      if (CommonDataKeys.PROJECT == dataId) {
        return myEditor.getFileEditorManager().getProject();
      }
      return null;
    }
  }

  private void checkConsistency() {
    LOG.assertTrue(myOwner.containsWindow(this), "EditorWindow not in collection");
  }

  @Override
  public DesktopFileEditorWithProviderComposite getSelectedEditor() {
    final TComp comp = (TComp)myTabbedPane.getSelectedComponent();

    if (comp != null) {
      return comp.myEditor;
    }
    return null;
  }

  public void setSelectedEditor(final DesktopEditorComposite editor, final boolean focusEditor) {
    if (editor != null) {
      final int index = findFileIndex(editor.getFile());
      if (index != -1) {
        UIUtil.invokeLaterIfNeeded(() -> {
          myTabbedPane.setSelectedIndex(index, focusEditor);
        });
      }
    }
  }

  @RequiredUIAccess
  @Override
  public void setEditor(@Nullable final FileEditorWithProviderComposite editor, final boolean selectEditor, final boolean focusEditor) {
    if (editor != null) {
      onBeforeSetEditor(editor.getFile());

      final int index = findEditorIndex(editor);
      if (index != -1) {
        if (selectEditor) {
          setSelectedEditor((DesktopFileEditorWithProviderComposite)editor, focusEditor);
        }
      }
      else {
        int indexToInsert;

        Integer initialIndex = editor.getFile().getUserData(INITIAL_INDEX_KEY);
        if (initialIndex != null) {
          indexToInsert = initialIndex;
        }
        else if (Registry.is("ide.editor.tabs.open.at.the.end")) {
          indexToInsert = myTabbedPane.getTabCount();
        }
        else {
          int selectedIndex = myTabbedPane.getSelectedIndex();
          if (selectedIndex >= 0) {
            indexToInsert = selectedIndex + 1;
          }
          else {
            indexToInsert = 0;
          }
        }

        final VirtualFile file = editor.getFile();
        final Image template = AllIcons.FileTypes.Text;
        myTabbedPane.insertTab(file, Image.empty(template.getWidth(), template.getHeight()), new TComp(this, (DesktopFileEditorWithProviderComposite)editor), null, indexToInsert, this);
        trimToSize(UISettings.getInstance().getEditorTabLimit(), file, false);
        if (selectEditor) {
          setSelectedEditor((DesktopFileEditorWithProviderComposite)editor, focusEditor);
        }
        myOwner.updateFileIcon(file);
        myOwner.updateFileColor(file);
      }
      myOwner.setCurrentWindow(this, false);
    }
    myOwner.getComponent().validate();
  }

  protected void onBeforeSetEditor(VirtualFile file) {
  }

  private boolean splitAvailable() {
    return getTabCount() >= 1;
  }

  @Override
  @Nullable
  public DesktopFileEditorWindow split(final int orientation, boolean forceSplit, @Nullable VirtualFile virtualFile, boolean focusNew) {
    checkConsistency();
    final FileEditorManagerImpl fileEditorManager = myOwner.getManager();
    if (splitAvailable()) {
      if (!forceSplit && inSplitter()) {
        final DesktopFileEditorWindow[] siblings = findSiblings();
        final DesktopFileEditorWindow target = siblings[0];
        if (virtualFile != null) {
          final FileEditor[] editors = fileEditorManager.openFileImpl3(UIAccess.get(), target, virtualFile, focusNew, null, true).first;
          syncCaretIfPossible(editors);
        }
        return target;
      }
      final JPanel panel = myPanel;
      panel.setBorder(null);
      final int tabCount = getTabCount();
      if (tabCount != 0) {
        final DesktopFileEditorWithProviderComposite firstEC = getEditorAt(0);
        myPanel = new JPanel(new BorderLayout());
        myPanel.setOpaque(false);

        final Splitter splitter = new OnePixelSplitter(orientation == JSplitPane.VERTICAL_SPLIT, 0.5f, 0.1f, 0.9f);
        final DesktopFileEditorWindow res = new DesktopFileEditorWindow(myOwner);
        final DesktopFileEditorWithProviderComposite selectedEditor = getSelectedEditor();
        panel.remove(myTabbedPane.getComponent());
        panel.add(splitter, BorderLayout.CENTER);
        splitter.setFirstComponent(myPanel);
        myPanel.add(myTabbedPane.getComponent(), BorderLayout.CENTER);
        splitter.setSecondComponent(res.myPanel);
          /*
          for (int i = 0; i != tabCount; ++i) {
            final EditorWithProviderComposite eC = getEditorAt(i);
            final VirtualFile file = eC.getFile();
            fileEditorManager.openFileImpl3(res, file, false, null);
            res.setFilePinned (file, isFilePinned (file));
          }
          */
        // open only selected file in the new splitter instead of opening all tabs
        final VirtualFile file = selectedEditor.getFile();

        if (virtualFile == null) {
          for (FileEditorAssociateFinder finder : FileEditorAssociateFinder.EP_NAME.getExtensionList()) {
            VirtualFile associatedFile = finder.getAssociatedFileToOpen(fileEditorManager.getProject(), file);

            if (associatedFile != null) {
              virtualFile = associatedFile;
              break;
            }
          }
        }

        final VirtualFile nextFile = virtualFile == null ? file : virtualFile;
        final FileEditor[] editors = fileEditorManager.openFileImpl3(UIAccess.get(), res, nextFile, focusNew, null, true).first;
        syncCaretIfPossible(editors);
        res.setFilePinned(nextFile, isFilePinned(file));
        if (!focusNew) {
          res.setSelectedEditor(selectedEditor, true);
          getGlobalInstance().doWhenFocusSettlesDown(() -> {
            getGlobalInstance().requestFocus(selectedEditor.getComponent(), true);
          });
        }
        panel.revalidate();
        return res;
      }
    }
    return null;
  }

  /**
   * Tries to setup caret and viewport for the given editor from the selected one.
   *
   * @param toSync editor to setup caret and viewport for
   */
  private void syncCaretIfPossible(@Nullable FileEditor[] toSync) {
    if (toSync == null) {
      return;
    }

    final DesktopFileEditorWithProviderComposite from = getSelectedEditor();
    if (from == null) {
      return;
    }

    final FileEditor caretSource = from.getSelectedEditor();
    if (!(caretSource instanceof TextEditor)) {
      return;
    }

    final Editor editorFrom = ((TextEditor)caretSource).getEditor();
    final int offset = editorFrom.getCaretModel().getOffset();
    if (offset <= 0) {
      return;
    }

    final int scrollOffset = editorFrom.getScrollingModel().getVerticalScrollOffset();

    for (FileEditor fileEditor : toSync) {
      if (!(fileEditor instanceof TextEditor)) {
        continue;
      }
      final Editor editor = ((TextEditor)fileEditor).getEditor();
      if (editorFrom.getDocument() == editor.getDocument()) {
        editor.getCaretModel().moveToOffset(offset);
        final ScrollingModel scrollingModel = editor.getScrollingModel();
        scrollingModel.scrollVertically(scrollOffset);

        SwingUtilities.invokeLater(() -> {
          if (!editor.isDisposed()) {
            scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE);
          }
        });
      }
    }
  }

  @Nonnull
  @Override
  public DesktopFileEditorWindow[] findSiblings() {
    checkConsistency();
    final ArrayList<DesktopFileEditorWindow> res = new ArrayList<>();
    if (myPanel.getParent() instanceof Splitter) {
      final Splitter splitter = (Splitter)myPanel.getParent();
      for (final DesktopFileEditorWindow win : myOwner.getWindows()) {
        if (win != this && SwingUtilities.isDescendingFrom(win.myPanel, splitter)) {
          res.add(win);
        }
      }
    }
    return res.toArray(new DesktopFileEditorWindow[res.size()]);
  }

  @Override
  public void changeOrientation() {
    checkConsistency();
    final Container parent = myPanel.getParent();
    if (parent instanceof Splitter) {
      final Splitter splitter = (Splitter)parent;
      splitter.setOrientation(!splitter.getOrientation());
    }
  }

  @Override
  public void unsplit(boolean setCurrent) {
    checkConsistency();
    final Container splitter = myPanel.getParent();

    if (!(splitter instanceof Splitter)) return;

    FileEditorWithProviderComposite editorToSelect = getSelectedEditor();
    final DesktopFileEditorWindow[] siblings = findSiblings();
    final JPanel parent = (JPanel)splitter.getParent();

    for (DesktopFileEditorWindow eachSibling : siblings) {
      // selected editors will be added first
      final DesktopFileEditorWithProviderComposite selected = eachSibling.getSelectedEditor();
      if (editorToSelect == null) {
        editorToSelect = selected;
      }
    }

    for (final DesktopFileEditorWindow sibling : siblings) {
      final FileEditorWithProviderComposite[] siblingEditors = sibling.getEditors();
      for (final FileEditorWithProviderComposite siblingEditor : siblingEditors) {
        if (editorToSelect == null) {
          editorToSelect = siblingEditor;
        }
        processSiblingEditor((DesktopFileEditorWithProviderComposite)siblingEditor);
      }
      LOG.assertTrue(sibling != this);
      sibling.dispose();
    }
    parent.remove(splitter);
    parent.add(myTabbedPane.getComponent(), BorderLayout.CENTER);
    parent.revalidate();
    myPanel = parent;
    if (editorToSelect != null) {
      setSelectedEditor((DesktopEditorComposite)editorToSelect, true);
    }
    if (setCurrent) {
      myOwner.setCurrentWindow(this, false);
    }
  }

  private void processSiblingEditor(final DesktopFileEditorWithProviderComposite siblingEditor) {
    if (getTabCount() < UISettings.getInstance().getEditorTabLimit() && findFileComposite(siblingEditor.getFile()) == null) {
      setEditor(siblingEditor, true);
    }
    else {
      getManager().disposeComposite(siblingEditor);
    }
  }

  @Override
  public void unsplitAll() {
    checkConsistency();
    while (inSplitter()) {
      unsplit(true);
    }
  }

  @Override
  public boolean inSplitter() {
    checkConsistency();
    return myPanel.getParent() instanceof Splitter;
  }

  @Override
  public VirtualFile getSelectedFile() {
    checkConsistency();
    final DesktopFileEditorWithProviderComposite editor = getSelectedEditor();
    return editor == null ? null : editor.getFile();
  }

  private int findComponentIndex(final Component component) {
    for (int i = 0; i != getTabCount(); ++i) {
      final DesktopFileEditorWithProviderComposite editor = getEditorAt(i);
      if (editor.getComponent().equals(component)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  protected DesktopFileEditorWithProviderComposite getEditorAt(final int i) {
    final TComp comp;
    comp = (TComp)myTabbedPane.getComponentAt(i);
    return comp.myEditor;
  }

  @Override
  public boolean isFileOpen(final VirtualFile file) {
    return findFileComposite(file) != null;
  }

  @Override
  public boolean isFilePinned(final VirtualFile file) {
    final FileEditorComposite editorComposite = findFileComposite(file);
    if (editorComposite == null) {
      throw new IllegalArgumentException("file is not open: " + file.getPath());
    }
    return editorComposite.isPinned();
  }

  @Override
  public void setFilePinned(final VirtualFile file, final boolean pinned) {
    final DesktopEditorComposite editorComposite = findFileComposite(file);
    if (editorComposite == null) {
      throw new IllegalArgumentException("file is not open: " + file.getPath());
    }
    boolean wasPinned = editorComposite.isPinned();
    editorComposite.setPinned(pinned);
    if (wasPinned != pinned && ApplicationManager.getApplication().isDispatchThread()) {
      updateFileIcon(file);
    }
  }

  @Override
  protected void trimToSize(final int limit, @Nullable final VirtualFile fileToIgnore, final boolean transferFocus) {
    FileEditorManagerEx.getInstanceEx(getManager().getProject()).getReady(this).doWhenDone(() -> {
      final boolean closeNonModifiedFilesFirst = UISettings.getInstance().getCloseNonModifiedFilesFirst();
      final DesktopEditorComposite selectedComposite = getSelectedEditor();
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
    if (shouldCloseSelected()) {
      defaultCloseFile(selectedFile, transferFocus);
      closingOrder.remove(selectedFile);
    }

    for (VirtualFile file : closingOrder) {
      if (myTabbedPane.getTabCount() <= limit || myTabbedPane.getTabCount() == 0 || areAllTabsPinned(fileToIgnore)) {
        return;
      }
      if (fileCanBeClosed(file, fileToIgnore)) {
        defaultCloseFile(file, transferFocus);
      }
    }

  }

  private LinkedHashSet<VirtualFile> getTabClosingOrder(boolean closeNonModifiedFilesFirst) {
    final VirtualFile[] allFiles = getFiles();
    final Set<VirtualFile> histFiles = EditorHistoryManagerImpl.getInstance(getManager().getProject()).getFileSet();

    LinkedHashSet<VirtualFile> closingOrder = ContainerUtil.newLinkedHashSet();

    // first, we search for files not in history
    for (final VirtualFile file : allFiles) {
      if (!histFiles.contains(file)) {
        closingOrder.add(file);
      }
    }

    if (closeNonModifiedFilesFirst) {
      // Search in history
      for (final VirtualFile file : histFiles) {
        DesktopFileEditorWithProviderComposite composite = findFileComposite(file);
        if (composite != null && !myOwner.getManager().isChanged(composite)) {
          // we found non modified file
          closingOrder.add(file);
        }
      }

      // Search in tabbed pane
      for (int i = 0; i < myTabbedPane.getTabCount(); i++) {
        final VirtualFile file = getFileAt(i);
        if (!myOwner.getManager().isChanged(getEditorAt(i))) {
          // we found non modified file
          closingOrder.add(file);
        }
      }
    }

    // If it's not enough to close non-modified files only, try all other files.
    // Search in history from less frequently used.
    closingOrder.addAll(histFiles);

    // finally, close tabs by their order
    for (int i = 0; i < myTabbedPane.getTabCount(); i++) {
      closingOrder.add(getFileAt(i));
    }

    final VirtualFile selectedFile = getSelectedFile();
    closingOrder.remove(selectedFile);
    closingOrder.add(selectedFile); // selected should be closed last
    return closingOrder;
  }

  private boolean shouldCloseSelected() {
    if (!UISettings.getInstance().getReuseNotModifiedTabs() || !myOwner.getManager().getProject().isInitialized()) {
      return false;
    }

    VirtualFile file = getSelectedFile();
    if (file == null || !isFileOpen(file) || isFilePinned(file)) {
      return false;
    }
    DesktopFileEditorWithProviderComposite composite = findFileComposite(file);
    if (composite == null) return false;
    Component owner = ProjectIdeFocusManager.getInstance(myOwner.getManager().getProject()).getFocusOwner();
    if (owner == null || !SwingUtilities.isDescendingFrom(owner, composite.getSelectedEditor().getComponent())) return false;
    return !myOwner.getManager().isChanged(composite);
  }

  private boolean areAllTabsPinned(VirtualFile fileToIgnore) {
    for (int i = myTabbedPane.getTabCount() - 1; i >= 0; i--) {
      if (fileCanBeClosed(getFileAt(i), fileToIgnore)) {
        return false;
      }
    }
    return true;
  }

  private void defaultCloseFile(VirtualFile file, boolean transferFocus) {
    closeFile(file, true, transferFocus);
  }

  private boolean fileCanBeClosed(final VirtualFile file, @Nullable final VirtualFile fileToIgnore) {
    return isFileOpen(file) && !file.equals(fileToIgnore) && !isFilePinned(file);
  }

  @Override
  public void clear() {
    for (FileEditorWithProviderComposite composite : getEditors()) {
      Disposer.dispose(composite);
    }
  }

  @Nullable
  @Override
  public DesktopFileEditorWithProviderComposite findFileComposite(VirtualFile file) {
    return (DesktopFileEditorWithProviderComposite)super.findFileComposite(file);
  }

  @Override
  public String toString() {
    return "EditorWindow: files=" + Arrays.asList(getFiles());
  }
}
