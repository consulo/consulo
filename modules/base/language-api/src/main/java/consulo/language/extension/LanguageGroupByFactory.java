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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 24-Jun-22
 */
public class LanguageGroupByFactory<E extends LanguageExtension> implements Function<List<E>, ByLanguageValue<E>> {
  private static class ByLanguageValueWithBase<T extends LanguageExtension> implements ByLanguageValue<T> {
    private final Map<Language, T> myExtensions = new ConcurrentHashMap<>();

    private final T myDefaultImplementation;

    public ByLanguageValueWithBase(List<T> extensions, T defaultImplementation) {
      myDefaultImplementation = defaultImplementation;
      for (T extension : extensions) {
        myExtensions.put(extension.getLanguage(), extension);
      }
    }

    @Nullable
    @Override
    public T get(@Nonnull Language language) {
      T extension = myExtensions.get(language);
      if (extension != null) {
        return extension;
      }

      Language base = null;
      while ((base = language.getBaseLanguage()) != null) {
        T baseExtension = myExtensions.get(base);
        if (baseExtension != null) {
          myExtensions.put(language, baseExtension);
          return baseExtension;
        }
      }
      return myDefaultImplementation;
    }
  }

  @Nonnull
  public static <E1 extends LanguageExtension> Function<List<E1>, ByLanguageValue<E1>> build() {
    return build(null);
  }

  @Nonnull
  public static <E1 extends LanguageExtension> Function<List<E1>, ByLanguageValue<E1>> build(@Nullable E1 defaultImpl) {
    return new LanguageGroupByFactory<>(defaultImpl);
  }

  private final E myDefaultImplementation;

  private LanguageGroupByFactory(E defaultImpl) {
    myDefaultImplementation = defaultImpl;
  }

  @Override
  public ByLanguageValue<E> apply(List<E> es) {
    return new ByLanguageValueWithBase<>(es, myDefaultImplementation);
  }
}
