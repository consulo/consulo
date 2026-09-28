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

import consulo.application.Application;
import consulo.http.HttpMethod;
import consulo.http.HttpRequestBuilder;

import static org.mockito.Mockito.mock;

/**
 * Requests of the local platform, sent by the executor an {@link EachExecutorTest} runs the test with - where the two
 * behave differently, a test asks {@link #isHttpClient()}.
 *
 * @author VISTALL
 * @since 2026-09-28
 */
public abstract class HttpRequestTestCase {
    private boolean myHttpClient;
    private HttpRequestBuilderFactoryImpl myFactory;

    void setHttpClient(boolean httpClient) {
        myHttpClient = httpClient;
        myFactory = new HttpRequestBuilderFactoryImpl(mock(Application.class), httpClient);
    }

    /**
     * Sent by {@link java.net.http.HttpClient} - otherwise by {@link java.net.URLConnection}.
     */
    protected boolean isHttpClient() {
        return myHttpClient;
    }

    protected HttpRequestBuilder request(String url, HttpMethod method) {
        return myFactory.newBuilder(url, method).useProxy(false);
    }
}
