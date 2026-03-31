/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.pathMacro.impl.internal.builtin;

import consulo.dataContext.DataContext;
import consulo.localize.LocalizeValue;
import consulo.pathMacro.PromptingMacro;
import consulo.pathMacro.SecondQueueExpandMacro;
import consulo.pathMacro.localize.PathMacroLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import org.jspecify.annotations.Nullable;

public final class PromptMacro extends PromptingMacro implements SecondQueueExpandMacro {
  @Override
  public String getName() {
    return "Prompt";
  }

  @Override
  public LocalizeValue getDescription() {
    return PathMacroLocalize.macroPrompt();
  }

  @Override
  @RequiredUIAccess
  protected @Nullable String promptUser(DataContext dataContext) {
    return Messages.showInputDialog(
      PathMacroLocalize.promptEnterParameters().get(),
      PathMacroLocalize.titleInput().get(),
      UIUtil.getQuestionIcon()
    );
  }

  @Override
  public void cachePreview(DataContext dataContext) {
    myCachedPreview = PathMacroLocalize.macroPromptPreview().get();
  }
}
