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

/**
 * An address of the IDE machine leading to a port of a platform's machine, see {@link PlatformNetwork#forward}.
 *
 * @author VISTALL
 * @since 2026-11-12
 */
public interface ForwardedPort extends AutoCloseable {
    String host();

    int port();

    /**
     * Stops forwarding - the connections made through it are closed too.
     */
    @Override
    void close();
}
