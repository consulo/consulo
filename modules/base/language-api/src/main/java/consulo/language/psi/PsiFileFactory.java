/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.language.psi;

import consulo.annotation.DeprecationInfo;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.util.IncorrectOperationException;
import consulo.language.version.LanguageVersion;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author max
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface PsiFileFactory {
    public static Key<PsiFile> ORIGINAL_FILE = Key.create("ORIGINAL_FILE");

    static PsiFileFactory getInstance(Project project) {
        return project.getInstance(PsiFileFactory.class);
    }

    /**
     * Please use {@link #createFileFromText(String, FileType, CharSequence)},
     * since file type detecting by file extension becomes vulnerable when file type mappings are changed.
     * <p/>
     * Creates a file from the specified text.
     *
     * @param name the name of the file to create (the extension of the name determines the file type).
     * @param text the text of the file to create.
     * @return the created file.
     * @throws IncorrectOperationException if the file type with specified extension is binary.
     */
    @Deprecated
    @Nonnull
    PsiFile createFileFromText(@Nonnull String name, @Nonnull String text);

    @Nonnull
    PsiFile createFileFromText(@Nonnull String fileName, @Nonnull FileType fileType, @Nonnull CharSequence text);

    @Nonnull
    PsiFile createFileFromText(@Nonnull String name,
                               @Nonnull FileType fileType,
                               @Nonnull CharSequence text,
                               long modificationStamp,
                               boolean physical);

    @Nonnull
    PsiFile createFileFromText(@Nonnull String name,
                               @Nonnull FileType fileType,
                               @Nonnull CharSequence text,
                               long modificationStamp,
                               boolean physical,
                               boolean markAsCopy);

    PsiFile createFileFromText(@Nonnull String name, @Nonnull Language language, @Nonnull CharSequence text);

    PsiFile createFileFromText(@Nonnull String name,
                               @Nonnull Language language,
                               @Nonnull LanguageVersion languageVersion,
                               @Nonnull CharSequence text);

    PsiFile createFileFromText(@Nonnull String name,
                               @Nonnull Language language,
                               @Nonnull CharSequence text,
                               boolean physical,
                               boolean markAsCopy);

    PsiFile createFileFromText(@Nonnull String name,
                               @Nonnull Language language,
                               @Nonnull CharSequence text,
                               boolean physical,
                               boolean markAsCopy,
                               boolean noSizeLimit);

    @Deprecated
    @DeprecationInfo("Use #createFileFromText() without Language parameter")
    @Nullable
    PsiFile createFileFromText(@Nonnull String name,
                               @Nonnull Language language,
                               @Nonnull LanguageVersion languageVersion,
                               @Nonnull CharSequence text,
                               boolean physical,
                               boolean markAsCopy,
                               boolean noSizeLimit);

    @Nullable
    default PsiFile createFileFromText(@Nonnull String name,
                                       @Nonnull LanguageVersion languageVersion,
                                       @Nonnull CharSequence text,
                                       boolean physical,
                                       boolean markAsCopy,
                                       boolean noSizeLimit) {
        return createFileFromText(name, languageVersion, text, physical, markAsCopy, noSizeLimit, null);
    }

    @Nullable
    PsiFile createFileFromText(@Nonnull String name,
                               @Nonnull LanguageVersion languageVersion,
                               @Nonnull CharSequence text,
                               boolean physical,
                               boolean markAsCopy,
                               boolean noSizeLimit,
                               @Nullable VirtualFile original);

    PsiFile createFileFromText(FileType fileType, String fileName, CharSequence chars, int startOffset, int endOffset);

    @Nullable
    PsiFile createFileFromText(@Nonnull CharSequence chars, @Nonnull PsiFile original);

    @Nullable
    PsiElement createElementFromText(@Nullable final String text,
                                     @Nonnull final Language language,
                                     @Nonnull final LanguageVersion languageVersion,
                                     @Nonnull final IElementType type,
                                     @Nullable final PsiElement context);
}