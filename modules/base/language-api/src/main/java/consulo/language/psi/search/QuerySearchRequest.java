// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.search;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.util.Query;
import consulo.application.util.ReadActionProcessor;
import consulo.application.util.function.Processor;
import consulo.language.psi.PsiReference;

import javax.annotation.Nonnull;
import java.util.function.BiPredicate;

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
                            @Nonnull final BiPredicate<? super PsiReference, ? super SearchRequestCollector> processor) {
    this.query = query;
    this.collector = collector;
    if (inReadAction) {
      this.processor = new ReadActionProcessor<>() {
        @RequiredReadAction
        @Override
        public boolean processInReadAction(PsiReference psiReference) {
          return processor.test(psiReference, collector);
        }
      };
    }
    else {
      this.processor = psiReference -> processor.test(psiReference, collector);
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
