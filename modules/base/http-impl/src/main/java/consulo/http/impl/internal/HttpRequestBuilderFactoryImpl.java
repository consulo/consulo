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

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.http.HttpCertificateManager;
import consulo.http.HttpMethod;
import consulo.http.HttpProxyManager;
import consulo.http.HttpRequestBuilder;
import consulo.http.HttpRequestBuilderFactory;
import consulo.http.impl.internal.local.LocalHttpClientExecutor;
import consulo.http.impl.internal.local.LocalUrlConnectionExecutor;
import consulo.platform.Platform;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-02-13
 */
@ServiceImpl
@Singleton
public class HttpRequestBuilderFactoryImpl implements HttpRequestBuilderFactory {
    /**
     * Sends the requests of the local platform with {@link java.net.http.HttpClient} instead of
     * {@link java.net.URLConnection}.
     */
    public static final String USE_HTTP_CLIENT_PROPERTY = "consulo.http.use.http.client";

    private final Application myApplication;
    private final HttpRequestExecutor myLocalExecutor;

    @Inject
    public HttpRequestBuilderFactoryImpl(Application application,
                                         HttpProxyManager proxyManager,
                                         HttpCertificateManager certificateManager) {
        this(application, proxyManager, certificateManager, Boolean.getBoolean(USE_HTTP_CLIENT_PROPERTY));
    }

    HttpRequestBuilderFactoryImpl(Application application,
                                  HttpProxyManager proxyManager,
                                  HttpCertificateManager certificateManager,
                                  boolean useHttpClient) {
        myApplication = application;

        LocalUrlConnectionExecutor urlConnectionExecutor = new LocalUrlConnectionExecutor(proxyManager, certificateManager);
        myLocalExecutor = useHttpClient
            ? new LocalHttpClientExecutor(proxyManager, certificateManager, urlConnectionExecutor, () -> getProgressIndicator(application))
            : urlConnectionExecutor;
    }

    private static @Nullable ProgressIndicator getProgressIndicator(Application application) {
        ProgressIndicatorProvider provider = application.getProgressManager();
        return provider == null ? null : provider.getProgressIndicator();
    }

    @Override
    public HttpRequestBuilder newBuilder(Platform platform, String url, HttpMethod httpMethod) {
        if (!Platform.LOCAL.equals(platform.getId())) {
            // TODO send from a remote platform
            throw new UnsupportedOperationException("Requests can not be sent from the platform " + platform.getId());
        }
        return newBuilder(url, httpMethod);
    }

    /**
     * The current platform is the local one - it is not asked for.
     */
    @Override
    public HttpRequestBuilder newBuilder(String url, HttpMethod httpMethod) {
        return new HttpRequestBuilderImpl(myApplication, myLocalExecutor, url, httpMethod);
    }
}
