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
package consulo.http;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.platform.Platform;

/**
 * @author VISTALL
 * @since 2026-02-13
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface HttpRequestBuilderFactory {
    /**
     * A request which is sent from the given platform - through its network, so {@code localhost} is the platform.
     *
     * @throws UnsupportedOperationException if requests can not be sent from the platform
     */
    HttpRequestBuilder newBuilder(Platform platform, String url, HttpMethod httpMethod);

    /**
     * A request which is sent from the {@link Platform#current() current} platform.
     */
    default HttpRequestBuilder newBuilder(String url, HttpMethod httpMethod) {
        return newBuilder(Platform.current(), url, httpMethod);
    }
}
