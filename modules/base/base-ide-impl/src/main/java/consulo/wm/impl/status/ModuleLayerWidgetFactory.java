/*
 * Copyright 2013-2020 consulo.io
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
package consulo.wm.impl.status;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.Nls;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2020-11-01
 */
public class ModuleLayerWidgetFactory implements StatusBarWidgetFactory {
  @Nonnull
  @Override
  public String getId() {
    return "module-layers";
  }

  @Nls
  @Nonnull
  @Override
  public String getDisplayName() {
    return "Module Layer";
  }

  @Override
  public boolean isAvailable(@Nonnull Project project) {
    return true;
  }

  @Nonnull
  @Override
  public StatusBarWidget createWidget(@Nonnull Project project) {
    return new ModuleLayerWidget(project);
  }

  @Override
  public void disposeWidget(@Nonnull StatusBarWidget widget) {

  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return true;
  }
}
