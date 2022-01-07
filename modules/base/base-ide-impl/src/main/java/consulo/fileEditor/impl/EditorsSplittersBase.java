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
package consulo.fileEditor.impl;

import com.intellij.ide.ui.UISettingsListener;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.impl.FileEditorManagerImpl;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.FrameTitleBuilder;
import com.intellij.ui.JBColor;
import com.intellij.util.Alarm;
import com.intellij.util.containers.ArrayListSet;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.collection.ContainerUtil;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author VISTALL
 * @since 2018-05-11
 */
public abstract class EditorsSplittersBase<W extends EditorWindowBase> implements EditorsSplitters, Disposable {
  private static final Logger LOG = Logger.getInstance(EditorsSplittersBase.class);

  @Nonnull
  protected final Project myProject;
  protected final FileEditorManagerImpl myManager;
  private int myInsideChange;

  protected W myCurrentWindow;
  protected final Set<W> myWindows = new CopyOnWriteArraySet<>();
  protected Element mySplittersElement;  // temporarily used during initialization
  private final Alarm myIconUpdaterAlarm = new Alarm();
  private final Set<VirtualFile> myFilesToUpdateIconsFor = new HashSet<>();

  protected EditorsSplittersBase(Project project, FileEditorManagerImpl manager) {
    myProject = project;
    myManager = manager;

    project.getApplication().getMessageBus().connect(this).subscribe(UISettingsListener.TOPIC, source -> {
      if (!myManager.getProject().isOpen()) {
        return;
      }

      for (VirtualFile file : getOpenFiles()) {
        updateFileBackgroundColor(file);
        updateFileIcon(file);
        updateFileColor(file);
      }
    });
  }

  @Nonnull
  protected abstract W[] createArray(int size);

  @RequiredUIAccess
  protected abstract void createCurrentWindow();

  @Nonnull
  protected abstract ModalityState getComponentModality();

  protected void stopListeningFocus() {
  }

  public void afterFileClosed(VirtualFile file) {
  }

  public void afterFileOpen(VirtualFile file) {
  }

  @RequiredUIAccess
  @Override
  public void closeFile(VirtualFile file, boolean moveFocus) {
    final List<W> windows = findWindows(file);
    if (!windows.isEmpty()) {
      final VirtualFile nextFile = findNextFile(file);
      for (final W window : windows) {
        LOG.assertTrue(window.getSelectedEditor() != null);
        window.closeFile(file, false, moveFocus);
        if (window.getTabCount() == 0 && nextFile != null && myManager.getProject().isOpen()) {
          EditorWithProviderComposite newComposite = myManager.newEditorComposite(nextFile);
          window.setEditor(newComposite, moveFocus); // newComposite can be null
        }
      }
      // cleanup windows with no tabs
      for (final W window : windows) {
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

  @Nullable
  private VirtualFile findNextFile(final VirtualFile file) {
    final W[] windows = getWindows(); // TODO: use current file as base
    for (int i = 0; i != windows.length; ++i) {
      final VirtualFile[] files = windows[i].getFiles();
      for (final VirtualFile fileAt : files) {
        if (!Objects.equals(fileAt, file)) {
          return fileAt;
        }
      }
    }
    return null;
  }

  @Override
  public void setTabsPlacement(final int tabPlacement) {
    final W[] windows = getWindows();
    for (int i = 0; i != windows.length; ++i) {
      windows[i].setTabsPlacement(tabPlacement);
    }
  }

  @Override
  public void setTabLayoutPolicy(int scrollTabLayout) {
    final W[] windows = getWindows();
    for (int i = 0; i != windows.length; ++i) {
      windows[i].setTabLayoutPolicy(scrollTabLayout);
    }
  }

  @Override
  public void trimToSize(final int editor_tab_limit) {
    for (W window : myWindows) {
      window.trimToSize(editor_tab_limit, null, true);
    }
  }

  public FileEditorManagerImpl getManager() {
    return myManager;
  }

  private void updateFileIconImmediately(final VirtualFile file) {
    final Collection<W> windows = findWindows(file);
    for (W window : windows) {
      window.updateFileIcon(file);
    }
  }

  private void updateFileIconLater(VirtualFile file) {
    myFilesToUpdateIconsFor.add(file);
    myIconUpdaterAlarm.cancelAllRequests();
    myIconUpdaterAlarm.addRequest(() -> {
      if (myManager.getProject().isDisposed()) return;
      for (VirtualFile file1 : myFilesToUpdateIconsFor) {
        updateFileIconImmediately(file1);
      }
      myFilesToUpdateIconsFor.clear();
    }, 200, getComponentModality());
  }

  protected boolean showEmptyText() {
    return myCurrentWindow == null || myCurrentWindow.getFiles().length == 0;
  }

  @Override
  public void updateFileIcon(@Nonnull final VirtualFile file) {
    updateFileIconLater(file);
  }

  @Override
  public void updateFileColor(@Nonnull final VirtualFile file) {
    final Collection<W> windows = findWindows(file);
    for (W window : windows) {
      final int index = window.findEditorIndex(window.findFileComposite(file));
      LOG.assertTrue(index != -1);
      window.setForegroundAt(index, TargetAWT.to(myManager.getFileColor(file)));
      window.setWaveColor(index, myManager.isProblem(file) ? JBColor.red : null);
    }
  }

  @Override
  public void updateFileBackgroundColor(@Nonnull VirtualFile file) {
    final W[] windows = getWindows();
    for (int i = 0; i != windows.length; ++i) {
      windows[i].updateFileBackgroundColor(file);
    }
  }

  @Override
  @Nullable
  public VirtualFile getCurrentFile() {
    if (myCurrentWindow != null) {
      return myCurrentWindow.getSelectedFile();
    }
    return null;
  }

  @Nonnull
  @Override
  public EditorWindow getOrCreateCurrentWindow(final VirtualFile file) {
    final List<W> windows = findWindows(file);
    if (getCurrentWindow() == null) {
      final Iterator<W> iterator = myWindows.iterator();
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
  public void setCurrentWindow(@Nullable final EditorWindow window, final boolean requestFocus) {
    EditorWithProviderComposite newEditor = window == null ? null : window.getSelectedEditor();

    Runnable fireRunnable = () -> myManager.fireSelectionChanged(newEditor);

    setCurrentWindow((W)window);

    myManager.updateFileName(window == null ? null : window.getSelectedFile());

    if (window != null) {
      final EditorWithProviderComposite selectedEditor = window.getSelectedEditor();
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

  protected void setCurrentWindow(@Nullable final W currentWindow) {
    if (currentWindow != null && !myWindows.contains(currentWindow)) {
      throw new IllegalArgumentException(currentWindow + " is not a member of this container");
    }
    myCurrentWindow = currentWindow;
  }

  @Override
  public void readExternal(final Element element) {
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
  @Nullable
  public W getCurrentWindow() {
    return myCurrentWindow;
  }

  @Override
  public final void updateFileName(@Nullable final VirtualFile updatedFile) {
    final W[] windows = getWindows();
    for (int i = 0; i != windows.length; ++i) {
      for (VirtualFile file : windows[i].getFiles()) {
        if (updatedFile == null || file.getName().equals(updatedFile.getName())) {
          windows[i].updateFileName(file);
        }
      }
    }

    Project project = myManager.getProject();

    final IdeFrame frame = getFrame(project);
    if (frame != null) {
      VirtualFile file = getCurrentFile();

      File ioFile = file == null ? null : new File(file.getPresentableUrl());
      String fileTitle = null;
      if (file != null) {
        fileTitle = DumbService.isDumb(project) ? file.getName() : FrameTitleBuilder.getInstance().getFileTitle(project, file);
      }

      frame.setFileTitle(fileTitle, ioFile);
    }
  }

  @Override
  @Nonnull
  public VirtualFile[] getOpenFiles() {
    final Set<VirtualFile> files = new ArrayListSet<>();
    for (final W myWindow : myWindows) {
      final EditorWithProviderComposite[] editors = myWindow.getEditors();
      for (final EditorWithProviderComposite editor : editors) {
        VirtualFile file = editor.getFile();
        // background thread may call this method when invalid file is being removed
        // do not return it here as it will quietly drop out soon
        if (file.isValid()) {
          files.add(file);
        }
      }
    }
    return VfsUtilCore.toVirtualFileArray(files);
  }

  @Override
  @Nonnull
  public VirtualFile[] getSelectedFiles() {
    final Set<VirtualFile> files = new ArrayListSet<>();
    for (final W window : myWindows) {
      final VirtualFile file = window.getSelectedFile();
      if (file != null) {
        files.add(file);
      }
    }
    final VirtualFile[] virtualFiles = VfsUtilCore.toVirtualFileArray(files);
    final VirtualFile currentFile = getCurrentFile();
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
  @Nonnull
  @SuppressWarnings("unchecked")
  public FileEditor[] getSelectedEditors() {
    Set<W> windows = new HashSet<>(myWindows);
    final EditorWindow currentWindow = getCurrentWindow();
    if (currentWindow != null) {
      windows.add((W)currentWindow);
    }
    List<FileEditor> editors = new ArrayList<>();
    for (final W window : windows) {
      final EditorWithProviderComposite composite = window.getSelectedEditor();
      if (composite != null) {
        editors.add(composite.getSelectedEditor());
      }
    }
    return editors.toArray(new FileEditor[editors.size()]);
  }

  public void addWindow(W window) {
    myWindows.add(window);
  }

  public void removeWindow(W window) {
    myWindows.remove(window);
    if (myCurrentWindow == window) {
      myCurrentWindow = null;
    }
  }

  public boolean containsWindow(W window) {
    return myWindows.contains(window);
  }

  //---------------------------------------------------------

  @Override
  public EditorWithProviderComposite[] getEditorsComposites() {
    List<EditorWithProviderComposite> res = new ArrayList<>();

    for (final EditorWindow myWindow : myWindows) {
      final EditorWithProviderComposite[] editors = myWindow.getEditors();
      ContainerUtil.addAll(res, editors);
    }
    return res.toArray(new EditorWithProviderComposite[res.size()]);
  }

  //---------------------------------------------------------

  @Override
  @Nonnull
  public List<EditorWithProviderComposite> findEditorComposites(@Nonnull VirtualFile file) {
    List<EditorWithProviderComposite> res = new ArrayList<>();
    for (final EditorWindow window : myWindows) {
      final EditorWithProviderComposite fileComposite = window.findFileComposite(file);
      if (fileComposite != null) {
        res.add(fileComposite);
      }
    }
    return res;
  }

  @Nonnull
  protected List<W> findWindows(final VirtualFile file) {
    List<W> res = new ArrayList<>();
    for (W window : myWindows) {
      if (window.findFileComposite(file) != null) {
        res.add(window);
      }
    }
    return res;
  }

  @Override
  @Nonnull
  public W[] getWindows() {
    return myWindows.toArray(createArray(myWindows.size()));
  }

  @Override
  public void dispose() {
    myIconUpdaterAlarm.cancelAllRequests();
    stopListeningFocus();
  }

  protected IdeFrame getFrame(Project project) {
    final IdeFrame frame = WindowManagerEx.getInstance().getIdeFrame(project);
    LOG.assertTrue(ApplicationManager.getApplication().isUnitTestMode() || frame != null);
    return frame;
  }
}
