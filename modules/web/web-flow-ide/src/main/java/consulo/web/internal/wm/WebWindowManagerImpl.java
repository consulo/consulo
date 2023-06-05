/*
 * Copyright 2013-2017 consulo.io
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
package consulo.web.internal.wm;

import consulo.annotation.component.ServiceImpl;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.event.WindowManagerListener;
import consulo.project.ui.internal.IdeFrameEx;
import consulo.project.ui.internal.WindowManagerEx;
import consulo.project.ui.internal.ToolWindowLayout;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ide.impl.wm.impl.UnifiedWindowManagerImpl;
import jakarta.inject.Singleton;
import org.jdom.Element;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 24-Sep-17
 */
@Singleton
@ServiceImpl
@State(name = WindowManagerEx.ID, storages = @Storage(value = "window.manager.xml", roamingType = RoamingType.DISABLED), defaultStateFilePath = "/defaultState/WindowManager.xml")
public class WebWindowManagerImpl extends UnifiedWindowManagerImpl implements PersistentStateComponent<Element> {
  private final Map<Project, WebIdeFrameImpl> myProject2Frame = new HashMap<>();

  @Override
  public boolean isAlphaModeSupported() {
    return false;
  }

  @Override
  public void setAlphaModeRatio(Window window, float ratio) {

  }

  @Override
  public boolean isAlphaModeEnabled(Window window) {
    return false;
  }

  @Override
  public void setAlphaModeEnabled(Window window, boolean state) {

  }

  @Nullable
  @Override
  public consulo.ui.Window suggestParentWindow(@Nullable Project project) {
    return null;
  }

  @Nullable
  @Override
  public StatusBar getStatusBar(Project project) {
    IdeFrameEx ideFrame = getIdeFrame(project);
    return ideFrame == null ? null : ideFrame.getStatusBar();
  }

  @Override
  public StatusBar getStatusBar(@Nonnull Component c) {
    return null;
  }

  @Override
  public StatusBar getStatusBar(@Nonnull consulo.ui.Component c, @Nullable Project project) {
    if(project == null) {
      return null;
    }
    return getStatusBar(project);
  }

  @Override
  public IdeFrameEx getIdeFrame(@Nullable Project project) {
    return myProject2Frame.get(project);
  }

  @Override
  public boolean isInsideScreenBounds(int x, int y, int width) {
    return false;
  }

  @Override
  public boolean isInsideScreenBounds(int x, int y) {
    return false;
  }

  @Nonnull
  @Override
  public IdeFrame[] getAllProjectFrames() {
    return new IdeFrame[0];
  }

  @Nullable
  @Override
  public IdeFrame findVisibleIdeFrame() {
    return null;
  }

  @Override
  public void addListener(WindowManagerListener listener) {

  }

  @Override
  public void removeListener(WindowManagerListener listener) {

  }

  @Override
  public boolean isFullScreenSupportedInCurrentOS() {
    return false;
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public IdeFrameEx allocateFrame(@Nonnull Project project) {
    UIAccess.assertIsUIThread();

    WebIdeFrameImpl frame = new WebIdeFrameImpl(project);
    myProject2Frame.put(project, frame);

    return frame;
  }

  @Override
  public void releaseFrame(IdeFrameEx frame) {
    WebIdeFrameImpl ideFrame = myProject2Frame.remove(frame.getProject());
    assert ideFrame == frame;

    ideFrame.close();
  }

  @Override
  public Component getFocusedComponent(@Nonnull Window window) {
    return null;
  }

  @Nullable
  @Override
  public Component getFocusedComponent(@Nullable Project project) {
    return null;
  }

  @Override
  public consulo.ui.Window getMostRecentFocusedWindow() {
    return null;
  }

  @RequiredUIAccess
  @Override
  public IdeFrame findFrameFor(@Nullable Project project) {
    return null;
  }

  @Override
  public ToolWindowLayout getLayout() {
    return null;
  }

  @Override
  public void setLayout(ToolWindowLayout layout) {

  }

  @Override
  public void dispatchComponentEvent(ComponentEvent e) {

  }

  @Override
  public Rectangle getScreenBounds() {
    return null;
  }

  @Override
  public Rectangle getScreenBounds(@Nonnull Project project) {
    return null;
  }

  @Override
  public void setWindowMask(Window window, Shape mask) {

  }

  @Override
  public void setWindowShadow(Window window, WindowShadowMode mode) {

  }

  @Override
  public void resetWindow(Window window) {

  }

  @Override
  public void hideDialog(JDialog dialog, Project project) {

  }

  @Override
  public void adjustContainerWindow(Component c, Dimension oldSize, Dimension newSize) {

  }

  @Override
  public void disposeRootFrame() {

  }

  @Nullable
  @Override
  public Element getState() {
    return new Element("state");
  }

  @Override
  public void loadState(Element state) {

  }
}
