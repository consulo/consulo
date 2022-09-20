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
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.ProperTextRange;
import consulo.document.util.TextRange;
import consulo.language.editor.impl.highlight.MainHighlightingPassFactory;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPass;
import consulo.language.editor.Pass;
import consulo.language.editor.impl.highlight.VisibleHighlightingPassFactory;
import consulo.language.editor.impl.internal.daemon.FileStatusMapImpl;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import jakarta.inject.Inject;

import javax.annotation.Nonnull;

@ExtensionImpl
final class ChameleonSyntaxHighlightingPassFactory implements MainHighlightingPassFactory {
  @Inject
  ChameleonSyntaxHighlightingPassFactory() {
  }

  @Override
  public void register(@Nonnull Registrar registrar) {
    registrar.registerTextEditorHighlightingPass(this, null, new int[]{Pass.UPDATE_ALL}, false, -1);
  }

  @Nonnull
  @Override
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull Editor editor) {
    Project project = file.getProject();
    TextRange restrict = FileStatusMapImpl.getDirtyTextRange(editor, Pass.UPDATE_ALL);
    if (restrict == null) return new ProgressableTextEditorHighlightingPass.EmptyPass(project, editor.getDocument());
    ProperTextRange priority = VisibleHighlightingPassFactory.calculateVisibleRange(editor);
    return new ChameleonSyntaxHighlightingPass(project, file, editor.getDocument(), ProperTextRange.create(restrict), priority, editor, new DefaultHighlightInfoProcessor());
  }

  @Nonnull
  @Override
  public TextEditorHighlightingPass createMainHighlightingPass(@Nonnull PsiFile file, @Nonnull Document document, @Nonnull HighlightInfoProcessor highlightInfoProcessor) {
    ProperTextRange range = ProperTextRange.from(0, document.getTextLength());
    return new ChameleonSyntaxHighlightingPass(file.getProject(), file, document, range, range, null, highlightInfoProcessor);
  }
}
