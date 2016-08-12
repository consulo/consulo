/*
 * Copyright 2013 must-be.org
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
package org.mustbe.consulo.roots;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.intellij.openapi.util.NotNullFactory;
import consulo.roots.ContentFolderTypeProvider;
import org.jetbrains.annotations.NotNull;
import org.mustbe.consulo.roots.impl.*;

/**
 * @author VISTALL
 * @since 23:12/31.10.13
 */
public class ContentFolderScopes {
  private static final int ALL = 1;
  private static final int ALL_WITHOUT_EXCLUDE = 2;
  private static final int PRODUCTION = 3;
  private static final int TEST = 4;
  private static final int ONLY_PRODUCTION = 5;
  private static final int ONLY_TEST = 6;
  private static final int PRODUCTION_AND_TEST = 7;

  @NotNull
  public static Predicate<ContentFolderTypeProvider> all() {
    return all(true);
  }

  @NotNull
  public static Predicate<ContentFolderTypeProvider> of(@NotNull ContentFolderTypeProvider provider) {
    return Predicates.equalTo(provider);
  }

  @NotNull
  public static Predicate<ContentFolderTypeProvider> excluded() {
    return of(ExcludedContentFolderTypeProvider.getInstance());
  }

  @NotNull
  public static Predicate<ContentFolderTypeProvider> production() {
    return cacheScope(PRODUCTION, new NotNullFactory<Predicate<ContentFolderTypeProvider>>() {
      @NotNull
      @Override
      public Predicate<ContentFolderTypeProvider> create() {
        return Predicates.or(Predicates.<ContentFolderTypeProvider>equalTo(ProductionContentFolderTypeProvider.getInstance()),
                             Predicates.<ContentFolderTypeProvider>equalTo(ProductionResourceContentFolderTypeProvider.getInstance()));
      }
    });
  }

  @NotNull
  public static Predicate<ContentFolderTypeProvider> test() {
    return cacheScope(TEST, new NotNullFactory<Predicate<ContentFolderTypeProvider>>() {
      @NotNull
      @Override
      public Predicate<ContentFolderTypeProvider> create() {
        return Predicates.or(Predicates.<ContentFolderTypeProvider>equalTo(TestContentFolderTypeProvider.getInstance()),
                             Predicates.<ContentFolderTypeProvider>equalTo(TestResourceContentFolderTypeProvider.getInstance()));
      }
    });
  }

  @NotNull
  public static Predicate<ContentFolderTypeProvider> productionAndTest() {
    return cacheScope(PRODUCTION_AND_TEST, new NotNullFactory<Predicate<ContentFolderTypeProvider>>() {
      @NotNull
      @Override
      public Predicate<ContentFolderTypeProvider> create() {
        return Predicates.or(production(), test());
      }
    });
  }

  @NotNull
  public static Predicate<ContentFolderTypeProvider> onlyProduction() {
    return cacheScope(ONLY_PRODUCTION, new NotNullFactory<Predicate<ContentFolderTypeProvider>>() {
      @NotNull
      @Override
      public Predicate<ContentFolderTypeProvider> create() {
        return Predicates.<ContentFolderTypeProvider>equalTo(ProductionContentFolderTypeProvider.getInstance());
      }
    });
  }

  @NotNull
  public static Predicate<ContentFolderTypeProvider> onlyTest() {
    return cacheScope(ONLY_TEST, new NotNullFactory<Predicate<ContentFolderTypeProvider>>() {
      @NotNull
      @Override
      public Predicate<ContentFolderTypeProvider> create() {
        return Predicates.<ContentFolderTypeProvider>equalTo(TestContentFolderTypeProvider.getInstance());
      }
    });
  }

  @NotNull
  public static Predicate<ContentFolderTypeProvider> all(final boolean withExclude) {
    return cacheScope(withExclude ? ALL : ALL_WITHOUT_EXCLUDE, new NotNullFactory<Predicate<ContentFolderTypeProvider>>() {
      @NotNull
      @Override
      public Predicate<ContentFolderTypeProvider> create() {
        return withExclude
               ? Predicates.<ContentFolderTypeProvider>alwaysTrue()
               : Predicates.not(Predicates.<ContentFolderTypeProvider>equalTo(ExcludedContentFolderTypeProvider.getInstance()));
      }
    });
  }

  @NotNull
  private static Predicate<ContentFolderTypeProvider> cacheScope(int id, NotNullFactory<Predicate<ContentFolderTypeProvider>> lazyFactory) {
    return lazyFactory.create();
  }
}
