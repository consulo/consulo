/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.refactoring.rename;

import consulo.codeEditor.Editor;
import consulo.language.editor.refactoring.RefactoringSettings;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 2025-08-07
 */
public abstract class RenamePsiFileProcessorBase extends RenamePsiElementProcessor {
    @Override
    public RenameDialog createRenameDialog(Project project, PsiElement element, PsiElement nameSuggestionContext, Editor editor) {
        return new PsiFileRenameDialog(project, element, nameSuggestionContext, editor);
    }

    private static boolean getSearchForReferences(PsiElement element) {
        return element instanceof PsiFile
            ? RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_FILE
            : RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_DIRECTORY;
    }

    @Nonnull
    @Override
    public Collection<PsiReference> findReferences(PsiElement element) {
        if (!getSearchForReferences(element)) {
            return Collections.emptyList();
        }
        return super.findReferences(element);
    }

    protected static class PsiFileRenameDialog extends RenameWithOptionalReferencesDialog {
        public PsiFileRenameDialog(Project project, PsiElement element, PsiElement nameSuggestionContext, Editor editor) {
            super(project, element, nameSuggestionContext, editor);
        }

        @Override
        protected boolean getSearchForReferences() {
            return RenamePsiFileProcessorBase.getSearchForReferences(getPsiElement());
        }

        @Override
        protected void setSearchForReferences(boolean value) {
            if (getPsiElement() instanceof PsiFile) {
                RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_FILE = value;
            }
            else {
                RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_DIRECTORY = value;
            }
        }
    }
}
