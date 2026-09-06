// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal.roots;

import consulo.language.psi.stub.IndexableSetContributor;
import consulo.language.index.impl.internal.roots.kind.IndexableSetContributorOrigin;

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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IndexableSetContributorOriginImpl other)) {
            return false;
        }
        return myIndexableSetContributor.equals(other.myIndexableSetContributor);
    }

    @Override
    public int hashCode() {
        return myIndexableSetContributor.hashCode();
    }
}
