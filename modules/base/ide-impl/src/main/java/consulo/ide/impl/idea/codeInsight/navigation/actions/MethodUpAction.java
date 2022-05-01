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

package consulo.ide.impl.idea.codeInsight.navigation.actions;

import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.ide.impl.idea.codeInsight.actions.BaseCodeInsightAction;
import consulo.ide.impl.idea.codeInsight.navigation.MethodUpHandler;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.ide.impl.idea.ide.structureView.impl.TemplateLanguageStructureViewBuilder;
import consulo.ide.impl.idea.lang.LanguageStructureViewBuilder;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import javax.annotation.Nonnull;

public class MethodUpAction extends BaseCodeInsightAction {
  @Nonnull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new MethodUpHandler();
  }

  @Override
  protected boolean isValidForLookup() {
    return true;
  }

  @Override
  protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull final PsiFile file) {
    return checkValidForFile(file);
  }

  static boolean checkValidForFile(final PsiFile file) {
    final StructureViewBuilder structureViewBuilder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(file);
    return structureViewBuilder instanceof TreeBasedStructureViewBuilder || structureViewBuilder instanceof TemplateLanguageStructureViewBuilder;
  }
}