/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.language.internal;

import consulo.language.Language;
import consulo.language.file.LanguageFileType;
import consulo.language.psi.LanguageSubstitutors;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

/**
 * @author traff
 */
public class SubstitutedFileType extends LanguageFileType {
    private final FileType myOriginalFileType;
    
    private final FileType myFileType;

    private SubstitutedFileType(FileType originalFileType, LanguageFileType substitutionFileType, Language substitutedLanguage) {
        super(substitutedLanguage);
        myOriginalFileType = originalFileType;
        myFileType = substitutionFileType;
    }

    public static FileType substituteFileType(VirtualFile file, FileType fileType, Project project) {
        if (project == null) {
            return fileType;
        }
        if (fileType instanceof LanguageFileType) {
            Language language = ((LanguageFileType) fileType).getLanguage();
            Language substitutedLanguage = LanguageSubstitutors.substituteLanguage(language, file, project);
            LanguageFileType substFileType = substitutedLanguage.getAssociatedFileType();
            if (!substitutedLanguage.equals(language) && substFileType != null) {
                return new SubstitutedFileType(fileType, substFileType, substitutedLanguage);
            }
        }

        return fileType;
    }

    @Override
    public String getId() {
        return myFileType.getId();
    }

    @Override
    public LocalizeValue getDescription() {
        return myFileType.getDescription();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return myFileType.getDisplayName();
    }

    @Override
    public String getDefaultExtension() {
        return myFileType.getDefaultExtension();
    }

    @Override
    public Image getIcon() {
        return myFileType.getIcon();
    }

    @Override
    public @Nullable String getCharset(VirtualFile file, byte[] content) {
        return myFileType.getCharset(file, content);
    }

    public FileType getOriginalFileType() {
        return myOriginalFileType;
    }

    public FileType getFileType() {
        return myFileType;
    }

    public boolean isSameFileType() {
        return myFileType.equals(myOriginalFileType);
    }
}
