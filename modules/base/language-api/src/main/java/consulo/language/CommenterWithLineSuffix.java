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
package consulo.language;

import jakarta.annotation.Nonnull;

/**
 * Defines the support for line comments with suffix.
 *
 * @author traff
 */
public interface CommenterWithLineSuffix extends Commenter {
  /**
   * Returns the string which suffixes a line comment in the language.
   *
   * @return the line comment text
   */
  @Nonnull
  String getLineCommentSuffix();
}
