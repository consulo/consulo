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
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ex.ToolWindowManagerEx;
import com.intellij.openapi.wm.impl.commands.FinalizableCommand;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.condition.ModuleExtensionCondition;
import consulo.ui.UIAccess;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

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

  protected final Project myProject;

  protected ToolWindowManagerBase(Project project) {
    myProject = project;
  }

  protected void initAll(List<FinalizableCommand> commandsList) {
  }

  public void execute(@NotNull List<FinalizableCommand> commandList) {
  }

  protected void flushCommands() {
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

  public abstract boolean isToolWindowRegistered(String id);

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
}
