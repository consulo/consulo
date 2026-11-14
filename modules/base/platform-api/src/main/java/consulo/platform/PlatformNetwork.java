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
package consulo.platform;

import java.io.IOException;

/**
 * TCP of a platform's machine - connections are made and ports are bound there. For a remote platform the IDE machine
 * reaches them through the platform's own channel, no port of that machine is opened to the network.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public interface PlatformNetwork {
    /**
     * A connection to {@code host:port} as this machine resolves it - usually {@code 127.0.0.1}.
     */
    PlatformSocket connect(String host, int port) throws IOException;

    /**
     * A server socket of this machine - a process of this machine connects to {@link PlatformServerSocket#getPort()}
     * of {@code host}, the IDE accepts the connections.
     *
     * @param port {@code 0} for any free port
     */
    PlatformServerSocket bind(String host, int port) throws IOException;

    /**
     * An address of the IDE machine leading to {@code host:port} of this machine - for a client which can only connect by
     * an address (a debugger interface, an RMI client). Forwarded until closed.
     */
    default ForwardedPort forward(String host, int port) throws IOException {
        return forward(host, port, 0);
    }

    /**
     * Same as {@link #forward(String, int)}, on the given port of the IDE machine - for an address the client takes from
     * the other side as is (the stubs of RMI name the port of their server).
     *
     * @param localPort the port of the IDE machine, {@code 0} for any free one
     */
    ForwardedPort forward(String host, int port, int localPort) throws IOException;
}
