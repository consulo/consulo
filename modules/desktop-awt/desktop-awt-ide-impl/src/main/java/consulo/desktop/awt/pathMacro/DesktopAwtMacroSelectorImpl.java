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
package consulo.desktop.awt.pathMacro;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.ide.macro.MacrosDialog;
import consulo.module.Module;
import consulo.pathMacro.Macro;
import consulo.pathMacro.MacroSelector;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.concurrent.AsyncResult;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 10-Apr-22
 */
@Singleton
@ServiceImpl
public class DesktopAwtMacroSelectorImpl implements MacroSelector {
  @RequiredUIAccess
  @Override
  public void select(@Nullable Project project, @Nullable Module module, @Nonnull Consumer<Macro> macroConsumer) {
    MacrosDialog dialog = new MacrosDialog(project, module);
    AsyncResult<Void> result = dialog.showAsync();

    result.doWhenDone(() -> {
      Macro selectedMacro = dialog.getSelectedMacro();
      if (selectedMacro != null) {
        macroConsumer.accept(selectedMacro);
      }
    });
  }
}
