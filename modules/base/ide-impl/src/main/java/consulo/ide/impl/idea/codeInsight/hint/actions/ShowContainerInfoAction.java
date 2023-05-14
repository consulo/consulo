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
package consulo.ide.impl.idea.codeInsight.hint.actions;

import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.ide.impl.idea.codeInsight.hint.ShowContainerInfoHandler;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.dataContext.DataContext;
import consulo.language.editor.PlatformDataKeys;
import consulo.codeEditor.Editor;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class ShowContainerInfoAction extends BaseCodeInsightAction{
  @Nonnull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new ShowContainerInfoHandler();
  }

  @Override
  @Nullable
  protected Editor getBaseEditor(final DataContext dataContext, final Project project) {
    return dataContext.getData(PlatformDataKeys.EDITOR_EVEN_IF_INACTIVE);
  }

  @Override
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull final PsiFile file) {
    return PsiStructureViewFactory.createBuilderForFile(file) instanceof TreeBasedStructureViewBuilder;
  }
}