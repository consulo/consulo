// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.language.impl.internal.psi.stub.IndexedFileImpl;
import consulo.language.internal.SubstitutedFileType;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IndexedFile;
import consulo.project.Project;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

/**
 * Base implementation of filetype-hint-aware {@link FileBasedIndex.InputFilter}.
 * <p>
 * Contains default implementation of {@code acceptInput(project, file)} which delegate to hints.
 * <p>
 * If filetype is a {@link SubstitutedFileType} there are two options how to invoke {@link #acceptFileType}: with filetype before
 * substitution as an argument ({@link SubstitutedFileType#getOriginalFileType()}), or filetype after substitution
 * ({@link SubstitutedFileType#getFileType()}).
 * <p>
 * Directories are rejected by this filter
 *
 * @see consulo.language.psi.LanguageSubstitutor
 */
public abstract class BaseFileTypeInputFilter implements FileBasedIndex.InputFilter, FileTypeIndexingHint {
    private final FileTypeSubstitutionStrategy myFileTypeStrategy;

    /**
     * @param fileTypeStrategy strategy to resolve {@link SubstitutedFileType}. When in doubt - prefer
     *                         {@link FileTypeSubstitutionStrategy#BEFORE_SUBSTITUTION}, because calculating substituted file type is
     *                         not free.
     */
    protected BaseFileTypeInputFilter(FileTypeSubstitutionStrategy fileTypeStrategy) {
        myFileTypeStrategy = fileTypeStrategy;
    }

    @Override
    public final ThreeState acceptsFileTypeFastPath(FileType fileType) {
        FileType fileTypeToUse;
        if (fileType instanceof SubstitutedFileType substitutedFileType) {
            fileTypeToUse = myFileTypeStrategy == FileTypeSubstitutionStrategy.BEFORE_SUBSTITUTION
                ? substitutedFileType.getOriginalFileType()
                : substitutedFileType.getFileType();
        }
        else {
            fileTypeToUse = fileType;
        }

        return acceptFileType(fileTypeToUse);
    }

    @Override
    public final boolean acceptInput(@Nullable Project project, VirtualFile file) {
        IndexedFileImpl indexedFile = new IndexedFileImpl(file, file.getFileType());
        indexedFile.setProject(project);
        return acceptInput(indexedFile);
    }

    public final boolean acceptInput(IndexedFile file) {
        if (file.getFile().isDirectory()) {
            return false;
        }
        return switch (acceptsFileTypeFastPath(file.getFileType())) {
            case YES -> true;
            case NO -> false;
            case UNSURE -> slowPathIfFileTypeHintUnsure(file);
        };
    }

    public abstract ThreeState acceptFileType(FileType fileType);
}
