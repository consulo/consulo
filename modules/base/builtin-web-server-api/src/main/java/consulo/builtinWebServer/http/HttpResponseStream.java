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
package consulo.builtinWebServer.http;

import java.nio.charset.StandardCharsets;

/**
 * Sink of a chunked response body. Each {@link #write} is delivered to the client immediately,
 * which makes this suitable for Server-Sent Events as well as for a single deferred chunk.
 *
 * @author VISTALL
 * @since 2026-08-03
 */
public interface HttpResponseStream {
    void write(byte[] chunk);

    default void write(String chunk) {
        write(chunk.getBytes(StandardCharsets.UTF_8));
    }

    void close();

    boolean isOpen();

    /**
     * Fires once, either when the client disconnects or when {@link #close} is called.
     */
    void onClose(Runnable listener);
}
