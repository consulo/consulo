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

import consulo.dataContext.DataManager;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.FileEditorWithProviderComposite;
import consulo.fileEditor.event.FileEditorManagerBeforeListener;
import consulo.ui.ex.action.AnActionEvent;
import consulo.dataContext.DataContext;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.ide.impl.idea.openapi.fileEditor.impl.FileEditorManagerImpl;
import consulo.ide.impl.idea.openapi.fileEditor.impl.tabActions.CloseTab;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.disposer.Disposable;
import consulo.ide.impl.VfsIconUtil;
import consulo.ui.Component;
import consulo.ui.Tab;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.layout.TabbedLayout;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2018-05-09
 */
public class UnifiedFileEditorWindow extends FileEditorWindowBase implements FileEditorWindow, Disposable {
  private final Project myProject;
  private FileEditorManagerImpl myManager;
  private FileEditorsSplittersBase<UnifiedFileEditorWindow> myOwner;

  private TabbedLayout myTabbedLayout = TabbedLayout.create();

  private Map<FileEditorWithProviderComposite, Tab> myEditors = new LinkedHashMap<>();

  @RequiredUIAccess
  public UnifiedFileEditorWindow(Project project, FileEditorManagerImpl manager, FileEditorsSplittersBase<UnifiedFileEditorWindow> owner) {
    myProject = project;
    myManager = manager;
    myOwner = owner;

    myOwner.addWindow(this);
    if (myOwner.getCurrentWindow() == null) {
      myOwner.setCurrentWindow(this, false);
    }
  }

  @Override
  @Nonnull
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
    Tab tab = myEditors.get(editorAt);
    tab.clearText();
    tab.append(text);
  }

  @Override
  protected void setBackgroundColorAt(int index, Color color) {

  }

  @Override
  protected void setToolTipTextAt(int index, String text) {

  }

  @Override
  protected void setForegroundAt(int index, Color color) {

  }

  @Override
  protected void setWaveColor(int index, @Nullable Color color) {

  }

  @Override
  protected void setIconAt(int index, Image icon) {

  }

  @Override
  protected void setTabLayoutPolicy(int policy) {

  }

  @Override
  protected void trimToSize(int limit, @Nullable VirtualFile fileToIgnore, boolean transferFocus) {

  }

  @Nonnull
  @Override
  public FileEditorManagerImpl getManager() {
    return myManager;
  }

  @Nullable
  @Override
  public FileEditorWindow split(int orientation, boolean forceSplit, @Nullable VirtualFile virtualFile, boolean focusNew) {
    return null;
  }

  @Override
  public void unsplit(boolean setCurrent) {

  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Nonnull
  @Override
  public FileEditorWindow[] findSiblings() {
    return new FileEditorWindow[0];
  }

  @Nullable
  @Override
  public FileEditorWithProviderComposite getSelectedEditor() {
    if (myEditors.isEmpty()) {
      return null;
    }
    Map.Entry<FileEditorWithProviderComposite, Tab> entry = myEditors.entrySet().iterator().next();
    return entry.getKey();
  }

  @Nonnull
  @Override
  public FileEditorsSplittersBase<UnifiedFileEditorWindow> getOwner() {
    return myOwner;
  }

  @Override
  public VirtualFile getSelectedFile() {
    FileEditorWithProviderComposite selectedEditor = getSelectedEditor();
    if(selectedEditor != null) {
      return selectedEditor.getFile();
    }
    return null;
  }

  @Override
  public boolean inSplitter() {
    return false;
  }

  @Override
  public void closeFile(@Nonnull VirtualFile file, boolean disposeIfNeeded, boolean transferFocus) {
    final FileEditorManagerImpl editorManager = getManager();
    editorManager.runChange(splitters -> {
      final List<FileEditorWithProviderComposite> editors = splitters.findEditorComposites(file);
      if (editors.isEmpty()) return;
      try {
        final FileEditorWithProviderComposite editor = findFileComposite(file);

        final FileEditorManagerBeforeListener beforePublisher = editorManager.getProject().getMessageBus().syncPublisher(FileEditorManagerBeforeListener.class);

        beforePublisher.beforeFileClosed(editorManager, file);

        if (editor != null) {
          Tab tab = myEditors.remove(editor);
          if(tab != null) {
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
          final Project project = editorManager.getProject();
          if (!project.isDisposed()) {
            final FileEditorManagerListener afterPublisher = project.getMessageBus().syncPublisher(FileEditorManagerListener.class);
            afterPublisher.fileClosed(editorManager, file);
          }
        });

        ((UnifiedFileEditorsSplitters)splitters).afterFileClosed(file);
      }
    }, myOwner);
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
        Tab tab = myTabbedLayout.addTab(editor.getFile().getName(), editor.getUIComponent());
        tab.setCloseHandler((thisTab, component) -> {
          DataContext dataContext = DataManager.getInstance().getDataContext();
          new CloseTab(myTabbedLayout, myProject, editor.getFile(), this).actionPerformed(AnActionEvent.createFromInputEvent(null, "Test", null, dataContext));
        });
        tab.withIcon(VfsIconUtil.getIcon(editor.getFile(), 0, myManager.getProject()));
        myEditors.put(editor, tab);
      }
      else {
        Tab tab = myEditors.get(fileComposite);
        assert tab != null;
        tab.select();
      }
    }
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
