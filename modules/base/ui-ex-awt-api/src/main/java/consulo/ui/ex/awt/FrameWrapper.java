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
package consulo.ui.ex.awt;

import consulo.application.Application;
import consulo.application.ui.ApplicationWindowStateService;
import consulo.application.ui.WindowState;
import consulo.application.ui.WindowStateService;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import consulo.project.ui.ProjectWindowStateService;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.IdeRootPaneNorthExtension;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.ui.ex.awt.internal.*;
import consulo.ui.ex.awt.util.FocusWatcher;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.style.StyleManager;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrameWrapper implements Disposable, DataProvider {

  private String myDimensionKey = null;
  private JComponent myComponent = null;
  private JComponent myPreferredFocus = null;
  private String myTitle = "";
  private List<Image> myImages = null;
  private boolean myCloseOnEsc = false;
  private Window myFrame;
  private final Map<Key<?>, Object> myDataMap = new HashMap<>();
  private Project myProject;
  private final ProjectManagerListener myProjectListener = new MyProjectManagerListener();
  private FocusWatcher myFocusWatcher;

  private boolean myDisposed;

  protected StatusBar myStatusBar;
  private boolean myShown;
  private boolean myIsDialog;

  public FrameWrapper(Project project) {
    this(project, null);
  }

  public FrameWrapper(Project project, @Nullable @NonNls String dimensionServiceKey) {
    this(project, dimensionServiceKey, false);
  }

  public FrameWrapper(Project project, @Nullable @NonNls String dimensionServiceKey, boolean isDialog) {
    myDimensionKey = dimensionServiceKey;
    myIsDialog = isDialog;
    if (project != null) {
      setProject(project);
    }
  }

  public void setDimensionKey(String dimensionKey) {
    myDimensionKey = dimensionKey;
  }

  public void setData(Key<?> dataId, Object data) {
    myDataMap.put(dataId, data);
  }

  public void setProject(@Nonnull final Project project) {
    myProject = project;
    setData(Project.KEY, project);
    ProjectManager.getInstance().addProjectManagerListener(project, myProjectListener);
    Disposer.register(this, () -> ProjectManager.getInstance().removeProjectManagerListener(project, myProjectListener));
  }

  public void show() {
    show(true);
  }

  public void show(boolean restoreBounds) {
    final Window frame = getFrame();

    if (myStatusBar != null) {
      consulo.ui.Window uiWindow = TargetAWT.from(frame);
      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
      myStatusBar.install(ideFrame);
    }

    if (frame instanceof JFrame) {
      ((JFrame)frame).setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }
    else {
      ((JDialog)frame).setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    final WindowAdapter focusListener = new WindowAdapter() {
      @Override
      public void windowOpened(WindowEvent e) {
        IdeFocusManager fm = ProjectIdeFocusManager.getInstance(myProject);
        JComponent toFocus = getPreferredFocusedComponent();
        if (toFocus == null) {
          toFocus = fm.getFocusTargetFor(myComponent);
        }

        if (toFocus != null) {
          fm.requestFocus(toFocus, true);
        }
      }
    };
    frame.addWindowListener(focusListener);
    if (ModalityPerProjectEAPDescriptor.is()) {
      frame.setAlwaysOnTop(true);
    }
    Disposer.register(this, () -> frame.removeWindowListener(focusListener));
    if (myCloseOnEsc) addCloseOnEsc((RootPaneContainer)frame);
    ((RootPaneContainer)frame).getContentPane().add(myComponent, BorderLayout.CENTER);
    if (frame instanceof JFrame) {
      ((JFrame)frame).setTitle(myTitle);
    }
    else {
      ((JDialog)frame).setTitle(myTitle);
    }
    if (myImages != null) {
      // unwrap the image before setting as frame's icon
      frame.setIconImages(ContainerUtil.map(myImages, ImageUtil::toBufferedImage));
    }
    else {
      getPeerFactory().updateWindowIcon(myFrame, StyleManager.get().getCurrentStyle().isDark());
    }

    WindowState state = myDimensionKey == null ? null : getWindowStateService(myProject).getState(myDimensionKey, frame);
    if (restoreBounds) {
      loadFrameState(state);
    }

    myFocusWatcher = new FocusWatcher();
    myFocusWatcher.install(myComponent);
    myShown = true;
    frame.setVisible(true);
  }

  public void close() {
    Disposer.dispose(this);
  }

  public StatusBar getStatusBar() {
    return myStatusBar;
  }

  @Override
  public void dispose() {
    if (isDisposed()) return;

    Window frame = myFrame;
    StatusBar statusBar = myStatusBar;

    myFrame = null;
    myPreferredFocus = null;
    myProject = null;
    myDataMap.clear();
    if (myComponent != null && myFocusWatcher != null) {
      myFocusWatcher.deinstall(myComponent);
    }
    myFocusWatcher = null;
    myComponent = null;
    myImages = null;
    myDisposed = true;

    if (statusBar != null) {
      Disposer.dispose(statusBar);
    }

    if (frame != null) {
      frame.setVisible(false);

      JRootPane rootPane = ((RootPaneContainer)frame).getRootPane();
      frame.removeAll();
      DialogWrapper.cleanupRootPane(rootPane);

      consulo.ui.Window uiWindow = TargetAWT.from(frame);
      IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
      MouseGestureManager.getInstance().remove(ideFrame);

      frame.dispose();

      DialogWrapper.cleanupWindowListeners(frame);
    }
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  private void addCloseOnEsc(final RootPaneContainer frame) {
    JRootPane rootPane = frame.getRootPane();
    ActionListener closeAction = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (!InternalPopupUtil.handleEscKeyEvent()) {
          // if you remove this line problems will start happen on Mac OS X
          // 2 projects opened, call Cmd+D on the second opened project and then Esc.
          // Weird situation: 2nd IdeFrame will be active, but focus will be somewhere inside the 1st IdeFrame
          // App is unusable until Cmd+Tab, Cmd+tab
          FrameWrapper.this.myFrame.setVisible(false);
          close();
        }
      }
    };
    rootPane.registerKeyboardAction(closeAction, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    ActionUtil.registerForEveryKeyboardShortcut(rootPane, closeAction, CommonShortcuts.getCloseActiveWindow());
  }

  public Window getFrame() {
    assert !myDisposed : "Already disposed!";

    if (myFrame == null) {
      final IdeFrame parent = WindowManager.getInstance().getIdeFrame(myProject);
      myFrame = myIsDialog ? createJDialog(parent) : createJFrame(parent);
    }
    return myFrame;
  }

  private FrameWrapperPeerFactory getPeerFactory() {
    return Application.get().getInstance(FrameWrapperPeerFactory.class);
  }

  protected JFrame createJFrame(IdeFrame parent) {
    return getPeerFactory().createJFrame(this, parent);
  }

  protected JDialog createJDialog(IdeFrame parent) {
    return getPeerFactory().createJDialog(this, parent);
  }

  public IdeRootPaneNorthExtension getNorthExtension(String key) {
    return null;
  }

  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (Project.KEY == dataId) {
      return myProject;
    }
    return null;
  }

  @Nullable
  public Object getDataInner(Key<?> dataId) {
    Object data = getData(dataId);
    return data != null ? data : myDataMap.get(dataId);
  }

  public void setComponent(JComponent component) {
    myComponent = component;
  }

  public void setPreferredFocusedComponent(JComponent preferedFocus) {
    myPreferredFocus = preferedFocus;
  }

  public JComponent getPreferredFocusedComponent() {
    return myPreferredFocus;
  }

  public void closeOnEsc() {
    myCloseOnEsc = true;
  }

  public void setImage(Image image) {
    setImages(image != null ? Collections.singletonList(image) : Collections.emptyList());
  }

  public void setImages(List<Image> images) {
    myImages = images;
  }

  protected void loadFrameState(@Nullable WindowState state) {
    final Window frame = getFrame();
    if(state != null) {
      state.applyTo(frame);
    }
    else {
      final IdeFrame ideFrame = WindowManager.getInstance().getIdeFrame(myProject);
      if (ideFrame != null) {
        frame.setBounds(TargetAWT.to(ideFrame.suggestChildFrameBounds()));
      }
    }
    ((RootPaneContainer)frame).getRootPane().revalidate();
  }

  public void setTitle(String title) {
    myTitle = title;
  }

  public void addDisposable(@Nonnull Disposable disposable) {
    Disposer.register(this, disposable);
  }

  protected void setStatusBar(StatusBar statusBar) {
    if (myStatusBar != null) {
      Disposer.dispose(myStatusBar);
    }
    myStatusBar = statusBar;
  }

  public void setLocation(Point location) {
    getFrame().setLocation(location);
  }

  public void setSize(Dimension size) {
    getFrame().setSize(size);
  }

  @Nonnull
  private static WindowStateService getWindowStateService(@Nullable Project project) {
    return project == null ? ApplicationWindowStateService.getInstance() : ProjectWindowStateService.getInstance(project);
  }

  private class MyProjectManagerListener implements ProjectManagerListener {
    @Override
    public void projectClosing(Project project) {
      if (project == myProject) {
        close();
      }
    }
  }
}
