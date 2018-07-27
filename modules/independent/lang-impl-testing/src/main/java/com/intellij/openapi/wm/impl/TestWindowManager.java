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
package com.intellij.openapi.wm.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.TaskInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.BalloonHandler;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.WindowManagerListener;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.openapi.wm.ex.StatusBarEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import consulo.ui.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.util.Collections;
import java.util.List;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class TestWindowManager extends WindowManagerEx {
  private static final StatusBarEx ourStatusBar = new DummyStatusBar();

  public final void doNotSuggestAsParent(final Window window) {
  }

  @Override
  public StatusBar getStatusBar(@Nonnull Component c, @javax.annotation.Nullable Project project) {
    return null;
  }

  @Override
  public StatusBar getStatusBar(@Nonnull Component c) {
    return null;
  }

  @Override
  public final Window suggestParentWindow(@javax.annotation.Nullable final Project project) {
    return null;
  }

  @Override
  public final StatusBar getStatusBar(final Project project) {
    return ourStatusBar;
  }

  @Override
  public IdeFrameEx getIdeFrame(final Project project) {
    return null;
  }

  @Override
  public Rectangle getScreenBounds(@Nonnull Project project) {
    return null;
  }

  @Override
  public void setWindowMask(final Window window, final Shape mask) {
  }

  @Override
  public void resetWindow(final Window window) {
  }

  private static final class DummyStatusBar implements StatusBarEx {
    @Override
    public Dimension getSize() {
      return new Dimension(0, 0);
    }

    @Override
    public StatusBar createChild() {
      return null;
    }

    @Override
    public IdeFrame getFrame() {
      return null;
    }

    @Override
    public StatusBar findChild(Component c) {
      return null;
    }

    @Override
    public void install(IdeFrame frame) {
    }

    @Override
    public void setInfo(@Nullable String s, @javax.annotation.Nullable String requestor) {
    }

    @Override
    public String getInfoRequestor() {
      return null;
    }

    @Override
    public boolean isVisible() {
      return false;
    }

    @Override
    public final void setInfo(final String s) {
    }

    @Override
    public void addCustomIndicationComponent(@Nonnull JComponent c) {
    }

    @Override
    public void removeCustomIndicationComponent(@Nonnull JComponent c) {
    }

    @Override
    public void addProgress(@Nonnull ProgressIndicatorEx indicator, @Nonnull TaskInfo info) {
    }

    @Override
    public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
      return Collections.emptyList();
    }

    @Override
    public void addWidget(@Nonnull StatusBarWidget widget, @Nonnull Disposable parentDisposable) {
      Disposer.register(parentDisposable, widget);
    }

    @Override
    public void addWidget(@Nonnull StatusBarWidget widget, @Nonnull String anchor, @Nonnull Disposable parentDisposable) {
      Disposer.register(parentDisposable, widget);
    }

    @Override
    public void updateWidgets() {
    }

    @Override
    public void addWidget(@Nonnull StatusBarWidget widget) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void addWidget(@Nonnull StatusBarWidget widget, @Nonnull String anchor) {
    }

    @Override
    public void updateWidget(@Nonnull String id) {
    }

    @Override
    public StatusBarWidget getWidget(String id) {
      return null;
    }

    @Override
    public void removeWidget(@Nonnull String id) {
    }

    @Override
    public void fireNotificationPopup(@Nonnull JComponent content, final Color backgroundColor) {
    }

    @Override
    public JComponent getComponent() {
      return null;
    }

    @Override
    public final String getInfo() {
      return null;
    }

    @Override
    public void startRefreshIndication(final String tooltipText) {
    }

    @Override
    public void stopRefreshIndication() {
    }

    @Override
    public boolean isProcessWindowOpen() {
      return false;
    }

    @Override
    public void setProcessWindowOpen(final boolean open) {
    }

    @Override
    public void removeCustomIndicationComponents() {
    }

    @Override
    public BalloonHandler notifyProgressByBalloon(@Nonnull MessageType type, @Nonnull String htmlBody) {
      return new BalloonHandler() {
        public void hide() {
        }
      };
    }

    @Override
    public BalloonHandler notifyProgressByBalloon(@Nonnull MessageType type, @Nonnull String htmlBody, @javax.annotation.Nullable Icon icon, @javax.annotation.Nullable HyperlinkListener listener) {
      return new BalloonHandler() {
        public void hide() {
        }
      };
    }
  }

  @Override
  @Nonnull
  public IdeFrameEx[] getAllProjectFrames() {
    return new IdeFrameEx[0];
  }

  @Override
  public JFrame findVisibleFrame() {
    return null;
  }

  @Override
  public final IdeFrameImpl getFrame(final Project project) {
    return null;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public final IdeFrameEx allocateFrame(final Project project) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final void releaseFrame(final IdeFrameEx frame) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final Component getFocusedComponent(@Nonnull final Window window) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final Component getFocusedComponent(final Project project) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final Window getMostRecentFocusedWindow() {
    return null;
  }

  @Override
  public IdeFrame findFrameFor(@javax.annotation.Nullable Project project) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final CommandProcessorBase getCommandProcessor() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final ToolWindowLayout getLayout() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final void setLayout(final ToolWindowLayout layout) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final void dispatchComponentEvent(final ComponentEvent e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final Rectangle getScreenBounds() {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean isInsideScreenBounds(final int x, final int y, final int width) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean isInsideScreenBounds(final int x, final int y) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean isAlphaModeSupported() {
    return false;
  }

  @Override
  public final void setAlphaModeRatio(final Window window, final float ratio) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final boolean isAlphaModeEnabled(final Window window) {
    throw new UnsupportedOperationException();
  }

  @Override
  public final void setAlphaModeEnabled(final Window window, final boolean state) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setWindowShadow(Window window, WindowShadowMode mode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void hideDialog(JDialog dialog, Project project) {
    dialog.dispose();
  }

  @Override
  public void adjustContainerWindow(Component c, Dimension oldSize, Dimension newSize) {
  }

  @Override
  public void disposeRootFrame() {

  }

  @Override
  public void addListener(final WindowManagerListener listener) {
  }

  @Override
  public void removeListener(final WindowManagerListener listener) {
  }

  @Override
  public boolean isFullScreenSupportedInCurrentOS() {
    return false;
  }
}
