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
package consulo.util.lang.function;

import consulo.annotation.DeprecationInfo;

import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * @author max
 */
@Deprecated
@DeprecationInfo("Use BiFunction")
@FunctionalInterface
public interface PairFunction<Arg1, Arg2, ResultType> extends BiFunction<Arg1, Arg2, ResultType> {
  @Nullable
  ResultType fun(Arg1 t, Arg2 v);

  @Override
  default ResultType apply(Arg1 arg1, Arg2 arg2) {
    return fun(arg1, arg2);
  }
}
