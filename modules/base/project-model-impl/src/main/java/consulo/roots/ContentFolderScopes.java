/*
 * Copyright 2013-2016 consulo.io
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
package consulo.roots;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import consulo.roots.impl.*;
import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 23:12/31.10.13
 */
public class ContentFolderScopes {
  @Nonnull
  public static Predicate<ContentFolderTypeProvider> all() {
    return all(true);
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> of(@Nonnull ContentFolderTypeProvider provider) {
    return Predicates.equalTo(provider);
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> excluded() {
    return of(ExcludedContentFolderTypeProvider.getInstance());
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> production() {
    return Predicates.or(Predicates.<ContentFolderTypeProvider>equalTo(ProductionContentFolderTypeProvider.getInstance()),
                         Predicates.<ContentFolderTypeProvider>equalTo(ProductionResourceContentFolderTypeProvider.getInstance()));
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> test() {
    return Predicates.or(Predicates.<ContentFolderTypeProvider>equalTo(TestContentFolderTypeProvider.getInstance()),
                         Predicates.<ContentFolderTypeProvider>equalTo(TestResourceContentFolderTypeProvider.getInstance()));
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> productionAndTest() {
    return Predicates.or(production(), test());
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> onlyProduction() {
    return Predicates.<ContentFolderTypeProvider>equalTo(ProductionContentFolderTypeProvider.getInstance());
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> onlyTest() {
    return Predicates.<ContentFolderTypeProvider>equalTo(TestContentFolderTypeProvider.getInstance());
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> all(final boolean withExclude) {
    return withExclude
           ? Predicates.<ContentFolderTypeProvider>alwaysTrue()
           : Predicates.not(Predicates.<ContentFolderTypeProvider>equalTo(ExcludedContentFolderTypeProvider.getInstance()));
  }
}
