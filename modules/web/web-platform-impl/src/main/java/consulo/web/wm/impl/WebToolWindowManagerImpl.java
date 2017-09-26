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
import com.intellij.openapi.diagnostic.Logger;
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
import com.intellij.openapi.wm.impl.ToolWindowLayout;
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
  private static final Logger LOG = Logger.getInstance(WebToolWindowManagerImpl.class);

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
   /* WindowInfoImpl before = myLayout.getInfo(bean.id, false);
    boolean visible = before != null && before.isVisible();
    JLabel label = createInitializingLabel();
    ToolWindowAnchor toolWindowAnchor = ToolWindowAnchor.fromText(bean.anchor);
    final ToolWindowFactory factory = bean.getToolWindowFactory();
    ToolWindow window = registerToolWindow(bean.id, label, toolWindowAnchor, false, bean.canCloseContents, DumbService.isDumbAware(factory), factory.shouldBeAvailable(myProject));
    final ToolWindowImpl toolWindow = (ToolWindowImpl)registerDisposable(bean.id, myProject, window);
    toolWindow.setContentFactory(factory);
    if (bean.icon != null && toolWindow.getIcon() == null) {
      Icon icon = IconLoader.findIcon(bean.icon, factory.getClass());
      if (icon == null) {
        try {
          icon = IconLoader.getIcon(bean.icon);
        }
        catch (Exception ignored) {
        }
      }
      toolWindow.setIcon(icon);
    }

    WindowInfoImpl info = getInfo(bean.id);
    if (!info.isSplit() && bean.secondary && !info.wasRead()) {
      toolWindow.setSplitMode(true, null);
    }

    // ToolWindow activation is not needed anymore and should be removed in 2017
    toolWindow.setActivation(new ActionCallback()).setDone();
    final DumbAwareRunnable runnable = () -> {
      if (toolWindow.isDisposed()) return;

      toolWindow.ensureContentInitialized();
    };
    if (visible) {
      runnable.run();
    }
    else {
      UiNotifyConnector.doWhenFirstShown(label, () -> ApplicationManager.getApplication().invokeLater(runnable));
    } */
  }

  /*@NotNull
  private ToolWindow registerToolWindow(@NotNull final String id,
                                        @Nullable final JComponent component,
                                        @NotNull final ToolWindowAnchor anchor,
                                        boolean sideTool,
                                        boolean canCloseContent,
                                        final boolean canWorkInDumbMode,
                                        boolean shouldBeAvailable) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("enter: installToolWindow(" + id + "," + component + "," + anchor + "\")");
    }
    ApplicationManager.getApplication().assertIsDispatchThread();
    boolean known = myLayout.isToolWindowUnregistered(id);
    if (myLayout.isToolWindowRegistered(id)) {
      throw new IllegalArgumentException("window with id=\"" + id + "\" is already registered");
    }

    final WindowInfoImpl info = myLayout.register(id, anchor, sideTool);
    final boolean wasActive = info.isActive();
    final boolean wasVisible = info.isVisible();
    info.setActive(false);
    info.setVisible(false);
    if (!known) {
      info.setShowStripeButton(shouldBeAvailable);
    }

    // Create decorator

    WebToolWindowImpl toolWindow = new WebToolWindowImpl(this, id, canCloseContent, component);
    InternalDecorator decorator = new InternalDecorator(myProject, info.copy(), toolWindow, canWorkInDumbMode);
    ActivateToolWindowAction.ensureToolWindowActionRegistered(toolWindow);
    myId2InternalDecorator.put(id, decorator);
    decorator.addInternalDecoratorListener(myInternalDecoratorListener);
    toolWindow.addPropertyChangeListener(myToolWindowPropertyChangeListener);
    myId2FocusWatcher.put(id, new ToolWindowFocusWatcher(toolWindow));

    // Create and show tool button

    final StripeButton button = new StripeButton(decorator, myToolWindowsPane);
    myId2StripeButton.put(id, button);
    List<FinalizableCommand> commandsList = new ArrayList<>();
    appendAddButtonCmd(button, info, commandsList);

    // If preloaded info is visible or active then we have to show/activate the installed
    // tool window. This step has sense only for windows which are not in the auto hide
    // mode. But if tool window was active but its mode doesn't allow to activate it again
    // (for example, tool window is in auto hide mode) then we just activate editor component.

    if (!info.isAutoHide() && (info.isDocked() || info.isFloating())) {
      if (wasActive) {
        activateToolWindowImpl(info.getId(), commandsList, true, true);
      }
      else if (wasVisible) {
        showToolWindowImpl(info.getId(), false, commandsList);
      }
    }
    else if (wasActive) { // tool window was active but it cannot be activate again
      activateEditorComponentImpl(commandsList, true);
    }

    execute(commandsList);
    fireToolWindowRegistered(id);
    return toolWindow;
  }
            */
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
  public ToolWindowLayout getLayout() {
    return null;
  }

  @Override
  public void setLayoutToRestoreLater(ToolWindowLayout layout) {

  }

  @Override
  public ToolWindowLayout getLayoutToRestoreLater() {
    return null;
  }

  @Override
  public void setLayout(@NotNull ToolWindowLayout layout) {

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
