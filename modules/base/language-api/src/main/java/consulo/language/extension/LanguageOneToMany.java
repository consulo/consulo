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

import consulo.language.Language;
import consulo.util.collection.ContainerUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 27-Jun-22
 */
public final class LanguageOneToMany<E extends LanguageExtension> implements Function<List<E>, ByLanguageValue<List<E>>> {
  private static class ByLanguageValueImpl<T extends LanguageExtension> implements ByLanguageValue<List<T>> {
    private final Map<Language, List<T>> myExtensions = new ConcurrentHashMap<>();

    private final boolean myWithAnyLanguage;

    public ByLanguageValueImpl(List<T> extensions, boolean withAnyLanguage) {
      myWithAnyLanguage = withAnyLanguage;

      for (T extension : extensions) {
        myExtensions.computeIfAbsent(extension.getLanguage(), i -> new ArrayList<>()).add(extension);
      }
    }

    @Nonnull
    @Override
    public List<T> get(@Nonnull Language language) {
      List<T> extension = myExtensions.get(language);
      if (extension != null) {
        return joinAny(language, extension);
      }

      Language base = language;
      while ((base = base.getBaseLanguage()) != null) {
        List<T> baseExtension = myExtensions.get(base);
        if (baseExtension != null) {
          myExtensions.put(language, baseExtension);
          return joinAny(language, baseExtension);
        }
      }
      
      return joinAny(language, List.of());
    }

    private List<T> joinAny(Language language, List<T> result) {
      if (!myWithAnyLanguage || language == Language.ANY) {
        return result;
      }

      List<T> anyExtensions = get(Language.ANY);
      return ContainerUtil.concat(result, anyExtensions);
    }
  }

  @Nonnull
  public static <E1 extends LanguageExtension> Function<List<E1>, ByLanguageValue<List<E1>>> build(boolean withAnyLanguage) {
    return new LanguageOneToMany<>(withAnyLanguage);
  }

  private final boolean myWithAnyLanguage;

  private LanguageOneToMany(boolean withAnyLanguage) {
    myWithAnyLanguage = withAnyLanguage;
  }

  @Override
  public ByLanguageValue<List<E>> apply(List<E> es) {
    return new ByLanguageValueImpl<>(es, myWithAnyLanguage);
  }
}
