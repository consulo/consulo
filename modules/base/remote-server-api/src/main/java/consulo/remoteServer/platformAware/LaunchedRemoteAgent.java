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
package consulo.remoteServer.platformAware;

import org.jspecify.annotations.Nullable;

/**
 * A remote agent a plugin started itself - in a WSL distribution, a container, over ssh. Its endpoint lives only in memory.
 * <p>
 * Closing it stops the agent: the connection closes it after the platform disconnects, or when connecting fails.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
public interface LaunchedRemoteAgent extends AutoCloseable {
    String getHost();

    int getPort();

    boolean isUseTls();

    /**
     * @return SHA-256 of the agent leaf certificate (hex, colons allowed), empty for a CA-signed certificate
     */
    String getCertificateFingerprint();

    @Nullable String getToken();

    @Override
    void close();
}
