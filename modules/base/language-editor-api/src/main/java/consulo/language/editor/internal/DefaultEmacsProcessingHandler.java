/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.editor.internal;

import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.editor.action.EmacsProcessingHandler;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

/**
 * @author Denis Zhdanov
 * @since 2011-04-11
 */
public class DefaultEmacsProcessingHandler implements EmacsProcessingHandler {

  @Nonnull
  @Override
  public Result changeIndent(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
    return Result.CONTINUE;
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return Language.ANY;
  }
}
