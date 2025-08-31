/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.impl.idea.openapi.util;

import consulo.annotation.DeprecationInfo;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;

import jakarta.annotation.Nonnull;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 2019-03-07
 */
@Deprecated
@DeprecationInfo("Use consulo.disposer.util.DisposerUtil")
public class DisposerUtil {
  public static <T> void add(T element, @Nonnull Collection<T> result, @Nonnull Disposable parentDisposable) {
    if (result.add(element)) {
      Disposer.register(parentDisposable, () -> result.remove(element));
    }
  }
}
