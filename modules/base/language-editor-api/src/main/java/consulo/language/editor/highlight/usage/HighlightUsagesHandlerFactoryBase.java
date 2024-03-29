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
package consulo.language.editor.highlight.usage;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.language.editor.TargetElementUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public abstract class HighlightUsagesHandlerFactoryBase<T extends PsiElement> implements HighlightUsagesHandlerFactory<T> {
  @Nullable
  @Override
  @RequiredReadAction
  public final HighlightUsagesHandlerBase<T> createHighlightUsagesHandler(Editor editor, PsiFile file) {
    int offset = TargetElementUtil.adjustOffset(file, editor.getDocument(), editor.getCaretModel().getOffset());
    PsiElement target = file.findElementAt(offset);
    if (target == null) return null;
    return createHighlightUsagesHandler(editor, file, target);
  }

  @Nullable
  @RequiredReadAction
  public abstract HighlightUsagesHandlerBase<T> createHighlightUsagesHandler(@Nonnull Editor editor, @Nonnull PsiFile file, @Nonnull PsiElement target);
}
