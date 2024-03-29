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

package consulo.virtualFileSystem.fileType;

import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public interface FileNameMatcher {
  /**
   * @deprecated use {@link #acceptsCharSequence(CharSequence)}
   */
  @Deprecated
  default boolean accept(@Nonnull String fileName) {
    return acceptsCharSequence(fileName);
  }

  /**
   * This method must be overridden in specific matchers, it's default only for compatibility reasons.
   *
   * @return whether the given file name is accepted by this matcher.
   */
  default boolean acceptsCharSequence( @Nonnull CharSequence fileName) {
    return accept(fileName.toString());
  }

  @Nonnull
  String getPresentableString();
}
