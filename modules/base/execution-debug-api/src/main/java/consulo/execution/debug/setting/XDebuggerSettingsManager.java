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
package consulo.execution.debug.setting;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

import jakarta.annotation.Nonnull;

@ServiceAPI(ComponentScope.APPLICATION)
public abstract class XDebuggerSettingsManager {
  @Nonnull
  public static XDebuggerSettingsManager getInstance() {
    return Application.get().getInstance(XDebuggerSettingsManager.class);
  }

  public interface DataViewSettings {
    boolean isSortValues();

    boolean isAutoExpressions();

    int getValueLookupDelay();

    boolean isShowLibraryStackFrames();

    boolean isShowValuesInline();

    boolean isValueTooltipAutoShow();

    boolean isValueTooltipAutoShowOnSelection();
  }

  public interface GeneralViewSettings {
      boolean isSingleClickForDisablingBreakpoint();
  }

  @Nonnull
  public abstract DataViewSettings getDataViewSettings();

  @Nonnull
  public abstract GeneralViewSettings getGeneralSettings();
}