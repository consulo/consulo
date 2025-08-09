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
package consulo.language.editor.refactoring.impl.internal.changeSignature;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.progress.ProgressIndicator;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.Editor;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.TextRange;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import consulo.language.editor.highlight.TextEditorHighlightingPassFactory;
import consulo.language.editor.highlight.UpdateHighlightersUtil;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.refactoring.changeSignature.ChangeInfo;
import consulo.language.editor.refactoring.changeSignature.inplace.InplaceChangeSignature;
import consulo.language.editor.refactoring.changeSignature.inplace.LanguageChangeSignatureDetector;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;

@ExtensionImpl
public class ChangeSignaturePassFactory implements TextEditorHighlightingPassFactory {
    @Override
    public void register(@Nonnull Registrar registrar) {
        registrar.registerTextEditorHighlightingPass(this, null, null, true, -1);
    }

    @Override
    public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull Editor editor) {
        LanguageChangeSignatureDetector<ChangeInfo> detector =
            LanguageChangeSignatureDetector.forLanguage(file.getLanguage());
        if (detector == null) {
            return null;
        }

        return new ChangeSignaturePass(file.getProject(), file, editor);
    }

    private static class ChangeSignaturePass extends TextEditorHighlightingPass {
        @NonNls
        private static final String SIGNATURE_SHOULD_BE_POSSIBLY_CHANGED = "Signature change was detected";
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
        public void doCollectInformation(@Nonnull ProgressIndicator progress) {
        }

        @Override
        public void doApplyInformationToEditor() {
            HighlightInfo info = null;
            InplaceChangeSignature currentRefactoring = InplaceChangeSignature.getCurrentRefactoring(myEditor);
            if (currentRefactoring != null) {
                ChangeInfo changeInfo = currentRefactoring.getStableChange();
                PsiElement element = changeInfo.getMethod();
                int offset = myEditor.getCaretModel().getOffset();
                if (element == null || !element.isValid()) {
                    return;
                }
                TextRange elementTextRange = element.getTextRange();
                if (elementTextRange == null || !elementTextRange.contains(offset)) {
                    return;
                }
                LanguageChangeSignatureDetector<ChangeInfo> detector = LanguageChangeSignatureDetector.forLanguage(changeInfo.getLanguage());
                TextRange range = detector.getHighlightingRange(changeInfo);
                TextAttributes attributes = new TextAttributes(null, null,
                    myEditor.getColorsScheme().getAttributes(CodeInsightColors.WEAK_WARNING_ATTRIBUTES)
                        .getEffectColor(),
                    null, Font.PLAIN);
                HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION).range(range);
                builder.textAttributes(attributes);
                builder.descriptionAndTooltip(SIGNATURE_SHOULD_BE_POSSIBLY_CHANGED);
                builder.registerFix(new ApplyChangeSignatureAction(currentRefactoring.getInitialName()), null, null, null, null);
                info = builder.createUnconditionally();
            }
            Collection<HighlightInfo> infos = info != null ? Collections.singletonList(info) : Collections.emptyList();
            UpdateHighlightersUtil.setHighlightersToEditor(myProject, myDocument, 0, myFile.getTextLength(), infos, getColorsScheme(), getId());
        }
    }
}
