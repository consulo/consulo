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

package consulo.fileTemplate;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.fileTemplate.localize.FileTemplateLocalize;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface CreateFromTemplateHandler {
    ExtensionPointName<CreateFromTemplateHandler> EP_NAME = ExtensionPointName.create(CreateFromTemplateHandler.class);

    boolean handlesTemplate(FileTemplate template);

    default PsiElement createFromTemplate(
        Project project,
        PsiDirectory directory,
        String fileName,
        FileTemplate template,
        String templateText,
        Map<String, Object> props
    ) throws IncorrectOperationException {
        String newFileName = checkAppendExtension(fileName, template);

        if (FileTypeManager.getInstance().isFileIgnored(newFileName)) {
            throw new IncorrectOperationException(FileTemplateLocalize.errorFileNameIsIgnored(newFileName).get());
        }

        directory.checkCreateFile(newFileName);
        FileType type = FileTypeRegistry.getInstance().getFileTypeByFileName(newFileName);
        PsiFile file = PsiFileFactory.getInstance(project).createFileFromText(newFileName, type, templateText);

        if (template.isReformatCode()) {
            CodeStyleManager.getInstance(project).reformat(file);
        }

        file = (PsiFile)directory.add(file);
        return file;
    }

    @Nonnull
    default String checkAppendExtension(String fileName, FileTemplate template) {
        String suggestedFileNameEnd = "." + template.getExtension();

        if (!fileName.endsWith(suggestedFileNameEnd)) {
            fileName += suggestedFileNameEnd;
        }
        return fileName;
    }

    default boolean canCreate(PsiDirectory[] dirs) {
        return true;
    }

    default boolean isNameRequired() {
        return true;
    }

    default LocalizeValue getErrorMessage() {
        return FileTemplateLocalize.titleCannotCreateFile();
    }

    default void prepareProperties(Map<String, Object> props) {
    }

    @Nonnull
    default LocalizeValue commandName(@Nonnull FileTemplate template) {
        return FileTemplateLocalize.commandCreateFileFromTemplate();
    }
}
