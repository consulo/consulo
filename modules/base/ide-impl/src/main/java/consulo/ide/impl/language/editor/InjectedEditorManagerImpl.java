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
package consulo.ide.impl.language.editor;

import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.Caret;
import consulo.codeEditor.Editor;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.inject.impl.internal.InjectedLanguageUtil;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 20-Apr-22
 */
@Singleton
@ServiceImpl
public class InjectedEditorManagerImpl implements InjectedEditorManager {
  private final Project myProject;

  @Inject
  public InjectedEditorManagerImpl(Project project) {
    myProject = project;
  }

  @Nullable
  @Override
  public Editor openEditorFor(@Nonnull PsiFile file) {
    return InjectedLanguageUtil.openEditorFor(file, myProject);
  }

  @Override
  public Editor getEditorForInjectedLanguageNoCommit(@Nullable Editor editor, @Nullable PsiFile file, int offset) {
    return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file, offset);
  }

  @Override
  public Editor getEditorForInjectedLanguageNoCommit(@Nullable Editor editor, @Nullable Caret caret, @Nullable PsiFile file) {
    return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, caret, file);
  }

  @Override
  public Editor getEditorForInjectedLanguageNoCommit(@Nullable Editor editor, @Nullable PsiFile file) {
    return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, file);
  }
}
