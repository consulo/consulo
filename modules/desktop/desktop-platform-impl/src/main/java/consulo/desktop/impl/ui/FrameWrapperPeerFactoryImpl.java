/*
 * Copyright 2013-2019 consulo.io
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
package consulo.desktop.impl.ui;

import com.intellij.ide.DataManager;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.impl.MouseGestureManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.FrameWrapper;
import com.intellij.openapi.ui.FrameWrapperPeerFactory;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.IdeRootPaneNorthExtension;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.ex.LayoutFocusTraversalPolicyExt;
import com.intellij.openapi.wm.impl.DesktopIdeFrameImpl;
import com.intellij.openapi.wm.impl.IdeGlassPaneImpl;
import com.intellij.openapi.wm.impl.IdeMenuBar;
import com.intellij.ui.BalloonLayout;
import com.intellij.ui.FrameState;
import com.intellij.util.ui.UIUtil;
import consulo.ui.Window;
import consulo.ui.desktop.internal.window.JDialogAsUIWindow;
import consulo.ui.desktop.internal.window.JFrameAsUIWindow;
import consulo.ui.Rectangle2D;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * @author VISTALL
 * @since 2019-02-15
 */
@Singleton
public class FrameWrapperPeerFactoryImpl implements FrameWrapperPeerFactory {
  private static class MyJFrame extends JFrameAsUIWindow implements IdeFrame.Child, IdeFrameEx {

    private FrameWrapper myOwner;
    private final IdeFrame myParent;

    private String myFrameTitle;
    private String myFileTitle;
    private File myFile;
    private boolean myDisposing;

    private MyJFrame(FrameWrapper owner, IdeFrame parent) throws HeadlessException {
      myOwner = owner;
      myParent = parent;

      toUIWindow().putUserData(IdeFrame.KEY, this);
      toUIWindow().addUserDataProvider(this::getData);

      FrameState.setFrameStateListener(this);
      setGlassPane(new IdeGlassPaneImpl(getRootPane(), true));

      boolean setMenuOnFrame = SystemInfo.isMac;

      if (SystemInfo.isLinux) {
        final String desktop = System.getenv("XDG_CURRENT_DESKTOP");
        if ("Unity".equals(desktop) || "Unity:Unity7".equals(desktop)) {
          try {
            Class.forName("com.jarego.jayatana.Agent");
            setMenuOnFrame = true;
          }
          catch (ClassNotFoundException e) {
            // ignore
          }
        }
      }

      if (setMenuOnFrame) {
        setJMenuBar(new IdeMenuBar(ActionManagerEx.getInstanceEx(), DataManager.getInstance()));
      }

      MouseGestureManager.getInstance().add(this);
      setFocusTraversalPolicy(new LayoutFocusTraversalPolicyExt());
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    @Nonnull
    @Override
    public Window getWindow() {
      return toUIWindow();
    }

    @Override
    public JComponent getComponent() {
      return getRootPane();
    }

    @Override
    public StatusBar getStatusBar() {
      StatusBar ownerBar = myOwner != null ? myOwner.getStatusBar() : null;
      return ownerBar != null ? ownerBar : myParent != null ? myParent.getStatusBar() : null;
    }

    @Override
    public Rectangle2D suggestChildFrameBounds() {
      return myParent.suggestChildFrameBounds();
    }

    @Override
    public Project getProject() {
      return myParent.getProject();
    }

    @Override
    public void setFrameTitle(String title) {
      myFrameTitle = title;
      updateTitle();
    }

    @Override
    public void setFileTitle(String fileTitle, File ioFile) {
      myFileTitle = fileTitle;
      myFile = ioFile;
      updateTitle();
    }

    @Override
    public IdeRootPaneNorthExtension getNorthExtension(String key) {
      return myOwner.getNorthExtension(key);
    }

    @Override
    public BalloonLayout getBalloonLayout() {
      return null;
    }

    private void updateTitle() {
      DesktopIdeFrameImpl.updateTitle(this, myFrameTitle, myFileTitle, myFile);
    }

    @Override
    public IdeFrame getParentFrame() {
      return myParent;
    }

    @Override
    public void dispose() {
      FrameWrapper owner = myOwner;
      myOwner = null;
      if (owner == null || myDisposing) return;
      myDisposing = true;
      Disposer.dispose(owner);
      super.dispose();
      rootPane = null;
      setMenuBar((MenuBar)null);
    }

    private Object getData(@Nonnull Key<?> dataId) {
      if (IdeFrame.KEY == dataId) {
        return this;
      }
      return myOwner == null ? null : myOwner.getDataInner(dataId);
    }

    @Override
    public void paint(Graphics g) {
      UISettings.setupAntialiasing(g);
      super.paint(g);
    }
  }

  private static class MyJDialog extends JDialogAsUIWindow implements IdeFrame.Child, IdeFrameEx {

    private FrameWrapper myOwner;
    private final IdeFrame myParent;
    private boolean myDisposing;

    private MyJDialog(FrameWrapper owner, @Nullable IdeFrame parent) throws HeadlessException {
      super(parent == null ? null : parent.getWindow(), null);
      myOwner = owner;
      myParent = parent;
      setGlassPane(new IdeGlassPaneImpl(getRootPane()));
      getRootPane().putClientProperty("Window.style", "small");
      setBackground(UIUtil.getPanelBackground());
      MouseGestureManager.getInstance().add(this);
      setFocusTraversalPolicy(new LayoutFocusTraversalPolicyExt());
      setDefaultCloseOperation(DISPOSE_ON_CLOSE);

      toUIWindow().putUserData(IdeFrame.KEY, this);
      toUIWindow().addUserDataProvider(this::getData);
    }

    private Object getData(@Nonnull Key<?> dataId) {
      if (IdeFrame.KEY == dataId) {
        return this;
      }
      return myOwner == null ? null : myOwner.getDataInner(dataId);
    }

    @Nonnull
    @Override
    public consulo.ui.Window getWindow() {
      return toUIWindow();
    }

    @Override
    public JComponent getComponent() {
      return getRootPane();
    }

    @Override
    public StatusBar getStatusBar() {
      return null;
    }

    @Nullable
    @Override
    public BalloonLayout getBalloonLayout() {
      return null;
    }

    @Override
    public Rectangle2D suggestChildFrameBounds() {
      return myParent.suggestChildFrameBounds();
    }

    @Override
    public Project getProject() {
      return myParent.getProject();
    }

    @Override
    public void setFrameTitle(String title) {
      setTitle(title);
    }

    @Override
    public void setFileTitle(String fileTitle, File ioFile) {
      setTitle(fileTitle);
    }

    @Override
    public IdeRootPaneNorthExtension getNorthExtension(String key) {
      return null;
    }

    @Override
    public IdeFrame getParentFrame() {
      return myParent;
    }

    @Override
    public void dispose() {
      FrameWrapper owner = myOwner;
      myOwner = null;
      if (owner == null || myDisposing) return;
      myDisposing = true;
      Disposer.dispose(owner);
      super.dispose();
      rootPane = null;
    }

    @Override
    public void paint(Graphics g) {
      UISettings.setupAntialiasing(g);
      super.paint(g);
    }
  }

  @Override
  public JFrame createJFrame(FrameWrapper owner, IdeFrame parent) {
    return new MyJFrame(owner, parent);
  }

  @Override
  public JDialog createJDialog(FrameWrapper owner, IdeFrame parent) {
    return new MyJDialog(owner, parent);
  }
}
