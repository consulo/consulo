/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util;

import com.intellij.util.containers.ContainerUtil;
import consulo.util.collection.HashingStrategy;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * @author max
 */
public class QueryFactory<Result, Parameters> {
  private final List<QueryExecutor<Result, Parameters>> myExecutors = ContainerUtil.createLockFreeCopyOnWriteList();

  public void registerExecutor(@Nonnull QueryExecutor<Result, Parameters> executor) {
    myExecutors.add(executor);
  }

  public void unregisterExecutor(@Nonnull QueryExecutor<Result, Parameters> executor) {
    myExecutors.remove(executor);
  }

  /**
   * @return query to perform the search. @param parameters of the search
   */
  @Nonnull
  public final Query<Result> createQuery(@Nonnull Parameters parameters) {
    return new ExecutorsQuery<>(parameters, getExecutors());
  }


  @Nonnull
  protected List<QueryExecutor<Result, Parameters>> getExecutors() {
    return myExecutors;
  }

  public boolean hasAnyExecutors() {
    return !getExecutors().isEmpty();
  }

  /**
   * @param parameters of the search
   * @return query to perform the search. Obtained results are automatically filtered wrt. equals() relation.
   */
  public final Query<Result> createUniqueResultsQuery(@Nonnull Parameters parameters) {
    return new UniqueResultsQuery<Result, Result>(createQuery(parameters));
  }

  /**
   * @param parameters      of the search
   * @param hashingStrategy strategy to factor results
   * @return query to perform the search. Obtained results are automatically filtered wrt. equals() relation.
   */
  public final Query<Result> createUniqueResultsQuery(@Nonnull Parameters parameters,
                                                      @Nonnull HashingStrategy<Result> hashingStrategy) {
    return new UniqueResultsQuery<>(createQuery(parameters), hashingStrategy);
  }

  /**
   * @param parameters      of the search
   * @param hashingStrategy strategy to factor results
   * @param mapper          function that maps results to their mapping counterparts.
   * @return query to perform the search. Obtained results are mapped to whatever objects that are automatically filtered wrt. equals()
   *         relation. Storing mapped objects instead of original elements may be wise wrt to memory consumption.
   */
  public final <T> Query<Result> createUniqueResultsQuery(@Nonnull Parameters parameters,
                                                          @Nonnull HashingStrategy<T> hashingStrategy,
                                                          @Nonnull Function<Result, T> mapper) {
    return new UniqueResultsQuery<>(createQuery(parameters), hashingStrategy, mapper);
  }
}
