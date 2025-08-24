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
package consulo.language.editor.postfixTemplate;

import consulo.annotation.access.RequiredReadAction;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.surroundWith.Surrounder;
import consulo.language.internal.PsiFileInternal;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.template.TemplateLanguageUtil;
import consulo.language.util.LanguageUtil;
import consulo.project.Project;
import consulo.undoRedo.util.UndoUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

public abstract class PostfixTemplatesUtils {
    private PostfixTemplatesUtils() {
    }

    @Nonnull
    @RequiredReadAction
    public static PsiFile copyFile(@Nonnull PsiFile file, @Nonnull StringBuilder fileContentWithoutKey) {
        PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(file.getProject());
        FileType fileType = file.getFileType();
        Language language = LanguageUtil.getLanguageForPsi(file.getProject(), file.getVirtualFile(), fileType);
        PsiFile copy = language != null
            ? psiFileFactory.createFileFromText(file.getName(), language, fileContentWithoutKey, false, true)
            : psiFileFactory.createFileFromText(file.getName(), fileType, fileContentWithoutKey);

        if (copy instanceof PsiFileInternal copyFile) {
            copyFile.setOriginalFile(TemplateLanguageUtil.getBaseFile(file));
        }

        VirtualFile vFile = copy.getVirtualFile();
        if (vFile != null) {
            UndoUtil.disableUndoFor(vFile);
        }
        return copy;
    }

    @Nullable
    public static TextRange surround(@Nonnull Surrounder surrounder,
                                     @Nonnull Editor editor,
                                     @Nonnull PsiElement expr) {
        Project project = expr.getProject();
        PsiElement[] elements = {expr};
        if (surrounder.isApplicable(elements)) {
            return surrounder.surroundElements(project, editor, elements);
        }
        else {
            showErrorHint(project, editor);
        }
        return null;
    }

    public static void showErrorHint(@Nonnull Project project, @Nonnull Editor editor) {
        HintManager.getInstance().showErrorHint(editor, "Can't expand postfix template");
    }

    /**
     * Returns all templates registered in the provider, including the edited templates and builtin templates in their current state
     */
    @Nonnull
    public static Set<PostfixTemplate> getAvailableTemplates(@Nonnull PostfixTemplateProvider provider) {
        return provider.getTemplates();
    }

    @Nonnull
    public static String getLangForProvider(@Nonnull PostfixTemplateProvider provider) {
        return provider.getLanguage().getID();
    }
}
