// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.language.psi.stub.FileBasedIndex;
import consulo.language.psi.stub.IndexedFile;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.fileType.FileType;

/**
 * Returns {@code NO} for binary file types, and {@code UNSURE} for others (i.e. delegates to {@link #slowPathIfFileTypeHintUnsure}).
 */
public class NonBinaryFileTypeInputFilter extends BaseFileTypeInputFilter {
    private final FileBasedIndex.InputFilter myAcceptInput;

    public NonBinaryFileTypeInputFilter(FileBasedIndex.InputFilter acceptInput) {
        super(FileTypeSubstitutionStrategy.AFTER_SUBSTITUTION);
        myAcceptInput = acceptInput;
    }

    @Override
    public ThreeState acceptFileType(FileType fileType) {
        return fileType.isBinary() ? ThreeState.NO : ThreeState.UNSURE;
    }

    @Override
    public boolean slowPathIfFileTypeHintUnsure(IndexedFile file) {
        return myAcceptInput.acceptInput(file.getProject(), file.getFile());
    }
}
