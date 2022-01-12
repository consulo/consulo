/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.swt.wm.impl;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManagerListener;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.ToolWindowLayout;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.wm.impl.UnifiedWindowManagerImpl;
import jakarta.inject.Singleton;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 29/04/2021
 */
@Singleton
@State(name = WindowManagerEx.ID, storages = @Storage(value = "window.manager.xml", roamingType = RoamingType.DISABLED), defaultStateFilePath = "/defaultState/WindowManager.xml")
public class DesktopSwtWindowManagerImpl extends UnifiedWindowManagerImpl implements PersistentStateComponent<Element> {
  private final Map<Project, DesktopSwtIdeFrameImpl> myProject2Frame = new HashMap<>();

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
  public consulo.ui.Window findVisibleWindow() {
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

    DesktopSwtIdeFrameImpl frame = new DesktopSwtIdeFrameImpl(project);
    myProject2Frame.put(project, frame);

    return frame;
  }

  @Override
  public void releaseFrame(IdeFrameEx frame) {
    DesktopSwtIdeFrameImpl ideFrame = myProject2Frame.remove(frame.getProject());
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