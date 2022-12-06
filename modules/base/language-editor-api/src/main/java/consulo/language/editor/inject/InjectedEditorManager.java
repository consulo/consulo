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
package consulo.language.editor.inject;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 20-Apr-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface InjectedEditorManager {
  static InjectedEditorManager getInstance(@Nonnull Project project) {
    return project.getInstance(InjectedEditorManager.class);
  }

  @Nullable
  Editor openEditorFor(@Nonnull PsiFile file);

  /**
   * Invocation of this method on uncommitted {@code file} can lead to unexpected results, including throwing an exception!
   */
  @Contract("null,_,_->null;!null,_,_->!null")
  Editor getEditorForInjectedLanguageNoCommit(@Nullable Editor editor, @Nullable PsiFile file, final int offset);

  /**
   * Invocation of this method on uncommitted {@code file} can lead to unexpected results, including throwing an exception!
   */
  Editor getEditorForInjectedLanguageNoCommit(@Nullable Editor editor, @Nullable Caret caret, @Nullable PsiFile file);

  /**
   * Invocation of this method on uncommitted {@code file} can lead to unexpected results, including throwing an exception!
   */
  @Contract("null,_->null;!null,_->!null")
  Editor getEditorForInjectedLanguageNoCommit(@Nullable Editor editor, @Nullable PsiFile file);
}
