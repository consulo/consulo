/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.setting;

import consulo.configurable.Configurable;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.setting.DebuggerSettingsCategory;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.Nls;

class DataViewsConfigurable extends SubCompositeConfigurable implements Configurable.NoScroll {
  @Nonnull
  @Override
  public String getId() {
    return "debugger.dataViews";
  }

  @Nls
  @Override
  public String getDisplayName() {
    return XDebuggerBundle.message("debugger.dataViews.display.name");
  }

  @Override
  protected DataViewsConfigurableUi createRootUi() {
    return new DataViewsConfigurableUi();
  }

  @Nonnull
  @Override
  protected DebuggerSettingsCategory getCategory() {
    return DebuggerSettingsCategory.DATA_VIEWS;
  }

  @Nonnull
  @Override
  protected XDebuggerDataViewSettings getSettings() {
    return XDebuggerSettingManagerImpl.getInstanceImpl().getDataViewSettings();
  }
}