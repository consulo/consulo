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
package consulo.ide.impl.idea.util.containers;

import consulo.annotation.DeprecationInfo;

import java.util.function.Function;

@FunctionalInterface
@Deprecated
@DeprecationInfo("Use java.util.function.Function")
public interface Convertor<Src, Dst> extends Function<Src, Dst> {
  IntoSelf SELF = new IntoSelf();

  class IntoSelf<Src> implements Convertor<Src, Src> {
    public Src convert(Src o) {
      return o;
    }
  }

  Dst convert(Src o);

  @Override
  default Dst apply(Src src) {
    return convert(src);
  }
}
