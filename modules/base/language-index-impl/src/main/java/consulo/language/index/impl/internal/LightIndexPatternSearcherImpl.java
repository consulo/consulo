// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.search.IndexPatternOccurrence;
import consulo.language.psi.search.IndexPatternSearch;
import consulo.language.psi.search.LightIndexPatternSearcher;
import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

/**
 * @author irengrig
 */
@ExtensionImpl
public class LightIndexPatternSearcherImpl extends IndexPatternSearcher implements LightIndexPatternSearcher {
    @Override
    @RequiredReadAction
    public void processQuery(
        @Nonnull IndexPatternSearch.SearchParameters queryParameters,
        @Nonnull Predicate<? super IndexPatternOccurrence> consumer
    ) {
        executeImpl(queryParameters, consumer);
    }
}
