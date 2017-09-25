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
package consulo.web.wm.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.DesktopLayout;
import com.intellij.util.messages.MessageBusConnection;
import consulo.ui.ex.WGwtToolWindowPanel;
import consulo.web.application.WebApplication;
import consulo.wm.impl.ToolWindowManagerBase;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.util.List;

/**
 * @author VISTALL
 * @since 24-Sep-17
 */
@State(name = "ToolWindowManager", storages = @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED))
public class WebToolWindowManagerImpl extends ToolWindowManagerBase {
  private WindowManagerEx myWindowManager;

  private IdeFrameEx myFrame;

  private WGwtToolWindowPanel myToolWindowPanel;

  public WebToolWindowManagerImpl(WindowManagerEx windowManager, Project project) {
    super(project);
    myWindowManager = windowManager;
    if (project.isDefault()) {
      return;
    }

    MessageBusConnection busConnection = project.getMessageBus().connect();
    busConnection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
      @Override
      public void projectOpened(Project project) {
        if (project == myProject) {
          WebToolWindowManagerImpl.this.projectOpened();
        }
      }

      @Override
      public void projectClosed(Project project) {
        if (project == myProject) {
          WebToolWindowManagerImpl.this.projectClosed();
        }
      }
    });
  }

  private void projectOpened() {
    WebApplication.invokeOnCurrentSession(() -> {
      myFrame = myWindowManager.allocateFrame(myProject);

      myToolWindowPanel = new WGwtToolWindowPanel();

      // TODO [VISTALL]  IdeRootPane
      myFrame.getWindow().setContent(myToolWindowPanel);
    });
  }

  private void projectClosed() {
    myWindowManager.releaseFrame(myFrame);

    myFrame = null;
  }

  @Override
  public void initToolWindow(@NotNull ToolWindowEP bean) {

  }

  @Override
  public void addToolWindowManagerListener(@NotNull ToolWindowManagerListener l) {

  }

  @Override
  public void addToolWindowManagerListener(@NotNull ToolWindowManagerListener l, @NotNull Disposable parentDisposable) {

  }

  @Override
  public void removeToolWindowManagerListener(@NotNull ToolWindowManagerListener l) {

  }

  @Nullable
  @Override
  public String getLastActiveToolWindowId() {
    return null;
  }

  @Nullable
  @Override
  public String getLastActiveToolWindowId(@Nullable Condition<JComponent> condition) {
    return null;
  }

  @Override
  public DesktopLayout getLayout() {
    return null;
  }

  @Override
  public void setLayoutToRestoreLater(DesktopLayout layout) {

  }

  @Override
  public DesktopLayout getLayoutToRestoreLater() {
    return null;
  }

  @Override
  public void setLayout(@NotNull DesktopLayout layout) {

  }

  @Override
  public void clearSideStack() {

  }

  @Override
  public void hideToolWindow(@NotNull String id, boolean hideSide) {

  }

  @Override
  public List<String> getIdsOn(@NotNull ToolWindowAnchor anchor) {
    return null;
  }

  @Override
  public void dispose() {

  }

  @Nullable
  @Override
  public Element getState() {
    return new Element("state");
  }

  @Override
  public void loadState(Element state) {

  }

  @Override
  public boolean canShowNotification(@NotNull String toolWindowId) {
    return false;
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id, @NotNull JComponent component, @NotNull ToolWindowAnchor anchor) {
    return null;
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id, @NotNull JComponent component, @NotNull ToolWindowAnchor anchor, @NotNull Disposable parentDisposable) {
    return null;
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id, @NotNull JComponent component, @NotNull ToolWindowAnchor anchor, Disposable parentDisposable, boolean canWorkInDumbMode) {
    return null;
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id,
                                       @NotNull JComponent component,
                                       @NotNull ToolWindowAnchor anchor,
                                       Disposable parentDisposable,
                                       boolean canWorkInDumbMode,
                                       boolean canCloseContents) {
    return null;
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id, boolean canCloseContent, @NotNull ToolWindowAnchor anchor) {
    return null;
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id, boolean canCloseContent, @NotNull ToolWindowAnchor anchor, boolean secondary) {
    return null;
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id, boolean canCloseContent, @NotNull ToolWindowAnchor anchor, Disposable parentDisposable, boolean canWorkInDumbMode) {
    return null;
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id, boolean canCloseContent, @NotNull ToolWindowAnchor anchor, Disposable parentDisposable, boolean canWorkInDumbMode, boolean secondary) {
    return null;
  }

  @Override
  public void unregisterToolWindow(@NotNull String id) {

  }

  @Override
  public void activateEditorComponent() {

  }

  @Override
  public boolean isEditorComponentActive() {
    return false;
  }

  @NotNull
  @Override
  public String[] getToolWindowIds() {
    return new String[0];
  }

  @Nullable
  @Override
  public String getActiveToolWindowId() {
    return null;
  }

  @Override
  public ToolWindow getToolWindow(String id) {
    return null;
  }

  @Override
  public void invokeLater(@NotNull Runnable runnable) {

  }

  @NotNull
  @Override
  public IdeFocusManager getFocusManager() {
    return null;
  }

  @Override
  public void notifyByBalloon(@NotNull String toolWindowId, @NotNull MessageType type, @NotNull String htmlBody) {

  }

  @Override
  public void notifyByBalloon(@NotNull String toolWindowId, @NotNull MessageType type, @NotNull String htmlBody, @Nullable Icon icon, @Nullable HyperlinkListener listener) {

  }

  @Nullable
  @Override
  public Balloon getToolWindowBalloon(String id) {
    return null;
  }

  @Override
  public boolean isMaximized(@NotNull ToolWindow wnd) {
    return false;
  }

  @Override
  public void setMaximized(@NotNull ToolWindow wnd, boolean maximized) {

  }

  @Override
  public boolean isToolWindowRegistered(String id) {
    return false;
  }
}
