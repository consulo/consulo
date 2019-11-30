/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.util.lang.function;

import consulo.util.lang.Pair;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

/**
 * @author gregsh
 */
@SuppressWarnings("unchecked")
public class Functions {
  private static final Function ID = (MonoFunction) o -> o;

  private static final Function NULL = o -> null;

  private static final Function TO_STRING = String::valueOf;

  public static <A> MonoFunction<A> id() {
    return (MonoFunction<A>)ID;
  }

  public static <A, B> Function<A, B> constant(final B b) {
    return new Function<A, B>() {
      @Override
      public B apply(A a) {
        return b;
      }
    };
  }

  public static <A, B> Function<A, B> identity() {
    return ID;
  }

  public static <A, B> Function<A, B> cast(Class<B> clazz) {
    return ID;
  }

  public static <A, B, C> Function<A, C> compose(final Function<A, B> f1, final Function<B, ? extends C> f2) {
    return new Function<A, C>() {
      @Override
      public C apply(A a) {
        return f2.apply(f1.apply(a));
      }
    };
  }

  public static <A> Function<A, String> TO_STRING() {
    return TO_STRING;
  }

  public static <A, B> Function<A, B> fromMap(final Map<A, B> map) {
    return map::get;
  }

  private static final Function<Object, Class> TO_CLASS = Object::getClass;

  public static <T> Function<T, Class> TO_CLASS() {
    return (Function<T, Class>)TO_CLASS;
  }

  private static final Function PAIR_FIRST = (Function<Pair<?, ?>, Object>)pair -> Pair.getFirst(pair);

  private static final Function PAIR_SECOND = (Function<Pair<?, ?>, Object>)pair -> Pair.getSecond(pair);

  public static <A> Function<Pair<A, ?>, A> pairFirst() {
    return (Function<Pair<A, ?>, A>)PAIR_FIRST;
  }

  public static <B> Function<Pair<?, B>, B> pairSecond() {
    return (Function<Pair<?, B>, B>)PAIR_SECOND;
  }

  public static MonoFunction<Integer> intIncrement() {
    return integer -> integer + 1;
  }

  private static final Function WRAP_ARRAY = t -> t == null ? Collections.emptyList() : Arrays.asList(t);

  public static <T> Function<T[], Iterable<T>> wrapArray() {
    return (Function<T[], Iterable<T>>)WRAP_ARRAY;
  }
}