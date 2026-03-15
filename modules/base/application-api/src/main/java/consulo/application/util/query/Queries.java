// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util.query;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;

import java.util.Collection;
import java.util.function.Function;

/**
 * This class is intentionally package local.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public abstract class Queries {
  
  static Queries getInstance() {
    return Application.get().getInstance(Queries.class);
  }

  
  protected abstract <I, O> Query<O> transforming(Query<? extends I> base, Function<? super I, ? extends Collection<? extends O>> transformation);

  
  protected abstract <I, O> Query<O> flatMapping(Query<? extends I> base, Function<? super I, ? extends Query<? extends O>> mapper);
}
