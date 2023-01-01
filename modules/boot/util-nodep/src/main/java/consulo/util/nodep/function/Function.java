/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.util.nodep.function;

import consulo.util.nodep.Functions;

/**
 * @author max
 * @author Konstantin Bulenkov
 *
 * @see Functions for some common implementations
 *
 * Consider to use java.util.function.Function
 */
@SuppressWarnings({"unchecked"})
public interface Function<Param, Result> {
  Result fun(Param param);

  Function ID = new Function.Mono() {
    @Override
    public Object fun(Object o) {
      return o;
    }
  };

  Function TO_STRING = new Function() {
    @Override
    public Object fun(Object o) {
      return String.valueOf(o);
    }
  };

  interface Mono<T> extends Function<T, T> {}
}
