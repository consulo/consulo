/*
 * Copyright 2013-2016 consulo.io
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
package consulo.sandboxPlugin.ide.run;

import com.intellij.execution.Executor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import consulo.module.extension.ModuleExtensionHelper;
import consulo.sandboxPlugin.ide.module.extension.SandModuleExtension;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 19.03.14
 */
public class SandExecutor extends Executor {
  @Override
  public String getToolWindowId() {
    return "SandExecutor";
  }

  @Override
  public Image getToolWindowIcon() {
    return AllIcons.Toolwindows.ToolWindowInspection;
  }

  @Nonnull
  @Override
  public Image getIcon() {
    return AllIcons.Ide.HectorOn;
  }

  @Override
  public Image getDisabledIcon() {
    return AllIcons.Ide.HectorOff;
  }

  @Override
  public String getDescription() {
    return "Sand executor";
  }

  @Override
  public boolean isApplicable(@Nonnull Project project) {
    return ModuleExtensionHelper.getInstance(project).hasModuleExtension(SandModuleExtension.class);
  }

  @Nonnull
  @Override
  public String getActionName() {
    return "Sand";
  }

  @Nonnull
  @Override
  public String getId() {
    return "SandExecutor";
  }

  @Nonnull
  @Override
  public String getStartActionText() {
    return "Start Sand";
  }

  @Override
  public String getHelpId() {
    return null;
  }
}
