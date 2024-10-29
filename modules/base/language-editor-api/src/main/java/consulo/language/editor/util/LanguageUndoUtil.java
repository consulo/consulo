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
package consulo.language.editor.util;

import consulo.document.Document;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;

import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2022-02-16
 */
public class LanguageUndoUtil {
    /**
     * make undoable action in current document in order to Undo action work from current file
     *
     * @param file to make editors of to respond to undo action.
     */
    public static void markPsiFileForUndo(@Nonnull final PsiFile file) {
        Project project = file.getProject();
        final Document document = PsiDocumentManager.getInstance(project).getDocument(file);
        if (document == null) {
            return;
        }
        CommandProcessor.getInstance().addAffectedDocuments(project, document);
    }
}
