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
package consulo.language.util;

import consulo.annotation.access.RequiredWriteAction;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.FileContentUtilCore;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.project.ProjectLocator;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nullable;

import java.io.IOException;

/**
 * @author VISTALL
 * @since 2025-01-12
 */
public class LanguageFileContentUtil extends FileContentUtilCore {
    @RequiredWriteAction
    public static void setFileText(@Nullable Project project, final VirtualFile virtualFile, final String text) throws IOException {
        if (project == null) {
            project = ProjectLocator.getInstance().guessProjectForFile(virtualFile);
        }
        if (project != null) {
            final PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
            final PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
            final Document document = psiFile == null ? null : psiDocumentManager.getDocument(psiFile);
            if (document != null) {
                document.setText(text != null ? text : "");
                psiDocumentManager.commitDocument(document);
                FileDocumentManager.getInstance().saveDocument(document);
                return;
            }
        }
        VirtualFileUtil.saveText(virtualFile, text != null ? text : "");
        virtualFile.refresh(false, false);
    }
}
