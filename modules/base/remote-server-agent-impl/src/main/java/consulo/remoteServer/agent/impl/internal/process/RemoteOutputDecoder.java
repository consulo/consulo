/*
 * Copyright 2013-2026 consulo.io
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
package consulo.remoteServer.agent.impl.internal.process;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

class RemoteOutputDecoder {
    private final CharsetDecoder myDecoder;
    private ByteBuffer myPending = ByteBuffer.allocate(0);

    RemoteOutputDecoder(Charset charset) {
        myDecoder = charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);
    }

    String decode(byte[] bytes) {
        ByteBuffer input = ByteBuffer.allocate(myPending.remaining() + bytes.length);
        input.put(myPending).put(bytes).flip();

        CharBuffer output = CharBuffer.allocate((int) Math.ceil(input.remaining() * (double) myDecoder.maxCharsPerByte()) + 1);
        myDecoder.decode(input, output, false);
        myPending = input.slice();
        return output.flip().toString();
    }

    String flush() {
        CharBuffer output = CharBuffer.allocate(myPending.remaining() + 1);
        myDecoder.decode(myPending, output, true);
        myDecoder.flush(output);
        myPending = ByteBuffer.allocate(0);
        return output.flip().toString();
    }
}
