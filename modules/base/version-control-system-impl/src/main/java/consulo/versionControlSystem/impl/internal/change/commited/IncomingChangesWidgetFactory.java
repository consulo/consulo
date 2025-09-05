/*
 * Copyright 2013-2024 consulo.io
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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

/**
 * @author VISTALL
 * @since 03.05.2024
 */
@ExtensionImpl(id = "incommingChangesWidget", order = "after readOnlyWidget")
public class IncomingChangesWidgetFactory implements StatusBarWidgetFactory {
  @Nls
  @Nonnull
  @Override
  public String getDisplayName() {
    return "Incoming Changes";
  }

  @Override
  public boolean isAvailable(@Nonnull Project project) {
    IncomingChangesIndicator indicator = project.getInstance(IncomingChangesIndicator.class);
    return indicator.needIndicator();
  }

  @Nonnull
  @Override
  public StatusBarWidget createWidget(@Nonnull Project project) {
    IncomingChangesIndicator indicator = project.getInstance(IncomingChangesIndicator.class);
    return new IncomingChangesWidget(this, indicator);
  }

  @Override
  public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
    return false;
  }
}
