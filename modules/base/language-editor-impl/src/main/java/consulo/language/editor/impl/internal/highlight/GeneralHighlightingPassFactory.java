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

package consulo.language.editor.impl.internal.highlight;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.impl.highlight.MainHighlightingPassFactory;
import consulo.language.editor.Pass;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.internal.highlight.ProgressableTextEditorHighlightingPass.EmptyPass;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.editor.impl.highlight.VisibleHighlightingPassFactory;
import consulo.language.editor.impl.internal.daemon.FileStatusMapImpl;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author cdr
 */
@ExtensionImpl
public class GeneralHighlightingPassFactory implements MainHighlightingPassFactory {
  @Override
  public void register(@Nonnull Registrar registrar) {
    registrar.registerTextEditorHighlightingPass(this, null, new int[]{Pass.UPDATE_FOLDING}, false, Pass.UPDATE_ALL);
  }

  @Override
  @Nullable
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull final Editor editor) {
    TextRange textRange = FileStatusMapImpl.getDirtyTextRange(editor, Pass.UPDATE_ALL);
    if (textRange == null) return new EmptyPass(file.getProject(), editor.getDocument());
    ProperTextRange visibleRange = VisibleHighlightingPassFactory.calculateVisibleRange(editor);
    return new GeneralHighlightingPass(file.getProject(), file, editor.getDocument(), textRange.getStartOffset(), textRange.getEndOffset(), true, visibleRange, editor, new DefaultHighlightInfoProcessor());
  }

  @Override
  public TextEditorHighlightingPass createMainHighlightingPass(@Nonnull PsiFile file, @Nonnull Document document, @Nonnull HighlightInfoProcessor highlightInfoProcessor) {
    // no applying to the editor - for read-only analysis only
    return new GeneralHighlightingPass(file.getProject(), file, document, 0, file.getTextLength(), true, new ProperTextRange(0, document.getTextLength()), null, highlightInfoProcessor);
  }
}
