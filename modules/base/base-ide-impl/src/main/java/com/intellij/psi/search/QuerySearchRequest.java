// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.search;

import com.intellij.openapi.application.ReadActionProcessor;
import com.intellij.psi.PsiReference;
import com.intellij.util.PairProcessor;
import com.intellij.util.Processor;
import com.intellij.util.Query;
import javax.annotation.Nonnull;

/**
 * @author peter
 */
public class QuerySearchRequest {
  public final Query<PsiReference> query;
  public final SearchRequestCollector collector;
  public final Processor<? super PsiReference> processor;

  public QuerySearchRequest(@Nonnull Query<PsiReference> query,
                            @Nonnull final SearchRequestCollector collector,
                            boolean inReadAction,
                            @Nonnull final PairProcessor<? super PsiReference, ? super SearchRequestCollector> processor) {
    this.query = query;
    this.collector = collector;
    if (inReadAction) {
      this.processor = new ReadActionProcessor<PsiReference>() {
        @Override
        public boolean processInReadAction(PsiReference psiReference) {
          return processor.process(psiReference, collector);
        }
      };
    }
    else {
      this.processor = psiReference -> processor.process(psiReference, collector);
    }
  }

  public boolean runQuery() {
    return query.forEach(processor);
  }

  @Override
  public String toString() {
    return query + " -> " + collector;
  }
}
