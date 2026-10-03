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
package consulo.endpoint.impl.internal.client.generator;

import consulo.annotation.component.ExtensionImpl;
import consulo.endpoint.client.generator.AvailableClientSettings;
import consulo.endpoint.client.generator.ClientExample;
import consulo.endpoint.client.generator.ClientGenerator;
import consulo.endpoint.localize.EndpointLocalize;
import consulo.endpoint.oas.OasEndpointPath;
import consulo.endpoint.oas.OasHttpMethod;
import consulo.endpoint.oas.OasOperation;
import consulo.endpoint.oas.OpenApiSpecification;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

@ExtensionImpl(order = "last")
public final class CurlClientGenerator implements ClientGenerator {
    private static final String DEFAULT_BASE_URL = "http://localhost:8080";

    private final CurlClientSettings mySettings = new CurlClientSettings();

    @Override
    public LocalizeValue getTitle() {
        return EndpointLocalize.clientGeneratorCurlTitle();
    }

    @Override
    public AvailableClientSettings getAvailableClientSettings() {
        return mySettings;
    }

    @Override
    public @Nullable ClientExample generate(OpenApiSpecification openApiSpecification) {
        if (!openApiSpecification.getPaths().iterator().hasNext()) {
            return null;
        }

        boolean boilerplate = mySettings.getActualClientSettings().getBoilerplate();
        String baseUrl = boilerplate ? "$BASE_URL" : DEFAULT_BASE_URL;

        StringBuilder builder = new StringBuilder();
        if (boilerplate) {
            builder.append("#!/usr/bin/env bash\n");
            builder.append("set -euo pipefail\n\n");
            builder.append("BASE_URL=\"${BASE_URL:-").append(DEFAULT_BASE_URL).append("}\"\n\n");
        }

        boolean hasOperations = false;
        for (OasEndpointPath path : openApiSpecification.getPaths()) {
            for (OasOperation operation : path.getOperations()) {
                appendCurl(builder, operation, baseUrl + path.getAbsolutePath(), boilerplate);
                hasOperations = true;
            }
        }
        if (!hasOperations) {
            return null;
        }
        return ClientExample.fromFileExtension(builder.toString(), "sh");
    }

    private static void appendCurl(StringBuilder builder, OasOperation operation, String url, boolean boilerplate) {
        OasHttpMethod httpMethod = operation.getMethod();
        String method = httpMethod.getMethodName().toUpperCase(Locale.ROOT);
        builder.append("curl --globoff -X ").append(method).append(" \"").append(url).append('"');
        builder.append(" -H 'Accept: application/json'");
        if (httpMethod == OasHttpMethod.POST || httpMethod == OasHttpMethod.PUT || httpMethod == OasHttpMethod.PATCH) {
            builder.append(" -H 'Content-Type: application/json' -d '{}'");
        }
        if (boilerplate) {
            builder.append(" --fail --silent --show-error");
        }
        builder.append('\n');
    }
}
