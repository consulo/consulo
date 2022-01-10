/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.util.io;

import com.google.common.base.Charsets;
import com.intellij.util.text.CharArrayCharSequence;
import javax.annotation.Nonnull;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;

/**
 * @author VISTALL
 * @since 06-May-17
 * 
 */
// we must return string on subSequence() - JsonReaderEx will call toString in any case
public class CharSequenceBackedByChars extends CharArrayCharSequence {
  public ByteBuffer getByteBuffer() {
    return Charsets.UTF_8.encode(CharBuffer.wrap(myChars, myStart, length()));
  }

  public CharSequenceBackedByChars(CharBuffer charBuffer) {
    super(charBuffer.array(), charBuffer.arrayOffset(), charBuffer.position());
  }

  public CharSequenceBackedByChars(@Nonnull char... chars) {
    super(chars);
  }

  public CharSequenceBackedByChars(@Nonnull char[] chars, int start, int end) {
    super(chars, start, end);
  }

  public CharSequence subSequence(Integer start, int end) {
    return (start == 0 && end == length()) ? this : new String(myChars, myStart + start, end - start);
  }
}
