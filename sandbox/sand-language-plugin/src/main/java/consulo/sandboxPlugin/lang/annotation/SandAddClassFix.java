/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.lang.annotation;

import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.psi.SandFile;

/**
 * @author VISTALL
 */
public class SandAddClassFix implements IntentionAction {
    @Override
    public LocalizeValue getText() {
        return LocalizeValue.localizeTODO("Add class");
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
        return file instanceof SandFile;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile file) {
        PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
        Document document = documentManager.getDocument(file);
        if (document == null) {
            return;
        }

        CharSequence text = document.getCharsSequence();
        String separator = text.length() == 0 || text.charAt(text.length() - 1) == '\n' ? "" : "\n";
        document.insertString(text.length(), separator + "class Unnamed {}\n");
        documentManager.commitDocument(document);
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}
