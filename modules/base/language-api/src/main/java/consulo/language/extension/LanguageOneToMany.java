/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.extension;

import consulo.component.extension.ExtensionWalker;
import consulo.language.Language;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 27-Jun-22
 */
public final class LanguageOneToMany<E extends LanguageExtension> implements Function<ExtensionWalker<E>, ByLanguageValue<List<E>>> {
  private static class ByLanguageValueImpl<T extends LanguageExtension> implements ByLanguageValue<List<T>> {
    private final Map<Language, List<T>> myExtensions = new ConcurrentHashMap<>();

    private final boolean myWithAnyLanguage;

    private final Map<Language, List<T>> myRawExtension = new HashMap<>();
    private final Function<List<T>, List<T>> mySorter;

    public ByLanguageValueImpl(ExtensionWalker<T> walker, boolean withAnyLanguage) {
      myWithAnyLanguage = withAnyLanguage;
      mySorter = walker.sorter();
      walker.walk(extension -> myRawExtension.computeIfAbsent(extension.getLanguage(), i -> new ArrayList<>()).add(extension));
    }

    @Nonnull
    @Override
    public List<T> get(@Nonnull Language l) {
      return myExtensions.computeIfAbsent(l, language -> {
        Set<T> allExtensions = new HashSet<>();
        // add any
        if (myWithAnyLanguage) {
          allExtensions.addAll(myRawExtension.getOrDefault(Language.ANY, List.of()));
        }

        allExtensions.addAll(myRawExtension.getOrDefault(language, List.of()));

        Language base = language;
        while ((base = base.getBaseLanguage()) != null) {
          allExtensions.addAll(myRawExtension.getOrDefault(base, List.of()));
        }

        return mySorter.apply(new ArrayList<>(allExtensions));
      });
    }
  }

  @Nonnull
  public static <E1 extends LanguageExtension> Function<ExtensionWalker<E1>, ByLanguageValue<List<E1>>> build(boolean withAnyLanguage) {
    return new LanguageOneToMany<>(withAnyLanguage);
  }

  private final boolean myWithAnyLanguage;

  private LanguageOneToMany(boolean withAnyLanguage) {
    myWithAnyLanguage = withAnyLanguage;
  }

  @Override
  public ByLanguageValue<List<E>> apply(ExtensionWalker<E> es) {
    return new ByLanguageValueImpl<>(es, myWithAnyLanguage);
  }
}
