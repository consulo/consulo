/*
 * Copyright 2013-2022 consulo.io
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
package consulo.pathMacro;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.dataContext.DataContext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 10-Apr-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface MacroManager {
  @Nonnull
  public static MacroManager getInstance() {
    return Application.get().getInstance(MacroManager.class);
  }

  void cacheMacrosPreview(DataContext dataContext);

  @Nonnull
  Collection<Macro> getMacros();

  /**
   * Expands all macros that are found in the <code>str</code>.
   */
  @Nullable
  String expandMacrosInString(String str, boolean firstQueueExpand, DataContext dataContext) throws Macro.ExecutionCancelledException;

  @Nullable
  String expandSilentMarcos(String str, boolean firstQueueExpand, DataContext dataContext) throws Macro.ExecutionCancelledException;
}
