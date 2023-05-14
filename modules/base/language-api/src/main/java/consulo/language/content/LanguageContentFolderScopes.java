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
package consulo.language.content;

import consulo.content.ContentFolderTypeProvider;
import consulo.content.base.ExcludedContentFolderTypeProvider;

import jakarta.annotation.Nonnull;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 23:12/31.10.13
 */
public class LanguageContentFolderScopes {
  @Nonnull
  public static Predicate<ContentFolderTypeProvider> all() {
    return all(true);
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> of(@Nonnull ContentFolderTypeProvider provider) {
    return provider::equals;
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> excluded() {
    return of(ExcludedContentFolderTypeProvider.getInstance());
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> production() {
    return onlyProduction().or(it -> it.equals(ProductionResourceContentFolderTypeProvider.getInstance()));
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> test() {
    return onlyTest().or(it -> it.equals(TestResourceContentFolderTypeProvider.getInstance()));
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> productionAndTest() {
    return production().or(test());
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> onlyProduction() {
    return it -> it.equals(ProductionContentFolderTypeProvider.getInstance());
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> onlyTest() {
    return it -> it.equals(TestContentFolderTypeProvider.getInstance());
  }

  @Nonnull
  public static Predicate<ContentFolderTypeProvider> all(final boolean withExclude) {
    return withExclude ? it -> true : it -> !ExcludedContentFolderTypeProvider.getInstance().equals(it);
  }
}
