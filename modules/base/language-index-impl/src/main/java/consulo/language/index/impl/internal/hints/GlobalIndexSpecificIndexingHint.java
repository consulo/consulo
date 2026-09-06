// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.index.impl.internal.hints;

import consulo.index.io.IndexId;
import consulo.language.psi.stub.FileBasedIndex.InputFilter;

/**
 * Same as {@link FileTypeIndexingHint}, but to be used with {@link consulo.language.index.impl.internal.GlobalIndexFilter}. All the
 * general hint rules are applicable here and work the same way as they do for {@link FileTypeIndexingHint}.
 *
 * @see FileTypeIndexingHint
 */
public interface GlobalIndexSpecificIndexingHint {
    /**
     * @return index-specific hint. There may be several global hints. All the hints will be ANDed with all the other global and
     * non-global hints applicable to given indexId. Therefore, if current hint does not care about provided indexId,
     * it should return {@link AcceptAllRegularFilesIndexingHint}.
     */
    InputFilter globalInputFilterForIndex(IndexId<?, ?> indexId);
}
