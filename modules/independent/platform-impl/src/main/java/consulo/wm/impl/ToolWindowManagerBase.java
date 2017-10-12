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
package consulo.wm.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.openapi.wm.ex.ToolWindowManagerListener;
import com.intellij.openapi.wm.impl.CommandProcessor;
import com.intellij.openapi.wm.impl.ToolWindowImpl;
import com.intellij.openapi.wm.impl.ToolWindowLayout;
import com.intellij.openapi.wm.impl.WindowInfoImpl;
import com.intellij.openapi.wm.impl.commands.FinalizableCommand;
import com.intellij.openapi.wm.impl.commands.InvokeLaterCmd;
import com.intellij.util.ArrayUtil;
import com.intellij.util.EventDispatcher;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.condition.ModuleExtensionCondition;
import consulo.ui.RequiredUIAccess;
import consulo.ui.UIAccess;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public abstract class ToolWindowManagerBase extends ToolWindowManagerEx implements PersistentStateComponent<Element>, Disposable {
  public static class InitToolWindowsActivity implements StartupActivity, DumbAware {
    @Override
    public void runActivity(@NotNull UIAccess uiAccess, @NotNull Project project) {
      ToolWindowManagerEx ex = ToolWindowManagerEx.getInstanceEx(project);
      if (ex instanceof ToolWindowManagerBase) {
        ToolWindowManagerBase manager = (ToolWindowManagerBase)ex;
        List<FinalizableCommand> list = new ArrayList<>();
        manager.registerToolWindowsFromBeans(list);
        manager.initAll(list);

        uiAccess.give(() -> {
          manager.execute(list);
          manager.flushCommands();
        });
      }
    }
  }

  @NonNls
  protected static final String EDITOR_ELEMENT = "editor";
  @NonNls
  protected static final String ACTIVE_ATTR_VALUE = "active";
  @NonNls
  protected static final String FRAME_ELEMENT = "frame";
  @NonNls
  protected static final String X_ATTR = "x";
  @NonNls
  protected static final String Y_ATTR = "y";
  @NonNls
  protected static final String WIDTH_ATTR = "width";
  @NonNls
  protected static final String HEIGHT_ATTR = "height";
  @NonNls
  protected static final String EXTENDED_STATE_ATTR = "extended-state";
  @NonNls
  protected static final String LAYOUT_TO_RESTORE = "layout-to-restore";

  protected final Project myProject;
  protected final EventDispatcher<ToolWindowManagerListener> myDispatcher = EventDispatcher.create(ToolWindowManagerListener.class);
  protected final CommandProcessor myCommandProcessor = new CommandProcessor();
  protected final ToolWindowLayout myLayout = new ToolWindowLayout();
  protected ToolWindowLayout myLayoutToRestoreLater = null;
  protected boolean myEditorWasActive;

  protected ToolWindowManagerBase(Project project) {
    myProject = project;
  }

  protected void initAll(List<FinalizableCommand> commandsList) {
  }

  /**
   * This is helper method. It delegated its functionality to the WindowManager.
   * Before delegating it fires state changed.
   */
  public void execute(@NotNull List<FinalizableCommand> commandList) {
    for (FinalizableCommand each : commandList) {
      if (each.willChangeState()) {
        fireStateChanged();
        break;
      }
    }

    for (FinalizableCommand each : commandList) {
      each.beforeExecute(this);
    }
    myCommandProcessor.execute(commandList, myProject.getDisposed());
  }

  protected void flushCommands() {
    myCommandProcessor.flush();
  }

  protected void registerToolWindowsFromBeans(List<FinalizableCommand> list) {
    final ToolWindowEP[] beans = Extensions.getExtensions(ToolWindowEP.EP_NAME);
    for (final ToolWindowEP bean : beans) {
      if (checkCondition(myProject, bean)) {
        list.add(new FinalizableCommand(EmptyRunnable.INSTANCE) {
          @Override
          public void run() {
            initToolWindow(bean);
          }
        });
      }
    }

    myProject.getMessageBus().connect().subscribe(ModuleExtension.CHANGE_TOPIC, (oldExtension, newExtension) -> {
      boolean extensionVal = newExtension.isEnabled();
      for (final ToolWindowEP bean : beans) {
        boolean value = checkCondition(newExtension, bean);

        if (extensionVal && value) {
          if (isToolWindowRegistered(bean.id)) {
            continue;
          }
          initToolWindow(bean);
        }
        else if (!extensionVal && !value) {
          unregisterToolWindow(bean.id);
        }
      }
    });
  }

  public boolean isToolWindowRegistered(String id) {
    return myLayout.isToolWindowRegistered(id);
  }

  /**
   * Checkes whether the specified <code>id</code> defines installed tool
   * window. If it's not then throws <code>IllegalStateException</code>.
   *
   * @throws IllegalStateException if tool window isn't installed.
   */
  protected void checkId(final String id) {
    if (!myLayout.isToolWindowRegistered(id)) {
      throw new IllegalStateException("window with id=\"" + id + "\" isn't registered");
    }
  }

  @NotNull
  protected ToolWindow registerDisposable(@NotNull final String id, @NotNull final Disposable parentDisposable, @NotNull ToolWindow window) {
    Disposer.register(parentDisposable, () -> unregisterToolWindow(id));
    return window;
  }

  private static boolean checkCondition(Project project, ToolWindowEP toolWindowEP) {
    Condition<Project> condition = toolWindowEP.getCondition();
    if (condition != null && !condition.value(project)) {
      return false;
    }
    ModuleExtensionCondition moduleExtensionCondition = toolWindowEP.getModuleExtensionCondition();
    return moduleExtensionCondition.value(project);
  }

  private static boolean checkCondition(ModuleExtension<?> extension, ToolWindowEP toolWindowEP) {
    Condition<Project> condition = toolWindowEP.getCondition();
    if (condition != null && !condition.value(extension.getProject())) {
      return false;
    }
    ModuleExtensionCondition moduleExtensionCondition = toolWindowEP.getModuleExtensionCondition();
    return moduleExtensionCondition.value(extension);
  }

  protected void fireToolWindowRegistered(final String id) {
    myDispatcher.getMulticaster().toolWindowRegistered(id);
  }

  protected void fireStateChanged() {
    myDispatcher.getMulticaster().stateChanged();
  }

  /**
   * @return info for the tool window with specified <code>ID</code>.
   */
  protected WindowInfoImpl getInfo(final String id) {
    return myLayout.getInfo(id, true);
  }

  public void setDefaultContentUiType(ToolWindowImpl toolWindow, ToolWindowContentUiType type) {
    final WindowInfoImpl info = getInfo(toolWindow.getId());
    if (info.wasRead()) return;
    toolWindow.setContentUiType(type, null);
  }

  @Override
  public void invokeLater(@NotNull final Runnable runnable) {
    List<FinalizableCommand> commandList = new ArrayList<>();
    commandList.add(new InvokeLaterCmd(runnable, myCommandProcessor));
    execute(commandList);
  }

  @NotNull
  protected abstract ToolWindow registerToolWindow(@NotNull final String id,
                                        @Nullable final Object component,
                                        @NotNull final ToolWindowAnchor anchor,
                                        boolean sideTool,
                                        boolean canCloseContent,
                                        final boolean canWorkInDumbMode,
                                        boolean shouldBeAvailable);

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull final String id, @NotNull final JComponent component, @NotNull final ToolWindowAnchor anchor) {
    return registerToolWindow(id, component, anchor, false);
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull final String id,
                                       @NotNull JComponent component,
                                       @NotNull ToolWindowAnchor anchor,
                                       @NotNull Disposable parentDisposable) {
    return registerToolWindow(id, component, anchor, parentDisposable, false, false);
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id,
                                       @NotNull JComponent component,
                                       @NotNull ToolWindowAnchor anchor,
                                       @NotNull Disposable parentDisposable,
                                       boolean canWorkInDumbMode) {
    return registerToolWindow(id, component, anchor, parentDisposable, canWorkInDumbMode, false);
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull final String id,
                                       @NotNull JComponent component,
                                       @NotNull ToolWindowAnchor anchor,
                                       @NotNull Disposable parentDisposable,
                                       boolean canWorkInDumbMode,
                                       boolean canCloseContents) {
    return registerDisposable(id, parentDisposable, registerToolWindow(id, component, anchor, false, canCloseContents, canWorkInDumbMode, true));
  }

  @NotNull
  private ToolWindow registerToolWindow(@NotNull final String id,
                                        @NotNull final JComponent component,
                                        @NotNull final ToolWindowAnchor anchor,
                                        boolean canWorkInDumbMode) {
    return registerToolWindow(id, component, anchor, false, false, canWorkInDumbMode, true);
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull final String id, final boolean canCloseContent, @NotNull final ToolWindowAnchor anchor) {
    return registerToolWindow(id, null, anchor, false, canCloseContent, false, true);
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull final String id,
                                       final boolean canCloseContent,
                                       @NotNull final ToolWindowAnchor anchor,
                                       final boolean secondary) {
    return registerToolWindow(id, null, anchor, secondary, canCloseContent, false, true);
  }


  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull final String id,
                                       final boolean canCloseContent,
                                       @NotNull final ToolWindowAnchor anchor,
                                       @NotNull final Disposable parentDisposable,
                                       final boolean canWorkInDumbMode) {
    return registerToolWindow(id, canCloseContent, anchor, parentDisposable, canWorkInDumbMode, false);
  }

  @NotNull
  @Override
  public ToolWindow registerToolWindow(@NotNull String id,
                                       boolean canCloseContent,
                                       @NotNull ToolWindowAnchor anchor,
                                       Disposable parentDisposable,
                                       boolean canWorkInDumbMode,
                                       boolean secondary) {
    ToolWindow window = registerToolWindow(id, null, anchor, secondary, canCloseContent, canWorkInDumbMode, true);
    return registerDisposable(id, parentDisposable, window);
  }

  @Override
  public void addToolWindowManagerListener(@NotNull ToolWindowManagerListener l) {
    myDispatcher.addListener(l);
  }

  @Override
  public void addToolWindowManagerListener(@NotNull ToolWindowManagerListener l, @NotNull Disposable parentDisposable) {
    myDispatcher.addListener(l, parentDisposable);
  }

  @Override
  public void removeToolWindowManagerListener(@NotNull ToolWindowManagerListener l) {
    myDispatcher.removeListener(l);
  }

  @Override
  public void loadState(Element state) {
    for (Element e : state.getChildren()) {
      if (EDITOR_ELEMENT.equals(e.getName())) {
        myEditorWasActive = Boolean.valueOf(e.getAttributeValue(ACTIVE_ATTR_VALUE)).booleanValue();
      }
      else if (ToolWindowLayout.TAG.equals(e.getName())) {
        myLayout.readExternal(e);
      }
      else if (LAYOUT_TO_RESTORE.equals(e.getName())) {
        myLayoutToRestoreLater = new ToolWindowLayout();
        myLayoutToRestoreLater.readExternal(e);
      }
    }
  }

  @NotNull
  @RequiredUIAccess
  @Override
  public ToolWindowLayout getLayout() {
    UIAccess.assertIsUIThread();
    return myLayout;
  }

  @Override
  public List<String> getIdsOn(@NotNull final ToolWindowAnchor anchor) {
    return myLayout.getVisibleIdsOn(anchor, this);
  }

  @NotNull
  @Override
  public String[] getToolWindowIds() {
    final WindowInfoImpl[] infos = myLayout.getInfos();
    final String[] ids = ArrayUtil.newStringArray(infos.length);
    for (int i = 0; i < infos.length; i++) {
      ids[i] = infos[i].getId();
    }
    return ids;
  }

  @Override
  public void setLayoutToRestoreLater(ToolWindowLayout layout) {
    myLayoutToRestoreLater = layout;
  }

  @Override
  public ToolWindowLayout getLayoutToRestoreLater() {
    return myLayoutToRestoreLater;
  }

  @NotNull
  @Override
  public IdeFocusManager getFocusManager() {
    return IdeFocusManager.getInstance(myProject);
  }

  @RequiredUIAccess
  @Override
  public String getActiveToolWindowId() {
    UIAccess.assertIsUIThread();
    return myLayout.getActiveId();
  }
}
