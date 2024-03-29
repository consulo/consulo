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
package consulo.ide.impl.idea.codeInsight.actions;

import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

/**
 * Delegate class that performs actual work for {@link MultiCaretCodeInsightAction}
 */
public abstract class MultiCaretCodeInsightActionHandler {
  /**
   * Invoked for each caret in editor (in top-to-bottom order). <code>project</code> value is the same for all carets, <code>editor</code>
   * and <code>file</code> values can be different in presence of multi-root PSI and injected fragments. For injected fragments
   * caret instance will belong to corresponding injected editor.
   */
  public abstract void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Caret caret, @Nonnull PsiFile file);

  /**
   * Invoked after processing all carets.
   */
  public void postInvoke() {}
}