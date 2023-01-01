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
package consulo.component.extension;

import javax.annotation.Nonnull;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 28-Jun-22
 */
public final class ByClassGrouper<E> implements Function<List<E>, Function<Class, E>> {
  private static class GetterImpl<S> implements Function<Class, S> {
    private final Map<Class, S> myExtensionsByClass = new ConcurrentHashMap<>();

    public GetterImpl(List<S> extensions, Function<S, Class> getClassFunc) {
      for (S extension : extensions) {
        myExtensionsByClass.put(getClassFunc.apply(extension), extension);
      }
    }

    @Override
    public S apply(Class aClass) {
      S extension = myExtensionsByClass.get(aClass);
      // we found it by class
      if (extension != null) {
        return extension;
      }

      S valueFromSuper = processUntil(aClass, new HashSet<>(), myExtensionsByClass::get);
      if (valueFromSuper != null) {
        myExtensionsByClass.put(aClass, valueFromSuper);
        return valueFromSuper;
      }
      return null;
    }

    protected <S> S processUntil(Class baseClass, Set<Class> processed, Function<Class, S> getter) {
      if (!processed.add(baseClass)) {
        return null;
      }

      S value = getter.apply(baseClass);
      if (value != null) {
        return value;
      }

      Class superclass = baseClass.getSuperclass();
      if (superclass != null) {
        value = processUntil(superclass, processed, getter);
        if (value != null) {
          return value;
        }
      }

      for (Class interfaceClass : baseClass.getInterfaces()) {
        value = processUntil(interfaceClass, processed, getter);
        if (value != null) {
          return value;
        }
      }

      return null;
    }
  }

  @Nonnull
  public static <K> Function<List<K>, Function<Class, K>> build(Function<K, Class> getClassFunc) {
    return new ByClassGrouper<>(getClassFunc);
  }

  private final Function<E, Class> myGetClassFunc;

  private ByClassGrouper(Function<E, Class> getClassFunc) {
    myGetClassFunc = getClassFunc;
  }

  @Override
  public Function<Class, E> apply(List<E> extensions) {
    return new GetterImpl<>(extensions, myGetClassFunc);
  }
}
