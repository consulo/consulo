// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.search;

import com.intellij.psi.search.IndexPatternOccurrence;
import com.intellij.psi.search.searches.IndexPatternSearch;
import com.intellij.util.Processor;
import javax.annotation.Nonnull;

/**
 * @author irengrig
 */
public class LightIndexPatternSearcher extends IndexPatternSearcher {

  @Override
  public void processQuery(@Nonnull IndexPatternSearch.SearchParameters queryParameters, @Nonnull Processor<? super IndexPatternOccurrence> consumer) {
    executeImpl(queryParameters, consumer);
  }
}
