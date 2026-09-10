// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.language.psi.stub.IndexedFile;
import consulo.logging.Logger;
import consulo.util.lang.ThreeState;
import consulo.virtualFileSystem.fileType.FileType;

public final class AcceptAllRegularFilesIndexingHint extends BaseFileTypeInputFilter {
    private static final Logger LOG = Logger.getInstance(AcceptAllRegularFilesIndexingHint.class);

    public static final AcceptAllRegularFilesIndexingHint INSTANCE = new AcceptAllRegularFilesIndexingHint();

    private AcceptAllRegularFilesIndexingHint() {
        super(FileTypeSubstitutionStrategy.BEFORE_SUBSTITUTION);
    }

    @Override
    public ThreeState acceptFileType(FileType fileType) {
        return ThreeState.YES;
    }

    @Override
    public boolean slowPathIfFileTypeHintUnsure(IndexedFile file) {
        LOG.error("Should not be invoked. acceptFileType never returns UNSURE.");
        return true;
    }
}
