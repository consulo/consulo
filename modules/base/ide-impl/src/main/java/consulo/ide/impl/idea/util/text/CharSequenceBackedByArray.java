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

package consulo.ide.impl.idea.util.text;

import jakarta.annotation.Nonnull;

/**
 * A char sequence base on a char array, Gives access to that array. May be used for performance optimizations
 *
 * @author Maxim.Mossienko
 * @see consulo.ide.impl.idea.util.text.CharArrayExternalizable
 * @see consulo.ide.impl.idea.util.text.CharArrayUtil#getChars(CharSequence, char[], int)
 * @see consulo.ide.impl.idea.util.text.CharArrayUtil#fromSequenceWithoutCopying(CharSequence)
 */
public interface CharSequenceBackedByArray extends consulo.util.lang.CharSequenceBackedByArray {
  @Nonnull
  char[] getChars();

  void getChars(@Nonnull char[] dst, int dstOffset);
}
