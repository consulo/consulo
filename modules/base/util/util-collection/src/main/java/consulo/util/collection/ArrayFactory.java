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

/*
 * @author max
 */
package consulo.util.collection;

import javax.annotation.Nonnull;
import java.util.function.IntFunction;

@FunctionalInterface
public interface ArrayFactory<T> extends IntFunction<T[]> {
  @Nonnull
  static <K> ArrayFactory<K> of(@Nonnull IntFunction<K[]> factory) {
    return new ArrayFactory<>() {
      private K[] myEmptyArray;

      @Nonnull
      @Override
      public K[] create(int count) {
        if (count == 0) {
          if (myEmptyArray == null) {
            myEmptyArray = factory.apply(0);
          }
          return myEmptyArray;
        }
        else {
          return factory.apply(count);
        }
      }
    };
  }

  @Nonnull
  T[] create(int count);

  @Override
  default T[] apply(int count) {
    return create(count);
  }
}