/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileEditor.impl;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.DockableEditorTabbedContainer;
import consulo.fileEditor.FileEditorWindow;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.impl.idea.ui.tabs.JBTabs;
import consulo.ide.impl.idea.ui.tabs.TabInfo;
import consulo.ide.impl.idea.ui.tabs.impl.JBTabsImpl;
import consulo.project.Project;
import consulo.project.ui.wm.dock.DockableContent;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.RelativeRectangle;
import consulo.ui.ex.awt.internal.SwingDockContainer;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CopyOnWriteArraySet;

@Deprecated
@DeprecationInfo("Desktop only")
public class DesktopDockableEditorTabbedContainer implements DockableEditorTabbedContainer, SwingDockContainer {

  private final DesktopFileEditorsSplitters mySplitters;
  private final Project myProject;

  private final CopyOnWriteArraySet<Listener> myListeners = new CopyOnWriteArraySet<Listener>();

  private JBTabs myCurrentOver;
  private Image myCurrentOverImg;
  private TabInfo myCurrentOverInfo;

  private boolean myDisposeWhenEmpty;

  private boolean myWasEverShown;

  DesktopDockableEditorTabbedContainer(Project project) {
    this(project, null, true);
  }

  DesktopDockableEditorTabbedContainer(Project project, @Nullable DesktopFileEditorsSplitters splitters, boolean disposeWhenEmpty) {
    myProject = project;
    mySplitters = splitters;
    myDisposeWhenEmpty = disposeWhenEmpty;
  }

  @Override
  public String getDockContainerType() {
    return DockableEditorContainerFactory.TYPE;
  }

  @Override
  public Element getState() {
    Element editors = new Element("state");
    mySplitters.writeExternal(editors);
    return editors;
  }

  void fireContentClosed(VirtualFile file) {
    for (Listener each : myListeners) {
      each.contentRemoved(file);
    }
  }

  void fireContentOpen(VirtualFile file) {
    for (Listener each : myListeners) {
      each.contentAdded(file);
    }
  }

  @Override
  public RelativeRectangle getAcceptArea() {
    return new RelativeRectangle(mySplitters.getComponent());
  }

  @Override
  public RelativeRectangle getAcceptAreaFallback() {
    JRootPane root = mySplitters.getComponent().getRootPane();
    return root != null ? new RelativeRectangle(root) : new RelativeRectangle(mySplitters.getComponent());
  }

  @Nonnull
  @Override
  public ContentResponse getContentResponse(@Nonnull DockableContent content, RelativePoint point) {
    return getTabsAt(content, point) != null ? ContentResponse.ACCEPT_MOVE : ContentResponse.DENY;
  }

  @Nullable
  private JBTabs getTabsAt(DockableContent content, RelativePoint point) {
    if (content instanceof EditorTabbedContainer.DockableEditor) {
      JBTabs targetTabs = mySplitters.getTabsAt(point);
      if (targetTabs != null) {
        return targetTabs;
      } else {
        FileEditorWindow wnd = mySplitters.getCurrentWindow();
        if (wnd != null) {
          EditorTabbedContainer tabs = ((DesktopFileEditorWindow)wnd).getTabbedPane();
          if (tabs != null) {
            return tabs.getTabs();
          }
        } else {
          DesktopFileEditorWindow[] windows = mySplitters.getWindows();
          for (DesktopFileEditorWindow each : windows) {
            if (each.getTabbedPane() != null && each.getTabbedPane().getTabs() != null) {
              return each.getTabbedPane().getTabs();
            }
          }
        }
      }
    }

    return null;
  }

  @Override
  public void add(@Nonnull DockableContent content, RelativePoint dropTarget) {
    FileEditorWindow window = null;
    if (myCurrentOver != null) {
      final DataProvider provider = myCurrentOver.getDataProvider();
      if (provider != null) {
        window = provider.getDataUnchecked(FileEditorWindow.DATA_KEY);
      }
    }

    final EditorTabbedContainer.DockableEditor dockableEditor = (EditorTabbedContainer.DockableEditor)content;
    VirtualFile file = dockableEditor.getFile();


    if (window == null || window.isDisposed()) {
      window = mySplitters.getOrCreateCurrentWindow(file);
    }


    if (myCurrentOver != null) {
      int index = ((JBTabsImpl)myCurrentOver).getDropInfoIndex();
      file.putUserData(DesktopFileEditorWindow.INITIAL_INDEX_KEY, index);
    }

    ((FileEditorManagerImpl)FileEditorManagerEx.getInstanceEx(myProject)).openFileImpl2(UIAccess.get(), window, file, true);
    window.setFilePinned(file, dockableEditor.isPinned());
  }

  @Override
  public Image startDropOver(@Nonnull DockableContent content, RelativePoint point) {
    return null;
  }

  @Override
  public Image processDropOver(@Nonnull DockableContent content, RelativePoint point) {
    JBTabs current = getTabsAt(content, point);

    if (myCurrentOver != null && myCurrentOver != current) {
      resetDropOver(content);
    }

    if (myCurrentOver == null && current != null) {
      myCurrentOver = current;
      Presentation presentation = content.getPresentation();
      myCurrentOverInfo = new TabInfo(new JLabel("")).setText(presentation.getText()).setIcon(presentation.getIcon());
      myCurrentOverImg = myCurrentOver.startDropOver(myCurrentOverInfo, point);
    }

    if (myCurrentOver != null) {
      myCurrentOver.processDropOver(myCurrentOverInfo, point);
    }

    return myCurrentOverImg;
  }

  @Override
  public void resetDropOver(@Nonnull DockableContent content) {
    if (myCurrentOver != null) {
      myCurrentOver.resetDropOver(myCurrentOverInfo);
      myCurrentOver = null;
      myCurrentOverInfo = null;
      myCurrentOverImg = null;
    }
  }

  @Override
  public JComponent getContainerComponent() {
    return mySplitters.getComponent();
  }

  @Override
  @Nullable
  public DesktopFileEditorsSplitters getSplitters() {
    return mySplitters;
  }

  @RequiredUIAccess
  public void close(VirtualFile file) {
    mySplitters.closeFile(file, false);
  }

  @Override
  @RequiredUIAccess
  public void closeAll() {
    assert mySplitters != null;
    VirtualFile[] files = mySplitters.getOpenFiles();
    for (VirtualFile each : files) {
      close(each);
    }
  }

  @Override
  public void addListener(final Listener listener, Disposable parent) {
    myListeners.add(listener);
    Disposer.register(parent, () -> myListeners.remove(listener));
  }

  @Override
  public boolean isEmpty() {
    return mySplitters.isEmptyVisible();
  }

  @Override
  public void dispose() {
    if(UIAccess.isUIThread()) {
      closeAll();
    }
    else {
      UIAccess uiAccess = Application.get().getLastUIAccess();
      uiAccess.giveAndWait(this::closeAll);
    }
  }

  @Override
  public boolean isDisposeWhenEmpty() {
    return myDisposeWhenEmpty;
  }

  @Override
  public void showNotify() {
    if (!myWasEverShown) {
      myWasEverShown = true;
      getSplitters().openFiles(UIAccess.current());
    }
  }

  @Override
  public void hideNotify() {
  }
}
