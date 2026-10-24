// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.http.HttpCertificateManager;
import consulo.http.HttpProxyManager;

import java.net.http.HttpClient;
import java.time.Duration;

public class HttpClientFactoryBase implements HttpClientFactory {
    protected boolean useProxy() {
        return true;
    }

    protected long getConnectionTimeoutMillis() {
        return HttpProxyManager.CONNECTION_TIMEOUT;
    }

    @Override
    public HttpClient createClient() {
        HttpProxyManager proxyManager = HttpProxyManager.getInstance();
        return HttpClient.newBuilder()
            .executor(AppExecutorUtil.getAppExecutorService())
            .sslContext(HttpCertificateManager.getInstance().getSslContext())
            .authenticator(proxyManager.getProxyAuthenticator())
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofMillis(getConnectionTimeoutMillis()))
            .proxy(useProxy() ? proxyManager.getProxySelector() : HttpClient.Builder.NO_PROXY)
            .build();
    }
}
