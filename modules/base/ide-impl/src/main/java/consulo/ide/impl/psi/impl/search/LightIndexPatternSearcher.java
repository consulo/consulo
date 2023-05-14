// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.psi.impl.search;

import consulo.language.psi.search.IndexPatternOccurrence;
import consulo.language.psi.search.IndexPatternSearch;
import consulo.application.util.function.Processor;
import jakarta.annotation.Nonnull;

/**
 * @author irengrig
 */
public class LightIndexPatternSearcher extends IndexPatternSearcher {

  @Override
  public void processQuery(@Nonnull IndexPatternSearch.SearchParameters queryParameters, @Nonnull Processor<? super IndexPatternOccurrence> consumer) {
    executeImpl(queryParameters, consumer);
  }
}
