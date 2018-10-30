/*
 * Copyright 2013-2018 consulo.io
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
package com.intellij.credentialStore;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.io.ByteBufferUtil;
import com.intellij.util.text.CharArrayCharSequence;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * from kotlin
 * <p>
 * clearable only if specified explicitly.
 * <p>
 * Case
 * 1) you create OneTimeString manually on user input.
 * 2) you store it in CredentialStore
 * 3) you consume it... BUT native credentials store do not store credentials immediately - write is postponed, so, will be an critical error.
 * <p>
 * so, currently - only credentials store implementations should set this flag on get.
 */
public class OneTimeString extends CharArrayCharSequence {
  public static OneTimeString from(byte[] value) throws CharacterCodingException {
    return from(value, 0, value.length, false);
  }

  public static OneTimeString from(byte[] value, boolean clearable) throws CharacterCodingException {
    return from(value, 0, value.length, clearable);
  }

  public static OneTimeString from(byte[] value, int offset, boolean clearable) throws CharacterCodingException {
    return from(value, offset, value.length - offset, clearable);
  }

  public static OneTimeString from(byte[] value, int offset, int length, boolean clearable) throws CharacterCodingException {
    if (length == 0) {
      return new OneTimeString(ArrayUtil.EMPTY_CHAR_ARRAY);
    }

    // jdk decodes to heap array, but since this code is very critical, we cannot rely on it, so, we don't use Charsets.UTF_8.decode()
    CharsetDecoder charsetDecoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
    char[] charArray = new char[(int)(value.length * (double)charsetDecoder.maxCharsPerByte())];
    charsetDecoder.reset();
    CharBuffer charBuffer = CharBuffer.wrap(charArray);
    CoderResult cr = charsetDecoder.decode(ByteBuffer.wrap(value, offset, length), charBuffer, true);
    if (!cr.isUnderflow()) {
      cr.throwException();
    }

    cr = charsetDecoder.flush(charBuffer);
    if (!cr.isUnderflow()) {
      cr.throwException();
    }

    Arrays.fill(value, offset, offset + length, (byte)0);
    return new OneTimeString(charArray, 0, charBuffer.position(), clearable);
  }

  private final boolean clearable;
  private AtomicReference<String> consumed = new AtomicReference<>();

  public OneTimeString(String value) {
    this(value.toCharArray());
  }

  public OneTimeString(char[] value) {
    this(value, 0, value.length, false);
  }

  public OneTimeString(char[] value, boolean clearable) {
    this(value, 0, value.length, clearable);
  }

  public OneTimeString(char[] value, int offset, int length) {
    this(value, offset, length, false);
  }

  public OneTimeString(char[] value, int offset, int length, boolean clearable) {
    super(value, offset, offset + length);
    this.clearable = clearable;
  }

  private void consume(boolean willBeCleared) {
    if (!clearable) {
      return;
    }

    if (!willBeCleared) {
      Optional.ofNullable(consumed.get()).ifPresent(it -> {
        throw new IllegalStateException("Already consumed: " + it + "\n---\n");
      });
    }
    else if (!consumed.compareAndSet(null, ExceptionUtil.currentStackTrace())) {
      throw new IllegalStateException("Already consumed at " + consumed.get());
    }
  }

  public String toString(boolean clear) {
    consume(clear);
    String result = super.toString();
    clear();
    return result;
  }

  private void clear() {
    if (clearable) {
      Arrays.fill(myChars, myStart, myEnd, '\u0000');
    }
  }

  public void appendTo(StringBuilder builder) {
    consume(false);
    builder.append(myChars, myStart, length());
  }

  public byte[] toByteArray() {
    return toByteArray(true);
  }

  // string will be cleared and not valid after
  public byte[] toByteArray(boolean clear) {
    consume(clear);

    ByteBuffer result = StandardCharsets.UTF_8.encode(CharBuffer.wrap(myChars, myStart, length()));
    if (clear) {
      clear();
    }
    return ByteBufferUtil.toByteArray(result);
  }

  public char[] toCharArray() {
    return toCharArray(true);
  }

  public char[] toCharArray(boolean clear) {
    consume(clear);
    if (clear) {
      char[] result = new char[length()];
      getChars(result, 0);
      clear();
      return result;
    }
    else {
      return getChars();
    }
  }

  @Nonnull
  public OneTimeString clone(boolean clear, boolean clearable) {
    return new OneTimeString(toCharArray(clear), clearable);
  }

  @Nonnull
  public String toString() {
    return toString(false);
  }

  @Override
  public boolean equals(Object anObject) {
    if (anObject instanceof CharSequence) {
      return StringUtil.equals(this, (CharSequence)anObject);
    }
    return super.equals(anObject);
  }
}
