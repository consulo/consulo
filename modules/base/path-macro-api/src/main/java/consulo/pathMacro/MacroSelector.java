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
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 10-Apr-22
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface MacroSelector {
  static MacroSelector getInstance() {
    return Application.get().getInstance(MacroSelector.class);
  }

  @RequiredUIAccess
  default void select(@Nullable Project project, @RequiredUIAccess @Nonnull Consumer<Macro> macroConsumer) {
    select(project, null, macroConsumer);
  }

  @RequiredUIAccess
  void select(@Nullable Project project, @Nullable Module module, @RequiredUIAccess @Nonnull Consumer<Macro> macroConsumer);
}
