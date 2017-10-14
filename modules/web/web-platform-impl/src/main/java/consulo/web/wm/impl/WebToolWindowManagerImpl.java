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

import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.*;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.CommandProcessorBase;
import com.intellij.openapi.wm.impl.InternalDecoratorListener;
import com.intellij.openapi.wm.impl.WindowInfoImpl;
import com.intellij.openapi.wm.impl.commands.FinalizableCommand;
import com.intellij.util.messages.MessageBusConnection;
import consulo.ui.*;
import consulo.ui.ex.ToolWindowInternalDecorator;
import consulo.ui.ex.ToolWindowStripeButton;
import consulo.ui.ex.WGwtToolWindowPanel;
import consulo.ui.ex.WGwtToolWindowStripeButton;
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
@State(name = ToolWindowManagerBase.ID, storages = @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED))
public class WebToolWindowManagerImpl extends ToolWindowManagerBase {
  private IdeFrameEx myFrame;

  public WebToolWindowManagerImpl(Project project, WindowManagerEx windowManager) {
    super(project, windowManager);

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
      myFrame.getWindow().setContent((Component)myToolWindowPanel);
    });
  }

  private void projectClosed() {
    myWindowManager.releaseFrame(myFrame);

    myFrame = null;
  }

  @RequiredUIAccess
  @Override
  public void initToolWindow(@NotNull ToolWindowEP bean) {
    WindowInfoImpl before = myLayout.getInfo(bean.id, false);
    boolean visible = before != null && before.isVisible();
    Component label = createInitializingLabel();
    ToolWindowAnchor toolWindowAnchor = ToolWindowAnchor.fromText(bean.anchor);
    final ToolWindowFactory factory = bean.getToolWindowFactory();
    ToolWindow window = registerToolWindow(bean.id, label, toolWindowAnchor, false, bean.canCloseContents, DumbService.isDumbAware(factory), factory.shouldBeAvailable(myProject));
    final WebToolWindowImpl toolWindow = (WebToolWindowImpl)registerDisposable(bean.id, myProject, window);
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
      UIAccess.get().give(runnable); //TODO  UiNotifyConnector.doWhenFirstShown(label, () -> ApplicationManager.getApplication().invokeLater(runnable));
    }
  }

  @NotNull
  @Override
  protected CommandProcessorBase createCommandProcessor() {
    return new WebCommandProcessorImpl();
  }

  @NotNull
  @Override
  protected InternalDecoratorListener createInternalDecoratorListener() {
    return new MyInternalDecoratorListenerBase() {
      @Override
      public void resized(@NotNull ToolWindowInternalDecorator source) {

      }
    };
  }

  @NotNull
  @Override
  protected ToolWindowStripeButton createStripeButton(ToolWindowInternalDecorator internalDecorator) {
    return new WGwtToolWindowStripeButton((WebToolWindowInternalDecorator)internalDecorator, (WGwtToolWindowPanel)myToolWindowPanel);
  }

  @NotNull
  @Override
  protected ToolWindowEx createToolWindow(String id, boolean canCloseContent, @Nullable Object component) {
    return new WebToolWindowImpl(this, id, canCloseContent, component);
  }

  @NotNull
  @Override
  protected ToolWindowInternalDecorator createInternalDecorator(Project project, @NotNull WindowInfoImpl info, ToolWindowEx toolWindow, boolean dumbAware) {
    return new WebToolWindowInternalDecorator(project, info, (WebToolWindowImpl)toolWindow, dumbAware);
  }

  @Override
  protected void appendRequestFocusInToolWindowCmd(String id, List<FinalizableCommand> commandList, boolean forced) {

  }

  @Override
  protected void appendRemoveWindowedDecoratorCmd(WindowInfoImpl info, List<FinalizableCommand> commandsList) {

  }

  @Override
  protected void appendAddFloatingDecorator(ToolWindowInternalDecorator internalDecorator, List<FinalizableCommand> commandList, WindowInfoImpl toBeShownInfo) {

  }

  @Override
  protected void appendAddWindowedDecorator(ToolWindowInternalDecorator internalDecorator, List<FinalizableCommand> commandList, WindowInfoImpl toBeShownInfo) {

  }

  @Override
  protected void appendUpdateToolWindowsPaneCmd(List<FinalizableCommand> commandsList) {

  }

  @Override
  protected void activateEditorComponentImpl(List<FinalizableCommand> commandList, boolean forced) {

  }

  @Override
  protected boolean hasModalChild(WindowInfoImpl info) {
    return false;
  }

  @NotNull
  @RequiredUIAccess
  private static consulo.ui.Component createInitializingLabel() {
    Label label = Components.label("Initializing...");
    DockLayout dock = Layouts.dock();
    dock.center(label);
    return label;
  }

  @Nullable
  @Override
  public Element getState() {
    return new Element("state");
  }

  @Override
  public boolean canShowNotification(@NotNull String toolWindowId) {
    return false;
  }


  @Override
  public void activateEditorComponent() {

  }

  @Override
  public boolean isEditorComponentActive() {
    return false;
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
}
