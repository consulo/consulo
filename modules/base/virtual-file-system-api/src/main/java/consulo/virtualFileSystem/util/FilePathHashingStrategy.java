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
package consulo.virtualFileSystem.util;

import consulo.platform.Platform;
import consulo.util.collection.CharSequenceHashingStrategy;
import consulo.util.collection.HashingStrategy;
import jakarta.annotation.Nonnull;

/**
 * @author max
 */
public class FilePathHashingStrategy {
  private FilePathHashingStrategy() {
  }

  @Nonnull
  public static HashingStrategy<String> create() {
    return create(Platform.current().fs().isCaseSensitive());
  }

  @Nonnull
  public static HashingStrategy<CharSequence> createForCharSequence() {
    return Platform.current().fs().isCaseSensitive()
      ? CharSequenceHashingStrategy.CASE_SENSITIVE : CharSequenceHashingStrategy.CASE_INSENSITIVE;
  }

  @Nonnull
  public static HashingStrategy<String> create(boolean caseSensitive) {
    return caseSensitive ? HashingStrategy.canonical() : HashingStrategy.CaseInsensitiveStringHashingStrategy.INSTANCE;
  }
}
