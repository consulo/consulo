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
package consulo.ide.impl.idea.refactoring.changeSignature.inplace;

import consulo.ide.impl.idea.codeHighlighting.TextEditorHighlightingPass;
import consulo.ide.impl.idea.codeHighlighting.TextEditorHighlightingPassFactory;
import consulo.ide.impl.idea.codeInsight.daemon.impl.UpdateHighlightersUtil;
import consulo.language.editor.intention.QuickFixAction;
import consulo.ide.impl.idea.refactoring.changeSignature.ChangeInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.Editor;
import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.TextRange;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.awt.*;
import java.util.Collection;
import java.util.Collections;

public class ChangeSignaturePassFactory implements TextEditorHighlightingPassFactory {
  @Override
  public void register(@Nonnull Registrar registrar) {
    registrar.registerTextEditorHighlightingPass(this, null, null, true, -1);
  }

  @Override
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull final PsiFile file, @Nonnull final Editor editor) {
    LanguageChangeSignatureDetector<ChangeInfo> detector =
            LanguageChangeSignatureDetectors.INSTANCE.forLanguage(file.getLanguage());
    if (detector == null) return null;

    return new ChangeSignaturePass(file.getProject(), file, editor);
  }

  private static class ChangeSignaturePass extends TextEditorHighlightingPass {
    @NonNls private static final String SIGNATURE_SHOULD_BE_POSSIBLY_CHANGED = "Signature change was detected";
    private final Project myProject;
    private final PsiFile myFile;
    private final Editor myEditor;

    public ChangeSignaturePass(Project project, PsiFile file, Editor editor) {
      super(project, editor.getDocument(), true);
      myProject = project;
      myFile = file;
      myEditor = editor;
    }

    @RequiredReadAction
    @Override
    public void doCollectInformation(@Nonnull ProgressIndicator progress) {}

    @Override
    public void doApplyInformationToEditor() {
      HighlightInfo info = null;
      final InplaceChangeSignature currentRefactoring = InplaceChangeSignature.getCurrentRefactoring(myEditor);
      if (currentRefactoring != null) {
        final ChangeInfo changeInfo = currentRefactoring.getStableChange();
        final PsiElement element = changeInfo.getMethod();
        int offset = myEditor.getCaretModel().getOffset();
        if (element == null || !element.isValid()) return;
        final TextRange elementTextRange = element.getTextRange();
        if (elementTextRange == null || !elementTextRange.contains(offset)) return;
        final LanguageChangeSignatureDetector<ChangeInfo> detector = LanguageChangeSignatureDetectors.INSTANCE.forLanguage(changeInfo.getLanguage());
        TextRange range = detector.getHighlightingRange(changeInfo);
        TextAttributes attributes = new TextAttributes(null, null,
                                                       myEditor.getColorsScheme().getAttributes(CodeInsightColors.WEAK_WARNING_ATTRIBUTES)
                                                               .getEffectColor(),
                                                       null, Font.PLAIN);
        HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION).range(range);
        builder.textAttributes(attributes);
        builder.descriptionAndTooltip(SIGNATURE_SHOULD_BE_POSSIBLY_CHANGED);
        info = builder.createUnconditionally();
        QuickFixAction.registerQuickFixAction(info, new ApplyChangeSignatureAction(currentRefactoring.getInitialName()));
      }
      Collection<HighlightInfo> infos = info != null ? Collections.singletonList(info) : Collections.emptyList();
      UpdateHighlightersUtil.setHighlightersToEditor(myProject, myDocument, 0, myFile.getTextLength(), infos, getColorsScheme(), getId());
    }
  }
}
