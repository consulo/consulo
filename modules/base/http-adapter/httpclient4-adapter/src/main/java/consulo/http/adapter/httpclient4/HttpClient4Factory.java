/*
 * Copyright 2013-2024 consulo.io
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
package consulo.http.adapter.httpclient4;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.http.CertificateManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

/**
 * @author VISTALL
 * @since 2024-11-15
 */
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
@Singleton
public class HttpClient4Factory {
    private final CertificateManager myCertificateManager;

    @Inject
    public HttpClient4Factory(CertificateManager certificateManager) {
        myCertificateManager = certificateManager;
    }

    @Nonnull
    public HttpClientBuilder createBuilder() {
        return HttpClients.custom()
            .setSSLContext(myCertificateManager.getSslContext())
            .setSSLHostnameVerifier(myCertificateManager.getHostnameVerifier());
    }
}
