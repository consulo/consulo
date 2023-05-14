/*
 * Copyright 2013-2022 consulo.io
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
package consulo.util.io;

import consulo.util.lang.CharArrayUtil;

import jakarta.annotation.Nonnull;
import java.io.Reader;

/**
 * @author VISTALL
 * @since 23-Apr-22
 */
public class Readers {
  @Nonnull
  public static Reader readerFromCharSequence(@Nonnull CharSequence text) {
    char[] chars = CharArrayUtil.fromSequenceWithoutCopying(text);
    //noinspection IOResourceOpenedButNotSafelyClosed
    return chars == null ? new CharSequenceReader(text.toString()) : new UnsyncCharArrayReader(chars, 0, text.length());
  }
}
