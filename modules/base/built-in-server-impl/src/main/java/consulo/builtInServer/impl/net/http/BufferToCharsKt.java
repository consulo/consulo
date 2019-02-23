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
package consulo.builtInServer.impl.net.http;

import io.netty.buffer.ByteBuf;
import io.netty.util.CharsetUtil;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

/**
 * @author VISTALL
 * @since 06-May-17
 * <p>
 * from kotlin platform\platform-impl\src\org\jetbrains\io\bufferToChars.kt
 */
public class BufferToCharsKt {

  public static void readIntoCharBuffer(ByteBuf byteBuf, CharBuffer charBuffer) {
    readIntoCharBuffer(byteBuf, byteBuf.readableBytes(), charBuffer);
  }

  public static void readIntoCharBuffer(ByteBuf byteBuf, int byteCount, CharBuffer charBuffer) {
    CharsetDecoder decoder = CharsetUtil.decoder(CharsetUtil.UTF_8);
    if (byteBuf.nioBufferCount() == 1) {
      decodeString(decoder, byteBuf.internalNioBuffer(byteBuf.readerIndex(), byteCount), charBuffer);
    }
    else {
      ByteBuf buffer = byteBuf.alloc().heapBuffer(byteCount);
      try {
        buffer.writeBytes(byteBuf, byteBuf.readerIndex(), byteCount);
        decodeString(decoder, buffer.internalNioBuffer(0, byteCount), charBuffer);
      }
      finally {
        buffer.release();
      }
    }
  }

  private static void decodeString(CharsetDecoder decoder, ByteBuffer src, CharBuffer dst) {
    try {
      CoderResult cr = decoder.decode(src, dst, true);
      if (!cr.isUnderflow()) {
        cr.throwException();
      }
      cr = decoder.flush(dst);
      if (!cr.isUnderflow()) {
        cr.throwException();
      }
    }
    catch (CharacterCodingException x) {
      throw new IllegalStateException(x);
    }
  }
}
