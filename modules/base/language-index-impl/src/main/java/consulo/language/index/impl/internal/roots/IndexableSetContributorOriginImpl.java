// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.language.psi.stub.IndexableSetContributor;
import consulo.language.index.impl.internal.roots.kind.IndexableSetContributorOrigin;
import org.jspecify.annotations.Nullable;

class IndexableSetContributorOriginImpl implements IndexableSetContributorOrigin {
    private final IndexableSetContributor myIndexableSetContributor;

    IndexableSetContributorOriginImpl(IndexableSetContributor indexableSetContributor) {
        myIndexableSetContributor = indexableSetContributor;
    }

    @Override
    public IndexableSetContributor getIndexableSetContributor() {
        return myIndexableSetContributor;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return this == o
            || o instanceof IndexableSetContributorOriginImpl that && myIndexableSetContributor.equals(that.myIndexableSetContributor);
    }

    @Override
    public int hashCode() {
        return myIndexableSetContributor.hashCode();
    }
}
