// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.language.psi.stub.IndexedFile;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.fileType.FileType;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/**
 * Returns {@code YES} or {@code NO} for given filetype predicate. Never returns {@code UNSURE}, therefore {@code acceptInput} is never
 * invoked.
 */
public class FileTypeInputFilterPredicate extends BaseFileTypeInputFilter {
    private final Predicate<FileType> myPredicate;

    public FileTypeInputFilterPredicate(Predicate<FileType> predicate) {
        super(FileTypeSubstitutionStrategy.AFTER_SUBSTITUTION);
        myPredicate = predicate;
    }

    public FileTypeInputFilterPredicate(FileTypeSubstitutionStrategy fileTypeStrategy, Predicate<FileType> predicate) {
        super(fileTypeStrategy);
        myPredicate = predicate;
    }

    public FileTypeInputFilterPredicate(FileType... fileTypes) {
        this(fileTypeListPredicate(Arrays.asList(fileTypes)));
    }

    private static Predicate<FileType> fileTypeListPredicate(List<FileType> fileTypes) {
        return fileType -> fileTypes.contains(fileType);
    }

    @Override
    public boolean slowPathIfFileTypeHintUnsure(IndexedFile file) {
        return false; // for directories
    }

    @Override
    public ThreeState acceptFileType(FileType fileType) {
        return ThreeState.fromBoolean(myPredicate.test(fileType));
    }
}
