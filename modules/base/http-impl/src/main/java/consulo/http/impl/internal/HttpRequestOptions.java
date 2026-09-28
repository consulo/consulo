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
package consulo.http.impl.internal;

import consulo.http.HttpMethod;
import consulo.http.HttpVersion;
import org.jspecify.annotations.Nullable;

import javax.net.ssl.HostnameVerifier;
import java.util.Map;

/**
 * What a {@link HttpRequestBuilderImpl builder} was told to send - plain data, which an {@link HttpRequestExecutor}
 * sends.
 *
 * @param hostnameVerifier checks the host name of the local platform only - it can not be sent to another platform
 * @param useProxy         the proxy settings of the IDE - they apply to the local platform only
 * @author VISTALL
 * @since 2026-09-28
 */
public record HttpRequestOptions(
    String url,
    HttpMethod method,
    Map<String, String> headers,
    byte @Nullable [] body,
    @Nullable HttpVersion version,
    int connectTimeout,
    int readTimeout,
    int redirectLimit,
    boolean gzip,
    boolean forceHttps,
    boolean useProxy,
    @Nullable HostnameVerifier hostnameVerifier,
    boolean allowErrorCodes
) {
}
