// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.index.io.IndexId;
import consulo.language.index.impl.internal.GlobalIndexFilter;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.logging.Logger;
import consulo.virtualFileSystem.VirtualFile;

/**
 * {@link BaseGlobalFileTypeInputFilter} accepts all the directories
 */
public abstract class BaseGlobalFileTypeInputFilter implements GlobalIndexSpecificIndexingHint, GlobalIndexFilter {
    private final boolean myAcceptsDirectories;

    protected BaseGlobalFileTypeInputFilter() {
        this(true);
    }

    protected BaseGlobalFileTypeInputFilter(boolean acceptsDirectories) {
        myAcceptsDirectories = acceptsDirectories;
    }

    public boolean isAcceptsDirectories() {
        return myAcceptsDirectories;
    }

    @Override
    public final boolean isExcludedFromIndex(VirtualFile virtualFile, IndexId<?, ?> indexId) {
        Logger.getInstance(getClass()).error("Should not be invoked. Please use globalFileTypeHintForIndex instead");
        return false;
    }

    @Override
    public final FileBasedIndex.InputFilter globalInputFilterForIndex(IndexId<?, ?> indexId) {
        return affectsIndex(indexId) ? getFileTypeHintForAffectedIndex(indexId) : AcceptAllRegularFilesIndexingHint.INSTANCE;
    }

    protected abstract BaseFileTypeInputFilter getFileTypeHintForAffectedIndex(IndexId<?, ?> indexId);
}
