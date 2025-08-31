/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.Pass;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.highlight.TextEditorHighlightingPassFactory;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.codeEditor.impl.MarkupModelImpl;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.editor.impl.highlight.VisibleHighlightingPassFactory;
import consulo.language.editor.impl.internal.daemon.FileStatusMapImpl;
import consulo.language.editor.impl.internal.highlight.ProgressableTextEditorHighlightingPass;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author cdr
 */
@ExtensionImpl
public class LineMarkersPassFactory implements TextEditorHighlightingPassFactory {
  @Override
  public void register(@Nonnull Registrar registrar) {
    registrar.registerTextEditorHighlightingPass(this, null, new int[]{Pass.UPDATE_ALL}, false, Pass.LINE_MARKERS);
  }

  @Override
  @Nullable
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull Editor editor) {
    TextRange restrictRange = FileStatusMapImpl.getDirtyTextRange(editor, Pass.LINE_MARKERS);
    Document document = editor.getDocument();
    if (restrictRange == null) return new ProgressableTextEditorHighlightingPass.EmptyPass(file.getProject(), document);
    ProperTextRange visibleRange = VisibleHighlightingPassFactory.calculateVisibleRange(editor);
    return new LineMarkersPass(file.getProject(), file, document, expandRangeToCoverWholeLines(document, visibleRange), expandRangeToCoverWholeLines(document, restrictRange));
  }

  private static TextRange expandRangeToCoverWholeLines(@Nonnull Document document, TextRange textRange) {
    if (textRange == null) return null;
    return MarkupModelImpl.roundToLineBoundaries(document, textRange.getStartOffset(), textRange.getEndOffset());
  }
}