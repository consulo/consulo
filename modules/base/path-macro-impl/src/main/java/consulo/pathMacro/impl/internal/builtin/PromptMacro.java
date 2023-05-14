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

import consulo.pathMacro.PathMacroBundle;
import consulo.dataContext.DataContext;
import consulo.pathMacro.PromptingMacro;
import consulo.pathMacro.SecondQueueExpandMacro;
import consulo.ui.ex.awt.Messages;
import jakarta.annotation.Nullable;

public final class PromptMacro extends PromptingMacro implements SecondQueueExpandMacro {
  @Override
  public String getName() {
    return "Prompt";
  }

  @Override
  public String getDescription() {
    return PathMacroBundle.message("macro.prompt");
  }

  @Override
  @Nullable
  protected String promptUser(DataContext dataContext) {
    return Messages.showInputDialog(PathMacroBundle.message("prompt.enter.parameters"), PathMacroBundle.message("title.input"), Messages.getQuestionIcon());
  }

  @Override
  public void cachePreview(DataContext dataContext) {
    myCachedPreview = PathMacroBundle.message("macro.prompt.preview");
  }
}
