/*
 * Copyright 2000-2010 JetBrains s.r.o.
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
package consulo.application.util.function;

import consulo.annotation.DeprecationInfo;

import java.util.function.Predicate;

/**
 * @param <T> Input value type.
 * @see consulo.ide.impl.idea.util.CommonProcessors
 */
@Deprecated
@DeprecationInfo("Use Predicate")
public interface Processor<T> extends Predicate<T> {
  @Deprecated
  @DeprecationInfo(value = "Use CommonProcessors#alwaysTrue()")
  Processor TRUE = new Processor() {
    @Override
    public boolean process(Object o) {
      return true;
    }
  };
  /**
   * @param t consequently takes value of each element of the set this processor is passed to for processing.
   * @return {@code true} to continue processing or {@code false} to stop.
   */
  boolean process(T t);

  @Override
  default boolean test(T t) {
    return process(t);
  }
}