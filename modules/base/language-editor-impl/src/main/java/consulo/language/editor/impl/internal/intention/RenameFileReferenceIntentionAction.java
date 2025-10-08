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
package consulo.language.editor.impl.internal.intention;

import consulo.codeEditor.Editor;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiFile;
import consulo.language.psi.path.FileReference;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;

/**
 * @author peter
 */
public class RenameFileReferenceIntentionAction implements IntentionAction, LocalQuickFix {
    private final String myExistingElementName;
    private final FileReference myFileReference;

    public RenameFileReferenceIntentionAction(String existingElementName, FileReference fileReference) {
        myExistingElementName = existingElementName;
        myFileReference = fileReference;
    }

    @Override
    @Nonnull
    public LocalizeValue getText() {
        return CodeInsightLocalize.renameFileReferenceText(myExistingElementName);
    }

    @Override
    @Nonnull
    public LocalizeValue getName() {
        return getText();
    }

    @Override
    @RequiredUIAccess
    public void applyFix(@Nonnull Project project, @Nonnull ProblemDescriptor descriptor) {
        if (isAvailable(project, null, null)) {
            CommandProcessor.getInstance().newCommand()
                .project(project)
                .inWriteAction()
                .run(() -> invoke(project, null, descriptor.getPsiElement().getContainingFile()));
        }
    }

    @Override
    public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
            return;
        }
        myFileReference.handleElementRename(myExistingElementName);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
