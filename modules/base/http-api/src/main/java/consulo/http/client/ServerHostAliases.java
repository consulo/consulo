// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.http.client;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class ServerHostAliases {
    private ServerHostAliases() {
    }

    /**
     * Parses a host alias setting into a map from an alias host to the server.
     * <p>
     * The value is a comma-separated list of {@code <alias>=<server>} and {@code <alias>} entries.
     * An entry without a server maps the alias to {@code defaultServer}.
     * {@code parseServer} gets the trimmed server text and returns null for an invalid server.
     * The function ignores an entry with an invalid server.
     */
    public static <S extends ServerPath> Map<String, S> parseServerHostAliases(String value,
                                                                               S defaultServer,
                                                                               Function<String, @Nullable S> parseServer) {
        Map<String, S> result = new LinkedHashMap<>();
        for (String entry : value.split(",")) {
            int separator = entry.indexOf('=');
            String alias = (separator < 0 ? entry : entry.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
            if (alias.isEmpty()) {
                continue;
            }
            String serverText = separator < 0 ? "" : entry.substring(separator + 1).trim();
            S server = serverText.isEmpty() ? defaultServer : parseServer.apply(serverText);
            if (server != null) {
                result.put(alias, server);
            }
        }
        return result;
    }

    /**
     * Finds the server for {@code aliasServer}, the server from the alias setting.
     * The function prefers the server with the same URI as {@code aliasServer}.
     * Then it takes the first server with the same host, so the server URL comes from that server.
     * The function does not check the credentials for the server.
     * Without such a server, the function returns {@code aliasServer}.
     */
    public static <S extends ServerPath> S findServerForAlias(Collection<S> servers, S aliasServer) {
        URI aliasUri = aliasServer.toURI();
        for (S server : servers) {
            if (server.toURI().equals(aliasUri)) {
                return server;
            }
        }
        for (S server : servers) {
            String host = server.toURI().getHost();
            if (host != null && host.equalsIgnoreCase(aliasUri.getHost())) {
                return server;
            }
        }
        return aliasServer;
    }
}
